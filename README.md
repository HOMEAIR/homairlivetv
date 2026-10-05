# Home Air Live

Home Air Live is a dual-mode Android live TV application for Android TV/Google TV and Android phones/tablets.

## Current implementation

- TV-first fullscreen live player.
- Remote-friendly channel zapping with Page Up/Page Down, Channel Up/Down, D-pad Up/Down, numeric channel entry, Left to open the channel sidebar, and Back to close it.
- Mobile dashboard with touch-friendly browsing and vertical swipe channel switching.
- First-launch TV/Mobile mode selection with persisted preference.
- M3U/M3U8 playlist parsing with common IPTV metadata.
- HLS/DASH playback through AndroidX Media3.
- Retry/reconnect handling and configurable buffering.
- Favorites and recent channels.
- Secure runtime token storage using Android Keystore.
- Playlist URL and token configuration from the Settings screen rather than hard-coding secrets.
- GitHub Actions CI and signed GitHub Release workflows.
- Orange/white Home Air Live visual identity with animated triangle/yellow signal logo.

## Important security note

Do not hard-code a master playlist token in source code, resources, Gradle files, YAML, or the APK. APK secrets can be extracted.

This project stores a user-supplied stream token locally using Android Keystore encryption. For a higher-security deployment, replace the direct token flow with a backend/token-exchange service that issues short-lived stream credentials.

## Build

The repository is configured for Android Gradle Plugin 9.4.0, Gradle 9.6.0, Kotlin 2.4.10, Compose BOM 2026.09.00, and Media3 1.11.1.

Open the project in a current Android Studio release, or use Gradle 9.6 from the command line.

For the GitHub release workflow, configure:

- ANDROID_KEYSTORE_BASE64
- KEYSTORE_PASSWORD
- KEY_ALIAS
- KEY_PASSWORD

Then push a tag such as `v1.0.0`.

## Production playlist

Open Home Air Live -> Settings and enter your HTTPS M3U/M3U8 playlist endpoint and access token/header configuration.

Use only streams you are authorized to distribute.
