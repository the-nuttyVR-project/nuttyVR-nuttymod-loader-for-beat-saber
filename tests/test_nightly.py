#!/usr/bin/env python3
"""Exercise snapshot rejection paths without changing the actual APKs or source."""
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / 'tools'))
import prepare_nightly as nightly


class SnapshotTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.source_hash = nightly.source_digest()
        self.record = json.loads((nightly.SNAPSHOT / 'build.json').read_text())
        for _, name in nightly.MODULES.values():
            (self.folder / name).write_bytes(('fixture:' + name).encode())
            self.record['apks'][name] = hashlib.sha256((self.folder / name).read_bytes()).hexdigest()
        self.save()

    def save(self):
        (self.folder / 'build.json').write_text(json.dumps(self.record))

    def validate(self):
        with patch.object(nightly, 'source_digest', return_value=self.source_hash):
            return nightly.validate_snapshot(self.folder)

    def test_accept_matching_complete_snapshot(self):
        self.assertEqual(self.validate()['sourceSha256'], self.source_hash)

    def test_reject_changed_android_source(self):
        with patch.object(nightly, 'source_digest', return_value='0' * 64):
            with self.assertRaisesRegex(ValueError, 'does not match'):
                nightly.validate_snapshot(self.folder)

    def test_reject_modified_apk(self):
        name = next(iter(self.record['apks']))
        (self.folder / name).write_bytes(b'tampered')
        with self.assertRaisesRegex(ValueError, 'checksum mismatch'):
            self.validate()

    def test_reject_missing_apk(self):
        (self.folder / next(iter(self.record['apks']))).unlink()
        with self.assertRaisesRegex(ValueError, 'checksum mismatch'):
            self.validate()

    def test_reject_unexpected_file_in_record(self):
        self.record['apks']['../outside.apk'] = '0' * 64
        self.save()
        with self.assertRaisesRegex(ValueError, 'exactly the eight'):
            self.validate()

    def test_reject_missing_file_in_record(self):
        self.record['apks'].pop(next(iter(self.record['apks'])))
        self.save()
        with self.assertRaisesRegex(ValueError, 'exactly the eight'):
            self.validate()

    def test_reject_invalid_signer(self):
        self.record['signerSha256'] = 'not-a-certificate'
        self.save()
        with self.assertRaisesRegex(ValueError, 'signer fingerprint'):
            self.validate()

    def test_reject_certificate_mismatch(self):
        with patch.object(nightly, 'signer', return_value='0' * 64):
            with self.assertRaisesRegex(ValueError, 'certificate does not match'):
                nightly.verify_signers(self.folder, '1' * 64)

    def test_require_both_signing_secrets(self):
        with patch.dict(nightly.os.environ, {'NUTTYMOD_KEYSTORE_BASE64': 'present', 'NUTTYMOD_KEY_PASSWORD': ''}):
            with self.assertRaisesRegex(ValueError, 'Configure both'):
                nightly.prepare()


if __name__ == '__main__':
    unittest.main()
