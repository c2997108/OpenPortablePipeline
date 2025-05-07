#!/bin/bash
source ~/.bashrc
chmod 755 ./pp.py
chmod 755 "@selectedScript@"
nohup ./pp.py "@selectedScript@" @runcmd@ > log.txt 2>&1 &
echo $! > save_pid.txt
wait
