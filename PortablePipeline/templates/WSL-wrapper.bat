wsl bash -c "cat /proc/cmdline |grep vsyscall=emulate"
if not "%ERRORLEVEL%" == "0" (
 echo [wsl2] >> %USERPROFILE%\\.wslconfig
 echo kernelCommandLine = vsyscall=emulate >> %USERPROFILE%\\.wslconfig
 wsl --shutdown
)
wsl -l
if not "%ERRORLEVEL%" == "0" (
 echo INSTALLING WSL... After the WSL installation, restart your PC and enter your new user name and password.If you get an error, you probably need to turn on the virtualization support function in the BIOS.
 powershell.exe start-process cmd -verb runas -ArgumentList '/K \"wsl --install -d Ubuntu\"'
)
set i=1
:WAIT
wsl bash -c "cat /proc/cmdline |grep vsyscall=emulate"
if not "%ERRORLEVEL%" == "0" (
 echo Waiting for Ubuntu installation
 timeout 10
 set /a i+=1
 if %i% leq 60 goto WAIT
)
wsl bash -c "cat /proc/cmdline |grep vsyscall=emulate"
if not ERRORLEVEL 0 (
 mkdir %LOCALAPPDATA%\PPUbuntu20
 powershell -Command "Invoke-WebRequest http://suikou.fs.a.u-tokyo.ac.jp/pp/PPUbuntu20.tar -outFile PPUbuntu20.tar"
 wsl --import PPUbuntu20 %LOCALAPPDATA%/PPUbuntu20 PPUbuntu20.tar --version 2
 wsl -s PPUbuntu20
)
powershell.exe start-process bash -Wait -ArgumentList '-c \"cd @wslcurDir@; echo @password@ |sudo -S bash WSL-setup.sh\"'
powershell.exe start-process bash -ArgumentList '-c \"echo Do not close this window.; echo @password@ |sudo -S bash service docker start; bash wrapper.sh\"'


