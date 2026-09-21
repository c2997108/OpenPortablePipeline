@echo off
setlocal
rem このファイルがあるフォルダーからPortablePipelineを起動する。管理者権限は要求しない。
rem WSLのインストールが必要な場合は、そのときだけWSL用のバッチがwsl --installを昇格して実行する。
rem フォルダーのパスは環境変数とエンコード済みコマンドでPowerShellに渡すので、スペース、日本語、
rem 丸括弧、「&」、「'」が入っていても問題ない。
set "PP_DIR=%~dp0"
powershell -NoProfile -Command "$q = [string][char]39; $c = 'Set-Location -LiteralPath ' + $q + $env:PP_DIR.Replace($q, $q + $q) + $q + '; .\jre-21.0.7-full\bin\java -jar PortablePipeline.jar'; $e = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($c)); Start-Process powershell -ArgumentList '-EncodedCommand',$e"
