# أذكاري TV | Adhkari TV

Android TV / Google TV full-screen Arabic remembrance app with photographic landscape backgrounds and a native Android `DreamService`.

## Build an APK
1. Install Android Studio (with Android SDK 35 and JDK 17).
2. Open this directory as an Android Studio project.
3. Let Gradle sync. Select **Build > Build APK(s)**.
4. Find `app/build/outputs/apk/debug/app-debug.apk`.
5. Transfer it to your Sony KD-75X80K and sideload it using a file manager.

## Screensaver activation
The app registers `com.adhkaritv.app/.AdhkarDreamService`. Google TV may hide third-party dream services. With authorized ADB access, the user may run:

```
settings put secure screensaver_components com.adhkaritv.app/.AdhkarDreamService
settings put secure screensaver_enabled 1
```

These commands may not override Google TV Ambient Mode on all firmware versions. No root is required for the app itself.

## Important limitations
- **This ZIP is source code, not an APK.** It has not been compiled or device-tested in this environment.
- Backgrounds are camera photographs loaded online from Unsplash image URLs. They are not Google Arts & Culture assets; use of Google Arts & Culture media would require source-by-source reuse permission checks. Internet access is required for photos. If a photo fails, the background remains dark or shows the last successfully loaded photo.
- The content currently includes eight short Quranic supplications and a common remembrance, not a complete sourced morning/evening adhkar corpus. The time-of-day heading changes automatically, but the corpus is currently the same throughout the day.
- The app is silent and has no advertising, tracking SDKs, or analytics.
- For a production release, verify every text against a trusted Quran/hadith source and review photo rights and attribution.
