#!/bin/bash
#SBATCH --cpus-per-task @numCPU@
#SBATCH --mem @numMEM_total@g
#SBATCH -t 60-00:00:00
#SBATCH -p epyc,rome,medium

trap 'sleep 13; echo Detected SIGUSR2' SIGUSR2
echo JOBID: $SLURM_JOB_ID
echo HOSTNAME: $HOSTNAME
echo CPU: $SLURM_CPUS_PER_TASK
echo MEM: $SLURM_MEM_PER_NODE MB
date
export DIR_IMG="@imagefolder@"
source ~/.bashrc
chmod 755 ./pp.py
chmod 755 "@selectedScript@"
export RUNONSINGLENODE=y
export PATH="$PATH":/opt/pkg/singularity-ce/4.2.1/bin/
./pp.py "@selectedScript@" @runcmd@ > log.txt 2>&1

