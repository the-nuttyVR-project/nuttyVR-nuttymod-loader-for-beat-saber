#!/usr/bin/env python3
"""Dependency-free Android SDK builder, usable without a Gradle daemon."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from setup_project import MODULES, ROOT

SDK = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Android/Sdk')))
JAVA = Path(os.environ.get('JAVA_HOME', str(Path.home() / 'dev-tools/java-22')))
BT = SDK / 'build-tools' / os.environ.get('NUTTYVR_BUILD_TOOLS', '34.0.0')
PLATFORM = SDK / 'platforms/android-35/android.jar'
BUILD = ROOT / 'build/sdk'
DIST = ROOT / 'dist'
KEY = Path(os.environ.get('NUTTYMOD_KEYSTORE', str(ROOT / 'signing/debug.keystore')))
ALIAS = os.environ.get('NUTTYMOD_KEY_ALIAS', 'androiddebugkey')
ENV = dict(os.environ, JAVA_HOME=str(JAVA))
ENV['PATH'] = str(JAVA / 'bin') + os.pathsep + ENV.get('PATH', '')
ENV.setdefault('NUTTYMOD_KEY_PASSWORD', 'android')

def run(*args):
    return subprocess.run([str(arg) for arg in args], env=ENV, check=True)

def build():
    if not PLATFORM.is_file() or not (JAVA / 'bin/javac').is_file():
        raise SystemExit('Set ANDROID_HOME to SDK Platform 35 and JAVA_HOME to JDK 17 or newer.')
    for module in MODULES:
        if not (ROOT / module / 'src/main/AndroidManifest.xml').is_file():
            raise SystemExit('Missing tracked manifest. Run python3 tools/setup_project.py first.')
    if not KEY.is_file():
        if 'NUTTYMOD_KEYSTORE' in os.environ:
            raise SystemExit('NUTTYMOD_KEYSTORE does not exist.')
        KEY.parent.mkdir(parents=True, exist_ok=True)
        run(JAVA / 'bin/keytool', '-genkeypair', '-noprompt', '-keystore', KEY, '-alias', ALIAS,
            '-storepass:env', 'NUTTYMOD_KEY_PASSWORD', '-keypass:env', 'NUTTYMOD_KEY_PASSWORD',
            '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000', '-dname', 'CN=nuttyVR Development')
    if BUILD.exists():
        shutil.rmtree(BUILD)
    for folder in (BUILD / 'generated', BUILD / 'classes', BUILD / 'dex', DIST):
        folder.mkdir(parents=True, exist_ok=True)
    run(BT / 'aapt2', 'compile', '--dir', ROOT / 'common/src/main/res', '-o', BUILD / 'resources.zip')
    android = 'http://schemas.android.com/apk/res/android'
    ET.register_namespace('android', android)
    # Link each application while generating a shared R namespace for common code.
    order = ['rootmode', 'rootmodelib', 'nuttymod', 'vnedruntime', 'nuttymodruntime', 'demo', 'loader', 'hub']
    for role in order:
        print(f'Building {MODULES[role][1]}', flush=True)
        out = BUILD / role; out.mkdir()
        manifest = ET.parse(ROOT / role / 'src/main/AndroidManifest.xml')
        manifest.getroot().set('package', f'com.nuttyvr.{role}')
        manifest.getroot().insert(0, ET.Element('uses-sdk', {f'{{{android}}}minSdkVersion': '29', f'{{{android}}}targetSdkVersion': '34'}))
        manifest.write(out / 'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
        assets = out / 'assets'; assets.mkdir()
        bundled = ['loader'] if role == 'hub' else ['rootmode', 'rootmodelib', 'nuttymod', 'vnedruntime', 'nuttymodruntime'] if role == 'loader' else []
        if bundled:
            (assets / 'apks').mkdir()
            for dependency in bundled:
                shutil.copyfile(DIST / MODULES[dependency][1], assets / 'apks' / MODULES[dependency][1])
        run(BT / 'aapt2', 'link', '-o', out / 'resources.apk', '--manifest', out / 'AndroidManifest.xml',
            '-I', PLATFORM, '--java', BUILD / 'generated', '--custom-package', 'com.nuttyvr.common',
            '--min-sdk-version', '29', '--target-sdk-version', '34', '--version-code', '1', '--version-name', '0.1.0',
            '-A', assets, BUILD / 'resources.zip')
        if role == order[0]:
            sources = sorted((ROOT / 'common/src/main/java').rglob('*.java')) + sorted((BUILD / 'generated').rglob('*.java'))
            run(JAVA / 'bin/javac', '-g', '-encoding', 'UTF-8', '--release', '8', '-classpath', PLATFORM, '-d', BUILD / 'classes', *sources)
            run(JAVA / 'bin/jar', 'cf', BUILD / 'classes.jar', '-C', BUILD / 'classes', '.')
            d8_args = ['--release', '--min-api', '29', '--lib', PLATFORM, '--output', BUILD / 'dex', BUILD / 'classes.jar']
            cached = sorted((Path.home() / '.gradle/caches/modules-2/files-2.1/com.android.tools.build/builder/8.7.3').glob('*/*.jar'))
            d8 = os.environ.get('NUTTYMOD_D8_JAR') or (str(cached[0]) if cached else None)
            if d8:
                run(JAVA / 'bin/java', '-cp', d8, 'com.android.tools.r8.D8', *d8_args)
            else:
                run(BT / 'd8', *d8_args)
        shutil.copyfile(out / 'resources.apk', out / 'unsigned.apk')
        with zipfile.ZipFile(out / 'unsigned.apk', 'a', compression=zipfile.ZIP_DEFLATED) as archive:
            for dex in sorted((BUILD / 'dex').glob('*.dex')):
                archive.write(dex, dex.name)
        run(BT / 'zipalign', '-f', '4', out / 'unsigned.apk', out / 'aligned.apk')
        run(BT / 'apksigner', 'sign', '--ks', KEY, '--ks-key-alias', ALIAS,
            '--ks-pass', 'env:NUTTYMOD_KEY_PASSWORD', '--key-pass', 'env:NUTTYMOD_KEY_PASSWORD',
            '--v1-signing-enabled', 'false', '--v2-signing-enabled', 'true', '--v3-signing-enabled', 'true',
            '--out', DIST / MODULES[role][1], out / 'aligned.apk')
        run(BT / 'apksigner', 'verify', '--verbose', DIST / MODULES[role][1])
        run(BT / 'zipalign', '-c', '4', DIST / MODULES[role][1])
    (DIST / 'SHA256SUMS').write_text(''.join(f'{hashlib.sha256((DIST / name).read_bytes()).hexdigest()}  {name}\n' for _, name in MODULES.values()))
    print('Eight signed APKs built. See dist/SHA256SUMS.')

if __name__ == '__main__':
    build()
