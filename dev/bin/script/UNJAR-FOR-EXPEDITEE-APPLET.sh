#!/bin/bash

if [ ! -d unjarred-for-expeditee-applet ] ; then
  echo "Making directory 'unjarred-for-expeditee-applet'"
  mkdir unjarred-for-expeditee-applet
fi

cd unjarred-for-expeditee-applet

find ../releases/jars -name "*.jar" \
  -exec  jar xvf {} \; -print

/bin/rm -rf manifest.mf META-INF LICENSE* README* CONTRIBUTORS.*

cd ..
