# أذكاري | Athkari — Android TV

Full-screen Arabic adhkar over landscape photographs, with a live Kuwait clock, Gregorian and Hijri dates,
Kuwait prayer times and a live countdown to the next prayer. Runs as a normal TV app and as a screensaver
(`DreamService`). Built for the Sony Bravia KD-75X80K (Google TV), buildable entirely on GitHub Actions.

Package: `com.adhkaritv.app` (unchanged) · Version 2.0.0 (versionCode 2) · minSdk 26 · no third-party libraries.

---

## 1. What is on the screen

| Area | Content |
|---|---|
| Top-left | Kuwait clock, 12-hour, Arabic-Indic digits (`٥:٣٠ مساءً`); Gregorian date (`الجمعة، ٣٠ أكتوبر ٢٠٢٦`); Hijri date (`١٩ جمادى الأولى ١٤٤٨ هـ`) |
| Top-right | أذكاري / ATHKARI branding with a gold crescent |
| Centre | The dhikr (Amiri font), its heading (e.g. أذكار الصباح), repetition count when relevant, and the reference |
| Bottom | Translucent panel: الفجر · الشروق · الظهر · العصر · المغرب · العشاء; next prayer highlighted in gold; countdown line above it |

The overlay never fades; only the photo and the dhikr cross-fade. Margins are 5% for TV overscan. All sizes scale
from a 960-unit design grid to the TV's real resolution, so 1080p and 4K look the same.

Remote: **OK** opens the settings; **Back** exits. In the settings, arrows move, OK/Left changes a value, Right
changes it back.

## 2. How the times and dates are produced (and how they were checked)

**Clock and Gregorian date.** Always computed in the `Asia/Kuwait` time zone, whatever zone the TV is set to.
The screen re-reads the wall clock every second, so it is correct immediately after standby. The date changes at
midnight Kuwait time. If the TV reports a year before 2025 (clock never set), the app shows an Arabic warning instead
of prayer times rather than displaying unverified values.

**Prayer times — calculated on the TV, no internet needed.** I found no documented public API from the Kuwait
Ministry of Awqaf, so the app uses the standard PrayTimes.org astronomical algorithm with the **Kuwaiti method**:
Fajr 18°, Isha 17.5°, Asr standard (shadow = 1×), Maghrib at sunset; location Kuwait City (29.3759 N, 47.9774 E).
Times are rounded to the minute.

*Verification (30 Oct 2026):* identical, to the minute, to the Kuwaiti daily timetable published by Al-Anba and to
taqwim.net's Kuwait table: الفجر 4:39 · الشروق 5:59 · الظهر 11:32 · العصر 2:40 · المغرب 5:04 · العشاء 6:22.
Because times are computed for each Kuwait date, the app can never show yesterday's timetable as today's.
The official Awqaf timetable may occasionally differ by about a minute. Two alternative methods (Umm al-Qura,
Muslim World League) are available in the settings.

**Next prayer and countdown.** Sunrise is shown but is never the "next prayer". At the exact adhan minute the
countdown moves to the following prayer; it never goes negative. After Isha the panel switches to *tomorrow's*
times, labelled «مواقيت الغد», and counts down to tomorrow's Fajr. Between midnight and Fajr it counts down to
today's Fajr. No adhan audio is played.

**Hijri date.** Umm al-Qura calendar from Android's built-in ICU library. This is a Saudi tabulated calendar, **not
an official Kuwaiti source**. It matched the Hijri date printed in Kuwait on 30 Oct 2026 (19 Jumada al-Ula 1448),
but around the start of a month Kuwait's announced date can differ by one day — use «ضبط التاريخ الهجري»
(−1 / 0 / +1). The date changes at midnight (civil convention), not at Maghrib.

## 3. Adhkar

36 entries in `app/src/main/assets/adhkar.json` (was 8): morning, evening, morning-and-evening, tasbih, tahmid,
tahlil, istighfar, salat on the Prophet ﷺ, Prophetic supplications, general adhkar, Ayat al-Kursi and Quranic
supplications. The original 8 are kept.

