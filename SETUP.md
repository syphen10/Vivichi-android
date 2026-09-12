# Setting up Vivichi on a new machine

## 1. Clone

```bash
git clone https://github.com/syphen10/Vivichi-android.git
cd Vivichi-android
```

## 2. Files NOT in this repo (you must copy these manually)

These are gitignored on purpose — signing secrets must never be committed, especially to a
public repo.

| File | What it is | Replaceable? |
|---|---|---|
| `keystore/vivichi-release.jks` | **The release signing key** | ❌ **NO — see warning below** |
| `keystore.properties` | Passwords + alias for the above | ❌ No (must match the key) |
| `local.properties` | Your Android SDK path | ✅ Yes, auto-generated |

### ⚠️ The keystore is irreplaceable

Google Play permanently ties an app listing to its signing certificate. If
`vivichi-release.jks` is lost, **you can never publish an update to Vivichi again** — you'd have
to ship a brand new listing under a new package name and lose all installs, reviews, and
ranking. Back it up somewhere durable (password manager, encrypted cloud, offline drive).
Do not rely on a single PC having it.

`keystore.properties` format:

```properties
storeFile=keystore/vivichi-release.jks
storePassword=<the store password>
keyAlias=vivichi
keyPassword=<same as store password>
```

`local.properties` — just point it at your SDK (Android Studio writes this automatically when
you open the project):

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

## 3. Toolchain

- **JDK 17** is required. Newer JDKs (21/25) crash the Kotlin 1.9.x compiler daemon.
- Android SDK with `compileSdk 34` / `build-tools 34.0.0`.

Point Gradle at JDK 17 via `JAVA_HOME` before building.

## 4. Build

```bash
./gradlew assembleDebug      # debug APK for testing
./gradlew assembleRelease    # signed release APK
./gradlew bundleRelease      # signed .aab for the Play Store
```

Outputs:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab` ← upload this one to Play Console

## 5. Verify a release is signed with the right key

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Should report `CN=Vivichi`. If it reports `CN=Android Debug`, the keystore isn't being picked
up — check that `keystore.properties` exists and the path in it resolves.
