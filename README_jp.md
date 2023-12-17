# PortablePipeline

PortablePipelineはWindows、Macユーザが、手元のコンピュータもしくはリモートのサーバ、スパコン上で、NGSの解析をGUIで行うことを可能にするソフトウェアです。グリッドエンジンとしてSGEに対応しており、白金スパコン、遺伝研スパコンにジョブを分散実行可能です。また、本ソフトウェアの仕様に従って記述されたスクリプトは、CUIからも容易に呼び出せます。
![pp](https://user-images.githubusercontent.com/5350508/69688721-35e88f00-110a-11ea-8260-520f7554935f.png)

## 必要なシステム要件
### クライアント (GUI操作を行う端末)
- Windows 10, 11 もしくは Mac OSX (Java 8が動けばOK)
- ストレージ：たくさん　解析にもよるが1TBくらいあると安心。

### サーバ (データ解析を行うコンピュータ)
下記のいずれかが必要
#### 自前のサーバ
メモリ64GBの環境でテストしている。データによってはそれよりも少なくても動くこともあるし、それより多く必要なこともある。
- DockerもしくはSingularityインストール済みでSSHサーバ設定済みのLinux (CentOS6, CentOS7, Ubuntu16, Ubuntu18) : [Linuxサーバセットアップ方法](#Linuxサーバのセットアップ方法)、もしくはWindows 10のWindows Subsystem for Linuxを利用したUbuntu : [WSLサーバセットアップ方法](#WSLサーバのセットアップ方法)、もしくはMac OSXでhomebrewのcoreutilsとDocker Desktop for MacをインストールしたMac : [Macサーバセットアップ方法](#Macをサーバとして使用する場合のセットアップ方法)。
- python3

#### スパコン (無料アカウントあり）
- 遺伝研DDBJスパコン https://sc.ddbj.nig.ac.jp/ja
- 東大ヒトゲノム解析センタースパコン https://supcom.hgc.jp/japanese/

## 操作方法
1. 最新のPortablePipelineのリリースをダウンロードして解凍する。 日本語のフォルダやスペースを含むフォルダの中に解凍しないでください。
  
  https://github.com/c2997108/OpenPortablePipeline/releases
  
  Win: PortablePipeline-win-vXXX.zip  
  Mac: PortablePipeline-mac-vXXX.tar.gz  

2. 解凍されたファイルの中で、Windowsならば「PortablePipeline.bat」を、Macであれば「PortablePipeline.command」をダブルクリックして起動する。
Windowsユーザは、ジャンクションファイルの作成に管理者権限が必要なので、管理者で実行しても良いか聞かれると思うのでOKを押す。Macユーザは初回起動時のみ、OSの「System Preferences」→「Security & Privacy」→「General」タブ→「Open Anyway」をクリックして、実行を許可する必要がある。

3. ソフトウェアが起動したら、「Settings」タブを開いて、Linuxサーバに接続するか(「direct」を選択)、スパコンに接続するか(「ddbj」、「shirokane」を選択)、Windows単体で解析まで実行するか(「WSL」を選択)、Mac単体で解析まで実行するか(「Mac」を選択)を決めて、サーバに接続する場合は、必要なアカウント情報を入力する。設定を変更したら必ず「Save」をクリックすること。
例としてDDBJスパコンを使用する際の設定例を示す。
![image](https://user-images.githubusercontent.com/5350508/79914247-e95a7300-845f-11ea-8dc9-57f97d761ccd.png)

4. 「Analysis Scripts」タブを選択し、解析したいスクリプトを選ぶと、入力ファイル、オプションを入力する画面が表示される。入力ファイルは、「input_1」などと書かれたボタンをクリックしてから選択し、オプションで変更する必要がある箇所は変更してから、画面下の「Run」ボタンを押すと実行される。
例としてHello Worldとサーバ上で表示させてみるだけのテストプログラムを実行してみる場合の手順を示す。
![image](https://user-images.githubusercontent.com/5350508/79914547-769dc780-8460-11ea-86da-dcfc864fa67a.png)

5. 「Run」ボタンが押された後、サーバにデータを転送するので、恐らくしばらく時間がかかる。進捗はJavaとは別にパワーシェルかターミナルが開いているはずで、そちらにデータの転送状況が表示される。「Job List」のステータスが「Running」に変わったら、本ソフトウェアをいったん終了しても大丈夫。立ち上げていれば、30秒おきにサーバに進捗を確認しに行く。
例として、ジョブを投げたときのサーバ上のログを確認する画面と
![image](https://user-images.githubusercontent.com/5350508/79914860-ec099800-8460-11ea-8630-7c8567f6a137.png)
クライアントとサーバのファイルのやり取りを示すパワーシェルのスクリーンショットはこんな感じ。
![image](https://user-images.githubusercontent.com/5350508/79914912-ffb4fe80-8460-11ea-9390-02ad5b745189.png)

6. Statusがfinishedになれば解析が終わっているので、「open results」をクリックして解析結果ファイル一覧を見てみる。
![image](https://user-images.githubusercontent.com/5350508/79915563-232c7900-8462-11ea-97f0-fcffd17e2877.png)
結果ファイル一覧の例は下記のような感じ。下記は単にサーバ上でHello Worldを表示させただけなので、log.txtファイルにその痕跡が残っているだけだけど、マッピングなどを行えばここにbamファイルなどが表示される。
![image](https://user-images.githubusercontent.com/5350508/79915687-64248d80-8462-11ea-884f-46dc0047c7c6.png)

## 操作方法（コマンドライン版 Linux上でのみ使用可能）
1. DockerとPython3をインストールしておく。Dockerはsudoなしで実行できるように`sudo usermod -aG docker $USER`を実行して再ログインしておく。

2. GitHubからソースコード一式をダウンロードする。

`git clone https://github.com/c2997108/OpenPortablePipeline.git`

3. 使用可能なパイプライン一覧を見る

`path/to/OpenPortablePipeline/PortablePipeline/scripts/pp`

```
QC~jellyfish						assemble~platanus					nanopore~flye-sickle_se
QC~multi-fastqc						assemble~platanus-allele				nanopore~minimap2
RNA-seq~DEGanalysis					basic-tools~merge_table					nanopore~pilon
RNA-seq~HISAT2-StringTie-DEGanalysis			convert~FASTA_to_FASTQ					nanopore~sickle_se
...
```

4. ちゃんと動くかテストする

`path/to/OpenPortablePipeline/PortablePipeline/scripts/pp ZZZ~hello-world`

```
PID: 922021
c2997108/centos7:1 centos:centos6
using docker
++ docker pull c2997108/centos7:1
1: Pulling from c2997108/centos7
8ba884070f61: Pull complete
07a5b0e61101: Pull complete
Digest: sha256:76a0f89ef3201ce10bfa5907bf884d128028352769a90a201ca017a914634c4e
Status: Downloaded newer image for c2997108/centos7:1
docker.io/c2997108/centos7:1
++ set +ex
++ docker pull centos:centos6
centos6: Pulling from library/centos
ff50d722b382: Pull complete
Digest: sha256:a93df2e96e07f56ea48f215425c6f1673ab922927894595bb5c0ee4c5a955133
Status: Downloaded newer image for centos:centos6
docker.io/library/centos:centos6
++ set +ex
+ set -o pipefail
+ echo 'Hello World!'
Hello World!
+ post_processing
+ '[' 1 = 1 ']'
+ echo 0
+ exit
```

5. 例えば10x CNVの精子シングルセルデータを使って連鎖解析を行いゲノムを伸長する場合

今いるフォルダの中にシーケンスした10x CNVのリードを全部入れたフォルダ(例：`input_fastq`)と、伸長前のゲノムファイル(例：`contig.fasta`)を準備しておく。また、ライセンスの関係上、`cellranger-dna-1.1.0.tar.gz`を10xのウェブサイトからダウンロードしておく。次のコマンドを実行すると、CellRangerの解析＋VarTrixによるVCF作成＋SELDLA用インプットファイル作成が行われる。

```
path/to/OpenPortablePipeline/PortablePipeline/scripts/pp linkage-analysis~single-cell_CellRanger-VarTrix input_fastq/ contig.fasta cellranger-dna-1.1.0.tar.gz
```

出来上がった`pseudochr.re.fa.removedup.matrix.clean.txt.vcf`と`pseudochr.re.fa.removedup.matrix.clean.txt_clean.txt`と`pseudochr.re.fa.removedup.matrix.clean.txt.vcf2.family`をSELDLAのインプットとして入力すれば良い。SELDLA単体で実行しても良いし、Portable Pipelineを経由して実行してもよい。Portable PipelineのSELDLAは1回目の実行でキメラの細胞を検出して、それを除去して2回目のSELDLAを実行するようにしている。不要な場合はSELDLAを単体で実行するほうが良い。

```
path/to/OpenPortablePipeline/PortablePipeline/scripts/pp linkage-analysis~SELDLA -b "--exmatch 0.60 --clmatch 0.92 --spmatch 0.90 -p 0.03 -b 0.03 --NonZeroSampleRate=0.05 --NonZeroPhaseRate=0.1 -r 20000 --RateOfNotNASNP=0.001 --RateOfNotNALD=0.01 --ldseqnum 2" -r 10 -p pseudochr.re.fa.removedup.matrix.clean.txt_clean.txt contig.fasta pseudochr.re.fa.removedup.matrix.clean.txt.vcf pseudochr.re.fa.removedup.matrix.clean.txt.vcf2.family
```


## JAVA開発者用メモ
GitHubに50 MBを超えるファイルを登録しているので、git cloneで全てのファイルをダウンロードするには、git lfsのインストールが必要。git lfsを[このリンク先のページ](https://github.com/git-lfs/git-lfs/wiki/Installation)の手順でインストールしたあと、```git clone https://github.com/c2997108/OpenPortablePipeline.git``` とすればよい。

## スクリプト開発者へ仕様というかメモ
### 共通の制限というか仕様
- オプションは、「opt_」で始める変数とし、optiondef変数の中に記述する。そして、runcmd変数の中で例えば、「-a #opt_a#」などと記述する。
  そうすると、「-a 90」など指定してスクリプトが実行されれば、自動でopt_aに90が代入されている。
  これは、共通の前処理ファイルのcommon.shで実現されていて、common.shの中で、opt_[a-z]には引数の中の「-a 'xxx'」などから勝手にopt_a='xxx'と代入する。
  なので、runcmdの中で「-a #opt_a#」とすると分かりやすい。「-a opt_b」とすると混乱のもと。
  また、オプションは今のところ小文字のa-zを単体で使用すること。
- optiondefの中には3つのフィールドがある。1つ目はオプションを入れる変数名。任意の名前で良いのだけど、opt_aなどの「opt_」＋小文字の
  名前にしておくと、自動でgetoptでopt_aに入れてくれたりするので、オプションの変数名は「opt_a」などを推奨。
  2つ目はオプションの簡単な説明。3つ目はオプションのデフォルト値。
  common.shを読み込むと、自動で3つ目のフィールドの値が入った1つ目のフィールドの変数が出来る。
  例として-aオプションの場合、optiondefに記述された3つ目のデフォルト値でopt_aが作られ、さらに-aの引数があれば、その値でopt_aが上書きされている。
- 入力ファイルはinput_1とかのように「input_」で始めること。
  必須ではないオプショナルなファイルは、オプションの後に指定しておけば(例：runcmdの中で「-f #input_3#」など)、
  引数に-fでファイルを与えた場合のみ変数input_3にファイル名が代入される。
- inputdefの中には「:」区切りで4つのフィールドがある。1つ目は入力ファイルを入れる変数名。
  2つ目は複数ファイルの場合なのか(directoryという文字が入っていれば良い)、必須ではないファイルなのか(optionという文字が入っていれば良い)
  を意味する。directory_optionみたいな書き方も有効。3つ目は入力ファイルの説明。
  4つ目は拡張子を制限するときの書式。
- opt_hはヘルプ表示のため使えない。opt_m, opt_cはそれぞれメモリ、CPU数の変数として確保されているため使えない。
- WAITPARALLELの後はset -xになり、実行コマンドが明示的に表示されるモードになる
- 他のスクリプトを呼び出すときは、bash "$scriptdir"/xxx で良い。
  子スクリプトの中でさらに孫スクリプトの呼び出しも可能。階層の制限はなし。
  子、孫スクリプトたちの中で使われるsingularityのイメージ準備なども予め行われるので、並列化による不具合は起きない(はず)。
  ただし、docker等を呼び出した中で子スクリプトを使うと、スクリプトの絶対パスにアクセスできない可能性があり危険
- 複数の出力ファイルがあって、それらを次のスクリプトの入力にしたいときは、出力ファイルをいったんtar.gzで固めておく方針が良さそう。
- 並列の処理は、処理したいコマンドをechoで複数書いて、パイプの後ろに、DOPARALLEL (1ジョブ当たり複数のCPU)、DOPARALLELONE（1ジョブ当たり1CPU）と書く。
  SGEを使わない場合は、xargsで並列処理、SGEを使う場合は、xargs+qsubでグリッドエンジンに渡す。
  並列処理の最終的なコマンドの引数に空白を渡すためなどの理由で「'」を使いたいときは、echo中では「\'」と書いておくと、xargsによる削除を抜けられる。
- headをパイプの途中で使うと、パイプの前の段が途中で強制終了となりpipefailが生じるので途中では使わないのと、grepはヒット件数が0だとエラーになるので、|| trueなどをつけて対策すること。
- 「|」(パイプ)の後ろで、<del>DO_なり</del>ENV_を使うときは、ver1以降では(DO_xxx  < /dev/stdin), "("$ENV_xxx < /dev/stdin")"などというように括弧で囲むこと。->v1.2.0でDO_は不要に。ENV_は未検証。
- dockerを使う場合は途中でキャンセル処理はSGEなしの場合のみ完全対応。SGEを使うとqsubで投げたjobは削除するが、投げた先のサーバの中のdockerは停止されないので、docker stopを実行しないといけないが現在は対応していない。singularityの場合はSGEでもSGEがプロセスを停止させてくれるはず。
- <del>xargsの仕様として、「'」や「"」はエスケープ処理しておかないと消える。```echo 'set -eux; echo \"a   a\"'|xargs -I{} bash -c "{}"```</del>->v1.2.0でxargs -0とすることで影響なくなった。
- DOPARALLELの中で子スクリプトを呼び出す場合、bashコマンドの前にN_SCRIPT=$N_SCRIPTを付けて、子スクリプトであることを伝える必要がある。

### コマンドライン実行時の注意
- オプション扱いの入力だけど、-X input_1/ などになっていない入力の場合、空のオプション""や、空のフォルダを作ってそのフォルダを指定しておかないとスクリプトの中で変な挙動になる可能性がある。例えば「metagenome~clustering_pfam-annotation」では空のinput_2もしくはinput_3フォルダが無いとfind $input_2/などとしているため、空だとルート「/」からの検索をしてしまう。

### Portable Pipeline (GUI)から投げられるジョブに関すること
- Inputがoptionalで何も指定していない場合、空の文字列「''」が渡される。オプションが空の場合も同じ。

### dockerに起因する部分
- <del>ファイルは$PWD以下しかマウントされないので、親ディレクトリのファイルを使わない</del>->一応入力ファイルの10階層までシンボリックリンクを辿るようにしたけど、ディレクトリへのシンボリックリンクの後のシンボリックリンクなどは上手く取れないケースもまだある。
- dockerは実行したユーザの権限で実行されるので、/rootフォルダなどにはアクセスできないので、/root以外を使うこと。例：/usr/localなど

### podmanに起因する部分
- `podman run -i --rm -v /tmp:/tmp -w /tmp docker.io/c2997108/centos7:2 awk '{print $0}' <(cat /tmp/a.fa)`などの入力方式は`awk: fatal: cannot open file '/dev/fd/63' for reading (No such file or directory)`となりエラー。`cat /tmp/a.fa | podman run -i --rm -v /tmp:/tmp -w /tmp docker.io/c2997108/centos7:2-blast-taxid-2-KronaTools-2.7-pr2-mito-silva-3 awk '{print $0}' /dev/stdin`はOK。

### singularityに起因する部分
- コンテナの/root以下にツールなどをインストールしても、singularityはコンテナの/rootを普通はマウントしないようでアクセスできない。

### Maser用
- 入力フォルダは「$input_1」ではなく、「$input_1/」と書くこと。input_1がシンボリックリンクだとそれ以上追ってくれないから。
- 入力フォルダはMaserでオプション扱いで空の場合、空のフォルダができてしまう。

### SGE用
- def_slotと、mem_req, s_vmemのオプションを設定出来るようにSGEサーバの設定をしておくこと。
- ジョブが途中で停止させられ、再度最初から実行されることが頻繁に有り得るので、
  その状態を想定してフォルダを作るときは事前に作られている可能性を考慮する。
- wrapper.shの中で2行目に#$ -S があればSGEモードと判定され、子スクリプトがSGE上で実行される
- DOPARALLEL, DOPARALLELONEに対して必ず1つずつWAITPARALLELを使用すること。
  そうしないと、時間差で$workdir/finが出来てしまい、次回以降のWAITPARALLELが機能しなくなる。

### スパコン用
- javaの引数には-Xmx1Gなどを必ず指定すること。指定が無いとマシンの半分くらいのメモリ？を確保しようとしてこける。
  javaの実行前に```unset JAVA_TOOL_OPTIONS```を指定しないとスパコンの設定で少ないメモリ量を指定されてしまう。
- ```if [ -v AAA ]; then echo a; fi```
  などとやるにはbashのバージョンが古くてエラーになるので、
  ```if [ "${AAA:-}" != "" ]; then echo a; fi```
  とすること。
- 確保するメモリ量を20GB以上としないと、実行時に「FATAL:   While making image from oci registry: while building SIF from layers: unable to create new build: while ensuring correct compression algorithm:」となってsingularityのイメージを作れなくて失敗する。

### Mac用
- dockerを/usr/local/binにインストールし、Homebrewでcoreutils, gnu-sedを/usr/local/xxx/gnubinにインストールする必要あり。
  coreutils, gnu-sedが無いと、readlink, sed等が意図したように動いてくれない。=>一部対策済み
- Macにもともと入っているawkは二次元配列に非対応
- sedは-iオプションが違うのと、\(aa\|bb\)のような記述には非対応
- dockerで2つ以上のimageを並列ダウンロードすると仮想PCがメモリを異常に使用して固まる
- フォルダの名前が「.bam」などで終わる場合、ファイル選択ダイアログで「*.bam」に限定していると、そのフォルダの中に入れなくなる。
- ファイルの種類を```「*」```としていると、ほとんどのファイルを選択できなくなってしまう。
- egrepで```"[.]f(ast|)q([.]gz|)$"```は```egrep: empty (sub)expression```でエラーなので、```"[.](fastq|fq)([.]gz$|$)"```に書き換える。

### WSL用
- wslコマンドで起動したら、Ubuntuがデフォルトになるようにインストールしておく必要あり。
  2つ以上のディストリビューションをインストールしたならばwslconfigで設定しておく。
- 一度はUbuntuを起動し、ユーザ名、パスワードを設定しておく必要あり。
- WSLでは入力ファイルは元ファイルへハードリンクを貼れる場合はハードリンクを作るけど、その影響でgzip -dで解凍すると、元ファイルがハードリンクで消せないよというエラーを吐くので、gzip -dc > xxx として、明示的に別ファイルを作る必要がある。

## Linuxサーバのセットアップ方法
### CentOS7の場合
CentOSの場合、SSHサーバは基本インストール済みなので、あとはDockerをインストールすればよい。Dockerのインストール手順は良く変わるので、[公式サイト](https://docs.docker.com/install/linux/docker-ce/centos/)を見るのが好ましいが、例を挙げると下記の手順でインストール可能。
```
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce
sudo systemctl start docker
sudo systemctl enable docker
sudo docker run hello-world
```
hello-worldが無事に起動すればインストールは完了。
インストール後にrootでなくてもdockerを実行できるように下記のコマンドを実行する。
```
sudo groupadd docker
sudo usermod -aG docker $USER
```

## WSLサーバのセットアップ方法

基本的にはPortable PipelineからWSLモードで実行すれば、必要なツールをインストールしていくが、下記を手動でセットアップしておいたほうが無難かも。Administratorグループのユーザでログインして実行すること。（大規模な入力ファイルがある場合、少なくともHDDで起動しているWSL2だと、同じファイルを同時に複数のプロセスで読み込むステップで、ファイルの読み込みがエラーもなく中断されるというWSL2のバグらしき挙動に遭遇するため、Hyper-Vなどの仮想PCに普通のLinuxをインストールして使ったほうが良さそう。）

仮想化支援機能(Intel VTやAMD-V)がBIOSで有効になっている必要がある。WSL2を実行するために必要。市販のPCの半分くらいでデフォルトでは無効に設定されている。

1．Windows Subsystem for Linux (WSL)をインストールする。

WSL2以降が動作するWindows10もしくはWindows11を準備する。基本的にはPowerShellを管理者権限で開き (画面左下のWindowsロゴを右クリック→Windows PowerShell (管理者))、```wsl --install```を実行すればよい。

2．Windowsを再起動

3．再起動後に下記のような画面が表示されるので、WSLで新しく作るアカウントのユーザ名とパスワードを入力する。パスワードは表示されなくても入力されているので、入力したらEnterを押す。

![image](https://user-images.githubusercontent.com/5350508/180928671-036a54ff-1f8a-42ef-8b55-66b26d806cab.png)

もしエラーが出るようなら、Linux kernelを更新しておく必要があるかも。下記のページに従って更新すること。

https://learn.microsoft.com/ja-jp/windows/wsl/install-manual#step-4---download-the-linux-kernel-update-package

4．https://github.com/c2997108/OpenPortablePipeline/releases/download/v1.1.0/PortablePipeline-win-v1.1.0.zip
からPortable Pipelineをダウンロードして、解凍しておく。

![image](https://user-images.githubusercontent.com/5350508/180928713-3049cc36-97a7-47f4-9bb8-ec1f45383640.png)

5．解凍したフォルダの中にある「PortablePipeline(.bat)」をダブルクリックして起動する。すると、「WindowsによってPCが保護されました」と出るので、「詳細情報」をクリックして、

![image](https://user-images.githubusercontent.com/5350508/180928742-ea3f5a58-61fa-48c1-812c-2b4613bf7b86.png)

6．「実行」をクリックする。

![image](https://user-images.githubusercontent.com/5350508/180928761-9a2fff7e-7d13-4f47-be03-6dd971dcaad9.png)

7．ユーザアカウント制御画面が表示されるので、「はい（許可する）」をクリックする。

![image](https://user-images.githubusercontent.com/5350508/180928797-c5b7a550-2858-445b-90cd-64e1dde8f4fb.png)

8．Portable Pipelineが起動したら、「Settings」タブを開いて、「Preset:」をWSLにチェックを入れ、先ほど作成したWSLのユーザ名とパスワードを入力する。

![image](https://user-images.githubusercontent.com/5350508/180928832-fbf022ad-c85e-4cf2-8891-49a458d9b29b.png)

### WSL番外編1　使用するディストリビューションの手動インストール

```wsl --install```でインストールしたなら、自動でUbuntuがインストールされているはずだが、もし他のディストリビューションを使用したいならば、画面左下のWindowsロゴを左クリックし、スタートメニューの中から「Microsoft Store」を起動する。例としてUbuntu 20.04を使用したい場合は、ストアの「検索」をクリックし、「Ubuntu」と入力して検索を実行する。表示される「Ubuntu 20.04 LTS」をインストールし、起動する。

画面左下のWindowsロゴを左クリックし、スタートメニューの中からインストールしたUbuntuを起動する。 初回起動時にアカウント作成画面が表示され、ユーザ名、パスワードを入力する。

次にインストールしたディストリビューションを既定に設定するために、画面左下のWindowsロゴを右クリックし、メニューの中からPower Shellを開き、```wsl -l -v```を入力して、インストールしたディストリビューションの左横に```*```マークがついてデフォルトに指定されているか、またWSLのバージョンが2であるかどうかを確認する。

![image](https://user-images.githubusercontent.com/5350508/188283168-e50a9adc-c610-46c9-81f6-d29f339e7579.png)

もし別のディストリビューションが規定に指定されている場合は、```wsl --set-default PPUbuntu20```などと実行して、デフォルトになるように設定しておく。

また、Portable Pipeline実行時に設定されていなければ自動で追加されるが、CentOS6のdockerコンテナ用にhttps://qiita.com/nakat-t/items/271071eeb0c0c9143396 を参考に手動でvsyscall=emulateを有効にしておいても良い。

### WSL番外編2 Dockerの手動インストール

基本的には自動でインストールされるはずだけど、手動でインストールする場合はWindows用のDocker Desktopではなくて、Ubuntu用の公式の手順でWSLにDokcerをインストールする。

## Macをサーバとして使用する場合のセットアップ方法
1．Dockerのインストール

OSのバージョンが OS X Sierra 10.12以降であることを確認すること。Docker Desktop for Macを[公式サイト](https://download.docker.com/mac/stable/Docker.dmg)からダウンロードし、dmgファイルをダブルクリックし、指示に従ってインストールを完了する。

2．Dockerの設定変更

Dockerの仮想マシンはデフォルトではメモリ制限は低めになっているので、画面上部のDockerのアイコン(クジラの絵)をクリックして、Preferences…をクリックし、Advancedタブを開いて、CPUは自分のPCのコア数分、メモリはOS用に1~2GB程度除いた値を設定する。

3．Dockerの起動

Finderを開いて、アプリケーション→ユーティリティ→ターミナルを起動する。
```
docker run hello-world
```
が無事に実行されるか確認してみる。

Hello from Docker! と表示されればOK

4．Homebrewのインストール

Macに標準で入っているコマンドラインツールは10年くらい前のものなどもあって古いので、新しい必須のツール群をインストールしておく。ターミナルを開いて、下記のコマンドを一行ずつコピー＆ペーストする。
```
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

#次の2行は不要かもしれないけど、一応実行する。
sudo mkdir -p /usr/local/sbin /usr/local/opt
sudo chown $USER /usr/local/sbin /usr/local/opt

brew install grep gawk gzip ed htop iftop bash
brew install gnu-tar gnu-sed gnu-time gnu-getopt
brew install binutils findutils diffutils coreutils moreutils

echo 'export PATH=/usr/local/opt/coreutils/libexec/gnubin:/usr/local/bin:/usr/local/sbin:${PATH}
export PATH='$(dirname $(ls -ht `find /usr/local/|grep bin/grep$`|head -n 1))':$PATH
export PATH='$(dirname $(ls -ht `find /usr/local/|grep bin/bash$`|head -n 1))':$PATH
export PATH='$(dirname $(ls -ht `find /usr/local/|grep bin/xargs$`|head -n 1))':$PATH' >> ~/.bash_profile
source ~/.bash_profile
```
途中でMacのパスワードが聞かれるはずなので、入力する。

## GUIではなく、コマンドラインから実行する場合

```
/Path/To/PP/pp metagenome~silva_SSU+LSU -c 8 -m 32 -t 0.995 fastq/
```
などとすればよい。

もしグリッドエンジンで分散処理したい場合は

```
RUNPARALLEL=`echo -e '#!/bin/sh\n#$ -S /bin/bash\n#$ -cwd\n#$ -pe def_slot N_CPU\nsource ~/.bashrc\necho "$*"\neval "$*"'` pp metagenome~silva_SSU+LSU -c 8 -m 32 -t 0.995 fastq/
```

と、環境変数RUNPARALLELにグリッドエンジンのヘッダー＋「`eval "$*"`」とコマンドを実行させる部分が書いてあるスクリプトが入っていれば良い。
