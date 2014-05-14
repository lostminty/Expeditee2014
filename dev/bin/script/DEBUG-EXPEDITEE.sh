#!/bin/bash

if [ "x$expeditee_filespace_home" = "x" ] ; then
  expeditee_filespace_home="/tmp/expeditee-filespace-home"
fi

expeditee_jar="releases/Expeditee.jar"

echo "****"

cygpath --help >/dev/null 2>&1
if [ $? == 0 ] ; then
  echo "* Detected runing in Cygwin environment => Changing expeditee_filespace_home to Windows native format"
  expeditee_filespace_home=`cygpath -w "$expeditee_filespace_home"`
  expeditee_jar=`cygpath -w "$expeditee_jar"`
fi

echo "* Lauching Expeditee.jar with expeditee.home=$expeditee_filespace_home"
echo "****"

java -classpath "$expeditee_jar" \
    "-Dexpeditee.home=$expeditee_filespace_home" org.expeditee.gui.Browser
