#!/bin/bash
cd `dirname $0`
./jdk-21.0.1.aarch64.jdk/bin/java --module-path "./javafx-sdk-21.0.1.aarch64/lib" --add-modules javafx.controls,javafx.fxml -jar PortablePipeline.jar
