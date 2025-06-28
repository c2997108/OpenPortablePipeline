#!/bin/bash


onerror()
{
    status=$?
    script=$0
    line=$1
    shift

    set +x

    args=
    for i in "$@"; do
        args+="\"$i\" "
    done

    echo ""
    echo "------------------------------------------------------------"
    echo "Error occured on $script [Line $line]: Status $status"
    echo ""
    echo "PID: $$"
    echo "User: ${USER:-}"
    echo "Current directory: $PWD"
    echo "Command line: $script $args"
    echo "------------------------------------------------------------"
    echo ""

    if [ "$N_SCRIPT" = 1 ]; then
     rm -f "$workdir"/pp-singularity-flag
     if [ "${PP_USE_PARALLEL:-}" = "y" ]; then rm -f "$workdir"/wrapper.sh; fi
     echo $status > "$workdir"/fin_status
    fi

}

begintrap()
{
    set -e
    trap 'onerror $LINENO "$@"' ERR
}


container_setup()
{
if [ "$DIR_IMG" != "" ]; then
 DIR_IMG=`readlink -f $DIR_IMG || echo $DIR_IMG`
else
 DIR_IMG="$HOME"/img
fi
DIR_WORK="."

if [ "$N_SCRIPT" = 1 ]; then
 rm -f "$workdir"/pp-docker-list
 rm -f "$workdir"/pp-podman-list
 rm -f "$workdir"/qsub.log
fi

#Macの場合はdocker desktopのみを想定し、起動しておく
if [ `uname -s` = "Darwin" ]; then docker desktop start || (echo Please install Docker Desktop; exit 1); fi

#本当はPWDをrealpathに変換してから-v -wをしたほうがよいかも
#CON_DOCKER='docker run -v $PWD:$PWD -w $PWD -u root -i --rm '
#CON_DOCKERはENV_XXXのほうで使用され、FUNC_RUN_DOCKERのほうはDO_XXXのほうで使用される
CON_DOCKER='PPDOCNAME=pp`date +%Y%m%d_%H%M%S_%3N`_$RANDOM; echo $PPDOCNAME >> '"$workdir"'/pp-docker-list; docker run --name ${PPDOCNAME} -v $PWD:$PWD -w $PWD '"$PPDOCBINDS"' -u '`id -u`':'`id -g`' -i --rm '
function FUNC_RUN_DOCKER () {
 PP_RUN_IMAGE="$1"
 shift
 PP_RUN_DOCKER_CMD=("${@}")
 PPDOCNAME=pp`date +%Y%m%d_%H%M%S_%3N`_$RANDOM
 echo $PPDOCNAME >> "$workdir"/pp-docker-list
 docker run --name ${PPDOCNAME} -v $PWD:$PWD -w $PWD $PPDOCBINDS -u `id -u`:`id -g` -i --rm "$PP_RUN_IMAGE" "${PP_RUN_DOCKER_CMD[@]}"
}
function FUNC_RUN_DOCKER_GPU () {
 PP_RUN_IMAGE="$1"
 shift
 PP_RUN_DOCKER_CMD=("${@}")
 PPDOCNAME=pp`date +%Y%m%d_%H%M%S_%3N`_$RANDOM
 echo $PPDOCNAME >> "$workdir"/pp-docker-list
 docker run --name ${PPDOCNAME} -v $PWD:$PWD -w $PWD $PPDOCBINDS -u `id -u`:`id -g` -i --rm --gpus all "$PP_RUN_IMAGE" "${PP_RUN_DOCKER_CMD[@]}"
}
CON_PODMAN='PPDOCNAME=pp`date +%Y%m%d_%H%M%S_%3N`_$RANDOM; echo $PPDOCNAME >> '"$workdir"'/pp-podman-list; podman run --name ${PPDOCNAME} -v $PWD:$PWD -w $PWD '"$PPDOCBINDS"' -u root -i --rm '
function FUNC_RUN_PODMAN () {
 PP_RUN_IMAGE="$1"
 shift
 PP_RUN_DOCKER_CMD=("${@}")
 PPDOCNAME=pp`date +%Y%m%d_%H%M%S_%3N`_$RANDOM
 echo $PPDOCNAME >> "$workdir"/pp-podman-list
 #PODMANのほうは、dockerhubからのイメージにdocker.io/とつける必要がある
 PP_RUN_IMAGE=`echo "$PP_RUN_IMAGE"|awk -F'/' '{if(NF==2){$0="docker.io/"$0}; print $0}'`
 podman run --name ${PPDOCNAME} -v $PWD:$PWD -w $PWD $PPDOCBINDS -u root -i --rm "$PP_RUN_IMAGE" "${PP_RUN_DOCKER_CMD[@]}"
}
CON_SING='singularity exec --no-home -B $PWD:$PWD --pwd $PWD '"$PPSINGBINDS "
CON_SING_GPU='singularity exec --no-home -B $PWD:$PWD --pwd $PWD '"$PPSINGBINDS --nv "
CHECK_SING="singularity"
CON_APPT='apptainer exec --no-home -B $PWD:$PWD --pwd $PWD '"$PPSINGBINDS "
CON_APPT_GPU='apptainer exec --no-home -B $PWD:$PWD --pwd $PWD '"$PPSINGBINDS --nv "
CHECK_APPT="apptainer"

mkdir -p "$DIR_IMG"
DIR_IMG="`readlink -f "$DIR_IMG" || echo $DIR_IMG`"
mkdir -p "$DIR_WORK"
DIR_WORK="`readlink -f "$DIR_WORK" || echo $DIR_WORK`"
DIR_CUR="$PWD"
if [ `echo "$DIR_IMG"|grep " "|wc -l` = 1 -o `echo "$DIR_WORK"|grep " "|wc -l` = 1 -o `echo "$DIR_CUR"|grep " "|wc -l` = 1 ]; then
 echo Current, Work and Image directory should not contain space character in absolute path;
 echo 1 > "$workdir"/fin_status;
 exit 1;
fi
DIR_SRC="$(dirname "`readlink -f "$0" || echo "$0"`")"

cd "$DIR_WORK";
#SCRIPT0=""
#SCRIPT1=$(echo `eval "cat $0 $SCRIPT0"|sed 's/[ \t'"'"']/\n/g'|grep '^"$scriptdir"/'|sort|uniq`)
#while [ "$SCRIPT0" != "$SCRIPT1" ]; do
# SCRIPT0="$SCRIPT1"
# echo "script: $0 $SCRIPT0"
# SCRIPT1=$(echo `eval "cat $0 $SCRIPT0"|sed 's/[ \t'"'"']/\n/g'|grep '^"$scriptdir"/'|sort|uniq`)
#done

# 元のスクリプトファイルから使用されている子スクリプトをすべて列挙する
PP_SCRIPTS=( )
PP_SCRIPT_SEARCH(){
 local SCRIPT1="$scriptdir"/"$1"
 echo "script: $SCRIPT1"
 while IFS= read -r line; do
    # IFS= は入力フィールドセパレータを空に設定し、先頭や末尾の空白をそのまま保持します。read -r はバックスラッシュをエスケープ文字として扱わないようにします。
    # 配列内に行がすでに存在するか配列を展開し、正規表現で確認
    if ! [[ " ${PP_SCRIPTS[*]} " =~ " $line " ]]; then
        PP_SCRIPTS+=("$line")
        PP_SCRIPT_SEARCH "$line" #再帰呼び出し
    fi
 done < <( (cat "$SCRIPT1"|sed 's/[ \t'"'"']/\n/g'|grep '^"$scriptdir"/'|sed 's%^"$scriptdir"/%%';
            cat "$SCRIPT1"|grep PP_DO_CHILD|sed 's/.*PP_DO_CHILD//; s/^ *//'|cut -f 1 -d ' ';
            cat "$SCRIPT1"|grep PP_ENV_CHILD|sed 's/.*PP_ENV_CHILD//; s/^ *//'|cut -f 1 -d ' ' )|sort|uniq )
}
PP_SCRIPT_SEARCH `basename "$0"` #$0には元のスクリプトファイルの名前が入っている

IMS=$((for line in "${PP_SCRIPTS[@]}"; do
    cat "$line"
 done | grep "^export IM_"|cut -f 2 -d =|sed 's/"//g';
 set |grep ^IM_|cut -f 2 -d =)|sort|uniq)
echo "Containers: "$IMS

#IMS=$((eval "cat $0 $SCRIPT0"|grep "^export IM_"|cut -f 2 -d =|sed 's/"//g';
#       set |grep ^IM_|cut -f 2 -d =)|sort|uniq)
#echo $IMS

if [ "$PP_USE_SING" = "y" ]; then touch "$workdir"/pp-singularity-flag; else rm -f "$workdir"/pp-singularity-flag; fi
if [ ! -e "$workdir"/pp-singularity-flag -a `docker images 2> /dev/null |head -n 1|grep "^REPO"|wc -l` = 1 -a "$PP_USE_PODMAN" != "y" ]; then
 echo using docker;
 CON="$CON_DOCKER";

 TEMPDOCIMG=`docker images|awk '{print $1":"$2}'|tail -n+2`
 for i in $IMS; do
  if [ `echo "$TEMPDOCIMG"|grep "^$i$"|wc -l` = 0 ]; then
   set -ex
   docker pull $i #docker pull --platform=linux/amd64 $i
   set +ex
  fi
 done
elif [ ! -e "$workdir"/pp-singularity-flag -a `podman images 2> /dev/null |head -n 1|grep "^REPO"|wc -l` = 1 ]; then
 echo using podman;
 CON="$CON_PODMAN";

 TEMPDOCIMG=`podman images|awk '{print $1":"$2}'|tail -n+2`
 for i in $IMS; do
  #PODMANのほうは、dockerhubからのイメージにdocker.io/とつける必要がある。centos:7などの省略系のイメージは完全には名前が一致しないので再度pullしてしまう
  i=`echo "$i"|awk -F'/' '{if(NF==2){$0="docker.io/"$0}; print $0}'`
  if [ `echo "$TEMPDOCIMG"|grep "^$i$"|wc -l` = 0 ]; then
   set -ex
   podman pull $i #podman pull --platform=linux/amd64 $i
   set +ex
  fi
 done
else
 echo Checking singularity
 set +ex
 #singularityが動くかチェックし、動かなければまずはシステムに登録されているSylabsCloudを使おうとし、それに失敗したら新しくSylabsCloudを登録して使い、再度チェック
 if [ ! -e "$DIR_IMG"/hello-world ]; then
  singularity pull --name hello-world docker://hello-world || wget -O hello-world http://suikou.fs.a.u-tokyo.ac.jp/pp/img/hello-world
  mkdir -p "$DIR_IMG"
  mv hello-world "$DIR_IMG"
 fi
 $CHECK_SING run "$DIR_IMG"/hello-world > /dev/null
# singularityのlibraryは頻繁にアクセスできないことがあるようで利用可能かどうかのチェックに失敗するので、dockerのhello-worldを使うことにした
# ( $CHECK_SING run library://godlovedc/funny/lolcow ||
#   (($CHECK_SING remote use SylabsCloud || ($CHECK_SING remote add --no-login SylabsCloud cloud.sycloud.io && $CHECK_SING remote use SylabsCloud)) && $CHECK_SING run library://godlovedc/funny/lolcow) )
 if [ $? = 0 ];then
  echo using singularity;
  CON="$CON_SING""$DIR_IMG/";
  CON_GPU="$CON_SING_GPU""$DIR_IMG/";
  for i in $IMS; do
   if [ ! -e "$DIR_IMG"/$i ]; then
    set -ex
    singularity pull --name "`basename $i`" docker://$i || wget -O "`basename $i`" http://suikou.fs.a.u-tokyo.ac.jp/pp/img/"$i";
    mkdir -p "$DIR_IMG/`dirname $i`";
    mv "`basename $i`" "$DIR_IMG/`dirname $i`";
    set +ex
   fi
  done
  cd "$DIR_CUR"; #不要な気がする

 else
  echo Checking apptainer
  set +ex
  #apptainerの場合、事前に下記が必要
  #apptainer remote add --no-login SylabsCloud cloud.sycloud.io
  #apptainer remote use SylabsCloud
  #apptainer remote list
  ( $CHECK_APPT run library://godlovedc/funny/lolcow ||
   (($CHECK_APPT remote use SylabsCloud || ($CHECK_APPT remote add --no-login SylabsCloud cloud.sycloud.io && $CHECK_APPT remote use SylabsCloud)) && $CHECK_APPT run library://godlovedc/funny/lolcow) )
  if [ $? = 0 ];then
   echo using apptainer;
   CON="$CON_APPT""$DIR_IMG/";
   CON_GPU="$CON_APPT_GPU""$DIR_IMG/";
   for i in $IMS; do
    if [ ! -e "$DIR_IMG"/$i ]; then
     set -ex
     apptainer pull --name "`basename $i`" docker://$i || wget -O "`basename $i`" http://suikou.fs.a.u-tokyo.ac.jp/pp/img/"$i";
     mkdir -p "$DIR_IMG/`dirname $i`";
     mv "`basename $i`" "$DIR_IMG/`dirname $i`";
     set +ex
    fi
   done
   cd "$DIR_CUR"; #不要な気がする

  elif [ `$CHECK_SING 2>&1|head -n 1|grep -i usage|wc -l` = 1 ]; then
   #Terraのpp in docker用 c2997108/ubuntu:20.04-singularity_pp4
   #chroot時に/proc, /devなどがないことによるエラーが多発 javaではjava: error while loading shared libraries: libjli.so: cannot open shared object file: No such file or directoryなど
   #unshare --user --map-root-user --mount-proc --pid --fork /sbin/chroot / echo Checking chroot => こっちを使うくらいならsingularityで./mconfig --prefix=/path --without-suidでビルドしたのを使ったほうが良さそう
   chroot / echo Checking chroot
   if [ $? = 0 ];then
    #echo using unshare_chroot;
    echo using chroot_singularity;
    cat << 'EOF' > run-chroot.sh
#!/bin/bash
     DIR_IMG="$1"
     shift
     imagename="$1"
     shift
     ppcmd=""
     while [ "$1" != "" ]; do
      ppcmd="$ppcmd '$1'"
      shift
     done
     set -x
     mkdir -p "$DIR_IMG/sandbox/$imagename"/`dirname "$PWD"`
     #--copy-linksにしておかないと、DIR_IMGとPWDのディスクが異なる場合に、symlinkがあるとなぜか？戻りのrsyncでrsync: failed to hard-link ... : Invalid cross-device link (18)となる
     #--link-destを使うと＞で保存したファイルが初期化されてしまう。本来は--link-dest=../$(realpath --relative-to="$DIR_IMG/sandbox/$imagename"/`dirname "$PWD"` `dirname "$PWD"`)を使いたいのだけど。
     rsync -a --copy-links --modify-window=-1 --update --exclude="$DIR_IMG" "$PWD" "$DIR_IMG/sandbox/$imagename"/`dirname "$PWD"`
     set -e
     trap 'echo Line: $LINENO "$@"; rsync -a --copy-links --modify-window=-1 --update "$DIR_IMG/sandbox/$imagename"/"$PWD" `dirname "$PWD"`' ERR
     #unshare --user --map-root-user --mount-proc --pid --fork /sbin/chroot "$DIR_IMG/sandbox/$imagename" /bin/bash -c "mkdir -p /proc /dev/pts; mount -t proc proc /proc; mount -t devpts devpts /dev/pts; cd \"$PWD\"; $ppcmd"
     chroot "$DIR_IMG/sandbox/$imagename" /bin/bash -c "export LD_LIBRARY_PATH=/usr/local/lib; cd \"$PWD\"; $ppcmd"
     #--link-destを使うと＞で保存したファイルが初期化されてしまう。本来は--link-dest=$(realpath --relative-to=`dirname "$PWD"` "$DIR_IMG/sandbox/$imagename"/`dirname "$PWD"`)を使いたいのだけど。
     set +e
     rsync -a --copy-links --modify-window=-1 --update "$DIR_IMG/sandbox/$imagename"/"$PWD" `dirname "$PWD"`
EOF

    CON="bash $PWD/run-chroot.sh $DIR_IMG/ ";
    for i in $IMS; do
     if [ ! -e "$DIR_IMG"/sandbox/$i ]; then
      set -ex
      singularity build --sandbox "`basename $i`" docker://$i;
      mkdir -p "$DIR_IMG/sandbox/`dirname $i`";
      mv "`basename $i`" "$DIR_IMG/sandbox/`dirname $i`";
      set +ex
     fi
    done
    cd "$DIR_CUR"; #不要な気がする

   else
    echo docker, singularity, apptainer or chroot should be available;
    echo 1 > "$workdir"/fin_status;
    exit 1;
   fi
  else
   echo docker, singularity or apptainer should be installed;
   echo 1 > "$workdir"/fin_status;
   exit 1;
  fi
 fi
fi


for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do export ENV_$i="$CON"$(eval "echo \$IM_`echo $i`")" "; done
shopt -s expand_aliases
#Docker使用時に$CONの中に;が入っていてパイプが途中で切れてしまうので、その対策
if [ "$CON" = "$CON_DOCKER" ]; then
 for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do alias DO_$i="FUNC_RUN_DOCKER "$(eval "echo \$IM_`echo $i`")" "; done
 for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do alias DOGPU_$i="FUNC_RUN_DOCKER_GPU "$(eval "echo \$IM_`echo $i`")" "; done
elif [ "$CON" = "$CON_PODMAN" ]; then
 for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do alias DO_$i="FUNC_RUN_PODMAN "$(eval "echo \$IM_`echo $i`")" "; done
 for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do alias DOGPU_$i="FUNC_RUN_PODMAN "$(eval "echo \$IM_`echo $i`")" "; done
else
 #singularity
 for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do alias DO_$i="$CON"$(eval "echo \$IM_`echo $i`")" "; done
 for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do alias DOGPU_$i="$CON_GPU"$(eval "echo \$IM_`echo $i`")" "; done
fi
#for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do
# BASH_ALIASES[$i]=`alias|grep "^alias DO_$i="|sed "s/^alias DO_$i=//; s/^'//; s/'$//"`;
#done #for bash ver.3

}

#SGE用のWAIT関数設定。もしSGEでなければ後ほどaliasで上書きする
WAITPARALLEL0(){
   set +x;
   while : ; do
    if [ -e $workdir/fin ]; then
     rm -f $workdir/fin; break;
    fi;
    sleep 1;
   done;
   lastjob=`awk '$0~"^Your job "{id=$3} END{print id}' $workdir/qsub.log`;
   mkdir -p pp_log_$lastjob;
   awk -v prefix=`echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` '$0~"^Your job "{print prefix".o"$3}' $workdir/qsub.log|
    while read i; do
     if [ ! -e $i -o "`tail -n 1 $i`" != "CMD_FIN_STATUS: 0" ]; then
      echo Failed: $i;
     fi;
     if [ -e $i ]; then
      mv $i pp_log_$lastjob;
     fi
    done > qsub.log2;
   mv $workdir/qsub.log $workdir/qsub.history
   set -x;
}
WAITPARALLEL(){
   WAITPARALLEL0
   if [ "`cat qsub.log2`" != "" ]; then
    #失敗したジョブの2回目のqsub実行
    cat qsub.log2|sed 's/^Failed: //'|while read i; do
     j=`awk -v jobid=$(echo $i|sed 's/.*o//') 'flag==1{flag=2; print $0} $0~"^Your job "&&$3==jobid{flag=1}' $workdir/qsub.history` #j=qsub.sh or qsubone.sh
     k="`awk -v jobid=$(echo $i|sed 's/.*o//') 'flag==2{flag=3; print $0} flag==1{flag=2} $0~\"^Your job \"&&$3==jobid{flag=1}' $workdir/qsub.history`" #k=cmd #「"」で囲まないと展開される
     qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/$j "$k"| grep submitted >> $workdir/qsub.log
     echo "$j" >> $workdir/qsub.log
     echo "$k" >> $workdir/qsub.log
    done
    qsub -hold_jid `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` $workdir/qsubone.sh touch "$workdir"/fin
    mv qsub.log2 qsub.failed.1.log

    WAITPARALLEL0
    if [ "`cat qsub.log2`" != "" ]; then
     #2回実行してもダメならエラー終了
     cat qsub.log2;
     mv qsub.log2 qsub.failed.2.log
     echo 1 > $workdir/fin_status;
     exit 1;
    fi
   fi;
}
parallel_setup(){
 #RUNPARALLEL=`echo -e '#!/bin/sh\n#$ -S /bin/bash\n#$ -cwd\n#$ -pe def_slot N_CPU\n#$ -l mem_req=N_MEM_GG,s_vmem=N_MEM_GG\nsource ~/.bashrc\necho "$*"\neval "$*"'`
 #RUNPARALLEL=`echo -e '#!/bin/sh\n#$ -S /bin/bash\n#$ -cwd\n#$ -pe def_slot N_CPU\nsource ~/.bashrc\necho "$*"\neval "$*"'`
 #if [ -v RUNPARALLEL ]; then
 if [ "${PP_USE_PARALLEL:-}" = "y" ]; then
  #-l s_vmemは指定しないほうがJAVAを使ったプログラムの余計なエラーは少なくなるけどスパコン用の練習で付けておく場合。pp -gでは使用されない
  #CMD: "$@"で配列として入ってくるコマンドは、qsubのときに「"」で囲まれた単一の文字列として入ってくるので、実質配列ではなくて文字列。「"」と「@」はエスケープされている。
  RUNPARALLEL=`echo -e '#!/bin/sh\n#$ -S /bin/bash\n#$ -cwd\n#$ -pe def_slot N_CPU\n#$ -l mem_req=N_MEM_GG,s_vmem=N_MEM_GG\necho NSLOTS: $NSLOTS SGE_JOB_SPOOL_DIR: $SGE_JOB_SPOOL_DIR\necho CMD: "$@"\nsource ~/.bashrc\neval "$@"\necho CMD_FIN_STATUS: $?'`
 elif [ "${PP_USE_PARALLEL_NO_SVMEM:-}" = "y" ]; then
  #pp -gで使用されるモード
  RUNPARALLEL=`echo -e '#!/bin/sh\n#$ -S /bin/bash\n#$ -cwd\n#$ -pe def_slot N_CPU\n#$ -l mem_req=N_MEM_GG\necho NSLOTS: $NSLOTS SGE_JOB_SPOOL_DIR: $SGE_JOB_SPOOL_DIR\necho CMD: "$@"\nsource ~/.bashrc\neval "$@"\necho CMD_FIN_STATUS: $?'`
 fi
 if [ "${RUNONSINGLENODE:-}" = "" ] && ( [ "${RUNPARALLEL:-}" != "" ] || [ "`head -n 2 $workdir/wrapper.sh 2> /dev/null|tail -n 1|awk '{print substr($0,1,5)}'`" = "#$ -S" ] || [ "$N_SCRIPT" != 1 -a -e "$workdir"/qsubone.sh ] ); then
  #ddbjではなく and (pp -gのオプションがついている場合 or GUIからssh SGE, shiroオプションで投げられた場合 or 子スクリプトでqsubone.shが作られている場合)
  if [ "${RUNPARALLEL:-}" != "" ] || [ "`head -n 2 $workdir/wrapper.sh 2> /dev/null|tail -n 1|awk '{print substr($0,1,5)}'`" = "#$ -S" ]; then
   #qsubone.shが出来ている子スクリプト以外の場合
   if [ "`head -n 2 $workdir/wrapper.sh 2> /dev/null|tail -n 1|awk '{print substr($0,1,5)}'`" = "#$ -S" ];then
    #GUIからssh SGE, ddbj, shiroオプションで投げられた場合
    grep "^#" "$workdir"/wrapper.sh > "$workdir"/qsub.sh
    grep "^#" "$workdir"/wrapper.sh|grep -v def_slot > "$workdir"/qsubone.sh
    echo "#$ -pe def_slot 1" >> "$workdir"/qsubone.sh
    echo -e 'echo NSLOTS: $NSLOTS SGE_JOB_SPOOL_DIR: $SGE_JOB_SPOOL_DIR\necho CMD: "$@"\nsource ~/.bashrc\neval "$@"\necho CMD_FIN_STATUS: $?' >> "$workdir"/qsub.sh
    echo -e 'echo NSLOTS: $NSLOTS SGE_JOB_SPOOL_DIR: $SGE_JOB_SPOOL_DIR\necho CMD: "$@"\nsource ~/.bashrc\neval "$@"\necho CMD_FIN_STATUS: $?' >> "$workdir"/qsubone.sh
   else
    #pp -gのオプションがついている場合
    echo "$RUNPARALLEL"|sed 's/N_CPU/'$N_CPU'/g'|sed 's/N_MEM_G/'`awk -v a=$N_MEM -v b=$N_CPU 'BEGIN{print a/1024/1024/b}'`'/g' > "$workdir"/qsub.sh
    echo "$RUNPARALLEL"|sed 's/N_CPU/1/g'|sed 's/N_MEM_G/'`awk -v a=$N_MEM -v b=$N_CPU 'BEGIN{print a/1024/1024/b}'`'/g' > "$workdir"/qsubone.sh
   fi
   chmod 755 "$workdir"/qsub.sh "$workdir"/qsubone.sh
  fi
  if [ "$N_SCRIPT" = 1 ]; then
   if [ -e "$workdir/fin" ]; then rm "$workdir/fin"; fi
   if [ -e "$workdir/qsub.log" ]; then rm "$workdir/qsub.log"; fi
   if [ -e "$workdir/qsub.log2" ]; then rm "$workdir/qsub.log2"; fi
  fi

  cat << 'EOF' > "$workdir"/run-pp-qsubone.sh
#!/bin/bash
qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsubone.sh "$@"
while [ $? != 0 ]; do
 sleep 1
 qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsubone.sh "$@"
done
EOF
  cat << 'EOF' > "$workdir"/run-pp-qsub.sh
#!/bin/bash
qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsub.sh "$@"
while [ $? != 0 ]; do
 sleep 1
 qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsub.sh "$@"
done
EOF
  export workdir

  alias DOPARALLELONE='sed "s/\"/\\\\\"/g; s/[$]/\\\\$/g"|xargs -d'"'"'\n'"'"' -I {} bash -c "bash $workdir/run-pp-qsubone.sh \"{}\" | grep submitted ; echo qsubone.sh; echo \"{}\" " >> $workdir/qsub.log; qsub -hold_jid `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` $workdir/qsubone.sh touch '"$workdir"'/fin'
  alias DOPARALLEL='sed "s/\"/\\\\\"/g; s/[$]/\\\\$/g"|xargs -d'"'"'\n'"'"' -I {} bash -c "bash $workdir/run-pp-qsub.sh \"{}\" | grep submitted ; echo qsub.sh; echo \"{}\" " >> $workdir/qsub.log; qsub -hold_jid `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` $workdir/qsubone.sh touch '"$workdir"'/fin'
  #alias WAITPARALLEL='set +x; while : ; do if [ -e $workdir/fin ]; then rm -f $workdir/fin; break; fi; sleep 1; done; lastjob=`awk "END{print \\$3}" $workdir/qsub.log`; mkdir -p pp_log_$lastjob; awk -v prefix=`echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` "{print prefix\".o\"\$3}" $workdir/qsub.log|while read i; do if [ "`tail -n 1 $i`" != "CMD_FIN_STATUS: 0" ]; then echo Failed: $i; fi; mv $i pp_log_$lastjob; done > qsub.log2; rm -f $workdir/qsub.log; if [ "`cat qsub.log2`" != "" ]; then cat qsub.log2; echo 1 > $workdir/fin_status; exit 1; fi; set -x;'
 else
  #ddbjもしくはpp -gがついていないかGUIでdirect, sshなどのSGEを使わない場合
  #xargs -d'\n'はtr '\n' '\0'|xargs -0と同じ。Macではxargs -d'\n'が使えないので。
  if [ `uname -s` = "Darwin" -a `which xargs` = "/usr/bin/xargs" ]; then
   #Mac標準のxargsの場合、文字列が長すぎるとエラーになるから-Sオプションで長くしておく
   alias DOPARALLELONE='tr '"'"'\n'"'"' '"'"'\0'"'"' | xargs -0 -S 100000000 -I {} -P $N_CPU bash -c "{}"'
   alias DOPARALLEL='tr '"'"'\n'"'"' '"'"'\0'"'"' | xargs -0 -S 100000000 -I {} -P 1 bash -c "{}"'
   alias WAITPARALLEL=''
  else
   #Mac標準以外
   alias DOPARALLELONE='tr '"'"'\n'"'"' '"'"'\0'"'"' | xargs -0 -I {} -P $N_CPU bash -c "{}"'
   alias DOPARALLEL='tr '"'"'\n'"'"' '"'"'\0'"'"' | xargs -0 -I {} -P 1 bash -c "{}"'
   alias WAITPARALLEL=''
  fi
 fi
}

usage_exit()
{
 echo "$explanation"
 echo "$runcmd"
 echo "$inputdef"
 echo "$optiondef"
 sleep 0.1
 exit 0
}

PP_DO_CHILD(){
 CMD_CHILD="$1"
 # "shift" コマンドで最初の引数を除外します。
 shift
 bash "$scriptdir"/"$CMD_CHILD" "$@"
}

PP_ENV_CHILD(){
 CMD_CHILD="$1"
 # "shift" コマンドで最初の引数を除外します。
 shift
 echo N_SCRIPT=$N_SCRIPT bash "$scriptdir"/"$CMD_CHILD" "$@"
}

for i in a b c d e f g h i j k l m n o p q r s t u v w x y z; do unset opt_$i; done

while getopts ":a:b:c:d:e:f:g:hi:j:k:l:m:n:o:p:q:r:s:t:u:v:w:x:y:z:" OPT
do
  case $OPT in
    a) OPT_FLAG_a=1;opt_a=$OPTARG ;;
    b) OPT_FLAG_b=1;opt_b=$OPTARG ;;
    c) OPT_FLAG_c=1;opt_c=$OPTARG ;;
    d) OPT_FLAG_d=1;opt_d=$OPTARG ;;
    e) OPT_FLAG_e=1;opt_e=$OPTARG ;;
    f) OPT_FLAG_f=1;opt_f=$OPTARG ;;
    g) OPT_FLAG_g=1;opt_g=$OPTARG ;;
    h) OPT_FLAG_h=1;;
    i) OPT_FLAG_i=1;opt_i=$OPTARG ;;
    j) OPT_FLAG_j=1;opt_j=$OPTARG ;;
    k) OPT_FLAG_k=1;opt_k=$OPTARG ;;
    l) OPT_FLAG_l=1;opt_l=$OPTARG ;;
    m) OPT_FLAG_m=1;opt_m=$OPTARG ;;
    n) OPT_FLAG_n=1;opt_n=$OPTARG ;;
    o) OPT_FLAG_o=1;opt_o=$OPTARG ;;
    p) OPT_FLAG_p=1;opt_p=$OPTARG ;;
    q) OPT_FLAG_q=1;opt_q=$OPTARG ;;
    r) OPT_FLAG_r=1;opt_r=$OPTARG ;;
    s) OPT_FLAG_s=1;opt_s=$OPTARG ;;
    t) OPT_FLAG_t=1;opt_t=$OPTARG ;;
    u) OPT_FLAG_u=1;opt_u=$OPTARG ;;
    v) OPT_FLAG_v=1;opt_v=$OPTARG ;;
    w) OPT_FLAG_w=1;opt_w=$OPTARG ;;
    x) OPT_FLAG_x=1;opt_x=$OPTARG ;;
    y) OPT_FLAG_y=1;opt_y=$OPTARG ;;
    z) OPT_FLAG_z=1;opt_z=$OPTARG ;;
    :) echo  "[ERROR] Option argument is undefined.";;
    \?) echo "[ERROR] Undefined options.";;
  esac
