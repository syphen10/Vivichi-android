# Vivichi — current status

Running handover note: where the app is, what's blocked, and what to do next.
Update this whenever something here stops being true.

**Last updated:** 23 September 2026 · **Current version:** 1.4.6 (versionCode 16)

---

## Where things stand

| | |
|---|---|
| Latest built version | **1.4.6 / versionCode 16**, signed with the release key |
| Uploaded to Play | **No.** The newest build on Play is **1.4.2 (12)**, in closed testing. 1.4.3–1.4.6 exist only locally. |
| Ads | Requesting correctly, but **Google serves almost nothing** — see [Ads are blocked](#ads-are-blocked-not-a-code-problem) |
| Store screenshots | Regenerated from 1.4.5 — current UI, in `store-assets/screenshots/` |
| Repos | Public + private both pushed and in sync |

### Where the builds are

Signed builds live in **both** working folders, identically:

```
C:\claude code\VivichiAndroid\release-builds\        (gitignored — local only)
C:\claude code\Vivichi-private-repo\release-builds\  (committed — the real backup)
```

`vivichi-<version>-vc<code>.aab` is the Play upload; the `.apk` beside it installs directly on a
phone. Only the newest version is kept; older ones are in the private repo's git history.

### To upload 1.4.6

Play Console → Test and release → the track → Create new release → upload the `.aab`.
Release name `1.4.6 (16)`. Notes:

```
<en-US>
Much smoother scrolling and fewer freezes, especially on budget phones.
Custom habits: clearer XP selection, a name check, and habits added after their time now start tomorrow.
The Play screen now scrolls up so you can see your buddy react.
</en-US>
```

---

## Ads are blocked (not a code problem)

The app side is proven working: AdMob recorded **276 real ad requests** from the app, and 5 ads did
show. The ad unit IDs and app ID baked into the release APK are the real ones. What's missing is
account-level setup, and **no code change can fix it**:

1. **The AdMob app shows "Requires review".** Monetised apps must be listed on a supported app
   store. Vivichi is in *closed testing*, which has no public listing, so AdMob can't link it.
   **Vivichi has to be published publicly on Play** (production, or a public open-testing listing)
   before AdMob → Apps → Vivichi → *Add shop* can link it and the review can happen.
2. **The AdMob account isn't verified.** AdMob → Payments → enter payment details (name, address,
   tax). Google won't review the app until this is done. Independent of everything else — do it
   first, it takes time.
3. **No privacy message configured.** The app's own diagnostics reported:
   *"Publisher misconfiguration: no form(s) configured for the input app ID."*
   Fix in AdMob → Privacy & messaging: create and publish a GDPR message and a US states message
   for Vivichi.

Until 1 and 2 are done, fill stays near zero, so **the banner will look absent on a real phone**.
That's expected. It takes no space unless an ad actually loads, by design, so no-fill and no-ad
look identical.

### Checking ad status on a phone (1.4.6+)

**More → tap the "Premium & Ads" heading 7 times** → a sheet shows consent status, whether the SDK
started, request counts, and the last error in plain language, with a **Copy** button. Hidden on
purpose. Code in `monetize/AdDiagnostics.kt`.

---

## Performance work (the main thread of recent versions)

The app was reported at "about 10 fps" on a **3 GB RAM phone**. Four causes were found and fixed;
none of them removed any visual feature.

1. **Ads SDK on the main thread** (1.4.5, the big one). ANR traces showed Google's ads SDK parsing
   ad responses (JSON, URI decoding) on the UI thread for seconds. The app made this constant by
   fetching a rewarded ad at SDK init, on every `onResume`, and on endless backoff retries.
   Now a rewarded ad is fetched **only while a "Watch ad" button is on screen**
   (`WatchAdButton` → `RewardedAds.offerShown/offerHidden`), retries cap at 2, and the banner's
   first request waits 5s so it doesn't land during startup.
2. **Nothing ever stopped animating** (1.4.4). Every decoration ran its own infinite animation
   forever. They now share one clock (`ui/components/Ambient.kt`) that **pauses while anything
   scrolls** (resumes ~350 ms after) and ticks at **30 fps on phones under 4 GB RAM**. All reads
   happen in draw/`graphicsLayer` lambdas, so a tick repaints one layer and never recomposes.
3. **Entrance animations re-fired on every scroll** (1.4.3). `enterFromBelow` ran a fresh spring
   per row each time it scrolled into view. Now only cards present when a tab opens animate
   (`LocalScreenOpenedAt`); rows reached by scrolling just appear.
4. **Emoji reloaded constantly** (1.4.4). Each Twemoji SVG went through a full Coil request per
   appearance. Now rendered once per pixel size off the main thread into a 12 MB LRU of
   `ImageBitmap`s (`ui/components/EmojiIcon.kt` → `SvgIcon`).

**Not yet verified on the user's phone.** The emulator on the build PC cannot measure this: its
freezes trace to `libEGL_emulation` (software graphics), and Android's own apps freeze on it too.
Confirming these fixes needs the phone connected over USB, or the user's own judgement.

---

## Other recent changes

- **Custom habits** (1.4.2): Add Habit always adds; a habit whose time already passed today reads
  "Starts tomorrow" instead of counting as missed (`GameLogic.startsTomorrow`); blank name shows an
  error; the selected XP chip is filled with colour and a tick; default time is the next full hour.
- **Playground** (1.4.2): tapping a play action scrolls back up so the pet's reaction is visible.

---

## Working notes for the next session

**Emulator.** AVD `vivichi_shots`. Launch with **`-gpu host`** — `-gpu angle_indirect` is software
rendering and makes everything, including Android itself, unusably slow:

```
emulator -avd vivichi_shots -no-window -gpu host -memory 4096 -cores 4 -no-snapshot -no-audio -no-boot-anim
```

Wait for `/proc/loadavg` to drop below ~2 after boot before trusting anything. Tab bar y=2232,
x = 57 / 226 / 382 / 540 / 697 / 855 / 1012 (Home…More).

**Showcase profile** (debug builds only) fills a flattering save and hides the test banner:

```
adb shell am start -n com.vivichi.app/.MainActivity --ez showcase true
```

Set the device clock to ~13:20 first (`adb shell "date MMDDhhmmYYYY.ss"`, with `auto_time` off), or
the habit states won't look right.

**Store screenshots.** Raw captures go in `store-assets-source/raw-v<ver>/` with the names the
script expects (`home, habits, buddies, coins, themes, shade_clean, play, stats`), then:

```powershell
tools\store-assets\make_screens.ps1 -OutDir store-assets\screenshots `
  -FontDir store-assets-source\fonts -RawDir store-assets-source\raw-v1.4
```

For the notification-shade shot, snooze the emulator's own system notifications first, or they
appear in the picture:

```
adb shell "cmd notification snooze --for 7200000 '-1|android|55|null|1000'"
```

**Editing source files: use a real editor, not `sed`/`perl` for anything non-ASCII.** The code is
full of emoji and en dashes; shell substitutions on Windows re-encode whole files into mojibake.
Check with `grep -c "ð\|â€"` if something looks wrong.

**Release checklist.** Bump `versionCode`/`versionName` → `./gradlew testDebugUnitTest bundleRelease
assembleRelease` → verify with `apksigner verify --print-certs` (expect SHA-256
`4F:FB:7B:A4:B0:E9:AD:B2:…`, the key registered with Play) → copy named builds into **both**
`release-builds` folders → commit and push the public repo → pull into the private repo, commit
the builds, push.
