#!/bin/bash
########################
#### This start script can be used to read data from the Latronic INFO adapter device via TCP/IP.
#### The data read is forwarded to an InfluxDB database.
########################

########################
#### Adopt the address of the adapter device to your needs.
########################

device=192.168.8.138:8899

########################
#### Adopt the InfluxDB access data to your needs.
########################

INFLUXDB_HOST=http://127.0.0.1:8086
INFLUXDB_DBNAME=MeteringData
INFLUXDB_DSNAME=Demo_01
INFLUXDB_USER=admin
INFLUXDB_PASSWD=passwd


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
    echo "Working Directory '$WORKING_DIR' does not exist"
    exit
fi

CUSTOM_CLASSPATH="$WORKING_DIR/libs/SmlToJson.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/annotations-13.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/commons-logging-1.2.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/converter-moshi-2.4.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/fluent-hc-4.5.3.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/httpclient-4.5.3.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/httpcore-4.4.6.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/influxdb-java-2.15-SNAPSHOT.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/jackson-annotations-3.0-SNAPSHOT.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/jackson-core-3.0.0-SNAPSHOT.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/jackson-databind-3.0.0-SNAPSHOT.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/jopt-simple-6.0-alpha-1.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/jsml-1.1.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/json.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/kotlin-stdlib-1.2.50.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/kotlin-stdlib-common-1.2.50.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/logging-interceptor-3.11.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/influxdb-java-2.15-SNAPSHOT.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/log4j-api-2.11.1.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/log4j-core-2.11.1.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/moshi-1.7.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/okhttp-3.11.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/okio-2.1.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/procbridge-1.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/retrofit-2.4.0.jar"
CUSTOM_CLASSPATH=$CUSTOM_CLASSPATH":$WORKING_DIR/libs/RXTXcomm-2.2pre2.jar"


MAIN_CLASS="org.lackmann.mme.tools.SmlToJSONClient"

ARGUMENTS=" --tcp $device"
ARGUMENTS=$ARGUMENTS" --retries 3"
#ARGUMENTS=$ARGUMENTS" --ipc 192.168.178.46:8077"
#ARGUMENTS=$ARGUMENTS" --ipc 8077"
#ARGUMENTS=$ARGUMENTS" --rest http://127.0.0.1:8081/rest/items/MeterReading_JSON"
ARGUMENTS=$ARGUMENTS" --influx_url $INFLUXDB_HOST --influx_dbname $INFLUXDB_DBNAME --influx_dsname $INFLUXDB_DSNAME --influx_login $INFLUXDB_USER --influx_password $INFLUXDB_PASSWD"
ARGUMENTS=$ARGUMENTS" --wait 10"

# echo "java -cp $CUSTOM_CLASSPATH $MAIN_CLASS $ARGUMENTS"

java -cp $CUSTOM_CLASSPATH $MAIN_CLASS $ARGUMENTS

