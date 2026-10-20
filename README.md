# Open Mojang

**Unofficial open-source Minecraft Bedrock launcher for Android — v0.1**

Looks and feels like the official Minecraft launcher. Version catalog, Microsoft account layer, skins, system install detect, and same-day mirror links for every Mojang build.

> Not affiliated with Mojang, Microsoft, or Xbox.

## Version

**0.1.0** — stays on 0.1 until explicitly bumped.

## Features (0.1)

| Area | Status |
|------|--------|
| Official-style UI (dirt/grass, MC buttons) | Done |
| Version catalog + changelogs | Done (9 builds) |
| Open download mirrors (mcpe-planet / mcpedl) | Done |
| Detect system Minecraft + Launch | Done |
| Microsoft account UI + local store | Done (demo sign-in) |
| Skin list + apply hooks | Done (apply needs real MSAL) |
| DownloadEngine (OkHttp → FileProvider) | Done |
| GitHub Actions CI | In progress |
| Real MSAL Azure login | Next |
| Direct APK scrape | Next |

## Architecture

```
app/src/main/java/com/openmojang/launcher/
├── MainActivity.kt
├── OpenMojangApp.kt
├── data/
│   ├── AccountStore.kt      # SharedPreferences profile
│   ├── AuthManager.kt       # Demo + MSAL hook
│   ├── DownloadEngine.kt    # APK download + install intent
│   ├── InstalledVersions.kt # System package + tracked builds
│   ├── SkinManager.kt       # Local PNG skins
│   └── VersionRepository.kt # Catalog + changelogs
└── ui/
    ├── OpenMojangApp.kt     # Nav + home
    ├── theme/Theme.kt
    └── screens/
        ├── AccountScreen.kt
        ├── SettingsScreen.kt
        ├── SkinScreen.kt
        └── VersionManagerScreen.kt
```

## Build

```bash
git clone https://github.com/certier/open-mojang.git
cd open-mojang
# Android Studio: open folder, sync, Run
# or: gradle wrapper && ./gradlew assembleDebug
```

Requirements: JDK 17, Android SDK 35, Android Studio.

## License

MIT
