#!/bin/bash
export PATH=/usr/local/bin:${PATH}
# Homebrewで入れたGNU coreutils/sedがあれば使う（Intelは/usr/local、Apple Siliconは/opt/homebrew）
for pp_gnu_dir in /opt/homebrew/opt/gnu-sed/libexec/gnubin /opt/homebrew/opt/coreutils/libexec/gnubin /usr/local/opt/gnu-sed/libexec/gnubin /usr/local/opt/coreutils/libexec/gnubin; do
 if [ -d "$pp_gnu_dir" ]; then export PATH="$pp_gnu_dir:${PATH}"; fi
done
source ~/.bash_profile
chmod 755 ./pp.py
chmod 755 "@selectedScript@"
nohup ./pp.py "@selectedScript@" @runcmd@ > log.txt 2>&1 &
echo $! > save_pid.txt
