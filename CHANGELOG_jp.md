next
- WSL Ubuntu 22でdockerが起動しない不具合修正
- salmon, bbmapを追加

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
