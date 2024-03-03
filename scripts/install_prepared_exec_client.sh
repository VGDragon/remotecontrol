#!/bin/bash

# getting the current directory
CURRENT_DIR="$(PWD)"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd $SCRIPT_DIR
KEY_TO_BE_USED="${CURRENT_DIR}/${1}"

# if no or wrong parameters are passed, exit
if [ -z "$KEY_TO_BE_USED" ]; then
  echo "No key to be used was passed. Exiting."
  exit 1
fi
if [ ! -f "$KEY_TO_BE_USED" ]; then
  echo "The key to be used does not exist. Exiting."
  exit 1
fi

KEY_SERVER_NAME=$(basename -- "$KEY_TO_BE_USED")
# getting the file extension
KEY_CLIENT_NAME="${KEY_SERVER_NAME##*.}"


TARGET_DIR="/apps/remotecontrol"
TARGET_EXECUTE_CLIENT_DIR="${TARGET_DIR}/exec_client"
KEYPAIRS_DIR="${TARGET_DIR}/keypairs"
USER_NAME="remotecontrol"

if id -u "$USER_NAME" >/dev/null 2>&1; then
  echo "user ${USER_NAME} already exists. Skipping creation."
else
  # create the user
  useradd -r -s /bin/false ${USER_NAME}
fi

# Create the target directory
mkdir -p "${TARGET_SERVER_DIR}"
mkdir -p "${TARGET_EXECUTE_CLIENT_DIR}"

# Copy the files
cp "${SCRIPT_DIR}/remotecontrol.jar" "${TARGET_DIR}/remotecontrol.jar"
cp "${SCRIPT_DIR}/start_server.sh" "${TARGET_EXECUTE_CLIENT_DIR}/start_execute_client.sh"
cp "${SCRIPT_DIR}/data.json" "${TARGET_EXECUTE_CLIENT_DIR}/data.json"

# Modify the data.json file
sed -i 's/"isServer":true/"isServer":false/g' "${TARGET_EXECUTE_CLIENT_DIR}/dara.json"
sed -i 's/"isClient":false/"isClient":true/g' "${TARGET_EXECUTE_CLIENT_DIR}/dara.json"
sed -i 's/"exec":false/"exec":true/g' "${TARGET_EXECUTE_CLIENT_DIR}/dara.json"
#sed -i 's/"computerName":""/"computerName":""/g' "${TARGET_SERVER_DIR}/dara.json"

# prepare the server
/usr/lib/jvm/java-21-openjdk-amd64/bin/java -jar /apps/remotcontrol/remotecontrol.jar prepare

cp "${KEY_TO_BE_USED}" "${KEYPAIRS_DIR}/${KEY_SERVER_NAME}"
sed -i 's/"computerName":""/"computerName":"'"${KEY_CLIENT_NAME}"'"/g' "${TARGET_SERVER_DIR}/dara.json"

# set the permissions
chmod -R 777 "${TARGET_DIR}"

# add the service
cp "${SCRIPT_DIR}/remotecontrol_exec_client.service" "/etc/systemd/system/remotecontrol_exec_client.service"
systemctl enable remotecontrol_exec_client.service
systemctl start remotecontrol_exec_client.service


