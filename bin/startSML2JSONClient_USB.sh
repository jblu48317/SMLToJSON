#!/bin/bash

########################
#### This start script can be used to read data from the linux USB device only
#### The SML data record stream read from the USB device ist printed to STDOUT.
########################

########################
#### Adopt the name of the USB device to your needs.
########################

device=/dev/ttyUSB1

########################
#### Adopting the software's working directory is not needed in normal case.
########################

WORKING_DIR="../"

########################
#### Don't change anything beyond this line
########################

WORKING_DIR=`realpath $WORKING_DIR`

if [ ! -d $WORKING_DIR ]
then
    echo
    echo "Working directory '$WORKING_DIR' does not exist."
    echo "Check the path to the working directory."
    exit 1
fi

if [ ! -d $WORKING_DIR/bin ]
then
    echo "Working directory '$WORKING_DIR/bin' does not exist."
    echo "Check the installation in '$WORKING_DIR'."
    exit 1
fi

if [ ! -d $WORKING_DIR/libs ]
then
    echo
    echo "Working directory '$WORKING_DIR/libs' does not exist."
    echo "Check the installation in '$WORKING_DIR'."
    exit 1
fi

if [[ ! -c $device ]]
then
    echo
    echo "USB device '$device' does not exist."
    device=/dev/ttyUSB0
    echo "Switched to device '$device'."
fi

if [[ ! -c $device ]]
then
    echo
    echo "USB device '$device' does not exist."
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
