#!/usr/bin/env python3

import signal
import os
import subprocess
import sys
import re
import shlex

# This script will work with python 2.7 or later, 3.x is OK. Python 2.6 can not stop the running command with signal.

def handler(signum, frame):
    #print(os.getpid())
    print("signal={}".format(signum))
    args = ['pstree', '-pla', str(os.getpid())]
    args2 = ['pstree', '-la', str(os.getpid())]

    try:
        o = subprocess.check_output(args)
        o2 = subprocess.check_output(args2)
        print(o) #the result of "pstree -pla [pp.py ID]"

        #for sge
        try:
            f = open('qsub.log', 'r')
            datalist = f.readlines()
            for data in datalist:
                print(data)
                jobid = data.split(' ')
                if len(jobid)>2 and jobid[2].isdigit():
                    try:
                        proc = subprocess.Popen(["qdel", jobid[2]])
                    except TypeError as e:
                        print('qdel TypeError:', e)
            f.close()
        except:
            print('qsub.log might not exist')

        #for docker on tty
        for i in o2.decode().strip().split('\n'):
            m = re.match(r'.*docker run --name ([^ ]+)', i)
            try:
                print("docker stop -t 10 "+m.group(1))
                proc = subprocess.Popen(["docker", "stop", "-t", "10", m.group(1)])
            except:
                1

        #for docker on sge or tty
        #however docker jobs on other hosts by sge can not be killed
        try:
            with open("pp-docker-list", "r") as tf:
                lines = tf.read().split('\n')
            for i in lines:
                try:
                    print("docker stop -t 10 "+i)
                    proc = subprocess.Popen(["docker", "stop", "-t", "10", i])
                except:
                    1
        except:
            print('pp-docker-list might not exist')

        #for podman on tty
        for i in o2.decode().strip().split('\n'):
            m = re.match(r'.*podman run --name ([^ ]+)', i)
            try:
                print("podman stop -t 10 "+m.group(1))
                proc = subprocess.Popen(["podman", "stop", "-t", "10", m.group(1)])
            except:
                1

        #for podman on sge or tty
        #however podman jobs on other hosts by sge can not be killed
        try:
            with open("pp-podman-list", "r") as tf:
                lines = tf.read().split('\n')
            for i in lines:
                try:
                    print("podman stop -t 10 "+i)
                    proc = subprocess.Popen(["podman", "stop", "-t", "10", i])
                except:
                    1
        except:
            print('pp-podman-list might not exist')

        #for singularity and pp.py process
        for i in o.decode().strip().split('\n'):
            m = re.match(r'.*,([0-9]+)[^0-9]*', i)
            try:
                print("kill "+m.group(1))
                if str(os.getpid()) != m.group(1):
                    os.kill(int(m.group(1)), signal.SIGKILL)
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
    log_file = open("pp_log.txt", "w")
    escaped_args = [shlex.quote(arg) for arg in sys.argv]
    argv_str = " ".join(escaped_args)
    print(argv_str)
    log_file.write(argv_str+"\n")

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

    process = subprocess.Popen(argv2, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=my_env)
    while process.poll() is None:
        output = process.stdout.readline()
        print(output.decode().strip(), flush=True)
        log_file.write(output.decode())
    #print('status:',process.poll())
except:
    print("pp runtime error.")
