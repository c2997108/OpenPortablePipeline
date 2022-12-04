次回予告

- ```Comparative-genomics~make-align-for-mauve```の追加
- ```post-assemble~dotplot-by-last```の追加
- ```metagenome~Taxonomic-classifications-by-10core-genes```の追加
- ```metagenome~MAG-annotation-by-10core-genes```の追加
- ```metagenome~MAG-annotation-by-SILVA-SSU-LSU```の追加
- ```metagenome~silva_SSU+LSU-paired-end```でFASTQが.gz圧縮されているときにペアエンドを正しく取得できていない不具合を修正。

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
