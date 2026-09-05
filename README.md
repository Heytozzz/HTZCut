# HTZCut

Dialogue, narration and cinematic events mod for Minecraft, with
event-triggered audio delivery. Built on **NeoForge 1.21.1**, with an
architecture designed for easy porting to other versions/loaders.

> Status: skeleton stage. See checklist below.

## Architecture

The project is split into two Gradle modules to keep porting cheap:

```
htzcut-core/                 Pure Java, zero Minecraft/NeoForge imports.
                              Trigger model, YAML config, audio routing
                              contracts, permission contracts, event
                              dispatch logic.

htzcut-neoforge-1.21.1/      NeoForge 1.21.1 integration layer.
                              Real event hooks, networking, LuckPerms
                              (mod + plugin) detection, Simple Voice Chat
                              integration, HTTP fallback, web editor.
```

**Rule:** `htzcut-core` must never import `net.minecraft.*` or
`net.neoforged.*`. Porting to a new Minecraft version or loader means
writing a new integration module against the same `core` contracts, not
rewriting business logic.

## Audio model

Two audio categories, distinguished purely by **which folder the file
lives in** - never a config field:

```
audios/
├── dialogues/   -> dynamic, server-provided, routed through
│                   Simple Voice Chat (if available) or an HTTP
│                   fallback with client-side caching
└── ui/          -> static, bundled in the jar, played through
                    vanilla's sound system (menus, HUD, clicks)
```

Dialogue audio only ever needs a plain `.ogg`. Everything else -
Opus transcoding for Simple Voice Chat, HTTP-ready copies, per-language
resolution - happens automatically in an internal cache the user never
has to touch.

Event YAML stays intentionally minimal:

```yaml
id: "welcome_dragon_slain"
trigger:
  type: ADVANCEMENT
  value: "minecraft:end/kill_dragon"
audio:
  id: "dragon_defeated"
  file: "dragon_defeated.ogg"
narration:
  text_key: "htzcut.narration.dragon_defeated"
  fallback_locale: "en_us"
conditions:
  permission: "htzcut.trigger.dragon"
  once_per_player: true
```

## Soft dependencies

- **LuckPerms** - detected at runtime as either a NeoForge mod or a
  Bukkit plugin (hybrid servers), falling back to vanilla OP permissions
  if neither is present. See `permission/PermissionCheckerFactory`.
- **Simple Voice Chat** - used for dialogue audio delivery when the
  player has an active voice connection; otherwise the HTTP+cache
  fallback is used automatically, per player. See `audio/`.

## Versioning

Version lives in `version.properties` (`major.minor.patch`). Bump it with:

```
./gradlew bumpVersion -Ptype=BIG    # major++, resets minor/patch
./gradlew bumpVersion -Ptype=MID    # minor++, resets patch
./gradlew bumpVersion -Ptype=PATCH  # patch++
```

## Build

```
./gradlew build
```

CI runs the same build on every push via GitHub Actions
(`.github/workflows/build.yml`) and uploads the resulting jar as an
artifact.

## Checklist - Stage 1 (skeleton)

- [x] Multi-module Gradle structure (`core`, `neoforge-1.21.1`)
- [x] Core contracts: `AudioDeliveryChannel`, `AudioAssetResolver`,
      `PermissionChecker`, `TriggerType` / `HTZTriggerFired`
- [x] `EventDefinition` model + YAML loader (simplified schema)
- [x] `EventDispatcher` (trigger matching, conditions, once-per-player)
- [x] Runtime-safe LuckPerms detection (mod / plugin / vanilla fallback)
- [x] Audio folder convention (`audios/dialogues`, `audios/ui`)
- [x] `AudioWatcherService`, `SimpleVoiceChatDeliveryChannel`,
      `HttpCacheDeliveryChannel` stubs
- [x] `neoforge.mods.toml` with LuckPerms + Simple Voice Chat softdepends
- [x] `version.properties` + `bumpVersion` task
- [x] CI workflow, `.gitignore`, this README

## Next stage

- Real NeoForge event hooks -> `HTZTriggerFired` translation
- Audio transcoding pipeline (`.ogg` -> opus frames + HTTP-ready copy)
- Simple Voice Chat + HTTP delivery channel implementations
- Web editor (event CRUD, audio upload, permission assignment)
