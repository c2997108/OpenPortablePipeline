# ver1.0 (2022/1/17)
- pythonのラッパーから各スクリプトを起動するように変更した。これにより、実行途中のDockerコンテナをストップさせる処理を挟むことが出来るようになり、Portable Pipelineからキャンセル処理をしたときに実行中の解析ツールを停止することができるようになった。
- post-assemble~dotplot-by-minimap2のバグで、たまに対角線を挟んで対象とならないことがあるのを修正。minimap2がクエリーとDBを逆にすると結果が異なる仕様を把握していなかったことが原因。クエリーとDBを入れ替えて2回実行することで解決。

# 0.9e (2020/1/16)
- ssh秘密鍵のパスフレーズに対応
- ジョブ、スクリプト選択画面の境界サイズを変更可能に