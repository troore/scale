#!/bin/sh

export SCALE=`pwd`/..
export SCALEHOME=$SCALE
if [ -z "$1" ]
    then
    export SCALERELEASE=scale
else
    export SCALERELEASE=$1
fi

export CLASSDEST=$SCALE/$SCALERELEASE/classes
export CLASSPATH=$CLASSPATH:$CLASSDEST:$SCALE/$SCALERELEASE

export JAVA=`which java`
export JAVAC=`which javac`
export JAVACFLAGS="-d $CLASSDEST -source 1.5"
export JAVACFLAGS=$JAVACFLAGS" -g"

export TCC_COMPILER_PATH=$CLASSDEST
