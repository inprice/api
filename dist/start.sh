#!/bin/bash
set -e

export APP=./api-1.0.0.jar
export MAIN_CLASS=io.inprice.scrapper.api.Application

export JVM_ARGS='-Dlog4j.configuration=file:./conf/log4j.properties -Xms512m -Xmx1024m'

DIR="$( pwd )"
source "$DIR"/setenv.sh
"$DIR"/daemon/start-daemon.sh $APP $@
