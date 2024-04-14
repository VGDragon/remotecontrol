#!/usr/bin/bash
cd /apps/remotecontrol/server
# look if /apps/remotecontrol/remotecontrol.jar.update exist
if [ -f /apps/remotecontrol/remotecontrol.jar.update ]; then
    # if exist, copy it to /apps/remotecontrol/remotecontrol.jar
    cp /apps/remotecontrol/remotecontrol.jar.update /apps/remotecontrol/remotecontrol.jar
    # remove /apps/remotecontrol/remotecontrol.jar.update
    rm /apps/remotecontrol/remotecontrol.jar.update
fi
/usr/lib/jvm/java-21-openjdk-amd64/bin/java -jar /apps/remotecontrol/remotecontrol.jar