#!/usr/bin/env python3
from pathlib import Path
import hashlib
import zipfile
from setup_project import MODULES, ROOT

dist = ROOT / 'dist'
excluded = {'.git', '.gradle', '.idea', '.openai', 'build', 'dist', 'signing', 'prebuilt', '__pycache__'}
source = dist / 'nuttyVR-nuttymod-loader-0.1.0-source.zip'
with zipfile.ZipFile(source, 'w', zipfile.ZIP_DEFLATED) as archive:
    for file in sorted(ROOT.rglob('*')):
        relative = file.relative_to(ROOT)
        if file.is_file() and not set(relative.parts).intersection(excluded) and file.name not in {'local.properties', 'dist.zip'}:
            archive.write(file, Path('nuttyvr-nuttymod-loader') / relative)
bundle = dist / 'nuttyVR-nuttymod-loader-0.1.0-APKs.zip'
with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as archive:
    for _, name in MODULES.values():
        archive.write(dist / name, name)
    for name in ('README.md', 'VALIDATION.md', 'LICENSE', 'examples/mint-rush.nuttymod', 'docs/GAME-INTEGRATION.md', 'docs/NIGHTLY.md'):
        archive.write(ROOT / name, name)
    archive.write(dist / 'SHA256SUMS', 'SHA256SUMS')
(dist / 'RELEASE-SHA256SUMS').write_text(''.join(f'{hashlib.sha256(file.read_bytes()).hexdigest()}  {file.name}\n' for file in (source, bundle)))
print(f'{source.name}: {source.stat().st_size:,} bytes')
print(f'{bundle.name}: {bundle.stat().st_size:,} bytes')
