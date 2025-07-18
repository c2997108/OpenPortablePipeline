# 次回予告

# 1.5.3 (2025/07/18)
- ```annotation~PASA```のtrinityrnaseqのdockerイメージのバージョン更新
- ```post-assemble~dotplot-by-last```でトップヒットだけのアライメントにしていたのを全部のヒットを出力するように変更
- `nanopore~get-consensus`でsingularityでは動かなかったのを修正

# 1.5.2 (2025/06/28)
- ```nanopore~filter-lambda-phage-reads```でトリミング後のリード長が0bpの場合は除去するように設定
- ```assemble~hifiasm```でhifiasmのバージョンをv0.25.0にアップデート
- armネイティブなdockerコンテナも併せて登録しておき、arm CPUの場合はネイティブのコンテナイメージを使用する方針に変更。強制的に`linux/amd64`イメージをロードするようにしていたけど、その指定を外した。`assemble~megahit`,`QC~jellyfish`,`assemble~hifiasm`,`nanopore~get-consensus`,`ZZZ~hello-world`,`ZZZ~wait-10sec`,`ZZZ~wait-1min`,`nanopore~split-barcode`
- MacでHomebrewを使用しない環境での実行サポートを強化。xargsによる並列化対応。
- Javaのシステムプロパティ「PP_BIN_DIR（書きかえないスクリプトファイルなどのパス）」、「PP_OUT_DIR（書き込むoutput, settings, jobsファイルなど）」を読み込むように設定。環境変数PP_BASE_DIRはJavaからは直接使用しないようにした。
- GUIでゴミ箱ボタンを押したら、ジョブ履歴から消すだけではなくて、outputフォルダの中のファイルも消すように変更。
- Mac用に.dmgファイルを配布するようにした。

# 1.5.1 (2025/06/04)
- ```assemble~oatk```を追加
- ```nanopore~get-consensus```でマルチプルアライメントの作成をmuscleから高速でCPU数を指定可能なmafftへ変更
- GUIのメニューでカテゴリごとにアイコンを追加

# 1.5.0 (2025/05/07)
- DDBJスパコンがSGE→SLURMに変更となり、ジョブの投げ方が変わったため対応した。
- OpenJDKを使用していたが、JavaFXを最初から含んでいるLiberica JREに変更し、ARM版の配布パッケージも作成するようにした。
- ```QC~jellyfish```でナノポアなどのようにシーケンスエラーが多い場合に、シーケンスエラー分のkmer回数を推定して除去したのちにゲノムサイズを推定する機能を追加。
- ```post-assemble~busco_v5```でデフォルトオプションを自動推定```--auto-lineage```に変更。
- 大きいゲノムの場合にエラーになることがあったので、```mapping-nanopore~minimap2```でminimap2のバージョンを2.24から2.28に上げた。
- ```post-assemble~coverage-length-graph_by-minimap2```を追加。
- ```Comparative-genomics~FastANI```を追加。
- ```nanopore~split-barcode```で0塩基のリードを出力しないように修正。
- ```post-assemble~Repeatmodeler```で無料版のRepBaseを使ったアノテーションを追加。
- ```annotation~Helixer-with-GPU```とは別にGPUを使わないバージョン```annotation~Helixer```を追加。
- サンガーシーケンスの代わりにナノポアを使って精度の高いリードが欲しいときに使う```nanopore~get-consensus```を追加。

