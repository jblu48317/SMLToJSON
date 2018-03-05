#!/bin/bash

device=/dev/ttyUSB1

if [[ ! -c $device ]]
then
        device=/dev/ttyUSB0
fi

if [[ ! -c $device ]]
then
        logger `echo "Kein USB Device vorhanden"`
        exit 1
fi


WORKING_DIR="/home/openhabian/jsml"

CUSTOM_CLASSPATH="/usr/share/java/RXTXcomm.jar:$WORKING_DIR/SmlToJson.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/rxtxcomm_api-2.2pre2-11_bundle.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/procbridge-1.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/jopt-simple-6.0-alpha-1.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/json.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/jsml-1.1.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/fluent-hc-4.5.3.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/httpclient-4.5.3.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/httpcore-4.4.6.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/commons-logging-1.2.jar"

MAIN_CLASS="org.lackmann.mme.tools.SmlToJSONClient"

ARGUMENTS=" --device $device"
ARGUMENTS=$ARGUMENTS" --retries 3"
#ARGUMENTS=$ARGUMENTS" --ipc 8077"
#ARGUMENTS=$ARGUMENTS" --rest http://127.0.0.1:8081/rest/items/MeterReading_JSON"
ARGUMENTS=$ARGUMENTS" --wait 5"

java -Djava.library.path=/usr/lib/jni -cp $CUSTOM_CLASSPATH $MAIN_CLASS $ARGUMENTS
