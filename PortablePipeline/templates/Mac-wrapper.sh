#!/bin/bash
export PATH=/usr/local/bin:${PATH}
export PATH=/usr/local/opt/coreutils/libexec/gnubin:${PATH}
source ~/.bash_profile
chmod 755 ./pp.py
chmod 755 "@selectedScript@"
nohup ./pp.py "@selectedScript@" @runcmd@ > log.txt 2>&1 &
echo $! > save_pid.txt