# 1.4.3 (2025/02/11)
- ```post-assemble~busco_v5```でminiprotを使用するBUSCO v5.8.0にアップデート
- ```nanopore~split-barcode```でprimer.fastaの配列名にタブ文字が入っている場合、sample.txtファイルにタブ区切りが複数連続する場合にエラーになっていたのを対応。ナノポアのリードがライゲーションによって複数リードが連結されてシーケンスされる？現象に対して閾値以下のプライマーのヒットでもそこでリードを区切り、キメラが生成されにくくなるように対応。このキメラ対応を行っていない旧バージョンを```nanopore~split-barcode_with_slightly_misaligned_primer```として残す。
- ```QC~jellyfish```で入力ファイルがFASTAの場合も対応可能に。予測ゲノムサイズを自動で算出するようにした。
- singularityを使用可能かどうかをチェックするときにsingularityのライブラリーではなく、dockerのhello-worldを使用するように変更。homeフォルダをマウントしないように```--no-home```を追加。
- ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end```, ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_paired-end```で厳密な一致のみを集計しやすくするために、一致率やアライメント長でのフィルタリングオプションを追加。メンテナンスが不十分になっていた```metagenome~silva_SSU+LSU```, ```metagenome~silva_SSU+LSU-paired-end```を削除。
- ```annotation~Helixer```を追加。```annotation~BRAKER3```でトップヒットが1subjectに複数ヒットあった場合にスコアが低くなるのを修正。

# 1.4.2 (2024/11/05)
- インプットファイルのシンボリックリンクが相対パスだったときにエラーになる場合があるケースに対応。
- ```post-assemble~coverage-length-graph```のグラフ表示バグを修正。
- ```nanopore~split-barcode```でinputのバーコード部分を探す方法として5'の8bpを見て同一のプライマーをまとめたのち、アダプター部分を探すように変更。その後3'側でも同じように行う。縮重塩基Hのバグを修正。
- ```nanopore~flye```を削除し、```assemble~flye```, ```trimming~sickle-se```, ```trimming~sickle-pe```を追加。

# 1.4.1 (2024/08/02)

- ```SNPcall~bcftools-mpileup```でコンティグ長がちょうど1Mbpの倍数の長さの場合にバグになるのを修正。
- Windows ARMのWSLで解析を実行できるようにdocker周りを修正。
- SGEモードの時にqsubが失敗すると再実行するようにした。
- ```nanopore~flye```でインプットがfastq.gzの場合も対応するように修正。
- ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end```,```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_paired-end```でFASTQの分割リード数を設定できるようにした。
- ```post-assemble~coverage-length-graph```で縦軸をコンティグ長の合計の場合のグラフを追加。

# 1.4.0 (2024/05/15)

- ```mapping-smallRNA-miRDeep2```を追加。
- ```annotation~BRAKER3```でゲノムが断片化しすぎているときにGeneMarkがエラーになるので、GeneMarkのmin_contigパラメーターを変更できるようにした。
- SGEを使って分散してジョブを実行する場合、ジョブがノードに入った後でエラーになった場合は1回だけ再実行する機能を追加。
- ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end```がv1.3.2以降、入力のFASTQがgz圧縮されている場合にエラーになっていたのを修正。
- apptainerをコンテナエンジンとして利用可能になった。
- ```annotation~PASA```, ```annotation~FINDER```の試験的な追加。
- ```annotation~Funannotate```を追加。
- Portable Pipelineのスクリプトから、ほかのPortable Pipelineのスクリプトを呼び出す方法として```PP_DO_CHILD```, ```PP_ENV_CHILD```を追加。
- ```nanopore~split-barcode```で2段階目のフィルタリング時のバグを修正。
- 複数ユーザに対応するため、環境変数「```PP_BASE_DIR```」に設定したフォルダにoutputやsettings.json, jobs.jsonを書き出すようにした。

# 1.3.8 (2024/04/11)

- ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_paired-end```がv1.3.2以降でペアエンドとして処理できなくなっていたのを修正。
- DDBJのスパコン使用時にDDBJサーバ内の.bashrcに```source /home/geadmin/AGER/ager/common/settings.sh```が書かれていないと実行されない不具合を修正。

# 1.3.7 (2024/03/31)

- sshでジョブのチェックをする際にセッションを切断せずに新しくSSHのセッションを張り、サーバに負荷がかかるのを修正。
- DDBJのスパコンにジョブを投げてジョブが始まらない場合に、待ち行列の何番目か、CPU数をいくつ以下にしたらすぐに実行されるかなどの情報を表示するようにした。
- DDBJで使用するキューはmedium.qを指定するようにした。
- DDBJではwrapper.shを実行した後、ジョブをグリッドエンジンで並列実行するのではなく、wrapper.shを実行しているジョブの中一つで完結するように変更。混んでいてジョブを分けるといつ実行されるかわからないため。

# 1.3.6 (2024/03/29)

- WSLでdockerを実行する場合に、途中でジョブを止めるとdockerも停止するように修正。
- sshモードのときジョブの終了判定をたまに間違ってしまい異常終了となるのを修正。
- ジョブを実行後にログが出るまで時間がかかっていたのを修正。
- ```annotation~BRAKER3```がGUIからだと起動できなかった問題を修正。

# 1.3.5 (2024/03/18)

- ```nanopore~split-barcode```でバーコード部分だけを検出して2段階目のフィルタリングを行う機能を追加
  
# 1.3.4 (2024/03/08)

- ```mapping-illumina~bbmap```で入力ファイルが複数ある場合、baiインデックスを正しく作成できていなかった点を修正。
- SGE使用時にDOPARALLELでジョブを分散させる際、ジョブが0個の場合エラーになっていたのを修正。
- v1.3.2以降に```RNA-seq~Trinity-kallisto-sleuth```で比較解析が上手くいかなくなっていたのを修正。結果ファイルのサンプル名から.gzを削除するように変更。
- ```mapping-illumina~bwa_mem```, ```mapping-illumina~parabricks```で並列処理が正しく実行されないのを修正。

# 1.3.3 (2024/01/17)

- GUIからジョブを投げる際に、30秒に一度データをチェックするタイミングとたまたま一致した場合、ステータスはabortなのに実行されるという状態を修正。
- v1.3.2でコマンドライン実行時にhelpの内容が表示されないことがある場合を修正
- v1.3.2でコマンドライン実行時に```-s```, ```-g```オプションを無視していたのを修正
- v1.3.2でLinux、Macモードの時にチェックが正常に行えずにabortしていたのを修正
- GPUを使用するツールに対応。DOGPU_XXXとつければよい。
- ```mapping-illumina~parabricks```を追加。
- ```annotation~SpliceAI```を追加。
- inputがシンボリックリンクの場合に、リンク先を探す挙動を改善

  
# 1.3.2 (2024/01/10)

- ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end```, ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_paired-end```でインプットファイルを分割してblastするように変更
- ```RNA-seq~HISAT2-StringTie-DEGanalysis```のcuffdiffがエラーになっていたのを修正
- GUIモードでジョブの終了チェックをpidやsge jobidでも行う
- SGEモードの時に中断した場合に並列実行中のジョブを削除する機能を追加
- コマンドラインで実行した場合も標準出力・エラー出力を```pp_log.txt```ファイルにも保存
- sshでsingularityを使用した場合wrapper.shを上書きしてしまう挙動を修正
- ```mapping-nanopore~minimap2```で入力ファイルが複数の場合、並列で処理できるように修正
- ```SNPcall~bcftools-mpileup```でbamのインデックスファイル(.bai)を指定していない場合、自動で作成するように変更。
- ```WGS~genotyping-by-mpileup```, ```SNPcall~bcftools-mpileup```で分割したファイルを連結する際のcatをdocker経由で実行するとなぜか出力が途中で切れるファイルが出てくるので、catはdockerを経由せずにホストのシステムのcatを使うように変更。
- 1.3.0以降のWSLモードが正常に実行されず、すぐに異常終了ステータスになっていたのを修正。

# 1.3.1 (2023/12/21)

- 出力フォルダに「~」という名前のフォルダが出来る不具合を修正
- グリッドエンジンに投げたときのログファイルをpp_logフォルダに移動するように変更
- Linuxモードで入力ファイルがシンボリックリンクの際にシンボリックリンクそのものをコピーしていた挙動を変更し、シンボリックリンクへのシンボリックリンクを作るようにした

# 1.3.0 (2023/12/18)

- ```QC~seqkit```を追加。
- ```Hi-C~YaHS```を追加。
- ```nanopore~split-barcode```で最大スコアの組み合わせを出力しないバグを修正
- ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end```, ```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_paired-end```で葉緑体と植物の18Sが競合してunknownとなる場合に葉緑体を優先するように修正。
- Java8->Java21へアップデート
- sshでlinuxサーバに接続していたモードの名前を「direct」から「ssh」へと変更
- linuxでGUI起動に対応。GUIで起動しているサーバで直接解析するモードとして「linux」を追加
- 秘密鍵を使用したssh接続時に使用する秘密鍵は```ssh-keygen -m PEM```とオプションをつけて作成するように注意書きを追加


