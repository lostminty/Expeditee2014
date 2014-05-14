#!/bin/bash

# If you don't have public-key authentication set up for your SSH login,
# You won't be able to use this script, because the SSH connections won't be able to login automatically
# So either set up public-key authentication, or you can manually start every SSH tunnel and login

# You should change the username and hostname to your own username and the hostname of your server
username=jts21
hostname=toro.cms.waikato.ac.nz
# You should change the port to the port of your server (leave it at 3000 if not sure)
port=3000

# ----------------------------------------------------#
# NOTHING BELOW THIS POINT SHOULD NEED TO BE MODIFIED #
# ----------------------------------------------------#

# get the offseted port values
frameserverPort=$port
framesaverPort=$((port+1))
messagePort=$((port+2))
infserverPort=$((port+3))
infupdatePort=$((port+4))
imageserverPort=$((port+5))
imagesaverPort=$((port+6))

# get the jar file for UDPTunnel
jarFile=$(dirname $0)/../UDPTunnel.jar
