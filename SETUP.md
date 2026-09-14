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

- **JDK 17** (point Gradle at it via `JAVA_HOME` before building).
- Android SDK platform 36 (`compileSdk 36`).
- Kotlin 2.3 / AGP 8.10. The Google Mobile Ads and Play Billing libraries are compiled with
  Kotlin 2.3, so the project's Kotlin version can't drop below that.

## 3b. Ads & Premium configuration

- **AdMob:** builds use Google's public *test* ad IDs until real ones are set. Once the AdMob app
  and a *Rewarded* ad unit exist, add them to `gradle.properties`:

  ```properties
  vivichi.admobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
  vivichi.rewardedUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
  ```

  Debug builds always keep test ads regardless (clicking your own live ads can get the AdMob
  account suspended).
- **Premium:** a one-time in-app product with ID `vivichi_premium` must exist (and be active) in
  Play Console → Monetize → Products → In-app products. Purchases only work in builds installed
  from Google Play (e.g. via a testing track), not sideloaded APKs.

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
