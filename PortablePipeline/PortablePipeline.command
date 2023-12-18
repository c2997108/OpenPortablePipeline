#!/bin/bash
cd `dirname $0`
./jdk-21.0.1/bin/java --module-path "./javafx-sdk-21.0.1/lib/" --add-modules javafx.controls,javafx.fxml -jar PortablePipeline.jar
