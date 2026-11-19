# Android release signing

The Android workflow always builds a debug APK. It also builds and uploads a signed release APK only when all four GitHub Actions repository secrets below are configured:

- `ANDROID_KEYSTORE_BASE64`: the existing release keystore file encoded as Base64.
- `ANDROID_STORE_PASSWORD`: the keystore password.
- `ANDROID_KEY_ALIAS`: the existing key alias.
- `ANDROID_KEY_PASSWORD`: the key password.

Add them in GitHub → Settings → Secrets and variables → Actions → New repository secret.

**Important:** use the exact same keystore and key alias that signed the previous production APK. Never generate a replacement keystore for an app that must update existing installations. Do not commit `release.keystore` or `release.properties`; both are ignored by Git.

The current application version is 1.1.0 (versionCode 2). The release workflow does not enable wallet deposits; payment activation is controlled separately by the Supabase payment settings and the server-side Binance configuration.
