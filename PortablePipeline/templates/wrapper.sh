#!/bin/bash
#$ -S /bin/bash
#$ -cwd
#$ -pe def_slot 1
#$ -l mem_req=8G,s_vmem=8G
bash jellyfish.sh '8' '8' 'input_1' > log.txt 2>&1