# 1.2.8 (2023/12/04)
- ```metagenome~silva-SSU-LSU_PR2_NCBI-16S-mito-plastid_single-end```, ```metagenome~silva-SSU-LSU_PR2_NCBI-16S-mito-plastid_paired-end```を```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end```,```metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_paired-end```に変更。SILVAはバクテリア専用、PR2は真核生物専用にした。
- ```RNA-seq~clustering```サンプル名の区切り文字をタブに指定。
- ```annotation~BRAKER2```を```annotation~BRAKER3```に変更。GeneMark-ES/ET/EPのラインセンスキーが必要なくなった。
- pp.pyの`-g`オプションでグリッドエンジン動作時にqsub.shの最後にステータスを出力していなかったのを修正。
- ```SNPcall~xatlas-glnexus```を追加。
- ```mapping-illumina~bwa_mem```でFASTQファイル名が_R1を含むペアエンドリードをシングルエンドと認識していたバグを修正。
- ```QC~kmergenie```、```post-assemble~Repeatmodeler```を追加。
- ```SNPcall~bcftools-mpileup```で複数サンプルをSNPコールする際に、1リードなど少ないサンプルのジェノタイプが本来0/0もしくは1/1になるべき時に0/1とヘテロになる箇所を修正。
- ```post-assemble~coverage-length-graph```でコンティグの平均デプスからゲノムサイズを推定する機能を追加。
- ENV_XXXで並列処理する際に、コマンド文の中に「"」や「@」が入っているとうまく処理できなかった問題に対応。

# 1.2.7 (2023/09/21)
- Some Grid Engine (a fork of Son of Grid Engine at University of Liverpool)標準のセットアップ方法に従い子ノードでローカルのディスクにSGEをインストールした際にqacctが使えないことへの対応。
- ```RNA-seq~HISAT2-StringTie-DEGanalysis```でgtfに登録されているchromosomeのIDがない場合gtfからレコードを削除。cuffdiffへ渡るオプションがエスケープ処理を失敗していたのを修正。
- ```RNA-seq~Trinity-kallisto-sleuth```でサンプルが1つの場合でもアノテーションのステップまでは実行されるように修正。
- ```statistics~DESeq2```,```statistics~edgeR```はもともとTrinityのパイプライン用に作っていたけど、これだけ実行したいときにinput_2のサンプル情報ファイルの書式を修正して実行しやすくする機能を追加。
- ```assemble~hifiasm```を追加。

# 1.2.6 (2023/07/21)
- podman使用時に-uオプションを使わないように変更
- apptainerで`library://`設定が無い場合に対応

# 1.2.5 (2023/05/24)
- ```metagenome~mapping-to-MAG```、```metagenome~mapping-to-MAG-with-full-assembly```にminimum mapQのオプションを追加
- ```post-assemble~coverage-length-graph```にminimum alignment, minimum mapQオプションを追加、個別のFASTQに対してカバレッジを計算するように追加
- DDBJスパコン 2023年アップデートに対応。singularity pullが失敗した場合に、直接イメージをダウンロードする回避策を追加

