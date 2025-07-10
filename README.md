# PortablePipeline

[日本語版 (Japanese)](https://github.com/c2997108/OpenPortablePipeline/blob/master/README_jp.md)

PortablePipeline is a software that gives Windows and Mac users the ability to conduct NGS analysis through a graphical interface, on either a local computer, remote server. It supports SGE as a grid engine and can execute jobs on a supercomputer such as SHIROKANE and NIG in Japan. Migration to SLURM job scheduler on the NIG supercomputer completed (2025). Registered scripts can also be easily called from the command-line interface.

![image](https://github.com/user-attachments/assets/3f183d9d-739a-4a73-9cbd-8aecf4c1fe2f)

## Analysis Result Examples

Typical analysis result examples:

  * [Metagenome/eDNA analysis: metagenome\~silva-SSU-LSU\_PR2\_NCBI-mito-plastid\_MitoFish\_single-end](https://suikou.fs.a.u-tokyo.ac.jp/pp/metagenome~silva-SSU-LSU_PR2_NCBI-mito-plastid_MitoFish_single-end/)
  * [RNA-seq analysis with genome: RNA-seq\~HISAT2-StringTie-DEGanalysis](https://suikou.fs.a.u-tokyo.ac.jp/pp/RNA-seq~HISAT2-StringTie-DEGanalysis/)
  * [RNA-seq analysis without genome: RNA-seq\~Trinity-kallisto-sleuth](https://suikou.fs.a.u-tokyo.ac.jp/pp/RNA-seq~Trinity-kallisto-sleuth/)

## Required System Specifications

### Client (Terminal for GUI operation)

  * Windows, Mac, or Linux running Java 21 or later.
  * Storage: A lot. 1TB is good for analysis, though it depends on the analysis.

### Server (Computer for data analysis)

I test on an environment with 64GB of memory and 500GB of disk space. Depending on the data, it may run with less or require more. One of the following servers is required:

#### Self-owned Server

  * Linux Server: A Linux server with Docker, Podman, Singularity, or Apptainer installed, and Python 3 or later installed (if used as a remote server, configured for SSH login) [Linux Server Setup](#linux-server-setup-instructions).
  * Windows WSL: Ubuntu using Windows Subsystem for Linux on Windows 10 or later: [WSL Server Setup](#WSL-Server-Setup-Instructions). If WSL is selected in the settings, it will automatically set up WSL and install Docker on WSL Ubuntu, preparing the environment automatically. Note that WSL may not be able to perform disk access in parallel, and errors may occur unless the number of CPUs is set to 1. The WSL virtual machine has a small virtual disk size (around 100GB), so some scripts may not run due to insufficient capacity.
  * Mac with Homebrew coreutils and Docker Desktop for Mac installed: [Mac Server Setup](#Mac-Server-Setup-Instructions). Docker's virtual machine has a small virtual disk, so some scripts may not run due to insufficient capacity.

#### Supercomputer (Free account available)

Supercomputers have strict memory limits, and even if not actually used, the declared memory amount can lead to termination, so unexpected shutdowns may occur.

  * DDBJ Supercomputer, National Institute of Genetics: [https://sc.ddbj.nig.ac.jp/ja](https://sc.ddbj.nig.ac.jp/ja)
  * Human Genome Center Supercomputer, University of Tokyo: [https://supcom.hgc.jp/japanese/](https://supcom.hgc.jp/japanese/)

## Client Installation

Download the latest PortablePipeline release from the following page:

[https://github.com/c2997108/OpenPortablePipeline/releases](https://github.com/c2997108/OpenPortablePipeline/releases)


### For Windows Users

If you're using an Intel/AMD CPU, download `PortablePipeline-windows-amd64-vx.x.x.zip`.
If you're using an ARM-based CPU, download `PortablePipeline-windows-aarch64-vx.x.x.zip`.
After downloading, right-click the zip file and select **"Extract All..."**, then click **"Next"** in the dialog that appears.
**Do not extract the files into a folder with Japanese characters or spaces in its name.**

<img width="300" alt="image" src="https://github.com/user-attachments/assets/561d67ac-6d00-4f46-b612-8be663217394" />

<img width="252" alt="image" src="https://github.com/user-attachments/assets/52caadb8-f406-41ab-9cfd-83103756a7b5" />

![image](https://github.com/user-attachments/assets/28c8d5e0-b3d0-464a-9b8e-df72b3e1b275)

In the extracted folder, double-click on **`PortablePipeline.bat`** to start the application.

![image](https://github.com/user-attachments/assets/aae0f918-bc15-4ff3-8d97-dd33e0a05c3f)

On the first launch, Windows Defender may block the app. Click on **"More info"**:

![image](https://github.com/user-attachments/assets/779477bd-20a8-451b-8560-d3f38390030d)

Then click **"Run anyway"**.

![image](https://github.com/user-attachments/assets/83194a11-5f95-4495-b455-0b7bf737b620)

Since creating junction files on Windows requires administrator privileges, a prompt will appear asking for permission. Click **OK** to proceed.
*You can run the program without admin privileges, but note that it won’t be able to create junctions for input files. Instead, files will be copied, requiring more disk space.*

<img width="367" alt="image" src="https://github.com/user-attachments/assets/8354e5ea-bbf8-4037-b9f2-c430f7079509" />

#### Startup Screen

![image](https://github.com/user-attachments/assets/3f183d9d-739a-4a73-9cbd-8aecf4c1fe2f)


### For Mac Users (M1 and later)

Download `PortablePipeline-x.x.x.dmg`.

<img width="520" alt="image" src="https://github.com/user-attachments/assets/3eea167c-ed52-4169-a4b8-7196e6b76ff7" />

Open the downloaded DMG file and drag **`PortablePipeline`** into the **Applications** folder.

<img width="488" alt="image" src="https://github.com/user-attachments/assets/f3ffd060-62b8-4d16-b525-b986cc2aaa0f" />

When you try to launch PortablePipeline from the Applications folder:

<img width="582" alt="image" src="https://github.com/user-attachments/assets/adb0c8a0-716b-4698-8027-95b62b50dc16" />

<img width="247" alt="image" src="https://github.com/user-attachments/assets/bdf1d392-54ca-468a-aaa7-dd67a25c01a6" />

You may see an error saying **"PortablePipeline can't be opened"**. Click **"OK"**, then open **System Settings**, scroll down to **Privacy & Security**, and click **"Open Anyway"** next to the message indicating that PortablePipeline was blocked.

<img width="712" alt="image" src="https://github.com/user-attachments/assets/3a2275db-605a-4eda-a00e-845f0a911bdd" />

Another warning will appear — click **"Open"** again.

<img width="251" alt="image" src="https://github.com/user-attachments/assets/b0deb036-3e8c-4650-ae2f-2c46d1776767" />

Finally, you'll be prompted to authenticate using fingerprint or password.

<img width="239" alt="image" src="https://github.com/user-attachments/assets/d945ad9b-66d1-4b1a-b68e-e09c18441e64" />

## How to Execute Analysis

1.  Once the software starts, open the "Settings" tab and select whether to connect to a Linux server ("ssh"), a supercomputer ("ddbj", "shirokane"), run analysis on Windows alone ("WSL"), run analysis on Mac alone ("Mac"), or run directly on Linux ("Linux"). If connecting to a server, enter the required account information.
    An example of settings when using the DDBJ supercomputer is shown.

2.  Select the "Analysis Scripts" tab and choose the script you want to analyze. A screen will appear for entering input files and options. Click the button labeled "input\_1" or similar to select input files, change necessary options, and then click the "Run" button at the bottom of the screen to execute.
    As an example, here are the steps to run a test program that simply displays "Hello World" on the server.

3.  After the "Run" button is pressed, data will be transferred to the server, which will likely take some time. Progress should be displayed in a PowerShell or terminal window that opens separately from Java, showing the data transfer status. Once the status in "Job List" changes to "Running," you can close this software. If kept open, it will check the progress on the server every 30 seconds.
    As an example, here is the screen to check the server logs when a job is submitted, and
    a screenshot of PowerShell showing client-server file interaction.

4.  Once the Status is "finished", the analysis is complete. Click "open results" to view the analysis result file list.
    An example of the result file list is shown below. Since this was just displaying "Hello World" on the server, only a trace remains in the log.txt file, but if mapping or other operations were performed, bam files would be displayed here.

## Linux Server Setup Instructions

### For Rocky Linux 9

Setup method when using a GPU. Assumes NVIDIA drivers are installed. If not yet installed, refer to [https://www.server-world.info/query?os=CentOS\_7\&p=nvidia](https://www.server-world.info/query?os=CentOS_7&p=nvidia).

```
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
curl -s -L https://nvidia.github.io/nvidia-docker/centos8/nvidia-docker.repo | sudo tee /etc/yum.repos.d/nvidia-docker.repo
sudo dnf -y install nvidia-container-toolkit
sudo usermod -aG docker $USER
sudo systemctl start docker
sudo systemctl enable docker
sudo docker run -it --rm --gpus all nvcr.io/nvidia/clara/clara-parabricks:4.1.0-1 nvidia-smi #Check if GPU can be used inside docker
```

## WSL Server Setup Instructions

Basically, if you run Portable Pipeline in WSL mode, it will install the necessary tools, but you can also set up the following manually. Log in as a user in the Administrator group to execute. (For large input files, with WSL2 running on an HDD, you might encounter a bug-like behavior in WSL2 where file reading is interrupted without errors during steps where multiple processes read the same file simultaneously. Therefore, it might be better to install regular Linux on a virtual PC like Hyper-V).

Virtualization support (Intel VT or AMD-V) must be enabled in the BIOS. This is required to run WSL2. About half of commercially available PCs have this disabled by default.

1.  Install Windows Subsystem for Linux (WSL).

    Prepare Windows 10 or Windows 11 capable of running WSL2 or later. Basically, open PowerShell as an administrator (right-click the Windows logo in the lower-left corner -\> Windows PowerShell (Admin)) and run `wsl --install`.

2.  Restart Windows.

3.  After restarting, a screen like the one below will be displayed. Enter the username and password for the new account to be created in WSL. The password will not be displayed, but it is being entered, so press Enter after typing it.

    If an error occurs, you may need to update the Linux kernel. Follow the instructions on the following page to update:

    https://learn.microsoft.com/ja-jp/windows/wsl/install-manual\#step-4---download-the-linux-kernel-update-package

4.  Download Portable Pipeline from https://github.com/c2997108/OpenPortablePipeline/releases/download/v1.1.0/PortablePipeline-win-v1.1.0.zip and extract it.

5.  Double-click "PortablePipeline(.bat)" inside the extracted folder to launch it. When "Windows protected your PC" appears, click "More info".

6.  Click "Run".

7.  The User Account Control screen will appear; click "Yes (Allow)".

8.  Once Portable Pipeline launches, open the "Settings" tab, check "Preset: WSL," and enter the WSL username and password you just created.

## Mac Server Setup Instructions (M1 and later Macs)

If Rosetta is not installed, or if you are unsure whether it is installed, run the following command for now:

```
softwareupdate --install-rosetta --agree-to-license
```

Homebrew Installation

```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
(echo; echo 'eval "$(/opt/homebrew/bin/brew shellenv)"') >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

After installing Homebrew, install Podman with the following command:

```
brew install podman
# Download the podman virtual machine. Set CPU count and memory to match your PC. For volume, specify the absolute path of the folder you will use for analysis. (The example below is for 4 CPUs, 4GB memory, and analyzing in the current folder.)
podman machine init --cpus 4 --memory 4096 --volume "$PWD:$PWD"
# Start the podman virtual machine (do this every time after OS reboot)
podman machine start
        #To start a podman VM automatically at login, also install the cask
        #"podman-desktop".
```

Install the latest versions of essential tools like grep and awk on Mac:

```
brew install grep gawk gzip bash
brew install gnu-tar gnu-sed gnu-getopt

#xargs
brew install findutils
#parallel
brew install moreutils
#cat, ls, nproc
brew install coreutils

echo 'export PATH="/opt/homebrew/opt/grep/libexec/gnubin:$PATH"' >> ~/.bash_profile
echo 'export PATH="/opt/homebrew/opt/gawk/libexec/gnubin:$PATH"' >> ~/.bash_profile
echo 'export PATH="/opt/homebrew/bin:$PATH"' >> ~/.bash_profile

echo 'export PATH="/opt/homebrew/opt/gnu-sed/libexec/gnubin:$PATH"' >> ~/.bash_profile
echo 'export PATH="/opt/homebrew/opt/gnu-tar/libexec/gnubin:$PATH"' >> ~/.bash_profile
echo 'export PATH="/opt/homebrew/opt/gnu-getopt/bin:$PATH"' >> ~/.bash_profile

echo 'export PATH="/opt/homebrew/opt/findutils/libexec/gnubin:$PATH"' >> ~/.bash_profile
echo 'export PATH="/opt/homebrew/opt/coreutils/libexec/gnubin:$PATH"' >> ~/.bash_profile
```
