# Integrating a compatible game

NuttyMod 0.1 is an opt-in declarative game profile API. The example game uses Android Canvas and reads the active profile when it resumes. It runs inside its own normal app process. The companion runtime supplies data through a signature-protected ContentProvider; it does not inject code into another app.

## Profile format

See `examples/mint-rush.nuttymod`. Required keys are `schema` (`nuttymod/1`), `id`, `name`, `version`, `targetPackage`, `targetVersion`, `color` (`#RRGGBB`), and `speed` (finite number 0.5–2.0). Maximum size is 64 KiB. Unknown fields are rejected. IDs allow lowercase letters, numbers, dots, dashes and underscores, begin with a letter, and are 3–64 characters. Game package and exact game version must both match. There are no wildcard versions, filesystem paths, executable scripts or native libraries in this format.

## Reading a profile

Add the `:common` module to the game or copy `NuttyModClient`, `ModSpec`, `ModRules` and the registry URI constant into your project. The example uses:

```java
ModSpec profile = NuttyModClient.activeForThisGame(this);
double speed = profile == null ? 1.0 : profile.speed;
int color = profile == null ? Color.rgb(249, 167, 76)
                            : Color.parseColor(profile.color);
```

For this initial private API, **sign the game and runtime with the same certificate**. Add to the game's manifest:

```xml
<uses-permission android:name="com.nuttyvr.permission.MODS" />
<queries>
    <package android:name="com.nuttyvr.nuttymodruntime" />
</queries>
```

The provider defines `com.nuttyvr.permission.MODS` with `signature` protection. An independently signed game requires a future explicit authorization protocol; giving it the URI alone is insufficient. Never distribute the publisher's signing key to third-party game authors.

Registry URI: `content://com.nuttyvr.nuttymodruntime.mods/mods`. Query returns `id`, `name`, `version`, `target`, `targetVersion`, `color`, `speed`, `enabled`, and normalized `manifest`. Insert a `manifest` string to import disabled. Update `/mods/{id}` with `enabled` equal to 0 or 1. Enabling atomically disables any other profile for that exact game/version. Delete `/mods/{id}` to remove. Collection-wide delete and arbitrary SQL selections are unsupported. Maximum 64 profiles.

## Root Mode Library

`content://com.nuttyvr.rootmodelib.status/status` returns `model`, `sdk`, `uid`, and `rootGranted` for the library process. Access requires `com.nuttyvr.permission.ROOT_STATUS` with the same signing identity. The library remains an ordinary app. Root Mode separately runs the constant command `su -c id` only after a user click, waits up to ten seconds and reports whether the subprocess had UID 0. It does not provide arbitrary root commands or a root IPC broker.

## Beat Saber adapter boundary

The project detects `com.beatgames.beatsaber`, displays its installed version and can launch it. No Beat Saber version is declared supported by this build. To add support, first establish the user's headset, firmware, owned game version, patching method, mod format and actual loader ABI. Implement an adapter for that version's supported mod loader and test it on hardware. Android's per-app sandbox prevents these regular APKs from reading another game's private data or attaching to its process just because a companion is installed. The app therefore reports **Adapter required** rather than claiming injection succeeded.

Existing QMOD files and shell VNED plugins are not converted to profiles. A valid JSON profile targeting Beat Saber can be stored, but the game will not consume it without an integration; importing/enabling metadata is not proof of game compatibility.
