#!/usr/bin/env python3
"""Materialize standard Android application manifests for this APK suite."""
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parent.parent
MODULES = {
    'hub': ('nuttyVR Hub', 'nuttyVR hub.apk'),
    'loader': ('nuttymod Loader for VR', 'nuttymod loader for __vr.apk'),
    'rootmode': ('Root Mode', 'rootmode.apk'),
    'rootmodelib': ('Root Mode Library', 'rootmodelib.apk'),
    'nuttymod': ('nuttymod', 'nuttymod.apk'),
    'vnedruntime': ('VNED Runtime', 'vnedruntime.apk'),
    'nuttymodruntime': ('nuttymod Runtime', 'nuttymodruntime.apk'),
    'demo': ('nuttyVR Rhythm Demo', 'nuttyVR demo game.apk'),
}

def setup():
    for role, (label, _) in MODULES.items():
        folder = ROOT / role / 'src/main'
        folder.mkdir(parents=True, exist_ok=True)
        permissions = ''
        provider = ''
        if role == 'nuttymodruntime':
            permissions = '<permission android:name="com.nuttyvr.permission.MODS" android:protectionLevel="signature" />'
            provider = '<provider android:name="com.nuttyvr.common.RuntimeProvider" android:authorities="com.nuttyvr.nuttymodruntime.mods" android:exported="true" android:permission="com.nuttyvr.permission.MODS" />'
        if role == 'rootmodelib':
            permissions = '<permission android:name="com.nuttyvr.permission.ROOT_STATUS" android:protectionLevel="signature" />'
            provider = '<provider android:name="com.nuttyvr.common.RootStatusProvider" android:authorities="com.nuttyvr.rootmodelib.status" android:exported="true" android:permission="com.nuttyvr.permission.ROOT_STATUS" />'
        if role in ('hub', 'loader'):
            permissions += '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />'
            provider = f'<provider android:name="com.nuttyvr.common.ApkProvider" android:authorities="com.nuttyvr.{role}.apks" android:exported="false" android:grantUriPermissions="true" />'
        queries = ''.join(f'<package android:name="com.nuttyvr.{item}" />' for item in MODULES)
        activity = 'DemoActivity' if role == 'demo' else 'MainActivity'
        manifest = f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    {permissions}
    <uses-permission android:name="com.nuttyvr.permission.MODS" />
    <uses-permission android:name="com.nuttyvr.permission.ROOT_STATUS" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
    <uses-feature android:name="android.hardware.vr.headtracking" android:version="1" android:required="false" />
    <queries>{queries}<package android:name="com.beatgames.beatsaber" /></queries>
    <application android:label="{escape(label)}" android:icon="@drawable/ic_nutty" android:theme="@style/AppTheme"
        android:allowBackup="false" android:fullBackupContent="false" android:hardwareAccelerated="true" android:supportsRtl="true">
        <meta-data android:name="com.oculus.supportedDevices" android:value="quest2|questpro|quest3|quest3s" />
        <activity android:name="com.nuttyvr.common.{activity}" android:exported="true" android:resizeableActivity="true"
            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden">
            <layout android:defaultWidth="1000dp" android:defaultHeight="720dp" android:minWidth="480dp" android:minHeight="360dp" />
            <intent-filter><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent-filter>
        </activity>
        {provider}
    </application>
</manifest>
'''
        (folder / 'AndroidManifest.xml').write_text(manifest)
        (ROOT / role / 'build.gradle').write_text('// Android configuration is shared in the root build.gradle.\n')
    (ROOT / 'common/build.gradle').write_text('// Android library configuration is shared in the root build.gradle.\n')

if __name__ == '__main__':
    setup()
    print('Prepared eight Android modules and the shared library.')
