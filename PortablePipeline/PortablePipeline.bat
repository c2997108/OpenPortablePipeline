:$curdir=Get-Location
:$cdcmd='cd '+$curdir+';'

powershell start-process powershell -verb runas -ArgumentList '-Command', 'cd ', %~dp0, ';', '.\jdk-21.0.1\bin\java --module-path "./javafx-sdk-21.0.1/lib/" --add-modules javafx.controls,javafx.fxml -jar PortablePipeline.jar'

