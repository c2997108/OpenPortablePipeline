# PortablePipeline

## Requirements
### Server
- Linux (CentOS6, CentOS7, Ubuntu16, Ubuntu18)
- Docker or Singularity
- SSH server
- Memory: 100 GB over is recommended

### Client
- Java 1.8 https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html
- Storage: 1 TB over is recommended

## Install
### Server: CentOS7
```
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce
sudo groupadd docker
sudo usermod -aG docker $USER
sudo systemctl start docker
sudo systemctl enable docker
sudo docker run hello-world
```

### Client: windows
- install Java Runtime Environment 1.8
- download https://codeload.github.com/c2997108/PortablePipeline/zip/ver0.5 and unzip
- run PortablePipeline.bat
