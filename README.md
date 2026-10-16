# What is going on (WIGO)

[English](#what-is-going-on-wigo) · [Русский](#русский)

See other players' live Minecraft menus beside them in the world. WIGO sends menu state and drawing instructions; the observer renders them with their own textures, models, fonts and translations. No screenshots, video or resource packs are streamed.

[Downloads](https://github.com/EyeCrasher07/what-is-going-on/releases) · [Modrinth](https://modrinth.com/project/what-is-going-on) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-is-going-on-wigo) · [Issues](https://github.com/EyeCrasher07/what-is-going-on/issues) · [Contact](https://guns.lol/eyecrasher)

## Getting started

1. Download **one** WIGO JAR for your exact Minecraft version and loader. The filename is `wigo-<loader>-<minecraft>-1.0.0.jar`.
2. Install it on both players' clients and on the dedicated server. For a world opened to LAN, install it on the host and the joining players.
3. Fabric also requires the matching [Fabric API](https://modrinth.com/mod/fabric-api) and Fabric Loader **0.19.5 or newer**. Forge and NeoForge builds do not use Fabric API.
4. Restart Minecraft, join the same world, and approach the other player. The default sharing distance is **8 blocks**. Open a supported menu to share it.
5. Press **F8** to enable or disable WIGO, open settings, or view connection and update information.

Players can watch the panel but cannot click or control another player's menu. WIGO must be installed on the server/host for multiplayer sharing.

## Features

- Live inventories, creative tabs, containers, crafting and workstations, trading, advancements, statistics, books, chat drafts, pause menus and supported settings screens.
- Item icons, stack counts, container names, cursor movement and complete item tooltips, including their backgrounds.
- A centered F8 window with **My menus**, **Other menus** and **Info**.
- Automatic traffic reduction through short IDs, cached descriptions, change updates and combined messages. Text entered by a player is preserved. Standard translated text is looked up locally in the menu owner's language; missing translations can fall back to the original text.
- Russian and English WIGO settings. Vanilla menu resources come from the observing client.
- Known OptiFine chat and settings replacements are recognized. Other unsupported screens can show a named placeholder, which observers may hide.

### My menus

Choose whether to share your menus and use the adjacent gear to select individual menu types. A server ban locks that menu's control and explains why. WIGO's own F8 screens are **private by default**; sharing them requires opting in here and permission from the server.

You can also:

- Share with everyone, a whitelist, or everyone except a blacklist. The player picker has a searchable list.
- Allow viewing from both sides, only in front, or only behind you.
- Reduce your sharing distance within the server's limit, when permitted.
- Enable an observer count and place its eye indicator anywhere on your screen. By default, invisible players and spectators still receive menus but are excluded from this count.
- Select the cursor observers see: the normal arrow, food, an empty air bubble, search, a withered hardcore heart, a heart, or a bug. This does not replace your system cursor.

### Other menus

These settings change how other players' panels look **on your client**:

- Visibility and unsupported-menu placeholders.
- Transparency, size, manual brightness from **0–200%**, or smooth automatic brightness based on nearby world lighting.
- Height up to **±2 blocks**, upward/downward tilt up to **±60°**, and curvature in either direction.
- Optional realistic mirroring when looking at a panel from its back.
- A reset for height and tilt; each settings tab also has a reset button with confirmation.

When lowered, the panel's bottom edge stops above the floor, taking size, tilt and curvature into account. Panels keep their size in narrow spaces; this is not automatic wall or ceiling fitting.

**Economical** is the default display mode: data is sent while your character is on an allowed side of the owner. **Smooth** continues receiving within range on both sides and uses your camera, including F5, to decide visibility and fade at the boundary. Smooth therefore uses more traffic when the panel is hidden. Server bans, sharing choices, player lists and distance limits apply to both modes.

### Info

Info contains project and contact links, the server connection state, received data and the installed version. A background GitHub request checks for a compatible update once per client launch. **Version: Latest** is green; **Version: New available** is yellow. Network errors have a separate status and do not interrupt the game. Updates are not installed automatically.

The received-data counter measures **decoded JSON**, not compressed network packets or total internet usage. It is not a measure of compression savings.

## Server configuration

The server or world host creates `config/wigo-server.toml`. Descriptions appear immediately above their settings in English, then Russian; individual menu rules are at the bottom.

| Setting in `[sharing]` | Default | Effect |
| --- | --- | --- |
| `max_menu_distance` | `8` | Maximum sharing distance, from 1 to 128 blocks. |
| `allow_player_distance` | `true` | Owners may reduce their distance. If false, everyone uses the server maximum. |
| `count_invisible_observers` | `false` | Include players with the Invisibility effect in the owner's count. |
| `count_spectator_observers` | `false` | Include spectators (GM3) in the count. |
| `allow_observer_counter` | `true` | Permit the counter. If false, no count is sent and its F8 controls are locked. |
| `allow_smooth_mode` | `true` | Permit Smooth. If false, everyone uses Economical. |

The two counting filters affect the number sent to the owner, not access to menus. The server sends the filtered total without revealing observer identities. Server restrictions preserve personal preferences so they can take effect again when permitted.

In `[menus]`, `true` permits sharing and `false` prevents transmission of that menu. The owner can still use it normally. For example:

```toml
[menus]
"minecraft:ender_chest" = false
"wigo:settings" = false
```

Menu rules reload within about five seconds. An invalid edit keeps the last valid rules. Menu types absent from a Minecraft version are unused. There are no network-format switches: compression is automatic. Personal settings are stored in `config/wigo.json`.

## Supported builds

WIGO **1.0.0** contains **90 builds**: 35 Fabric, 32 Forge and 23 NeoForge. Choose the exact Minecraft/loader pair; a dash means no build for that pair. The dependency versions below are the build and verification targets.

| Minecraft | Fabric API | Forge | NeoForge |
| --- | --- | --- | --- |
| 1.16.5 | 0.42.0+1.16 | 1.16.5-36.2.42 | — |
| 1.17 | 0.36.0+1.17 | — | — |
| 1.17.1 | 0.46.1+1.17 | 1.17.1-37.1.1 | — |
| 1.18 | 0.44.0+1.18 | 1.18-38.0.17 | — |
| 1.18.1 | 0.46.6+1.18 | 1.18.1-39.1.2 | — |
| 1.18.2 | 0.77.0+1.18.2 | 1.18.2-40.3.12 | — |
| 1.19 | 0.58.0+1.19 | 1.19-41.1.0 | — |
| 1.19.1 | 0.58.5+1.19.1 | 1.19.1-42.0.9 | — |
| 1.19.2 | 0.77.0+1.19.2 | 1.19.2-43.5.2 | — |
| 1.19.3 | 0.76.1+1.19.3 | 1.19.3-44.1.23 | — |
| 1.19.4 | 0.87.2+1.19.4 | 1.19.4-45.4.5 | — |
| 1.20 | 0.83.0+1.20 | 1.20-46.0.14 | — |
| 1.20.1 | 0.92.12+1.20.1 | 1.20.1-47.4.26 | 1.20.1-47.1.106 |
| 1.20.2 | 0.91.6+1.20.2 | 1.20.2-48.1.0 | 20.2.93 |
| 1.20.3 | 0.91.1+1.20.3 | 1.20.3-49.0.2 | 20.3.8-beta |
| 1.20.4 | 0.97.3+1.20.4 | 1.20.4-49.2.9 | 20.4.251 |
| 1.20.5 | 0.97.8+1.20.5 | — | 20.5.21-beta |
| 1.20.6 | 0.100.8+1.20.6 | 1.20.6-50.2.10 | 20.6.141 |
| 1.21 | 0.102.0+1.21 | 1.21-51.0.33 | 21.0.167 |
| 1.21.1 | 0.116.17+1.21.1 | 1.21.1-52.1.16 | 21.1.257 |
| 1.21.2 | 0.106.1+1.21.2 | — | 21.2.1-beta |
| 1.21.3 | 0.114.1+1.21.3 | 1.21.3-53.1.12 | 21.3.97 |
| 1.21.4 | 0.119.4+1.21.4 | 1.21.4-54.1.18 | 21.4.158 |
| 1.21.5 | 0.128.2+1.21.5 | 1.21.5-55.1.14 | 21.5.98 |
| 1.21.6 | 0.128.2+1.21.6 | 1.21.6-56.0.9 | 21.6.20-beta |
| 1.21.7 | 0.129.0+1.21.7 | 1.21.7-57.0.3 | 21.7.25-beta |
| 1.21.8 | 0.136.1+1.21.8 | 1.21.8-58.1.22 | 21.8.54 |
| 1.21.9 | 0.134.1+1.21.9 | 1.21.9-59.0.5 | 21.9.16-beta |
| 1.21.10 | 0.138.4+1.21.10 | 1.21.10-60.1.15 | 21.10.64 |
| 1.21.11 | 0.141.6+1.21.11 | 1.21.11-61.2.1 | 21.11.45 |
| 26.1 | 0.155.3+26.1.2 | 26.1-62.0.9 | 26.1.0.19-beta |
| 26.1.1 | 0.155.3+26.1.2 | 26.1.1-63.0.2 | 26.1.1.15-beta |
| 26.1.2 | 0.155.3+26.1.2 | 26.1.2-64.1.3 | 26.1.2.114 |
| 26.2 | 0.161.0+26.2 | 26.2-65.1.3 | 26.2.0.88 |
| 26.3 | 0.162.0+26.3 | 26.3-66.0.10 | 26.3.0.52-beta |

Runtime Java: **17** for Minecraft 1.16.5–1.20.4 except the special Forge 1.16.5 build, **21** for 1.20.5–1.21.11, and **25** for 26.1–26.3.

Forge **1.16.5** targets Java **8** and supports Forge **36.2.20–36.2.42**, including 36.2.34. Verification also covered Java 16.0.1 (`java-runtime-alpha`) and OptiFine **HD U G8**. This does not guarantee compatibility with every OptiFine release, shader or modded screen.

## Building from source

Each `fabric/<minecraft>`, `forge/<minecraft>` and `neoforge/<minecraft>` directory contains its actual version-specific source, resources and Gradle wrapper. Shared archive helpers are in `gradle/`. No local development adapters or private directories are required.

Use **JDK 25** to run Forge builds and builds for Minecraft 26.x; use **JDK 21** for other Fabric/NeoForge builds. Also install JDK 17 for older versions and JDK 21 where the selected project's compiler toolchain requires it. Gradle must be able to discover those JDKs. Forge 1.16.5 compiles with JDK 17 and Jabel to produce Java 8 bytecode; Jabel is not a runtime dependency.

For example, from the repository root:

```sh
cd fabric/26.3
./gradlew build
```

On Windows, use `gradlew.bat build`. The installable file is in the selected project's `build/libs/` and has no `-dev`, `-slim` or `-sources` suffix. Fabric projects also run the executable protocol checks during `build`; `./gradlew verifyProtocol` runs those checks separately.

All 90 public projects were rebuilt and their JAR contents compared with the tested release artifacts. Runtime verification included native Minecraft rendering across the version matrix and installed Forge/OptiFine 1.16.5 clients on Java 8 and 16. These checks cover the listed builds; they do not promise support for every third-party screen or rendering modification.

Created by **EyeCrasher**. Source code is licensed under [GPL-3.0-only](LICENSE).

---

## Русский

WIGO показывает живые меню других игроков рядом с ними в мире. По сети передаются состояние меню и команды отрисовки. Наблюдатель рисует их своими текстурами, моделями, шрифтами и переводами. Скриншоты, видео и ресурспаки не пересылаются.

[Скачать](https://github.com/EyeCrasher07/what-is-going-on/releases) · [Modrinth](https://modrinth.com/project/what-is-going-on) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/what-is-going-on-wigo) · [Сообщить об ошибке](https://github.com/EyeCrasher07/what-is-going-on/issues) · [Связаться с автором](https://guns.lol/eyecrasher)

### Установка и начало игры

1. Скачай **один** JAR WIGO для своей точной версии Minecraft и загрузчика. Формат имени: `wigo-<загрузчик>-<Minecraft>-1.0.0.jar`.
2. Установи его обоим игрокам и на выделенный сервер. Для мира, открытого по сети, мод нужен у хоста и подключающихся игроков.
3. Для Fabric также нужны подходящий [Fabric API](https://modrinth.com/mod/fabric-api) и Fabric Loader **0.19.5 или новее**. Forge и NeoForge не используют Fabric API.
4. Перезапусти Minecraft, зайди в один мир с другом и подойди к нему. По умолчанию меню видно в пределах **8 блоков**. Открой поддерживаемое меню, чтобы поделиться им.
5. Нажми **F8**, чтобы включить или выключить WIGO, открыть настройки или посмотреть информацию о подключении и обновлениях.

Наблюдатель видит панель, но не может нажимать её кнопки или управлять чужим меню. Для сетевой передачи WIGO должен быть установлен на сервере или у хоста.

### Возможности

- Инвентарь, вкладки креатива, хранилища, верстаки и другие рабочие блоки, торговля, достижения, статистика, книги, черновики чата, пауза и поддерживаемые настройки.
- Предметы, количество в стопке, названия хранилищ, движения курсора и полные подсказки с фоном.
- Окно F8 по центру экрана с разделами **«Мои меню»**, **«Чужие меню»** и **Info**.
- Автоматическая экономия трафика: короткие ID, кэш описаний, передача изменений и объединение сообщений. Введённый игроком текст сохраняется. Стандартные надписи берутся из локальных переводов на языке владельца меню; при отсутствии перевода можно получить исходную надпись.
- Русский и английский язык настроек WIGO. Ресурсы ванильных меню берутся у наблюдателя.
- Поддержка известных замен чата и настроек OptiFine. Для остальных неподдерживаемых экранов можно показывать плашку с названием или скрывать её.

### Мои меню

Здесь можно включить передачу своих меню, а шестерёнка рядом открывает выбор отдельных типов. Если сервер запретил меню, его переключатель заблокирован с пояснением. Собственные окна WIGO на F8 **по умолчанию не передаются**: их нужно отдельно включить, и сервер должен это разрешать.

Также можно:

- Делиться со всеми, с белым списком или со всеми, кроме чёрного списка. Для выбора игроков есть список с поиском.
- Показывать меню с обеих сторон, только спереди или только сзади.
- Уменьшить расстояние показа в пределах серверного ограничения, если администратор разрешил.
- Включить счётчик наблюдателей и расположить индикатор с глазом в удобном месте экрана. По умолчанию невидимые игроки и наблюдатели в GM3 получают меню, но в счётчик не входят.
- Выбрать курсор для наблюдателей: обычная стрелка, еда, пустой пузырёк воздуха, лупа, иссушенное хардкорное сердце, сердце или жук. Системный курсор владельца не меняется.

### Чужие меню

Эти настройки меняют чужие панели **только у тебя**:

- Показ меню и плашек неподдерживаемых экранов.
- Прозрачность, размер, ручная яркость **0–200%** или плавная автояркость по освещению мира рядом с панелью.
- Высота до **±2 блоков**, наклон вверх и вниз до **±60°**, изгиб в обе стороны.
- Реалистичное отражение панели при взгляде с обратной стороны.
- Сброс высоты и наклона; у каждой вкладки также есть сброс её настроек с подтверждением.

При опускании нижний край останавливается над полом с учётом размера, наклона и изгиба. В тесных местах размер сохраняется; автоматической подгонки под стены и потолок нет.

**Экономный** режим включён по умолчанию: данные приходят, пока персонаж находится с разрешённой стороны владельца. **Плавный** получает данные с обеих сторон в пределах радиуса, а видимость и плавное появление определяет камера, включая F5. Поэтому плавный режим расходует больше трафика, пока панель скрыта. Запреты сервера, личные настройки передачи, списки игроков и расстояние действуют в обоих режимах.

### Info

Здесь находятся ссылки на проект и контакты, состояние подключения, полученные данные и версия мода. Один фоновый запрос к GitHub при запуске клиента проверяет совместимое обновление. Зелёная надпись **«Версия: последняя»** означает актуальную версию; жёлтая **«Версия: доступна новая»** — наличие обновления. Ошибка сети показывается отдельно и не мешает игре. Автоматической установки нет.

Счётчик полученных данных показывает **распакованный JSON**, а не размер сжатых пакетов или весь интернет-трафик. По нему нельзя измерить экономию от сжатия.

### Серверный конфиг

Сервер или хост создаёт `config/wigo-server.toml`. Описание стоит прямо над каждой настройкой: сначала английский блок, затем русский. Правила отдельных меню находятся в самом низу.

| Настройка в `[sharing]` | По умолчанию | Что делает |
| --- | --- | --- |
| `max_menu_distance` | `8` | Максимальный радиус показа от 1 до 128 блоков. |
| `allow_player_distance` | `true` | Разрешает владельцам уменьшать радиус. При false у всех действует максимум сервера. |
| `count_invisible_observers` | `false` | Учитывать игроков с эффектом невидимости в счётчике владельца. |
| `count_spectator_observers` | `false` | Учитывать наблюдателей в GM3 в счётчике. |
| `allow_observer_counter` | `true` | Разрешает счётчик. При false число не передаётся, настройки индикатора заблокированы. |
| `allow_smooth_mode` | `true` | Разрешает плавный режим. При false у всех экономный. |

Фильтры счётчика меняют итоговое число, но не доступ к меню. Сервер отправляет владельцу уже отфильтрованное количество без раскрытия личностей наблюдателей. Личные предпочтения сохраняются и снова действуют после снятия запрета.

В `[menus]` значение `true` разрешает передачу, а `false` запрещает её. Сам владелец продолжает пользоваться меню. Например:

```toml
[menus]
"minecraft:ender_chest" = false
"wigo:settings" = false
```

Правила меню обновляются примерно за пять секунд. Ошибочные правки сохраняют последние рабочие правила. Отсутствующие в версии Minecraft меню не используются. Переключателей сетевого формата нет: сжатие работает автоматически. Личные настройки хранятся в `config/wigo.json`.

### Поддерживаемые сборки

В WIGO **1.0.0** доступны **90 сборок**: 35 Fabric, 32 Forge и 23 NeoForge. Выбирай точное сочетание Minecraft и загрузчика; прочерк означает отсутствие сборки. В таблице указаны версии зависимостей, на которых собирался и проверялся мод.

| Minecraft | Fabric API | Forge | NeoForge |
| --- | --- | --- | --- |
| 1.16.5 | 0.42.0+1.16 | 1.16.5-36.2.42 | — |
| 1.17 | 0.36.0+1.17 | — | — |
| 1.17.1 | 0.46.1+1.17 | 1.17.1-37.1.1 | — |
| 1.18 | 0.44.0+1.18 | 1.18-38.0.17 | — |
| 1.18.1 | 0.46.6+1.18 | 1.18.1-39.1.2 | — |
| 1.18.2 | 0.77.0+1.18.2 | 1.18.2-40.3.12 | — |
| 1.19 | 0.58.0+1.19 | 1.19-41.1.0 | — |
| 1.19.1 | 0.58.5+1.19.1 | 1.19.1-42.0.9 | — |
| 1.19.2 | 0.77.0+1.19.2 | 1.19.2-43.5.2 | — |
| 1.19.3 | 0.76.1+1.19.3 | 1.19.3-44.1.23 | — |
| 1.19.4 | 0.87.2+1.19.4 | 1.19.4-45.4.5 | — |
| 1.20 | 0.83.0+1.20 | 1.20-46.0.14 | — |
| 1.20.1 | 0.92.12+1.20.1 | 1.20.1-47.4.26 | 1.20.1-47.1.106 |
| 1.20.2 | 0.91.6+1.20.2 | 1.20.2-48.1.0 | 20.2.93 |
| 1.20.3 | 0.91.1+1.20.3 | 1.20.3-49.0.2 | 20.3.8-beta |
| 1.20.4 | 0.97.3+1.20.4 | 1.20.4-49.2.9 | 20.4.251 |
| 1.20.5 | 0.97.8+1.20.5 | — | 20.5.21-beta |
| 1.20.6 | 0.100.8+1.20.6 | 1.20.6-50.2.10 | 20.6.141 |
| 1.21 | 0.102.0+1.21 | 1.21-51.0.33 | 21.0.167 |
| 1.21.1 | 0.116.17+1.21.1 | 1.21.1-52.1.16 | 21.1.257 |
| 1.21.2 | 0.106.1+1.21.2 | — | 21.2.1-beta |
| 1.21.3 | 0.114.1+1.21.3 | 1.21.3-53.1.12 | 21.3.97 |
| 1.21.4 | 0.119.4+1.21.4 | 1.21.4-54.1.18 | 21.4.158 |
| 1.21.5 | 0.128.2+1.21.5 | 1.21.5-55.1.14 | 21.5.98 |
| 1.21.6 | 0.128.2+1.21.6 | 1.21.6-56.0.9 | 21.6.20-beta |
| 1.21.7 | 0.129.0+1.21.7 | 1.21.7-57.0.3 | 21.7.25-beta |
| 1.21.8 | 0.136.1+1.21.8 | 1.21.8-58.1.22 | 21.8.54 |
| 1.21.9 | 0.134.1+1.21.9 | 1.21.9-59.0.5 | 21.9.16-beta |
| 1.21.10 | 0.138.4+1.21.10 | 1.21.10-60.1.15 | 21.10.64 |
| 1.21.11 | 0.141.6+1.21.11 | 1.21.11-61.2.1 | 21.11.45 |
| 26.1 | 0.155.3+26.1.2 | 26.1-62.0.9 | 26.1.0.19-beta |
| 26.1.1 | 0.155.3+26.1.2 | 26.1.1-63.0.2 | 26.1.1.15-beta |
| 26.1.2 | 0.155.3+26.1.2 | 26.1.2-64.1.3 | 26.1.2.114 |
| 26.2 | 0.161.0+26.2 | 26.2-65.1.3 | 26.2.0.88 |
| 26.3 | 0.162.0+26.3 | 26.3-66.0.10 | 26.3.0.52-beta |

Java для игры: **17** для Minecraft 1.16.5–1.20.4, кроме специальной сборки Forge 1.16.5; **21** для 1.20.5–1.21.11; **25** для 26.1–26.3.

Сборка **Forge 1.16.5** рассчитана на Java **8** и поддерживает Forge **36.2.20–36.2.42**, включая 36.2.34. Проверки также включали Java 16.0.1 из `java-runtime-alpha` и OptiFine **HD U G8**. Поддержка любой другой версии OptiFine, шейдера или модового экрана не гарантируется.

### Сборка из исходников

В каждой папке `fabric/<Minecraft>`, `forge/<Minecraft>` и `neoforge/<Minecraft>` находятся исходники и ресурсы соответствующей версии, а также Gradle wrapper. Общие настройки упаковки — в `gradle/`. Внутренние скрипты разработки и приватные папки для сборки не нужны.

Запускай сборки Forge и Minecraft 26.x на **JDK 25**, остальные Fabric/NeoForge — на **JDK 21**. Также установи JDK 17 для старых версий и JDK 21 там, где он нужен компилятору выбранного проекта. Gradle должен находить эти JDK. Forge 1.16.5 компилируется на JDK 17 с помощью Jabel в байткод Java 8; Jabel не нужен во время игры.

Пример из корня репозитория:

```sh
cd fabric/26.3
./gradlew build
```

В Windows используй `gradlew.bat build`. Готовый мод находится в `build/libs/` выбранного проекта; выбирай файл без суффиксов `-dev`, `-slim` и `-sources`. При сборке Fabric выполняются проверки протокола; отдельно их запускает `./gradlew verifyProtocol`.

Все 90 публичных проектов пересобраны, содержимое их JAR сверено с проверенными файлами релиза. Проверки игры включали отрисовку в настоящих клиентах Minecraft по матрице версий и установленные Forge/OptiFine 1.16.5 на Java 8 и 16. Это проверка перечисленных сборок, а не обещание совместимости с любыми сторонними экранами или изменениями рендеринга.

Автор — **EyeCrasher**. Исходники распространяются по [GPL-3.0-only](LICENSE).
