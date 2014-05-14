#!/bin/bash

if [ ! -d unjarred-for-apollo-applet ] ; then
  echo "Making directory 'unjarred-for-apollo-applet'"
  mkdir unjarred-for-apollo-applet
fi

cd unjarred-for-apollo-applet

for jf in ../jars_apollo/*.jar ; do 
  echo Unjarring $jf 
  jar xvf $jf 
done

/bin/rm manifest.mf
/bin/rm -rf META-INF

cd ..
