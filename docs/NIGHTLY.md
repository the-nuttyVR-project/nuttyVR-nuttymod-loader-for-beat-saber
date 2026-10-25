# Nightly downloads

The GitHub Actions workflow is `.github/workflows/nightly.yml`. It runs on pushes to `main`, daily at 08:17 UTC, and manually through **Actions → Nightly APKs → Run workflow**.

- [Complete nightly ZIP](https://nightly.link/the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber/workflows/nightly/main/nuttyvr-nightly.zip)
- [Hub APK ZIP](https://nightly.link/the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber/workflows/nightly/main/nuttyvr-hub.zip)
- [Workflow status](https://github.com/the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber/actions/workflows/nightly.yml)

These links become available after the first successful workflow run. nightly.link serves the latest successful run for this workflow/branch. Extract the ZIP before installing `nuttyVR hub.apk`. No nightly.link account is required for this public repository. Artifacts are retained for 14 days and refreshed by the daily workflow; GitHub scheduling and repository activity policies still apply.

The full ZIP contains all eight APKs, source and APK archives, checksums, the Mint Rush example, the installation guide and `NIGHTLY.json`. The Hub ZIP contains the Hub and metadata. The APK checksum file lists the whole suite; the Hub-only ZIP does not contain every listed file.

## Initial signed downloads

Without private signing secrets, CI verifies and uploads the checked-in APK snapshot at `prebuilt/nightly/`. It checks the fingerprint of all Android source/build inputs, every APK's SHA-256 and signing certificate, then runs the suite's profile-rule and packaging checks. Changed Android source cannot silently publish a stale snapshot: the workflow fails until the snapshot is regenerated or signing secrets are configured.

This mode republishes the verified existing build; it does not compile a new APK every night. `NIGHTLY.json` identifies the mode, source fingerprint and workflow commit. The original local signing key remains outside Git and the downloads. The snapshot contains APKs and a public certificate fingerprint, never a private key.

## Build every nightly from source

To compile changes in CI while preserving update compatibility, add these repository Actions secrets using the existing local signing key:

- `NUTTYMOD_KEYSTORE_BASE64`: base64 bytes of `signing/debug.keystore`.
- `NUTTYMOD_KEY_PASSWORD`: the key/store password (`android` for this initial development key).

Example commands on a trusted computer with GitHub CLI configured for this repository:

```sh
base64 -w0 signing/debug.keystore | gh secret set NUTTYMOD_KEYSTORE_BASE64 --repo the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber
printf '%s' 'android' | gh secret set NUTTYMOD_KEY_PASSWORD --repo the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber
```

Configure both secrets together. Signing uses the default alias `androiddebugkey`. CI decodes the key into a private temporary file, builds all modules with the same identity and removes the temporary directory before uploading artifacts. A key with a different certificate is rejected to avoid publishing incompatible updates. No action to set secrets is performed by the project itself.

Alternatively, rebuild locally with the original signer, run `bash test.sh`, run `python3 tools/prepare_nightly.py snapshot`, and commit the updated Android source together with all of `prebuilt/nightly/`. Documentation-only changes do not require replacing APKs.

## Compatibility

Nightly packaging does not add Beat Saber injection, native QMOD support, remote headset pairing or the ability to grant root. The included reference game is the only game with an implemented NuttyMod integration in this version. See `VALIDATION.md` for the remaining device checks.

## Service references

- [nightly.link documentation and link format](https://nightly.link/)
- [GitHub: storing workflow artifacts](https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts)