done
shift $(($OPTIND - 1))

req_args=`echo "$inputdef"|tail -n+2|awk '{if(NR>1){print old}; old=$0}'|awk -F':' '$2!~"option"{print $1}'|wc -l`
if [ "$OPT_FLAG_h" = 1 -o "$#" -lt "$req_args" ]; then
 usage_exit;
fi
for i in `echo "$runcmd"|awk '{for(i=1;i<=NF;i++){if($i~"^#input_"){if(old~"^-[a-zA-Z]$"){print substr($i,2,length($i)-2)"=\"${opt_"substr(old,2)":-}\""}}; old=$i}}'`; do
 eval export $i
done
for i in `echo "$runcmd"|awk '{for(i=1;i<=NF;i++){if($i~"^#input_"){if(old!~"^-[a-zA-Z]$"){print substr($i,2,length($i)-2)}}; old=$i}}'`; do
 export $i="${1:-}"; shift;
done
#for i in `echo "$inputdef"|tail -n+2|awk '{if(NR>1){print old}; old=$0}'|awk -F':' '$2!~"option"{print $1}'`; do export $i="$1"; shift; done
for i in `echo "$optiondef"|tail -n+2|awk '{if(NR>1){print old}; old=$0}'|awk -F':' '{print $1}'`; do
 k=`echo "$optiondef"|tail -n+2|awk '{if(NR>1){print old}; old=$0}'|awk -F':' '$1=="'$i'"{print $3}'|head -n 1`;
 export $i="$(eval echo \"\${$i:-$k}\")";
