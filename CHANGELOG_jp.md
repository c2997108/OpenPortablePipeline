次回予告
- ```QC~seqkit```を追加。

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
- ```nanopore~split-barcode```で最大スコアの組み合わせを出力しなかいバグを修正
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
