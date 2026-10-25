#!/usr/bin/env python3
"""Publish verifiable signed snapshots until private CI signing is configured."""
import argparse
import base64
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

from setup_project import MODULES, ROOT

SNAPSHOT = ROOT / 'prebuilt/nightly'
DIST = ROOT / 'dist'


def source_digest(root=ROOT):
    paths = [root / 'build.gradle', root / 'settings.gradle', root / 'gradle.properties',
             root / 'tools/build_apks.py', root / 'tools/setup_project.py']
    for module in ['common', *MODULES]:
        paths.append(root / module / 'build.gradle')
        paths.extend(file for file in (root / module / 'src').rglob('*') if file.is_file())
    digests = {path.relative_to(root).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest() for path in sorted(set(paths))}
    return hashlib.sha256(json.dumps(digests, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def signer(apk):
    sdk = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Android/Sdk')))
    java = Path(os.environ.get('JAVA_HOME', str(Path.home() / 'dev-tools/java-22')))
    env = dict(os.environ, JAVA_HOME=str(java))
    env['PATH'] = str(java / 'bin') + os.pathsep + env.get('PATH', '')
    command = sdk / 'build-tools' / os.environ.get('NUTTYVR_BUILD_TOOLS', '34.0.0') / 'apksigner'
    result = subprocess.run([str(command), 'verify', '--print-certs', str(apk)], check=True, capture_output=True, text=True, env=env)
    matches = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]{64})', result.stdout)
    if len(matches) != 1:
        raise ValueError('Expected exactly one APK signer')
    return matches[0].lower()


def record():
    value = json.loads((SNAPSHOT / 'build.json').read_text())
    if value.get('schema') != 'nuttyvr-nightly/1' or not re.fullmatch('[0-9a-f]{64}', value.get('signerSha256', '')):
        raise ValueError('Invalid signed snapshot record')
    return value


def validate_snapshot(folder=SNAPSHOT, root=ROOT):
    value = json.loads((folder / 'build.json').read_text())
    if value.get('schema') != 'nuttyvr-nightly/1' or value.get('sourceSha256') != source_digest(root):
        raise ValueError('Snapshot does not match the Android source. Configure private CI signing or regenerate the signed snapshot.')
    expected = {name for _, name in MODULES.values()}
    if set(value.get('apks', {})) != expected:
        raise ValueError('Snapshot must contain exactly the eight suite APKs')
    for name in sorted(expected):
        apk = folder / name
        if not apk.is_file() or hashlib.sha256(apk.read_bytes()).hexdigest() != value['apks'][name]:
            raise ValueError('Snapshot checksum mismatch: ' + name)
    if not re.fullmatch('[0-9a-f]{64}', value.get('signerSha256', '')):
        raise ValueError('Invalid snapshot signer fingerprint')
    return value


def verify_signers(folder, expected):
    for _, name in MODULES.values():
        if signer(folder / name) != expected:
            raise ValueError('Signing certificate does not match the initial nightly: ' + name)


def snapshot():
    identities = {signer(DIST / name) for _, name in MODULES.values()}
    if len(identities) != 1:
        raise ValueError('All APKs must use the same signing identity')
    value = {'schema': 'nuttyvr-nightly/1', 'version': '0.1.0', 'sourceSha256': source_digest(),
             'signerSha256': identities.pop(), 'apks': {name: hashlib.sha256((DIST / name).read_bytes()).hexdigest() for _, name in MODULES.values()}}
    SNAPSHOT.mkdir(parents=True, exist_ok=True)
    for _, name in MODULES.values():
        shutil.copyfile(DIST / name, SNAPSHOT / name)
    (SNAPSHOT / 'build.json').write_text(json.dumps(value, indent=2) + '\n')
    print('Prepared eight signed APK snapshots with Android-source and signer fingerprints.')


def provenance(mode, identity):
    DIST.mkdir(exist_ok=True)
    (DIST / 'NIGHTLY.json').write_text(json.dumps({
        'project': 'nuttyVR nuttymod loader', 'version': '0.1.0', 'mode': mode,
        'commit': os.environ.get('GITHUB_SHA', 'local'), 'sourceSha256': source_digest(),
        'signerSha256': identity, 'preparedAt': datetime.now(timezone.utc).isoformat(),
        'beatSaberInjectionImplemented': False, 'grantsRoot': False,
    }, indent=2) + '\n')


def restore():
    value = validate_snapshot()
    verify_signers(SNAPSHOT, value['signerSha256'])
    DIST.mkdir(exist_ok=True)
    for _, name in MODULES.values():
        shutil.copyfile(SNAPSHOT / name, DIST / name)
    (DIST / 'SHA256SUMS').write_text(''.join(f"{value['apks'][name]}  {name}\n" for _, name in MODULES.values()))
    provenance('verified-signed-snapshot', value['signerSha256'])
    print('Verified source fingerprint, APK bytes and certificates; restored the signed snapshot.')


def prepare():
    encoded = os.environ.get('NUTTYMOD_KEYSTORE_BASE64', '')
    password = os.environ.get('NUTTYMOD_KEY_PASSWORD', '')
    if bool(encoded) != bool(password):
        raise ValueError('Configure both NUTTYMOD_KEYSTORE_BASE64 and NUTTYMOD_KEY_PASSWORD, or neither.')
    if not encoded:
        restore()
        return
    # Private signing material exists only in a temporary directory and subprocess environment.
    with tempfile.TemporaryDirectory(prefix='nuttymod-signing-', dir=os.environ.get('RUNNER_TEMP')) as folder:
        key = Path(folder) / 'nightly.keystore'
        key.write_bytes(base64.b64decode(encoded, validate=True))
        key.chmod(0o600)
        env = dict(os.environ, NUTTYMOD_KEYSTORE=str(key))
        subprocess.run(['python3', str(ROOT / 'tools/build_apks.py')], check=True, env=env)
    identity = record()['signerSha256']
    verify_signers(DIST, identity)
    provenance('built-from-source', identity)
    print('Rebuilt from source using the existing private signing identity.')


def stage():
    target = ROOT / 'build/nightly'
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True)
    for _, name in MODULES.values():
        shutil.copyfile(DIST / name, target / name)
    for name in ['SHA256SUMS', 'RELEASE-SHA256SUMS', 'NIGHTLY.json',
                 'nuttyVR-nuttymod-loader-0.1.0-source.zip', 'nuttyVR-nuttymod-loader-0.1.0-APKs.zip']:
        shutil.copyfile(DIST / name, target / name)
    for name in ['README.md', 'VALIDATION.md', 'docs/NIGHTLY.md', 'examples/mint-rush.nuttymod']:
        shutil.copyfile(ROOT / name, target / Path(name).name)
    print('Staged complete nightly toolkit.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['snapshot', 'validate', 'restore', 'prepare', 'stage'])
    args = parser.parse_args()
    try:
        if args.command == 'validate':
            value = validate_snapshot(); verify_signers(SNAPSHOT, value['signerSha256']); print('Snapshot is valid.')
        else:
            globals()[args.command]()
    except (ValueError, FileNotFoundError) as error:
        raise SystemExit(str(error))
