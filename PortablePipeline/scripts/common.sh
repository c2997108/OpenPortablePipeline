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
    echo "User: $USER"
    echo "Current directory: $PWD"
    echo "Command line: $script $args"
    echo "------------------------------------------------------------"
    echo ""


    if [ "$N_SCRIPT" = 1 ]; then
#     if [ "$CON" = "$CON_DOCKER" ]; then
#      eval $CON $IM_CENTOS6 chmod -R a=rXw $workdir
#     fi
     echo $status > $workdir/fin_status
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
 DIR_IMG=~/img
fi
DIR_WORK="."


#CON_DOCKER='docker run -v $PWD:$PWD -w $PWD -u root -i --rm '
CON_DOCKER='PPDOCNAME=pp`date +%Y%m%d_%H%M%S_%3N`_$RANDOM; echo $PPDOCNAME >> '"$workdir"'/pp-docker-list; docker run --name ${PPDOCNAME} -v $PWD:$PWD -w $PWD '"$PPDOCBINDS"' -u '`id -u`':'`id -g`' -i --rm '
#if [ "`echo $PWD|grep '^/home'|wc -l`" = 1 ]; then
# CON_SING="singularity exec $PPSINGBINDS "
#else
# CON_SING="singularity exec -B $PWD:/mnt --pwd /mnt $PPSINGBINDS "
#fi
CON_SING="singularity exec -B $PWD:$PWD --pwd $PWD $PPSINGBINDS "
CHECK_SING="singularity"

mkdir -p "$DIR_IMG"
DIR_IMG="`readlink -f "$DIR_IMG" || echo $DIR_IMG`"
mkdir -p "$DIR_WORK"
DIR_WORK="`readlink -f "$DIR_WORK" || echo $DIR_WORK`"
DIR_CUR="$PWD"
if [ `echo "$DIR_IMG"|grep " "|wc -l` = 1 -o `echo "$DIR_WORK"|grep " "|wc -l` = 1 -o `echo "$DIR_CUR"|grep " "|wc -l` = 1 ]; then
 echo Current, Work and Image directory should not contain space character in absolute path;
 echo 1 > $workdir/fin_status;
 exit 1;
fi
DIR_SRC="$(dirname "`readlink -f "$0" || echo "$0"`")"

cd "$DIR_WORK";
SCRIPT0=""
SCRIPT1=$(echo `eval "cat $0 $SCRIPT0"|sed 's/[ \t'"'"']/\n/g'|grep '^"$scriptdir"/'|sort|uniq`)
while [ "$SCRIPT0" != "$SCRIPT1" ]; do
 SCRIPT0="$SCRIPT1"
 echo "script: $0 $SCRIPT0"
 SCRIPT1=$(echo `eval "cat $0 $SCRIPT0"|sed 's/[ \t'"'"']/\n/g'|grep '^"$scriptdir"/'|sort|uniq`)
done

IMS=$((eval "cat $0 $SCRIPT0"|grep "^export IM_"|cut -f 2 -d =|sed 's/"//g';
       set |grep ^IM_|cut -f 2 -d =)|sort|uniq)
echo $IMS

if [ `docker images 2> /dev/null |head -n 1|grep "^REPO"|wc -l` = 1 ]; then
 echo using docker;
 CON="$CON_DOCKER";

 TEMPDOCIMG=`docker images|awk '{print $1":"$2}'|tail -n+2`
 for i in $IMS; do
  if [ `echo "$TEMPDOCIMG"|grep "^$i$"|wc -l` = 0 ]; then
   set -ex
   docker pull $i
   set +ex
  fi
 done
elif [ `$CHECK_SING 2>&1|head -n 1|grep -i usage|wc -l` = 1 ]; then
 echo using singularity;
 CON="$CON_SING""$DIR_IMG/";

 for i in $IMS; do
  if [ ! -e "$DIR_IMG"/$i ]; then
   set -ex
   singularity pull --name "`basename $i`" docker://$i;
   mkdir -p "$DIR_IMG/`dirname $i`";
   mv "`basename $i`" "$DIR_IMG/`dirname $i`";
   set +ex
  fi
 done
 cd "$DIR_CUR";
