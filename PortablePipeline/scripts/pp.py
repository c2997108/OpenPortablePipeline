#!/usr/bin/env python3

import signal
import os
import subprocess
import sys
import re

# This script will work with python 2.7 or later, 3.x is OK. Python 2.6 can not stop the running command with signal.

def handler(signum, frame):
    #print(os.getpid())
    print("signal={}".format(signum))
    args = ['pstree', '-pla', str(os.getpid())]
    args2 = ['pstree', '-la', str(os.getpid())]

    try:
        o = subprocess.check_output(args)
        o2 = subprocess.check_output(args2)
        print(o)
        #for singularity
        for i in o.decode().strip().split('\n'):
            m = re.match(r'.*,([0-9]+)[^0-9]*', i)
            try:
                print("kill "+m.group(1))
                if str(os.getpid()) != m.group(1):
                    os.kill(int(m.group(1)), signal.SIGKILL)
            except:
                1
        #for docker on tty
        for i in o2.decode().strip().split('\n'):
            m = re.match(r'.*docker run --name ([^ ]+)', i)
            try:
                print("docker stop "+m.group(1))
                proc = subprocess.Popen(["docker", "stop", "-t", "10", m.group(1)])
            except:
                1
        #for docker on sge or tty
        try:
            with open("pp-docker-list", "r") as tf:
                lines = tf.read().split('\n')
            for i in lines:
                try:
                    print("docker stop "+i)
                    proc = subprocess.Popen(["docker", "stop", "-t", "10", i])
                except:
                    1
        except:
            1
        #for podman on tty
        for i in o2.decode().strip().split('\n'):
            m = re.match(r'.*podman run --name ([^ ]+)', i)
            try:
                print("podman stop "+m.group(1))
                proc = subprocess.Popen(["podman", "stop", "-t", "10", m.group(1)])
            except:
                1
        #for podman on sge or tty
        try:
            with open("pp-podman-list", "r") as tf:
                lines = tf.read().split('\n')
            for i in lines:
                try:
                    print("podman stop "+i)
                    proc = subprocess.Popen(["podman", "stop", "-t", "10", i])
                except:
                    1
        except:
            1
    except TypeError as e:
        print('catch TypeError:', e)
    sys.exit()

signal.signal(signal.SIGTERM, handler)
signal.signal(signal.SIGINT, handler)
signal.signal(signal.SIGUSR2, handler)

if len(sys.argv) == 1:
    subprocess.check_call('ls '+os.path.dirname(os.path.realpath(__file__))+'|grep "~"|column', shell=True)
    sys.exit()

try:
    print("PID: "+str(os.getpid()))
    #to kill a job in direct mode of pp
    with open("pp-pid", mode='w') as f:
        f.write(str(os.getpid()))
except:
    print("pp setup error.")

try:
    argv2 = sys.argv[1:]
    my_env = os.environ.copy()
    while argv2[0] == "-s" or argv2[0] == "-g":
      if argv2[0] == "-s":
        my_env["PP_USE_SING"]="y"
        argv2 = argv2[1:]
      if argv2[0] == "-g":
        my_env["PP_USE_PARALLEL_NO_SVMEM"]="y"
        argv2 = argv2[1:]
    argv2[0]=os.path.dirname(os.path.realpath(__file__))+"/"+argv2[0]
    #print(argv2)
    res = subprocess.check_call(argv2, env=my_env)
except:
    print("pp runtime error.")
