#!/bin/bash

trap ctrl_c INT

function ctrl_c() {
	echo -e "\nExiting"
	# clean up the SSH tunnels
	jobs -p | xargs kill
	exit 0
}

# load config
. $(dirname $0)/Frameshare-SSH-Tunnel-CONFIG.sh

# setup ssh tunnels
ssh -NL $frameserverPort:localhost:$frameserverPort ${username}@${hostname} &
ssh -NL $framesaverPort:localhost:$framesaverPort ${username}@${hostname} &
ssh -NL $messagePort:localhost:$messagePort ${username}@${hostname} &
ssh -NL $infserverPort:localhost:$infserverPort ${username}@${hostname} &
ssh -NL $infupdatePort:localhost:$infupdatePort ${username}@${hostname} &
ssh -NL $imageserverPort:localhost:$imageserverPort ${username}@${hostname} &
ssh -NL $imagesaverPort:localhost:$imagesaverPort ${username}@${hostname} &
# give the ssh instances time to connect
sleep 1
# tunnel udp connections through their equivalent tcp ports
java -jar $jarFile client $frameserverPort $framesaverPort $messagePort $infserverPort $infupdatePort
