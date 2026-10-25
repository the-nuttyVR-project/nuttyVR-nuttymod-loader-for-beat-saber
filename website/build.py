#!/usr/bin/env python3
"""Build a standalone GitHub Pages site from checked-in source and signed APKs."""
from pathlib import Path
import hashlib
import shutil
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
PROJECT = ROOT.parent
sys.path.insert(0, str(PROJECT / 'tools'))
from setup_project import MODULES
from prepare_nightly import validate_snapshot, SNAPSHOT

# Website publishing never needs a private key or an Android toolchain.
record = validate_snapshot()
dist = ROOT / 'dist'
if dist.exists():
    shutil.rmtree(dist)
shutil.copytree(ROOT / 'src', dist)
downloads = dist / 'downloads'
downloads.mkdir()
for _, name in MODULES.values():
    shutil.copyfile(SNAPSHOT / name, downloads / name)
(downloads / 'SHA256SUMS').write_text(''.join(f"{record['apks'][name]}  {name}\n" for _, name in MODULES.values()))

source = downloads / 'nuttyVR-nuttymod-loader-0.1.0-source.zip'
excluded = {'.git', '.gradle', '.idea', '.openai', 'build', 'dist', 'signing', 'prebuilt', '__pycache__'}
with zipfile.ZipFile(source, 'w', zipfile.ZIP_DEFLATED) as archive:
    for file in sorted(PROJECT.rglob('*')):
        relative = file.relative_to(PROJECT)
        if file.is_file() and not set(relative.parts).intersection(excluded) and file.name not in {'local.properties', 'dist.zip'}:
            archive.write(file, Path('nuttyvr-nuttymod-loader') / relative)

bundle = downloads / 'nuttyVR-nuttymod-loader-0.1.0-APKs.zip'
with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as archive:
    for _, name in MODULES.values():
        archive.write(downloads / name, name)
    for name in ['README.md', 'VALIDATION.md', 'LICENSE', 'docs/GAME-INTEGRATION.md', 'docs/NIGHTLY.md', 'docs/WEBSITE.md', 'examples/mint-rush.nuttymod']:
        archive.write(PROJECT / name, name)
    archive.write(downloads / 'SHA256SUMS', 'SHA256SUMS')

for name in ['README.md', 'VALIDATION.md', 'LICENSE']:
    shutil.copyfile(PROJECT / name, downloads / name)
shutil.copyfile(PROJECT / 'examples/mint-rush.nuttymod', downloads / 'mint-rush.nuttymod')
(downloads / 'RELEASE-SHA256SUMS').write_text(''.join(f'{hashlib.sha256(file.read_bytes()).hexdigest()}  {file.name}\n' for file in [source, bundle]))
print('Built GitHub Pages site with eight verified APKs, source, checksums and nightly links.')