else
 echo docker or singularity should be installed;
 echo 1 > $workdir/fin_status;
 exit 1;
fi

for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do export ENV_$i="$CON"$(eval "echo \$IM_`echo $i`")" "; done
shopt -s expand_aliases
for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do alias DO_$i="$CON"$(eval "echo \$IM_`echo $i`")" "; done
#for i in `set |grep ^IM_|cut -f 1 -d =|sed 's/^IM_//'`; do
# BASH_ALIASES[$i]=`alias|grep "^alias DO_$i="|sed "s/^alias DO_$i=//; s/^'//; s/'$//"`;
#done #for bash ver.3

}

parallel_setup(){
 #RUNPARALLEL=`echo -e '#!/bin/sh\n#$ -S /bin/bash\n#$ -cwd\n#$ -pe def_slot N_CPU\n#$ -l mem_req=N_MEM_GG,s_vmem=N_MEM_GG\nsource ~/.bashrc\necho "$*"\neval "$*"'`
 #RUNPARALLEL=`echo -e '#!/bin/sh\n#$ -S /bin/bash\n#$ -cwd\n#$ -pe def_slot N_CPU\nsource ~/.bashrc\necho "$*"\neval "$*"'`
 #if [ -v RUNPARALLEL ]; then
 if [ "${RUNPARALLEL:-}" != "" ]; then
  echo "$RUNPARALLEL"|sed 's/N_CPU/'$N_CPU'/g'|sed 's/N_MEM_G/'`awk -v a=$N_MEM -v b=$N_CPU 'BEGIN{print a/1024/1024/b}'`'/g' > $workdir/qsub.sh
  echo "$RUNPARALLEL"|sed 's/N_CPU/1/g'|sed 's/N_MEM_G/'`awk -v a=$N_MEM -v b=$N_CPU 'BEGIN{print a/1024/1024/b}'`'/g' > $workdir/qsubone.sh
  cp $workdir/qsub.sh "$workdir"/wrapper.sh
  chmod 755 $workdir/qsub.sh $workdir/qsubone.sh
  if [ "$N_SCRIPT" = 1 ]; then
   if [ -e "$workdir/fin" ]; then rm "$workdir/fin"; fi
   if [ -e "$workdir/qsub.log" ]; then rm "$workdir/qsub.log"; fi
   if [ -e "$workdir/qsub.log2" ]; then rm "$workdir/qsub.log2"; fi
  fi
  alias DOPARALLELONE='xargs -I {} bash -c "qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsubone.sh \"{}\""|grep submitted >> $workdir/qsub.log; qsub -hold_jid `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` $workdir/qsubone.sh touch $workdir/fin|grep submitted >> $workdir/qsub.log'
  alias WAITPARALLELONE='set +x; while : ; do if [ -e $workdir/fin ]; then rm -f $workdir/fin; break; fi; sleep 1; done; for i in $(awk "{print \$3}" $workdir/qsub.log); do qacct -j $i|egrep "^(failed|exit_status)"|tail -n 2|awk "\$2!=0{a++} END{if(a>0){print $i\" was failed\"}}"; done > qsub.log2; rm -f $workdir/qsub.log; if [ "`cat qsub.log2`" != "" ]; then cat qsub.log2; echo 1 > $workdir/fin_status; exit 1; fi; set -x'
  alias DOPARALLEL='xargs -I {} bash -c "qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsub.sh \"{}\""|grep submitted >> $workdir/qsub.log; qsub -hold_jid `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` $workdir/qsub.sh touch $workdir/fin|grep submitted >> $workdir/qsub.log'
  alias WAITPARALLEL='set +x; while : ; do if [ -e $workdir/fin ]; then rm -f $workdir/fin; break; fi; sleep 1; done; for i in $(awk "{print \$3}" $workdir/qsub.log); do qacct -j $i|egrep "^(failed|exit_status)"|tail -n 2|awk "\$2!=0{a++} END{if(a>0){print $i\" was failed\"}}"; done > qsub.log2; rm -f $workdir/qsub.log; if [ "`cat qsub.log2`" != "" ]; then cat qsub.log2; echo 1 > $workdir/fin_status; exit 1; fi; set -x'
 elif [ "`head -n 2 $workdir/wrapper.sh 2> /dev/null|tail -n 1|awk '{print substr($0,1,5)}'`" = "#$ -S" ];then
  grep "^#" "$workdir"/wrapper.sh > $workdir/qsub.sh
  grep "^#" "$workdir"/wrapper.sh|grep -v def_slot > $workdir/qsubone.sh
  echo "#$ -pe def_slot 1" >> $workdir/qsubone.sh
  echo 'source ~/.bashrc; echo "$*"; eval "$*"' >> $workdir/qsub.sh
  echo 'source ~/.bashrc; echo "$*"; eval "$*"' >> $workdir/qsubone.sh
  chmod 755 $workdir/qsub.sh $workdir/qsubone.sh
  if [ "$N_SCRIPT" = 1 ]; then
   if [ -e "$workdir/fin" ]; then rm "$workdir/fin"; fi
   if [ -e "$workdir/qsub.log" ]; then rm "$workdir/qsub.log"; fi
   if [ -e "$workdir/qsub.log2" ]; then rm "$workdir/qsub.log2"; fi
  fi
  alias DOPARALLELONE='xargs -I {} bash -c "qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsubone.sh \"{}\""|grep submitted >> $workdir/qsub.log; qsub -hold_jid `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` $workdir/qsubone.sh touch $workdir/fin|grep submitted >> $workdir/qsub.log'
  alias WAITPARALLELONE='set +x; while : ; do if [ -e $workdir/fin ]; then rm -f $workdir/fin; break; fi; sleep 1; done; for i in $(awk "{print \$3}" $workdir/qsub.log); do qacct -j $i|egrep "^(failed|exit_status)"|tail -n 2|awk "\$2!=0{a++} END{if(a>0){print $i\" was failed\"}}"; done > qsub.log2; rm -f $workdir/qsub.log; if [ "`cat qsub.log2`" != "" ]; then cat qsub.log2; echo 1 > $workdir/fin_status; exit 1; fi; set -x'
  alias DOPARALLEL='xargs -I {} bash -c "qsub -N `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` -j y $workdir/qsub.sh \"{}\""|grep submitted >> $workdir/qsub.log; qsub -hold_jid `echo $workdir|sed s/^[^a-zA-Z]/_/|sed s/[^a-zA-Z0-9]/_/g` $workdir/qsub.sh touch $workdir/fin|grep submitted >> $workdir/qsub.log'
  alias WAITPARALLEL='set +x; while : ; do if [ -e $workdir/fin ]; then rm -f $workdir/fin; break; fi; sleep 1; done; for i in $(awk "{print \$3}" $workdir/qsub.log); do qacct -j $i|egrep "^(failed|exit_status)"|tail -n 2|awk "\$2!=0{a++} END{if(a>0){print $i\" was failed\"}}"; done > qsub.log2; rm -f $workdir/qsub.log; if [ "`cat qsub.log2`" != "" ]; then cat qsub.log2; echo 1 > $workdir/fin_status; exit 1; fi; set -x'
 else
  alias DOPARALLELONE='xargs -I {} -P $N_CPU bash -c "{}"'
  alias WAITPARALLELONE=''
  alias DOPARALLEL='xargs -I {} -P 1 bash -c "{}"'
  alias WAITPARALLEL=''
 fi
}

