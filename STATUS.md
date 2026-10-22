# Vivichi — current status

Running handover note: where the app is, what's blocked, and what to do next.
Update this whenever something here stops being true.

**Last updated:** 22 October 2026 · **Current version:** 1.4.8 (versionCode 18 submitted; vc19 built spare)

---

## Where things stand

| | |
|---|---|
| Latest built version | **1.4.8 / versionCode 19**, signed with the release key. vc19 is byte-for-byte the same app as vc18 — only the version code differs, built because Play rejects a reused code. |
| Uploaded to Play | **Live on production.** vc18 is the published artifact; the listing is public at `play.google.com/store/apps/details?id=com.vivichi.app`. vc19 is an unused spare. |
| Privacy messages | **Done, 22 Oct.** GDPR ("Vivichi – EEA/UK consent") and US states ("Vivichi – US states") both **published** in AdMob → Privacy & messaging. Consent / Manage options / Do not consent all on; Close off. |
| Content rating | IARC rating generated 17 Oct. Global Rating ID `96e74018-6766-8f2f-84a0-3fac2be9cd1f`. Re-do the questionnaire if ads/IAP answers ever change. |
| Ads | **Unblocked, 22 Oct.** App confirmed in AdMob, `app-ads.txt` **verified**, both privacy messages published, ads declared in Play Console. Nothing is gating serving any more — see [Ads](#ads-unblocked-22-oct) |
| Developer site | `https://syphen10.github.io` (repo `syphen10/syphen10.github.io`) — serves `app-ads.txt` and the privacy policy, and is the website on the Play listing |
| Store screenshots | Regenerated from 1.4.5 — current UI, in `store-assets/screenshots/` |
| Repos | Public + private both pushed and in sync |

### Where the builds are

Signed builds live in **both** working folders, identically:

```
C:\claude code\VivichiAndroid\release-builds\        (gitignored — local only)
C:\claude code\Vivichi-private-repo\release-builds\  (committed — the real backup)
```

`vivichi-<version>-vc<code>.aab` is the Play upload; the `.apk` beside it installs directly on a
phone. Normally only the newest version is kept, older ones living in the private repo's git
history — but vc18 and vc19 are both present on purpose: vc18 is the artifact actually in review,
vc19 the spare.

Note: on this PC the working folders are under `F:\claude code\` rather than `C:\claude code\`.

### To upload the latest build

Play Console → Test and release → the track → Create new release → upload the `.aab`.
Release name `1.4.8 (18)`. Notes:

```
<en-US>
Much smoother scrolling and fewer freezes, especially on budget phones.
Custom habits: clearer XP selection, a name check, and habits added after their time now start tomorrow.
The Play screen now scrolls up so you can see your buddy react.
</en-US>
```

---

## Ads: unblocked (22 Oct)

Every account-level gate is now cleared. The app side was never the problem — no code change was
ever needed, and none was made.

1. ~~The AdMob app is unconfirmed.~~ **Done** — the app is added and confirmed in AdMob. The
   **Link to Google Play** step failed for a while with *"Couldn't find any matching Google Play
   apps"*; that was AdMob's Play catalogue index lagging a freshly published listing, not a fault.
   Package name, listing visibility and account ownership were all verified as correct at the time.
2. ~~The AdMob account isn't verified.~~ **Done** — the payments profile (1257-9429-4872, Pakistan,
   individual) shows *Verified on 13 Sept 2026*. Adding a bank account only matters for payouts.
3. ~~No privacy message configured.~~ **Done, 22 Oct** — GDPR and US states messages are both
   published. The app already had the UMP SDK and a privacy options entry point
   (`RewardedAds.showPrivacyOptions`, surfaced as **More → Ad privacy choices**), so no code change
   was needed.
4. ~~app-ads.txt not verified.~~ **Done, 22 Oct** — AdMob could not verify the app because the Play
   listing had no developer website. Fixed by standing up `https://syphen10.github.io`, serving
   `app-ads.txt` at the domain root, and adding that URL under Play Console → Grow users → Store
   presence → **Store settings** → store listing contact details. AdMob now reports **verified**.

**Still true and worth keeping:** never accept AdMob's offer to *"set up as a new AdMob app"*. It is
the pre-selected default on the confirmation screen and is suggested again in the failed-lookup
dialog. A new app means a new app ID that will not match the shipped binary — a code change, a
version bump and another Play review. Always choose *Add to an existing AdMob app* and verify the
ID reads `…~2181222962`.

Fill should now improve, but it ramps rather than switching on. A new app with ~10 installs in one
country is a rounding error to advertisers, so judge this once real production traffic arrives, not
on the next few hours. The banner takes no space unless an ad loads, so no-fill and no-ad still
look identical on a phone.

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

## Backups / data safety (1.4.8)

The save is one DataStore file at `files/datastore/vivichi_state.preferences_pb`, but the backup
rules only included `sharedpref` — so cloud backup and phone-to-phone transfer restored nothing and
users lost their pet, streak and history on a new phone. Both rule files now include
`domain="file" path="datastore/"`, and `android:dataExtractionRules` (Android 12+) was missing from
the manifest entirely and has been added.

Verified on the emulator: back up → `pm clear` → restore brings back the pet and habits.
(Note when testing: `bmgr` silently skips **force-stopped** apps, so launch the app first, then
`bmgr backupnow com.vivichi.app`, then `pm clear`, then `bmgr restore 1 com.vivichi.app`.)

### Security posture, for the record

No servers, no accounts, no network code of our own — only Google's ads and billing SDKs talk out.
Nothing to breach, and no path between users. Remaining, accepted: a user can edit their own save
(harmless), and Premium can be pirated on a rooted device since the entitlement is cached locally
after Play's check. Proper defence needs server-side receipt validation; not worth it at this size.

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
