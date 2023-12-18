#!/bin/bash
#$ -S /bin/bash
#$ -notify
#$ -cwd
#$ -pe def_slot @numCPU@
#$ -l mem_req=@numMEM_1core@G,s_vmem=@numMEM_1core@G
trap 'sleep 13; echo Detected SIGUSR2' SIGUSR2
export DIR_IMG="@imagefolder@"
source ~/.bashrc
chmod 755 ./pp.py
chmod 755 "@selectedScript@"
./pp.py "@selectedScript@" @runcmd@ > log.txt 2>&1
