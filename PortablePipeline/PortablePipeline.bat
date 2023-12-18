:$curdir=Get-Location
:$cdcmd='cd '+$curdir+';'

powershell start-process powershell -verb runas -ArgumentList '-Command', 'cd ', %~dp0, ';', '.\java\8\bin\java -jar PortablePipeline.jar'
