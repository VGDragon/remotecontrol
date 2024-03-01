#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_DIR
TARGET_DIR="/apps/remotecontrol"
TARGET_SERVER_DIR="${TARGET_DIR}/server"
USER_NAME="remotecontrol"

if id -u "$USER_NAME" >/dev/null 2>&1; then
  echo "user ${USER_NAME} already exists. Skipping creation."
else
  # create the user
  useradd -r -s /bin/false ${USER_NAME}
fi

# Create the target directory
mkdir -p "${TARGET_SERVER_DIR}"

# Copy the files
cp "${SCRIPT_DIR}/remotecontrol.jar" "${TARGET_DIR}/remotecontrol.jar"
cp "${SCRIPT_DIR}/start_server.sh" "${TARGET_SERVER_DIR}/start_server.sh"
cp "${SCRIPT_DIR}/data.json" "${TARGET_SERVER_DIR}/data.json"

# Modify the data.json file
sed -i 's/"isServer":false/"isServer":true/g' "${TARGET_SERVER_DIR}/dara.json"
sed -i 's/"isClient":true/"isClient":false/g' "${TARGET_SERVER_DIR}/dara.json"
sed -i 's/"exec":true/"exec":false/g' "${TARGET_SERVER_DIR}/dara.json"
#sed -i 's/"computerName":""/"computerName":""/g' "${TARGET_SERVER_DIR}/dara.json"

/usr/lib/jvm/java-21-openjdk-amd64/bin/java -jar /apps/remotcontrol/remotecontrol.jar prepare

# set the permissions
chmod -R 777 "${TARGET_DIR}"

# add the service
cp "${SCRIPT_DIR}/remotecontrol_server.service" "/etc/systemd/system/remotecontrol_server.service"
systemctl enable remotecontrol_server.service
systemctl start remotecontrol_server.service


