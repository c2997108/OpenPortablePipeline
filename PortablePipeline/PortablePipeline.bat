:$curdir=Get-Location
:$cdcmd='cd '+$curdir+';'

powershell start-process powershell -verb runas -ArgumentList '-Command', 'cd ', %~dp0, ';', '.\jre-21.0.7-full\bin\java -jar PortablePipeline.jar'

