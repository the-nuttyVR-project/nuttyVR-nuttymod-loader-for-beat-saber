# Validation — 0.1.0

Built on 2026-10-25 with OpenJDK 22, SDK Platform 35, Build Tools 34.0.0 and cached AGP 8.7.3 D8.

## Completed

- All Java code compiled and converted to Dex. AAPT2 compiled and linked resources for eight Android applications.
- All eight APKs passed Android `apksigner verify` and `zipalign -c`.
- `./test.sh`: **29 profile-rule assertions** covering valid profiles' field rules, exact game/version gates, path and command characters, invalid IDs, wildcard versions, color validation, non-finite speeds, bounds and control characters.
- `./test.sh`: **67 APK packaging and trust checks** covering package IDs, versions, minimum/target SDK, launcher activities, ZIP integrity, Dex, byte-for-byte nested APKs, one signing identity, declared signature permission boundaries and checksums.
- Hub embeds the exact delivered signed Loader. Loader embeds the exact five delivered signed companions.
- Android manifest package queries include all companions and Beat Saber. The APKs contain no network permission or bundled game assets. External guide links open the user's browser.
- The download page was built with the actual APKs, source archive and release archive. Every local page asset and download link resolves to a file.
- Nightly publishing preparation: the signed snapshot passed source-fingerprint, APK SHA-256 and signing certificate verification. Nine snapshot tests cover changed source, tampered/missing APKs, unexpected paths, missing manifest entries, certificate mismatch and incomplete signing secrets.

## Verification limits

No headset or Android emulator was available. ADB could not start its local listener (`Operation not permitted`), so installation, Android permission prompts, system installer return values, lifecycle recovery, provider permission enforcement, SQLite operations, document picking, root diagnostics and interactive rendering have **not** been tested on Android hardware. The source implements the demo integration, but its on-device behavior remains unverified. Compile/package checks are not an end-to-end device test.

Native QMOD loading, Beat Saber injection, game patching, remote headset pairing and granting root are not implemented. No Beat Saber version has been validated or declared compatible.

The website is local and unpublished. Node.js is unavailable, the Node download host cannot be resolved, and no browser preview tool is available. Static file/link validation passed; JavaScript syntax/execution and visual browser QA remain unverified. No hosted Site was registered.

The nightly workflow checks and uploads the signed snapshot to GitHub Actions. See the repository's Actions page for current run status and `NIGHTLY.json` in the download for the source commit and preparation mode.

## Device acceptance checks

1. Sideload the Hub; confirm it opens as a resizable panel on the intended headset with readable controls and controller pointer focus.
2. Connect the local headset. Install the Loader through Android's prompt; deny permission once and confirm no queue continues. Grant permission and retry.
3. Install all five companions. Cancel the second APK; confirm the queue stops. Retry and confirm already installed signed packages are skipped.
4. Interrupt the installer by restarting the Hub/Loader and exercise Resume. Confirm no false completion after a cancelled or blocked installation.
5. Confirm the Loader reports all five dependencies only after PackageManager detects matching versions and certificates.
6. Install the demo, add Mint Rush, enable it, then launch the game. Confirm mint blocks at 1.5×. Disable it, reopen, and confirm orange blocks at 1.0×.
7. Import a profile for the wrong game/version; confirm the demo ignores it. Try malformed JSON, an unknown field, out-of-range speed and a file over 64 KiB; each must be rejected. Reimporting the same ID must disable it.
8. Exercise enable/disable/remove, restart the runtime, and confirm persistence. Enable two profiles for the same exact game/version; only the last should remain enabled.
9. From a separately signed test APK, confirm both exported providers reject reads and writes without their signature permissions.
10. On an ordinary headset, Root Mode must report that root is unavailable. On a separately authorized rooted test device, test approval, denial and timeout of the fixed `su -c id` command. Do not infer app-wide root access from a successful subprocess.
11. Beat Saber must display its detected version or Not installed, and always show Adapter required. The Open game action must only launch the installed game.