# 1.2.4 (2023/4/26)
- ```mapping-nanopore~minimap2```にCDS予測オプションを追加
- ```nanopore~split-barcode```で最大スコアの組み合わせを出力しないバグを修正
- ```metagenome~PR2_NCBI-16S-mito-plastid```にSILVAを加えて```metagenome~silva-SSU-LSU_PR2_NCBI-16S-mito-plastid```に変更
- ```annotation~BRAKER2```で入力のFASTAファイルの改行コードがWindowsだったときの対応を行った
- ```metagenome~use-genbank-fasta-as-reference```でBLAST Databaseを入力可能にした。

# 1.2.3 (2023/3/22)
- ```post-assemble~dotplot-by-minimap2```のコンティグの向きを決めるときに、ヒットしている位置の重心の位置ではなくて、単純に＋－それぞれにヒットした長さの合計で向きを決めるように変更。
- ```SNPcall~bcftools-mpileup```, ```WGS~genotyping-by-mpileup```の並列性の改善
- ```RNA-seq~SNPcall-bbmap-callvariants```でミスコールを減らすようにdeletionサイズの最大値設定と、ソフトクリッピングオプションをデフォルトにした。
- コマンドラインから投げたSGEでの並列処理終了時の検証を高速化。GUIから投げた場合は変更なし。
- ```nanopore~minimap2```を```mapping-nanopore~minimap2```に変更して、cDNAをマッピングした時にGTFも出力できるように変更

# 1.2.2 (2023/3/8)
- podmanに対応
- ```annotation~blast-diamond```の追加

# 1.2.1 (2023/3/1)
- ```RNA-seq~SNPcall-bbmap-callvariants```の追加
- ```mapping-illumina~bbmap```のsamtools sort時のメモリー量変更
- ```Hi-C~SALSA```でhicファイルを生成するjuicerのバージョンをアップデート
- ```metagenome~PR2_NCBI-16S-mito-plastid_paired-end```ペアエンドを使っていなかったバグ修正

# 1.2.0 (2022/12/18)
- ```Comparative-genomics~make-align-for-mauve```の追加
- ```post-assemble~dotplot-by-last```の追加
- ```metagenome~Taxonomic-classifications-by-10core-genes```の追加
- ```metagenome~MAG-annotation-by-10core-genes```の追加
- ```metagenome~MAG-annotation-by-SILVA-SSU-LSU```の追加
- ```metagenome~silva_SSU+LSU-paired-end```でFASTQが.gz圧縮されているときにペアエンドを正しく取得できていない不具合を修正。
- ```RNA-seq~Trinity-kallisto-sleuth```でsorted.bamを拾っていなかったバグを修正
- DO_XXXの時に()で囲まなくて良いように関数化を行った。
- CUIにて、pp -sでsingularityを使用、pp -gでSGEを使用する機能を追加
- 並列実行時にxargsのオプションに`-d'\n'`を追加することで、「'」、「"」、「\」をエスケープなしで処理できるように変更。修正したスクリプトの一覧は下記になる。
```
mapping-illumina~bwa_mem
post-assemble~coverage-length-graph
WGS~genotyping-by-GATK
WGS~genotyping-by-mpileup
metagenome~mapping-to-MAG
RNA-seq~DEGanalysis
statistics~all-sample-combinations-DESeq2-edgeR
metagenome~clustering_pfam-annotation

Hi-C~SALSA
metagenome~mapping-to-MAG-with-full-assembly
```

# 1.1.3 (2022/10/19)
- ```metagenome~PR2_NCBI-16S-mito-plastid_single-end```の追加

