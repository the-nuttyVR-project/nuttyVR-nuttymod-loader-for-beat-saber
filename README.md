# nuttyVR — nuttymod loader

[nuttyVR website](https://the-nuttyvr-project.github.io/nuttyVR-nuttymod-loader-for-beat-saber/) · [Website source and deployment](docs/WEBSITE.md)

[Download nightly toolkit ZIP](https://nightly.link/the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber/workflows/nightly/main/nuttyvr-nightly.zip) · [Hub APK ZIP](https://nightly.link/the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber/workflows/nightly/main/nuttyvr-hub.zip) · [Build status](https://github.com/the-nuttyVR-project/nuttyVR-nuttymod-loader-for-beat-saber/actions/workflows/nightly.yml)

Nightly links become available after the first successful GitHub Actions run. Extract the ZIP before installing an APK. See [nightly setup](docs/NIGHTLY.md) for the signed snapshot mode and optional builds from source.

Android project and installable APK suite, version **0.1.0**. Includes a Hub, a dependency installer, a signed mod registry, a profile manager, device/root diagnostics, and a playable rhythm demo that reads a real mod profile.

**Compatibility:** the bundled Rhythm Demo implements NuttyMod. **Beat Saber patching and injection are not implemented.** A game-specific adapter and exact game version are required. Installing Root Mode does not root a headset. The Hub runs on the target headset; remote phone-to-headset pairing is not implemented.

## Installation

1. Download `nuttyVR hub.apk` and sideload it onto your headset.
2. Open **nuttyVR Hub** on the headset and click **Connect this headset**. This selects the local device.
3. Click **Install nuttymod Loader for VR**. Allow the Hub to install unknown apps if Android asks, then approve the APK installation.
4. Wait for Android to finish. Return to the Hub and click **Launch nuttymod Loader**.
5. Click **Install all dependencies**. Allow the Loader to install unknown apps, then approve these five APKs in order: `rootmode.apk`, `rootmodelib.apk`, `nuttymod.apk`, `vnedruntime.apk`, `nuttymodruntime.apk`.

The Hub includes the Loader, and the Loader includes all five companions. No download server is needed after obtaining the Hub. Cancel stops the queue. **Resume installation** can recover a saved queue after an interruption. The loader verifies installed package names, minimum version and signing identity. Staged APKs must also match the expected package, version and signing identity.

If your firmware does not expose Android's installer or install permission settings, install the APKs from your computer with Android Platform Tools instead. Example, from `dist/`:

```sh
adb devices
adb install -r 'nuttyVR hub.apk'
adb shell am start -n com.nuttyvr.hub/com.nuttyvr.common.MainActivity
```

Enable the headset's developer/debugging support and approve the USB debugging prompt before using ADB. Sideloaded panel apps are usually available under **Unknown Sources** on Quest. Actual availability depends on your firmware.

## Try actual mod loading

1. Install `nuttyVR demo game.apk`, which is included separately in the release bundle.
2. In the Loader, open **Mods → Add Mint Rush example → Import → Enable profile**.
3. Open **Games → Open Rhythm Demo**. The game reports **Mod loaded: Mint Rush**, draws mint blocks and uses speed **1.5×**.
4. Disable the profile and reopen the demo. It returns to orange blocks and speed **1.0×**.

You can also import `examples/mint-rush.nuttymod` with Android's document picker. Import requires confirmation, validates the profile, and leaves it disabled until you enable it. Reimporting an ID replaces and disables that profile. Exactly one profile can be enabled per target package and exact game version.

## APKs

| Download filename | Package | Function |
| --- | --- | --- |
| `nuttyVR hub.apk` | `com.nuttyvr.hub` | Local headset setup; bundled Loader installer |
| `nuttymod loader for __vr.apk` | `com.nuttyvr.loader` | Bundled dependency queue; games and profiles |
| `rootmode.apk` | `com.nuttyvr.rootmode` | Explicit check of an existing `su` installation |
| `rootmodelib.apk` | `com.nuttyvr.rootmodelib` | Signed read-only device diagnostics API |
| `nuttymod.apk` | `com.nuttyvr.nuttymod` | Import, enable, disable and remove profiles |
| `vnedruntime.apk` | `com.nuttyvr.vnedruntime` | Validate and import `nuttymod/1` profiles |
| `nuttymodruntime.apk` | `com.nuttyvr.nuttymodruntime` | Persistent SQLite profile registry and game API |
| `nuttyVR demo game.apk` | `com.nuttyvr.demo` | Playable reference game with the NuttyMod client |

VNED Runtime in this project handles the documented declarative `nuttymod/1` profile format. It does not execute the separate Quest ADB Guard project's `vned-1` shell plugins. These APKs do not include an IL2CPP hook framework, Scotland2, QuestLoader, QMOD installer, exploit, boot image patcher or Beat Saber libraries.

## Build

Requirements: Python 3, JDK 17+, Android SDK Platform 35 and Build Tools 34.0.0+. Set `JAVA_HOME` and `ANDROID_HOME` as needed. On this workspace, defaults use `~/dev-tools/java-22` and `~/Android/Sdk`.

```sh
./build.sh
./test.sh
python3 tools/package_release.py
```

The SDK builder runs AAPT2 → javac → D8 → zipalign → apksigner, builds companions first, bundles them in the Loader, then bundles the Loader in the Hub. It uses cached AGP 8.7.3 D8 when available; set `NUTTYMOD_D8_JAR` if needed. Output is in `dist/` with `SHA256SUMS`.

The first SDK build generates a local **development** key at `signing/debug.keystore`. Keep it to sign updates. Source bundles exclude this key. A new checkout gets a different key and cannot update an existing installation or access a differently signed runtime. For your own signing identity, set `NUTTYMOD_KEYSTORE`, `NUTTYMOD_KEY_ALIAS` and `NUTTYMOD_KEY_PASSWORD` in the environment. Use one identity for all modules.

The folder is also a conventional Android Studio / AGP 8.7.3 multi-module project. Use Gradle 8.9 and JDK 17+:

```sh
gradle :hub:assembleDebug :demo:assembleDebug
```

Gradle builds automatically generate the same nested APK structure using Android's local debug signing identity. Do not mix differently signed SDK-builder and Gradle APKs. No Gradle wrapper is bundled; Gradle must already be installed. The SDK builder avoids the daemon's local socket, which this environment blocks. Manifests and module files are tracked; `tools/setup_project.py` regenerates them when editing the module catalog.

See [game integration](docs/GAME-INTEGRATION.md) and [validation](VALIDATION.md).

## Download page

`website/src/` contains the responsive project page and an installation checklist that saves progress in the browser. Run `python3 website/build.py` to build `website/dist/` directly from the checked-in signed APK snapshots, with source, APK ZIPs and checksums. The **Publish website** GitHub Actions workflow deploys this folder to GitHub Pages. See [website setup](docs/WEBSITE.md) for details. Browser rendering and headset interactions still require visual/device testing.

## Platform references

- [Android PackageManager: APK inspection, signing and install permission](https://developer.android.com/reference/android/content/pm/PackageManager)
- [Android app security: sandboxing and signature permissions](https://developer.android.com/privacy-and-security/security-tips)
- [Beat Saber Modding Group: Quest modding](https://bsmg.wiki/quest-modding.html)
