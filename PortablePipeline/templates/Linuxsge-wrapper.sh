#!/bin/bash
export DIR_IMG="@imagefolder@"
source ~/.bashrc
chmod 755 ./pp.py
chmod 755 "@selectedScript@"
nohup ./pp.py -s -g "@selectedScript@" @runcmd@ > log.txt 2>&1 &
echo $! > save_pid.txt