- Morning adhkar appear from Fajr to Dhuhr, evening adhkar from Asr to midnight; inside those windows they
  alternate with general adhkar. At other times only general adhkar rotate.
- Only narrations from al-Bukhari and Muslim, or Sunan narrations graded sahih, were included; texts I could not
  confirm with confidence were left out (for example the «عفو كريم» wording, whose addition is disputed).
- References use the standard numbering (al-Bukhari per Fath al-Bari, Muslim per Abd al-Baqi). The references for
  Abu Dawud 5088 / Tirmidhi 3388, Abu Dawud 5068, Muslim 2709, Abu Dawud 1522 and Tirmidhi 2140 were checked online
  while building; the rest are well-known references from al-Bukhari and Muslim.
- **Recommended:** before sharing the app widely, have the texts reviewed once against a printed source (e.g.
  حصن المسلم). Editing the JSON file needs no programming.

## 4. Photos

The same 6 Unsplash landscape photographs as before, now listed in `assets/photos.json` so more can be added.
They are real camera photographs, free under the Unsplash License. The app requests them at the TV's resolution
(JPEG), keeps them in a disk cache, pre-fetches the next one, skips any URL that fails, and when offline rotates
through cached photos. If nothing is cached yet, a night-blue background is shown. Photos and adhkar change on
separate timers (defaults 60 s and 30 s).

Google Arts & Culture was not used: its images are not offered for reuse in apps.

## 5. Build on GitHub (no Android Studio)

1. Open https://github.com/alanzys-art/alanzysrepo.
2. Upload **the contents** of this ZIP to the repository root (Add file → Upload files), replacing the old files.
   The `.github` folder is hidden on Mac/Windows; if it does not upload, create the file
   `.github/workflows/build-apk.yml` in GitHub's editor and paste its contents.
3. Optional tidy-up: delete the old `app/src/main/res/drawable/banner.xml` from the repository. It is replaced by
   `banner.png`; if it stays, the build still works and the TV still uses the new PNG banner.
4. Commit to `main`. The **Actions** tab starts "Build Athkari APK" automatically (or run it with *Run workflow*).
5. When it finishes (green tick), open the run and download **Athkari-APK** (a ZIP containing `app-debug.apk`).

### Stable signing (do this once, before step 4)
Android only installs an update over an existing app if both were signed with the same key. GitHub creates a new
random debug key on every run, so without this step **every** new APK requires uninstalling the old one first.

1. Repository → **Settings → Secrets and variables → Actions → New repository secret**.
2. Name: `ATHKARI_DEBUG_KEYSTORE`. Value: the entire contents of the file `ATHKARI_DEBUG_KEYSTORE-secret.txt`
   (delivered separately, not in this ZIP — never commit it to the repository, which is public).
3. Keep a private copy of that file. Every future build will then carry the same signature.

## 6. Install on the Sony Bravia

1. **First time only:** uninstall the old version (Settings → Apps → أذكاري TV → Uninstall). It was signed with a
   different, temporary key, so the new APK cannot install over it.
2. Allow installation from your file app: Settings → Apps → Security & restrictions → Unknown sources → enable it for
   the app you will use (e.g. *File Commander* or *Send files to TV*).
3. Copy `app-debug.apk` to the TV (USB stick, or the *Send files to TV* app from your phone) and open it to install.
4. Launch **أذكاري** from the Apps row.
5. Later updates (once the signing secret is set): install the new APK directly — no uninstall needed.

## 7. Screensaver on Google TV (KD-75X80K)

The app registers `com.adhkaritv.app/.AdhkarDreamService`. Google TV's settings only offer Google's Ambient Mode
and do **not** list third-party screensavers, so it must be selected once over ADB from a computer:

1. TV: Settings → System → About → tap *Android TV OS build* 7 times (enables Developer options).
2. Settings → System → Developer options → enable *USB debugging* / *Network debugging*; note the TV's IP address.
3. On a computer with Android platform-tools:
   ```
   adb connect <TV-IP>
   adb shell settings put secure screensaver_components com.adhkaritv.app/.AdhkarDreamService
   adb shell settings put secure screensaver_enabled 1
   ```
   Accept the "Allow debugging" prompt on the TV.