# export $i="`eval echo \\\$$i`";
done

#input file realpath check
echo "Checking the realpath of input files."

#realpath -sがmacでは使えないので、シンボリックリンクを展開しないで絶対パスに変換する関数
function pp_realpath(){
 local target="$1"
 local abs_target
 case $target in
  /*) abs_target="$target" ;;
  *)  abs_target="$PWD/$target" ;;
 esac
 local abs_dir=$(dirname -- "$abs_target")
 local parent_dir
 parent_dir=$(cd -- "$abs_dir" && pwd) || parent_dir="$abs_dir"
 printf '%s\n' "$parent_dir"/$(basename "$target")| sed 's/\/\+/\//g'
}
#-followオプションはリンク先情報を使って判定するが、今回はおそらく不要。ディレクトリ中のファイルが深すぎると結果がたくさんあるときの再実行に時間がかかるので2階層(input_*/*/*)までに限定して調べる
find . -follow -maxdepth 2 | while read i; do pp_realpath $PWD/$i; done|
 awk -F/ '{path=""; for(i=2;i<=NF;i++){path=path"/"$i; print path}}'|
 sort|uniq|while read i; do if [ -L $i ]; then
  PP_BINDS_TEMP=`readlink "$i"`
  if [[ $PP_BINDS_TEMP =~ ^/ ]]; then
   pp_realpath "$PP_BINDS_TEMP"
  else
   pp_realpath `dirname "$i"`"/$PP_BINDS_TEMP"
  fi
 fi; done|sort|uniq > pp_symlink_list

cp pp_symlink_list pp_symlink_list_temp

pp_n=0
function find_link_path_recursive () {
 echo $((++pp_n))
 cat pp_symlink_list_temp | awk -F/ '{path=""; if(substr($0,1,1)!="/"){ppath=$1; print path}; for(i=2;i<=NF;i++){path=path"/"$i; print path}}'|
  sort|uniq|while read i; do if [ -L $i ]; then
   PP_BINDS_PATH=`readlink "$i"`
   if [[ $PP_BINDS_PATH =~ ^/ ]]; then
    pp_realpath "$PP_BINDS_PATH"
   else
    pp_realpath `dirname "$i"`"/$PP_BINDS_PATH"
   fi
  fi; done|sort|uniq > pp_symlink_list_temp2

 #検索結果が0でなければ
 if [ -s pp_symlink_list_temp2 ];then
  cat pp_symlink_list_temp2 >> pp_symlink_list;
  mv pp_symlink_list_temp2 pp_symlink_list_temp;
  find_link_path_recursive;
 else
  rm -f pp_symlink_list_temp pp_symlink_list_temp2
 fi
}

find_link_path_recursive

cat pp_symlink_list|sort|uniq|while read i; do dirname $i; done|sort|uniq > pp_bind_dir
rm pp_symlink_list

PPDOCBINDS=`cat pp_bind_dir | awk '{ORS=" "; print " -v "$0":"$0}'`
PPSINGBINDS=`cat pp_bind_dir | awk '{ORS=" "; print " -B "$0":"$0}'`


#N_CPU=`cat /proc/cpuinfo 2> /dev/null |grep ^processor|wc -l` #all CPU
N_CPU=`nproc` || N_CPU=0
if [ "$N_CPU" = "0" ]; then
 N_CPU=`getconf _NPROCESSORS_ONLN` #for mac
fi
N_MEM=`free -k 2> /dev/null |awk 'NR==1{if($6=="available"){flag=1}} NR==2&&flag==1{print $7}'` #free MEM (kB)
if [ "$N_MEM" = "" ]; then
 N_MEM=`cat /proc/meminfo 2> /dev/null |grep ^MemAvailable:|awk '{print $2}'` #free MEM (kB)
fi
if [ "$N_MEM" = "" ]; then
 N_MEM=`cat /proc/meminfo 2> /dev/null |grep ^MemTotal:|awk '{print int($2*0.9)}'` #total MEM (kB)
fi
if [ "$N_MEM" = "" ]; then
 N_MEM=`sysctl hw.memsize |awk '{print int($2/1024*0.9)}'` #total MEM (kB) for Mac
fi
N_CPU2=$opt_c
N_MEM2=`echo "$opt_m * 1024 * 1024"|bc 2> /dev/null |awk '{print int($0)}'`
if [ "$N_CPU" = "0" -a "$N_CPU2" = "" ];then
 N_CPU=4
elif [ "$N_CPU" = "0" -a "$N_CPU2" != "" ];then
 N_CPU=$N_CPU2
elif [ "$N_CPU" != "0" -a "$N_CPU2" != "" ];then
 if [ "$N_CPU2" -lt "$N_CPU" ]; then N_CPU=$N_CPU2; fi
fi
if [ "$N_MEM" = "" -a "$N_MEM2" = "" ];then
 N_MEM=8000000 #8GB
elif [ "$N_MEM" = "" -a "$N_MEM2" != "" ];then
 N_MEM=$N_MEM2
elif [ "$N_MEM" != "" -a "$N_MEM2" != "" ];then
 if [ "$N_MEM2" -lt "$N_MEM" ]; then N_MEM=$N_MEM2; fi
fi
N_MEM_K=`awk -v a=$N_MEM -v b=0.8 'BEGIN {print int(a*b)}'`
N_MEM_B=`expr $N_MEM_K '*' 1024`
N_MEM_M=`expr $N_MEM_K / 1024`
N_MEM_G=`expr $N_MEM_M / 1024`
N_MEM_B1=`expr $N_MEM_B / $N_CPU`
N_MEM_K1=`expr $N_MEM_K / $N_CPU`
N_MEM_M1=`expr $N_MEM_M / $N_CPU`
N_MEM_G1=`expr $N_MEM_G / $N_CPU`

N_SCRIPT=`expr ${N_SCRIPT:-0} + 1`
export N_SCRIPT

workdir="$PWD"
scriptdir=$(dirname `readlink -f "$0" || echo "$0"`)
#export IM_CENTOS6=centos:centos6
begintrap
container_setup
parallel_setup

#for MacOS
if [ `uname|awk '{print tolower($0)}'` = "darwin" ]; then
 export PATH=$(dirname `find /usr/local/|grep /gnu|grep /readlink$|head -n 1`):$PATH
 export PATH=$(dirname `find /usr/local/|grep /gnu|grep /sed$|head -n 1`):$PATH
fi

post_processing(){
 if [ "$N_SCRIPT" = 1 ]; then
  rm -f "$workdir"/pp-singularity-flag
  if [ "${PP_USE_PARALLEL:-}" = "y" ]; then rm -f "$workdir"/wrapper.sh; fi
  echo 0 > "$workdir"/fin_status
 fi
 exit
}

set -eux
set -o pipefail
