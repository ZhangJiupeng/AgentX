#! /bin/bash

name="agentx-server-1.3.0"
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
    echo -e "${name} has already removed. \n"
    exit 1
fi

echo "Uninstalling..."
${basePath}${name}/bin/agentx remove

echo "Removing folder..."
rm -r ${basePath}

echo -e "${name} has been removed.\n"
