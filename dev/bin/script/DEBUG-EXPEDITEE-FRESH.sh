#!/bin/bash

expeditee_filespace_home="/tmp/expeditee-filespace-home"

if [ -d "$expeditee_filespace_home" ] ; then
  echo "++++"
  echo "+ Deleteing previous expeditee.home: $expeditee_filespace_home" >&2
  echo "++++"
  /bin/rm -rf "$expeditee_filespace_home"

  mkdir "$expeditee_filespace_home"
fi

expeditee_filespace_home="/tmp/expeditee-filespace-home" DEBUG-EXPEDITEE.sh