4. The screensaver starts after the TV's idle timeout. Some firmware updates reset this choice; repeat step 3 if so.
   No root is needed. Inside the screensaver any key wakes the TV (standard behaviour); the settings are reached from
   the app itself.

## 8. Change log (v1.0.0 → v2.0.0)

| File | Change |
|---|---|
| `AdhkarView.java` | Rewritten layout: clock/dates, branding, centred dhikr, prayer panel and countdown; two-layer photo cross-fade with no black frames; morning/evening rotation; start/stop/destroy lifecycle; long texts auto-sized |
| `PrayerCalculator.java` | **New.** On-device prayer times (Kuwaiti method + 2 alternatives), next-prayer logic across midnight |
| `ArabicFormat.java` | **New.** Arabic-Indic digits, 12-hour clock, Arabic weekday/month names, countdown |
| `HijriDate.java` | **New.** Umm al-Qura Hijri date with ±1 day adjustment |
| `PhotoManager.java` | **New.** Disk cache, pre-fetch, screen-sized decoding, offline fallback |
| `AdhkarRepository.java` | **New.** Loads `adhkar.json` |
| `Prefs.java`, `SettingsActivity.java` | **New.** Arabic settings screen for the remote |
| `MainActivity.java` | OK/Menu opens settings; pauses work when hidden; first-launch hint |
| `AdhkarDreamService.java` | Starts/stops with `onDreamingStarted/Stopped`; releases everything on detach |
| `AndroidManifest.xml` | Name أذكاري, launcher icon, TV banner, theme, settings activity, touchscreen not required, RTL |
| `assets/adhkar.json`, `assets/photos.json` | **New.** Content files (36 adhkar; photo list) |
| `assets/fonts/Amiri-Regular.ttf` (+ licence) | **New.** Amiri font, SIL Open Font License |
| `res/mipmap-*/ic_launcher.png`, `res/drawable-*/banner.png`, `drawable-nodpi/crescent.png` | **New.** Icon, TV banner, in-app crescent |
| `res/values/strings.xml`, `styles.xml`, `drawable/window_bg.xml` | Name; night-blue theme so launch has no white flash |
| `res/drawable/banner.xml` | **Removed** (replaced by the PNG banner) |
| `app/build.gradle`, `settings.gradle` | Version 2.0.0; project name Athkari. SDK levels unchanged |
| `.github/workflows/build-apk.yml` | Same build steps; added the optional stable-signing step; artifact renamed Athkari-APK |

### v2.1
- Dhikr shown in a frosted-glass card (blurred copy of the current photo, thin light border); only the text fades.
- Prayer times in a matching glass card: city, next prayer, time remaining, a gold progress bar from the last adhan to the next, and the six times.
- Clock row: time | divider | Gregorian and Hijri dates.
- 19 background photos chosen by the owner (`assets/photos.json`).

## 9. Testing checklist

- [ ] App appears in the Apps row with the أذكاري banner; opens with the remote.
- [ ] Clock matches Kuwait time; ص/م correct; date and Hijri date correct (adjust Hijri in settings if needed).
- [ ] Six prayer times match the Awqaf / newspaper timetable for today (± 1 minute).
- [ ] Gold highlight and countdown point at the right prayer; after Isha the panel says «مواقيت الغد» and counts to Fajr.
- [ ] Leave it running past midnight: date changes and times recalculate without restarting.
- [ ] Photos fade smoothly with no black frame; adhkar change every 30 s; clock and panel never disappear.
- [ ] Unplug the network: no crash; cached photos keep rotating; prayer times still shown.
- [ ] OK opens settings; each value changes; Back returns and changes take effect.
- [ ] Put the TV in standby and wake it: clock and countdown are correct at once.
- [ ] Screensaver (after the ADB step) shows the same screen; any key wakes the TV.

## 10. Notes and limits

- The APK has not been built or run on a TV by me; see the delivery note for exactly what was tested.
- Ramadan's 120-minute Isha rule for the Umm al-Qura option is not applied (the default Kuwaiti method is unaffected).
- No adhan audio, advertising, analytics or tracking.