usage_exit()
{
 echo "$explanation"
 echo "$runcmd"
 echo "$inputdef"
 echo "$optiondef"
 exit 0
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
 export $i="$(eval echo \${$i:-$k})";
# export $i="`eval echo \\\$$i`";
done

#input file realpath check
echo "Checking the realpath of input files."
depth_max=10
function find_link_path_recursive () {
 # 明示的にローカル変数を使う
 local _count=${1}
 # 深さが最大となったら終了
 # _countは0始まりなので、depth_maxから1を引いている
 if [ ${_count} -ge $((${depth_max}-1)) ]; then
  echo "[${_count}] FINISH (Depth=${depth_max})"
 # 最大でなければ再帰呼び出し
 else
  local PPINDIRTMP="$2"
  echo ${_count} "$PPINDIRTMP"
  if [ -d "$PPINDIRTMP" ]; then #-dは最後の/の有無によらない
   PPINDIRTMP=`realpath -s "$PPINDIRTMP"` #最後の/を削る目的
   if [ -L "$PPINDIRTMP" ]; then #-Lは最後の/の有無で動作が変わる /がなければリンクと判定される
    #echo find_link_path1 $((${_count}+1)) "$PPINDIRTMP"
    find_link_path $((${_count}+1)) "$PPINDIRTMP"
   fi
   for j in `find "$PPINDIRTMP"/|tail -n+2`; do
    #echo ${_count} "$j"
    find_link_path_recursive $((${_count}+1)) "$j"
   done
  else
   if [ -L "$PPINDIRTMP" ]; then
    #echo find_link_path2 $((${_count}+1)) "$PPINDIRTMP"
    find_link_path $((${_count}+1)) "$PPINDIRTMP"
   fi
  fi
 fi
}
function find_link_path () {
 local _count=${1}
 if [ ${_count} -ge $((${depth_max}-1)) ]; then
  echo "[${_count}] FINISH (Depth=${depth_max})"
 else
  local _file=`readlink "${2}"`
  if [ "$_file" != "" ]; then
   local _is_abs=`echo "$_file"|awk '{if(substr($0,1,1)=="/"){print 1}else{print 0}}'`
   if [ "$_is_abs" = 0 ]; then
    _file=`dirname "$2"`/"$_file"
   fi
   local _dir=`dirname "$_file"`
   PPINDIRS+=(`realpath "$_dir"`)
   #echo "$_file"
   _file=`realpath -s "$_file"` #if文の判定式の中で``にするだけだとスペースがきちんと展開されない
   #echo "$_file"
   if [ -L "$_file" ]; then
    #echo "in find_link_path: " $_count "$_file"
    find_link_path_recursive $_count "$_file"
   fi
  fi
 fi
}

PPINDIRS=()
for i in `set|grep "^input_[0-9]\+="`; do
 PPINDIRTMP=`echo "$i"|sed 's/^input_[0-9]\+=//'`;
 find_link_path_recursive 0 "$PPINDIRTMP"
done
IFS=$'\n' PPINDIRS=(`printf "%s\n" "${PPINDIRS[@]}" |sort -u`);
unset IFS
PPDOCBINDS=""
PPSINGBINDS=""
for i in "${PPINDIRS[@]}"; do
 echo "$i";
 PPDOCBINDS="$PPDOCBINDS -v $i:$i"
 PPSINGBINDS="$PPSINGBINDS -B $i:$i"
done



N_CPU=`cat /proc/cpuinfo 2> /dev/null |grep ^processor|wc -l` #all CPU
if [ "$N_CPU" = "0" ]; then
 N_CPU=`getconf _NPROCESSORS_ONLN`
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

workdir=$PWD
scriptdir=$(dirname `readlink -f "$0" || echo "$0"`)
export IM_CENTOS6=centos:centos6
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
#  if [ "$CON" = "$CON_DOCKER" ]; then
#   DO_CENTOS6 chmod -R a=rXw .
#  fi
  echo 0 > $workdir/fin_status
 fi
 exit
}
