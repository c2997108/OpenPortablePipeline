# PortablePipeline

PortablePipeline is a software that gives Windows and Mac users the ability to conduct NGS analysis through a graphical interface, on either a local computer, remote server. It supports SGE as a grid engine and can execute jobs on a supercomputer such as SHIROKANE and NIG in Japan. Registered scripts can also be easily called from the command-line interface.
![pp](https://user-images.githubusercontent.com/5350508/69688721-35e88f00-110a-11ea-8260-520f7554935f.png)

## Required System Requirements
### Clients (Terminal that performs GUI operations)
- Windows 10/11 or Mac OSX (It's OK if Java 8 works.)
- Storage: It depends on a lot of analytics, but 1TB over is recommended.

### Servers (computer that performs data analysis)
- Tested with 64 GB of memory. Depending on the data, less may work and more may be needed.
- Linux (CentOS7 or later, Ubuntu 16 or later) with Docker or Singularity installed and an SSH server : [Linux Server Setup Instructions](#linux-server-setup-instructions), or Ubuntu with Windows Subsystem for Linux on Windows 10/11 : [WSL Server Setup Instructions](#WSL-Server-Setup-Instructions), or Mac OSX with homebrew coreutils and Docker Desktop for Mac : [Mac Server Setup Instructions](#Mac-Server-Setup-Instructions).

## How to use
1. Download and extract the latest PortablePipeline release.  

https://github.com/c2997108/OpenPortablePipeline/releases

Win: PortablePipeline-win-vXXX.zip  
Mac: PortablePipeline-mac-vXXX.tar.gz  

2. In the unzipped file, double-click "PortablePipeline.bat" on Windows or "PortablePipeline.command" on Mac to launch it.
Windows users need administrator privileges to create the junction file, so they will be asked if they can run it as an administrator, and then press OK. Mac users only need to allow it to run the first time by clicking "System Preferences" in OS System menu → "Security & Privacy" → "General" tab → "Open Anyway".

3.Once the software is running, go to the "Settings" tab and choose whether you want to connect to a Linux server (Select "direct"), a supercomputer (Select "ddbj" or "shirokane"), Windows (Select "WSL") or Mac (Select "Mac"), and if you want to connect to a server, enter the required account information. Be sure to click "Save" after changing the setting.

4.Select the "Analysis Scripts" tab, select the script you want to analyze, and you'll see a screen where you can enter an input file and options. The input file is selected by clicking a button labeled "input_1", etc., changing options that need to be changed, and then pressing the "Run" button at the bottom of the screen.

5.After the "Run" button is pressed, the data is transferred to the server, which may take some time. The progress should be displayed in a separate command prompt or terminal that shows the status of the data transfer. When the status of "Running" changes to "Job List", you may exit this software. If it's up, it goes to the server every 30 seconds to check on progress.

## Linux Server Setup Instructions
### for CentOS 7
CentOS has a basic SSH server installed, so all you need to do is install Docker. Docker installation instructions change a lot, so it's best to check out the [official site](https://docs.docker.com/install/linux/docker-ce/centos/), but for example, you can install it as follows.
```
sudo yum install -y yum-utils device-mapper-persistent-data lvm2
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce
sudo systemctl start docker
sudo systemctl enable docker
sudo docker run hello-world
```
Once hello-world is up and running, the installation is complete.
Run the following command to run docker after installation without having to be root.
```
sudo groupadd docker
sudo usermod -aG docker $USER
```

## WSL Server Setup Instructions
1．Install Windows Subsystem for Linux (WSL).

Verified that the version of Windows 10 is 1803 (Spring 2018), 1809 (Fall 2018), or 1903 (Spring 2019). Open PowerShell with administrator privileges to enable WSL. (Right-click the Windows logo in the lower-left corner of the screen → Windows PowerShell (Administrator))

Paste and run the following command to enable WSL functionality:.
````
Enable-WindowsOptionalFeature-Online-FeatureName Microsoft-Windows-Subsystem-Linux
````
Then restart Windows

2．Installing Ubuntu

Open "Microsoft Store" from the Start menu by left-clicking the Windows logo in the lower-left corner of the screen. Click "Search" for the store and type "Ubuntu" to run the search. Install and start the displayed "Ubuntu 18.04 LTS". (In my tests with other Ubuntu platforms like Ubuntu 16.04 LTS, it looked fine.)

Left-click on the Windows logo at the bottom left of the screen and start Ubuntu from the Start menu. When you first start it up, the account creation screen will appear and you will be prompted to enter your user name and password.

## Mac Server Setup Instructions
1．Installing Docker

Make sure the OS version is OS X Sierra 10.12 or later. Download Docker Desktop for Mac from [official site] (https://download.docker.com/mac/stable/Docker.dmg), double-click the dmg file and follow the instructions to complete the installation.

2．Docker Configuration Changes

Docker virtual machines have a low memory limit by default, so click the Docker icon (a picture of a whale) at the top of the screen, click Preferences..., open the Advanced tab, and set the CPU to the number of cores on your PC, and the memory to something like 1 ~ 2 GB for the OS.

3．Starting Docker

Open Finder and go to Applications - Utilities - Terminal.
````
docker run hello-world
````
to see if it works.

Hello from Docker! It is OK if it is displayed as.

4．Install Homebrew

Some of the command-line tools that come standard with the Mac are 10 years old, so install the new essential tools. Open a terminal and click
````
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

echo "export PATH=/usr/local/opt/coreutils/libexec/gnubin:/usr/local/bin:/usr/local/sbin:${PATH} >> ~/.bash_profile"
source ~/.bash_profile

# The next two lines may not be necessary? If you get an error in the "brew install", you need to run it.
sudo mkdir -p /usr/local/sbin /usr/local/opt
sudo chown $USER /usr/local/sbin /usr/local/opt

brew install grep gawk gzip ed htop iftop
brew install gnu-tar gnu-sed gnu-time gnu-getopt
brew install binutils findutils diffutils coreutils moreutils
````
You'll be asked for your Mac password.
