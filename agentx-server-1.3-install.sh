#! /bin/bash

name="agentx-server-1.3.0"
downloadUrl="https://github.com/ZhangJiupeng/AgentX/releases/download/v1.3.0/${name}.tar.gz"
basePath="/usr/local/agentx/"

clear
echo -e "\n\n\
      _                    _   __  __       \n\
     / \\   __ _  ___ _ __ | |_ \\ \\/ /    \n\
    / _ \\ / _\` |/ _ \\ \`_ \\| __| \\  /  \n\
   / ___ \\ (_| |  __/ | | | |_  /  \\      \n\
  /_/   \\_\\__, |\\___|_| |_|\\__ /_/\\_\\ \n\
          |___/                             \n\
                                            \n\
      AgentX Server 1.3.0 Installer         \n\
========================================="
if [ ! -d "${basePath}" ]; then 
    echo -e "Create folder \"${basePath}\""
    mkdir "${basePath}"
else
    echo -e "Folder \"${basePath}\" exists, please remove before install this program. \n"
    exit 1
fi

echo "Redirect to ${basePath}"
cd ${basePath}

echo "Downloading..."
wget -q ${downloadUrl}

echo "Extracting files..."
tar -xvf ${name}.tar.gz
rm ${name}.tar.gz

echo -e "\nConfig ${name}"
read -p "port: " port
read -s -p "password: " password

echo -e "\nMerging ${basePath}${name}/bin/config.json..."
echo -e "\
{
    \"host\": \"0.0.0.0\",\n\
    \"port\": ${port},\n\
    \"relayPort\": [],\n\
    \"protocol\": \"shadowsocks\",\n\
    \"encryption\": \"aes-256-cfb\",\n\
    \"password\": \"${password}\",\n\
    \"process\": [\"encrypt\"],\n\
    \"dnsCacheCapacity\": 1000,\n\
    \"writeLimit\": 0,\n\
    \"readLimit\": 0\n\
}
" > ${basePath}${name}/bin/config.json

echo -e "\nInstall service..."
${basePath}${name}/bin/agentx version
${basePath}${name}/bin/agentx installstart
echo -e "Done!\n"

