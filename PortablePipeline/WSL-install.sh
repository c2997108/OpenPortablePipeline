
if [ `which docker|wc -l` = 0 ];then
 sed -i 's/%sudo\tALL=(ALL:ALL) ALL/%sudo\tALL=NOPASSWD: ALL/' /etc/sudoers
 apt-get update
 apt install -y libltdl7 cgroupfs-mount
 cd
 wget https://download.docker.com/linux/ubuntu/dists/xenial/pool/stable/amd64/docker-ce_17.03.3~ce-0~ubuntu-xenial_amd64.deb
 dpkg -i docker-ce_17.03.3~ce-0~ubuntu-xenial_amd64.deb
fi

if [ `id -a $SUDO_USER|grep "(docker)"|wc -l` = 0 ]; then
 usermod -aG docker $SUDO_USER
fi
