#!/usr/bin/env python3
"""Build an owner-only APK using the existing local Gemini key. Never upload it.

The key is extractable from this APK. Ordinary builds remain credential-free.
"""
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

root = Path(__file__).resolve().parents[1]
props = {}
local = root / 'backend/local.properties'
if local.exists():
    for line in local.read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            name, value = line.split('=', 1)
            props[name.strip()] = value.strip()
key = os.environ.get('GEMINI_API_KEY', props.get('GEMINI_API_KEY', '')).strip()
model = os.environ.get('GEMINI_MODEL', props.get('GEMINI_MODEL', 'gemini-3.5-flash-lite')).strip()
if not key:
    raise SystemExit('No existing local Gemini key found.')
if not re.fullmatch(r'[A-Za-z0-9_.-]{20,256}', key):
    raise SystemExit('Existing key has an unsupported format; no private APK built.')
if not re.fullmatch(r'gemini-[A-Za-z0-9.-]{1,70}', model):
    raise SystemExit('Existing model has an unsupported format; no private APK built.')
apk = root / 'app/build/outputs/apk/debug/app-debug.apk'
output = root / 'work/private/DishRoulette-personal.apk'
output.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
with tempfile.TemporaryDirectory(prefix='dishroulette-private-') as private:
    setup = Path(private) / 'private-gemini-setup.json'
    setup.write_text(json.dumps({'key': key, 'model': model}))
    setup.chmod(0o600)
    try:
        subprocess.run([str(root / 'gradlew'), ':app:assembleDebug', '--no-configuration-cache',
                        '-PprivateSetupAssetsDir=' + private], cwd=root, check=True)
        with zipfile.ZipFile(apk) as archive:
            data = json.loads(archive.read('assets/private-gemini-setup.json'))
            if data != {'key': key, 'model': model}:
                raise RuntimeError('Private APK bootstrap verification failed')
        shutil.copyfile(apk, output)
        output.chmod(0o600)
    finally:
        # Restore ordinary build output so release commands cannot pick up this private APK.
        apk.unlink(missing_ok=True)
        subprocess.run([str(root / 'gradlew'), ':app:clean', ':app:assembleDebug',
                        '--no-configuration-cache'], cwd=root, check=True)
        with zipfile.ZipFile(apk) as archive:
            if 'assets/private-gemini-setup.json' in archive.namelist():
                apk.unlink()
                raise RuntimeError('Public build unexpectedly contains private bootstrap')
print('Private APK created in work/private/DishRoulette-personal.apk. Do not share or upload it.')
