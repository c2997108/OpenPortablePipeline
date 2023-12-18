#!/bin/bash
cd `dirname $(readlink -f $0)`

#if [ ! -e output ]; then mkdir -p ~/files/m64f/pp/output; fi
#if [ ! -e settings.json ]; then
# echo '{"hostname":"m64f.s","port":"22","user":"'"$USER"'","password":"'"$USER"'","workfolder":"~/files/m64f/pp/work","preset":"direct (SGE)","outputfolder":"output","scriptfolder":"scripts","imagefolder":"~/files/m64f/pp/img","checkdelete":"false"}' > settings.json
#fi

#unset LD_LIBRARY_PATH
#/suikou/tool/jdk1.8.0_301/bin/
/suikou/tool9/jdk-17.0.8/bin/java -cp ".":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx-swt.jar":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx.base.jar":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx.controls.jar":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx.fxml.jar":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx.graphics.jar":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx.media.jar":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx.swing.jar":"/suikou/download9/javafx-sdk-20.0.2/lib/javafx.web.jar" --module-path /suikou/download9/javafx-sdk-20.0.2/lib --add-modules javafx.controls,javafx.fxml -jar PortablePipeline.jar
