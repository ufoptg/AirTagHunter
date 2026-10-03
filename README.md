# AirTagHunter

Android AirTag / finder-tag hunter based on [Fieldwatch](https://github.com/OffGridPete/Fieldwatch) (MIT). Receive-only Wi-Fi + Bluetooth LE observer. Package id: `app.airtaghunter` (installs beside Fieldwatch).

New installs open Live filtered to **Finder tags** (BLE) and watch FINDER-class signatures (Apple AirTags, SmartTags, Tile, Chipolo, Find Hub, DULT, and other Extra attention / drone rows from upstream).

Accuracy tweaks vs upstream Fieldwatch (catalog 91):

- AirTags match registered Offline Finding (`0x004C` / type `0x12` / length `0x19`), the name `AirTag`, or UUID `FD44` — not a bare `0x12` scrap or a generic “Find My” name
- iPhones that also advertise Continuity stay **Apple Device**, not AirTag (same demotion as upstream)
- Live row shows **separated** vs **near owner** from the OF status byte (bit 2); separated uses the stronger chip
- Samsung SmartTags no longer match on company ID `0x0075` alone (that was every Samsung BLE radio)

See [FORK.md](FORK.md) for what changed vs Fieldwatch.

## Download the APK (GitHub Actions)

This fork builds on GitHub. There is no Play Store listing.

1. Push this repo to GitHub (or fork it).
2. Open **Actions** → **Android CI** → latest green run (or **Run workflow**).
3. Download the **airtaghunter-debug-apk** artifact → `AirTagHunter.apk`.
4. Or on a `v*` tag: use the **Releases** asset.

Local check after download:

```bash
sha256sum -c AirTagHunter.apk.sha256
```

Sideload on Android 10+:

- Allow install from Files / browser
- Turn on **Location**, **Wi-Fi**, and **Bluetooth**
- Grant Location, Nearby devices / Bluetooth, Nearby Wi-Fi, Notifications
- Play Protect may warn — expected for a debug sideload

```bash
adb install -r AirTagHunter.apk
```

## Hunt AirTags

1. Open AirTagHunter; accept the disclaimer; grant permissions.
2. Live should show Finder tags by default (Filters → **Finder tags** chip). Use **All traffic** if you want Wi-Fi and other classes too.
3. Tap a row → **Hunt** for relative RSSI while you walk.
4. Watchlist alerts fire for FINDER signatures and Extra attention / drone rows.

No guarantee every tag will appear (asleep, near-owner, OS scan limits). Not direction finding.

## Build locally

Needs JDK 17 and Android SDK platform / build-tools 35:

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

CI is the supported path: [`.github/workflows/android.yml`](.github/workflows/android.yml).

## Upstream

Stock catalog JSON under `dist/fieldwatch-signatures*.json` and Settings → **Update stock catalog from GitHub** still use the Fieldwatch pack format and upstream URL. License and liability text from Off Grid Pete LLC remain in `LICENSE` / the in-app disclaimer.
