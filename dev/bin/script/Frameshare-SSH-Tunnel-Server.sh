#!/bin/bash

# load config
. $(dirname $0)/Frameshare-SSH-Tunnel-CONFIG.sh

# tunnel udp connections through their equivalent tcp ports
java -jar $jarFile server $frameserverPort $framesaverPort $messagePort $infserverPort $infupdatePort
