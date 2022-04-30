
if [ `which docker|wc -l` = 0 ];then
 echo Please type your password of this WSL if you are asked.
 sudo apt update
 sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
 curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
 echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
 sudo apt update
 sudo apt install -y docker-ce docker-ce-cli containerd.io
fi

if [ `id -a $SUDO_USER|grep "(docker)"|wc -l` = 0 ]; then
 echo Please type your password of this WSL if you are asked.
 sudo usermod -aG docker $SUDO_USER
fi
