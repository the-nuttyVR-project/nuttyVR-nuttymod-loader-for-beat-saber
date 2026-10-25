#!/usr/bin/env python3
"""Check delivered APK identities, trust, embedding, SDK limits and registry boundaries."""
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / 'tools'))
from setup_project import MODULES
SDK = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Android/Sdk')))
JAVA = Path(os.environ.get('JAVA_HOME', str(Path.home() / 'dev-tools/java-22')))
BT = SDK / 'build-tools' / os.environ.get('NUTTYVR_BUILD_TOOLS', '34.0.0')
env = dict(os.environ, JAVA_HOME=str(JAVA))
env['PATH'] = str(JAVA / 'bin') + os.pathsep + env.get('PATH', '')
certs = set()
checks = 0

def check(value, message):
    global checks
    if not value:
        raise AssertionError(message)
    checks += 1

for role, (_, filename) in MODULES.items():
    apk = ROOT / 'dist' / filename
    result = subprocess.check_output([str(BT / 'apksigner'), 'verify', '--print-certs', str(apk)], env=env, text=True)
    digest = re.search(r'certificate SHA-256 digest: (\w+)', result)
    check(digest is not None, f'{role} is signed')
    certs.add(digest.group(1))
    data = subprocess.check_output([str(BT / 'aapt2'), 'dump', 'badging', str(apk)], env=env, text=True)
    check(f"package: name='com.nuttyvr.{role}'" in data, f'{role} package identity')
    check("versionName='0.1.0'" in data and "versionCode='1'" in data, f'{role} version')
    # Newer AAPT2 versions label this minSdkVersion instead of sdkVersion.
    minimum = re.search(r"(?m)^(?:minSdkVersion|sdkVersion):\s*['\"]?(\d+)['\"]?\s*$", data)
    target = re.search(r"(?m)^targetSdkVersion:\s*['\"]?(\d+)['\"]?\s*$", data)
    check(minimum is not None and minimum.group(1) == '29' and target is not None and target.group(1) == '34', f'{role} SDK metadata: {data}')
    check("launchable-activity: name='com.nuttyvr.common." in data, f'{role} has launcher')
    with zipfile.ZipFile(apk) as archive:
        check(archive.testzip() is None and 'classes.dex' in archive.namelist(), f'{role} ZIP and dex')
        children = ['loader'] if role == 'hub' else ['rootmode', 'rootmodelib', 'nuttymod', 'vnedruntime', 'nuttymodruntime'] if role == 'loader' else []
        for child in children:
            child_name = MODULES[child][1]
            check(archive.read('assets/apks/' + child_name) == (ROOT / 'dist' / child_name).read_bytes(), f'{role} embeds exact signed {child}')
    manifest = ET.parse(ROOT / role / 'src/main/AndroidManifest.xml').getroot()
    android = '{http://schemas.android.com/apk/res/android}'
    for provider in manifest.findall('application/provider'):
        if provider.get(android + 'exported') == 'true':
            check(provider.get(android + 'permission') in {'com.nuttyvr.permission.MODS', 'com.nuttyvr.permission.ROOT_STATUS'}, f'{role} provider protected')
    for permission in manifest.findall('permission'):
        check(permission.get(android + 'protectionLevel') == 'signature', f'{role} private permissions')
check(len(certs) == 1, 'All APKs share one signing identity')
for line in (ROOT / 'dist/SHA256SUMS').read_text().splitlines():
    digest, filename = line.split('  ', 1)
    check(hashlib.sha256((ROOT / 'dist' / filename).read_bytes()).hexdigest() == digest, f'Checksum: {filename}')
print(f'{checks} APK packaging and trust checks passed.')
