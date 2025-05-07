#!/bin/bash
cd `dirname $(readlink -f $0)`
./jre-21.0.7-full/bin/java -jar PortablePipeline.jar
