#!/usr/bin/env python3
from pathlib import Path
import shutil
import sys

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent
sys.path.insert(0, str(PROJECT / 'tools'))
from setup_project import MODULES

dist = ROOT / 'dist'
if dist.exists():
    shutil.rmtree(dist)
shutil.copytree(ROOT / 'src', dist)
downloads = dist / 'downloads'
downloads.mkdir()
for _, name in MODULES.values():
    shutil.copyfile(PROJECT / 'dist' / name, downloads / name)
for name in ['nuttyVR-nuttymod-loader-0.1.0-source.zip', 'nuttyVR-nuttymod-loader-0.1.0-APKs.zip', 'SHA256SUMS']:
    shutil.copyfile(PROJECT / 'dist' / name, downloads / name)
shutil.copyfile(PROJECT / 'README.md', downloads / 'README.md')
shutil.copyfile(PROJECT / 'examples/mint-rush.nuttymod', downloads / 'mint-rush.nuttymod')
print('Built download page with all APKs, source and release bundle.')