# 1.1.2 (2022/10/13)
- ver1以降でdockerを使用した際にパイプを使用する場合、パイプの後ろは(DO_XXX XXX)と()で囲む必要があったのだけど、その修正を忘れて```broken pipe```とエラーが出ていた件を修正。修正範囲：```annotation~Trinotate```, ```preprocessing~exclude-specific-entries-in-FASTA```


# 1.1.1 (2022/9/29)
- ```metagenome~PR2_NCBI-16S-mito-plastid_paired-end, metagenome~mapping-to-MAG, metagenome~mapping-to-MAG-with-full-assembly, preprocessing~download-SRA-FASTQ```を追加。
- ```RNA-seq~Trinity-kallisto-sleuth, statistics~all-sample-combinations-DESeq2-edgeR```のバグfix

# 1.1.0 (2022/7/25)
- WSL Ubuntu 22でdockerが起動しない不具合修正
- salmon, bbmap, metagenome~mapping-to-MAGを追加
- singularity使用時にフォルダを移動するとエラーになっていた問題修正
- Terra用にDocker(c2997108/ubuntu:20.04-singularity_pp)の中でchrootで実行する機能を追加(ただし、/procなどをバインドしないため、javaなどは動作しない)

# 1.0.9 (2022/5/12)
- WSL使用時にAdministrator権限のないユーザでログインした場合に、wsl --installでは管理者のアカウントにインストールされないので、wsl --importでセットアップする手法を追加
- linkage-analysis~single-cell_CellRanger-VarTrixのスクリプトでawkの出力区切り文字が環境によって反映されない件を修正
- Comparative-genomics~circos-plot-with-single-copy-geneでFASTAの配列名に「;」や「=」が入っているとエラーになる件を修正

# 1.0.8 (2022/5/9)
- オプションが-e, -E, -nで始まる場合に消されていた不具合修正
- Comparative-genomics~circos-plot-with-single-copy-geneを追加
- post-assemble~dotplot-by-minimap2のスクリプトのバグ修正

# 1.0.7 (2022/4/30)
- post-assemble~dotplot-by-minimap2スクリプトの中でsamtoolsをコンテナ経由で呼び出していなかった箇所を修正
- Windows 11でのWSL使用時の解析をサポート。WSL2使用時のみをサポートするように変更した。

# ver1.0.4 (2022/3/31)
- 入力ファイルがシンボリックリンクの場合に、もとのパスをdocker/singularityでマウントするように変更。singularityでは以前テストしたときは`スクリプトを起動するフォルダが/home以下か、もしくは登録されているコンテナに必ず/mntフォルダが含まれる必要がある。そうしないと、そのフォルダに--pwdで移れなくてエラーとなる。`という結果だったけど、現在のバージョンで試したときは問題なかったので/mntにバインドするのはやめて現在の絶対パスにそのままバインドするようにした。

# ver1.0.3 (2022/3/30)
- pp.pyのpythonを2->3に変更。CentOS8、Ubuntu20ではもはやpythonコマンドはないため。

# ver1.0.1 (2022/1/21)
- DO_, $ENV_にdockerやsingularityの起動だけでなく、コンテナIDを保存したりする処理を加えたことで「|」の後でDO_, $ENV_を使うことが対応できていなかった点の対応。

# ver1.0 (2022/1/17)
- pythonのラッパーから各スクリプトを起動するように変更した。これにより、実行途中のDockerコンテナをストップさせる処理を挟むことが出来るようになり、Portable Pipelineからキャンセル処理をしたときに実行中の解析ツールを停止することができるようになった。
- post-assemble~dotplot-by-minimap2のバグで、たまに対角線を挟んで対象とならないことがあるのを修正。minimap2がクエリーとDBを逆にすると結果が異なる仕様を把握していなかったことが原因。クエリーとDBを入れ替えて2回実行することで解決。

# 0.9e (2020/1/16)
- ssh秘密鍵のパスフレーズに対応
- ジョブ、スクリプト選択画面の境界サイズを変更可能に
