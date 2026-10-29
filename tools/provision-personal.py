#!/usr/bin/env python3
"""Provision the owner's existing Gemini key over USB into an installed debug app.

No key is put in APKs, command arguments, logs or shared storage. The app encrypts
and deletes the one-time private setup file. Use Settings for normal manual setup.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--device',required=True,help='Explicit adb device serial')
default_adb = (Path(os.environ.get('LOCALAPPDATA', str(Path.home()/'AppData/Local'))) / 'Android/Sdk/platform-tools/adb.exe'
               if os.name == 'nt' else Path.home()/'Android/Sdk/platform-tools/adb')
parser.add_argument('--adb',default=str(default_adb))
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
props={}
local=root/'backend/local.properties'
if local.exists():
    for line in local.read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            name,value=line.split('=',1);props[name.strip()]=value.strip()
key=os.environ.get('GEMINI_API_KEY',props.get('GEMINI_API_KEY','')).strip()
model=os.environ.get('GEMINI_MODEL',props.get('GEMINI_MODEL','gemini-3.5-flash-lite')).strip()
if not key:raise SystemExit('No key configured in the environment or backend/local.properties')
prefix=[args.adb,'-s',args.device]
def adb(*command,**kwargs):return subprocess.run(prefix+list(command),check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,**kwargs)
try:
    adb('shell','run-as','com.example.dishroulette','id')
    adb('shell','am','force-stop','com.example.dishroulette')
    adb('shell','run-as','com.example.dishroulette','mkdir','-p','files')
    adb('shell','run-as','com.example.dishroulette','sh','-c',"'umask 077; cat > files/personal-gemini-setup.json'",input=json.dumps({'key':key,'model':model}).encode())
    adb('shell','am','start','-n','com.example.dishroulette/.MainActivity')
    for _ in range(20):
        time.sleep(.5)
        remains=subprocess.run(prefix+['shell','run-as','com.example.dishroulette','test','-f','files/personal-gemini-setup.json'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0
        if not remains:break
    if remains:raise RuntimeError('One-time setup was not consumed')
    encrypted=adb('exec-out','run-as','com.example.dishroulette','cat','no_backup/gemini-credentials.bin').stdout
    if len(encrypted)<32 or key.encode() in encrypted:raise RuntimeError('Encrypted credential verification failed')
    print('Personal Gemini key provisioned and encrypted on the selected device. Plaintext setup deleted.')
except (subprocess.CalledProcessError,RuntimeError):
    subprocess.run(prefix+['shell','run-as','com.example.dishroulette','rm','-f','files/personal-gemini-setup.json'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    raise SystemExit('Provisioning failed. Confirm the device is unlocked and the debug app is installed; use Settings to connect manually.')
