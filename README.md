# What is going on (WIGO)

WIGO displays nearby players' open menus as panels beside them in the world. You can watch another player's menu without controlling it. Panels use your own textures and fonts.

## Features

- View inventories, containers, crafting, trading, books, chat drafts and supported game screens.
- See item icons, stack counts, cursor movements and tooltips.
- Choose which menus to share, who can view them, viewing sides and distance. Optional observer counter and cursor styles.
- Adjust panel size, transparency, manual or automatic brightness, height, tilt and curvature. Preview changes beside the settings.
- Set server limits for shared menus, distance, observer counts and display modes.

## Requirements

- A build matching your Minecraft version and loader: Fabric, Forge or NeoForge.
- Fabric builds require Fabric API and Fabric Loader 0.19.5 or newer.
- Java 17 for Minecraft 1.16.5–1.20.4, Java 21 for 1.20.5–1.21.11, or Java 25 for 26.x. Forge 1.16.5 also supports Java 8.

Controls added by other mods are not shared. Some custom screens added by other mods are unsupported.

## Getting started

1. Put WIGO in the `mods` folder on the server or LAN host and each participating client. Players without WIGO can still join.
2. Join the same world and open a supported menu. The default sharing distance is 8 blocks.
3. Press **F8**, then **Settings**. **My menus** controls sharing; **Other players' menus** changes how you see panels.

Ordinary chat text is shared by default; commands are hidden. You can change both in **My menus**. Unsupported-screen placeholders are hidden by default.

## Build

To build Fabric 26.3, use JDK 25 and run these commands from the repository root:

```sh
cd fabric/26.3
./gradlew build
```

The JAR is saved in `fabric/26.3/build/libs/`.

## License

[GPL-3.0-only](https://github.com/EyeCrasher07/what-is-going-on/blob/main/LICENSE).

[Source code](https://github.com/EyeCrasher07/what-is-going-on) · [Issues](https://github.com/EyeCrasher07/what-is-going-on/issues) · [Modrinth](https://modrinth.com/project/what-is-going-on) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-is-going-on-wigo) · [Contact](https://guns.lol/eyecrasher)
