# Open Mojang

**Unofficial open-source Minecraft Bedrock Edition launcher for Android.**

Looks and feels like the official Minecraft launcher. Downloads every Mojang build (stable + beta) the moment it appears, installs it, keeps old versions side-by-side, handles Microsoft accounts + skins, and shows real patch notes inside the same UI.

> Not affiliated with Mojang, Microsoft, or Xbox. You still need a legitimate Microsoft account that owns Bedrock.

## Features

- **Official UI clone** – same dirt/grass palette, button styles, typography, layout, and flow as the real Minecraft Android launcher
- **Instant updates** – pulls the newest release and beta the same day Mojang ships them
- **Version manager** – keep as many old builds as you want, switch instantly, delete what you don’t need
- **Microsoft account** – full sign-in, profile, and skin management (browse, upload, apply)
- **In-launcher patch notes** – “What Minecraft 26.52 fixes”, full changelogs, beta notes
- **Clean install flow** – download → verify → install with one tap (REQUEST_INSTALL_PACKAGES)
- **No forced mods** – pure vanilla launcher first, optional module support later

## Status

Actively built. Core structure, UI shell, download engine, and account layer are going in now.

## Building

Requirements:
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 34+

```bash
git clone https://github.com/certier/open-mojang.git
cd open-mojang
./gradlew assembleDebug
```

## License

MIT – do whatever you want, just don’t claim it’s official.

---

Built for people who want the real launcher experience without waiting on the Play Store.