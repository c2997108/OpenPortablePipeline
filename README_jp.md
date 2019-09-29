# PortablePipeline

PortablePipelineはWindows、Macユーザが、手元のコンピュータもしくはリモートのサーバ、スパコン上で、NGSの解析をGUIで行うことを可能にするソフトウェアです。グリッドエンジンとしてSGEに対応しており、白金スパコン、遺伝研スパコンにジョブを分散実行可能です。また、本ソフトウェアの仕様に従って記述されたスクリプトは、CUIからも容易に呼び出せます。

## 必要なシステム要件
### クライアント (GUI操作を行う端末)
- Windows 10 もしくは Mac OSX (Java 8が動けばOK)
- ストレージ：たくさん　解析にもよるが1TBくらいあると安心。

### サーバ (データ解析を行うコンピュータ)
- メモリ64GBの環境でテストしている。データによってはそれよりも少なくても動くこともあるし、それより多く必要なこともある。
- DockerもしくはSingularityインストール済みでSSHサーバ設定済みのLinux (CentOS6, CentOS7, Ubuntu16, Ubuntu18) : [Linuxサーバセットアップ方法](#Linuxサーバのセットアップ方法)、もしくはWindows 10のWindows Subsystem for Linuxを利用したUbuntu : [WSLサーバセットアップ方法](#WSLサーバのセットアップ方法)、もしくはMac OSXでhomebrewのcoreutilsとDocker Desktop for MacをインストールしたMac : [Macサーバセットアップ方法](#Macをサーバとして使用する場合のセットアップ方法)。

## 操作方法
1. 最新のPortablePipelineのリリースをダウンロードして解凍する。  
 Win https://github.com/c2997108/OpenPortablePipeline/releases/download/v0.9b/PortablePipeline-win-v0.9b.zip  
 Mac https://github.com/c2997108/OpenPortablePipeline/releases/download/v0.9b/PortablePipeline-mac-v0.9b.tar.gz 

2. 解凍されたファイルの中で、Windowsならば「PortablePipeline.bat」を、Macであれば「PortablePipeline.command」をダブルクリックして起動する。
Windowsユーザは、ジャンクションファイルの作成に管理者権限が必要なので、管理者で実行しても良いか聞かれると思うのでOKを押す。Macユーザは初回起動時のみ、OSの「System Preferences」→「Security & Privacy」→「General」タブ→「Open Anyway」をクリックして、実行を許可する必要がある。

3. ソフトウェアが起動したら、「Settings」タブを開いて、Linuxサーバに接続するか(「direct」を選択)、スパコンに接続するか(「ddbj」、「shirokane」を選択)、Windows単体で解析まで実行するか(「WSL」を選択)、Mac単体で解析まで実行するか(「Mac」を選択)を決めて、サーバに接続する場合は、必要なアカウント情報を入力する。設定を変更したら必ず「Save」をクリックすること。

4. 「Analysis Scripts」タブを選択し、解析したいスクリプトを選ぶと、入力ファイル、オプションを入力する画面が表示される。入力ファイルは、「input_1」などと書かれたボタンをクリックしてから選択し、オプションで変更する必要がある箇所は変更してから、画面下の「Run」ボタンを押すと実行される。

5. 「Run」ボタンが押された後、サーバにデータを転送するので、恐らくしばらく時間がかかる。進捗はJavaとは別にコマンドプロンプトかターミナルが開いているはずで、そちらにデータの転送状況が表示される。「Job List」のステータスが「Running」に変わったら、本ソフトウェアをいったん終了しても大丈夫。立ち上げていれば、30秒おきにサーバに進捗を確認しに行く。

## JAVA開発者用
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

### dockerに起因する部分
- ファイルは$PWD以下しかマウントされないので、親ディレクトリのファイルを使わない
- dockerで起動されるコマンドは1行で書くこと
- dockerはrootで起動されるので、rmは"rm -f"と書くこと
- dockerで作られたディレクトリはrootなので、dockerの外でそのディレクトリにファイルを作ろうとすると失敗する。ユーザ権限で先にディレクトリを作るべき。
- (debug用)途中で止めたいときは、docker stopを使わないと、コマンド終了まで待たされる

### singularityに起因する部分
- スクリプトを起動するフォルダが/home以下か、もしくは登録されているコンテナに必ず/mntフォルダが含まれる必要がある。そうしないと、そのフォルダに--pwdで移れなくてエラーとなる。

### Maser用
- 入力フォルダは「$input_1」ではなく、「$input_1/」と書くこと。input_1がシンボリックリンクだとそれ以上追ってくれないから。

### SGE用
- def_slotと、mem_req, s_vmemのオプションを設定出来るようにSGEサーバの設定をしておくこと。
- ジョブが途中で停止させられ、再度最初から実行されることが頻繁に有り得るので、
  その状態を想定してフォルダを作るときは事前に作られている可能性を考慮する。
- wrapper.shの中で2行目に#$ -S があればSGEモードと判定され、子スクリプトがSGE上で実行される
- DOPARALLEL, DOPARALLELONEに対して必ず1つずつWAITPARALLELを使用すること。
  そうしないと、時間差で$workdir/finが出来てしまい、次回以降のWAITPARALLELが機能しなくなる。
- DOPARALLELの中で子スクリプトを呼び出す場合、bashコマンドの前にN_SCRIPT=$N_SCRIPTを付けて、子スクリプトであることを伝える必要がある。

### スパコン用
- javaの引数には-Xmx1Gなどを必ず指定すること。指定が無いとマシンの半分くらいのメモリ？を確保しようとしてこける。

### Mac用
- dockerを/usr/local/binにインストールし、Homebrewでcoreutils, gnu-sedを/usr/local/xxx/gnubinにインストールする必要あり。
  coreutils, gnu-sedが無いと、readlink, sed等が意図したように動いてくれない。=>一部対策済み
- Macにもともと入っているawkは二次元配列に非対応
- sedは-iオプションが違うのと、\(aa\|bb\)のような記述には非対応
- dockerで2つ以上のimageを並列ダウンロードすると仮想PCがメモリを異常に使用して固まる
- フォルダの名前が「.bam」などで終わる場合、ファイル選択ダイアログで「*.bam」に限定していると、そのフォルダの中に入れなくなる。
- ファイルの種類を```「*」```としていると、ほとんどのファイルを選択できなくなってしまう。

### WSL用
- wslコマンドで起動したら、Ubuntuがデフォルトになるようにインストールしておく必要あり。
  2つ以上のディストリビューションをインストールしたならばwslconfigで設定しておく。
- 一度はUbuntuを起動し、ユーザ名、パスワードを設定しておく必要あり。
- WSL1ではcentos6のコンテナのbashは起動しない。直接chmodなどを実行する分には起動する。
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
1．Windows Subsystem for Linux (WSL)をインストールする。

Windows10のバージョンが、1803 (2018年春), 1809 (2018年秋), 1903 (2019年春)のいずれかであることを確認。 WSLを有効化するためPowerShellを管理者権限で開く。 (画面左下のWindowsロゴを右クリック→Windows PowerShell (管理者))

次のコマンドを貼り付けて実行し、WSLの機能を有効にする。
```
Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux
```
その後、Windowsを再起動

2．Ubuntuのインストール

画面左下のWindowsロゴを左クリックし、スタートメニューの中から「Microsoft Store」を起動する。ストアの「検索」をクリックし、「Ubuntu」と入力して検索を実行する。表示される「Ubuntu 18.04 LTS」をインストールし、起動する。 (ほかのUbuntu 16.04 LTS等のUbuntuでもテストした範囲では大丈夫そうだった。)

画面左下のWindowsロゴを左クリックし、スタートメニューの中からインストールしたUbuntuを起動する。 初回起動時にアカウント作成画面が表示され、ユーザ名、パスワードを入力する。

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

brew install grep gawk gzip ed htop iftop
brew install gnu-tar gnu-sed gnu-time gnu-getopt
brew install binutils findutils diffutils coreutils moreutils

echo 'export PATH=/usr/local/opt/coreutils/libexec/gnubin:/usr/local/bin:/usr/local/sbin:${PATH}
export PATH='$(dirname $(ls -ht `find /usr/local/|grep bin/grep$`|head -n 1))':$PATH
export PATH='$(dirname $(ls -ht `find /usr/local/|grep bin/xargs$`|head -n 1))':$PATH' >> ~/.bash_profile
source ~/.bash_profile
```
途中でMacのパスワードが聞かれるはずなので、入力する。
