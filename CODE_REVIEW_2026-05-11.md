# Memopoly code review (2026-05-11)

## Scope
- `core/src/main/java/com/memopoly/**`
- Focus: stability, networking, threading, UI lifecycle, maintainability.

## Critical issues

1. **Potential texture leak in Decks modal previews**
   - File: `Screens/MainMenuScreen.java`
   - In `showDecksDialog()`, preview textures are created for each deck row (`new Texture(...)`) but are not disposed when dialog closes.
   - Risk: memory growth if user opens deck modal many times.
   - Recommendation: cache previews with managed lifecycle (`AssetManager` / `Map<String, Texture>`) and dispose on screen `dispose()`, or dispose all dialog-created textures on close callback.

2. **`DeckRepository` stores absolute filesystem paths**
   - File: `modding/DeckRepository.java`
   - Current design saves absolute image paths in deck JSON.
   - Risk: decks break when files are moved / shared between machines / packed builds.
   - Recommendation: copy imported files into app-local deck folder (`modding/decks/<deck-id>/`) and save relative paths.

3. **Server lock usage around packet handling can reduce throughput and increase contention**
   - File: `network/GameServer.java`
   - `received()` wraps all packet handling in one global lock; some branches also re-enter sync.
   - Risk: long operations block all network packet processing.
   - Recommendation: keep lock sections minimal, avoid nested `synchronized(stateLock)` and move heavy operations outside lock where possible.

## High-priority improvements

4. **Hardcoded UI strings block localization rollout**
   - Files: `Screens/MainMenuScreen.java`, likely other screens.
   - Many labels/messages are hardcoded RU/EN text directly in code.
   - Recommendation: centralize into `LanguageManager` keys and text resources.

5. **Monolithic `GameServer` responsibilities**
   - File: `network/GameServer.java`
   - Class handles transport, game rules, turn flow, auction timers, and battle dispatch.
   - Risk: hard testing, fragile changes, regression risk.
   - Recommendation: split into services (`TurnService`, `CellResolver`, `AuctionService`, `BattleService`) with explicit interfaces.

6. **Insufficient validation and explicit server responses for invalid actions**
   - Files: `network/GameServer.java`, packets
   - Invalid client actions are often silently ignored.
   - Risk: poor debuggability and client UX inconsistency.
   - Recommendation: send structured reject/error packets for invalid actions.

## Medium-priority issues

7. **`Math.random()` for dice**
   - File: `network/GameServer.java`
   - Recommendation: switch to `ThreadLocalRandom.current().nextInt(1, 7)` for clarity and consistency.

8. **Verbose `System.out.println` logging in production paths**
   - Files: many (`GameServer`, screens)
   - Recommendation: unify with a logger and log levels.

9. **Potential UI scaling/accessibility constraints**
   - Files: `Screens/MainMenuScreen.java` and others
   - Fixed sizes (`270x80`, etc.) may not adapt to all resolutions.
   - Recommendation: parameterize sizing from viewport and use responsive layout constraints.

10. **Missing persistence/versioning strategy for modding data**
    - File: `modding/DeckRepository.java`
    - Recommendation: add JSON schema version + migration handling for future compatibility.

## Suggested staged roadmap (safe, non-breaking)

### Stage 1 (quick wins)
- Dispose deck preview textures correctly.
- Move deck image storage to app-local folder while keeping backward compatibility with absolute paths.
- Replace `System.out` with structured logger wrappers.

### Stage 2
- Introduce packetized error responses for invalid actions.
- Shrink `GameServer` synchronization scope and remove nested locks.
- Extract turn/cell logic services.

### Stage 3
- Full localization key migration.
- Introduce deck metadata schema versioning and migration tools.
- Add tests for turn rules, auction/battle intersections, and bankruptcy edge cases.

## Regression safety checklist
- Confirm host/guest lobby flow unchanged.
- Confirm game start guard (`>= 2` players) unchanged.
- Confirm deck creation/listing still works without texture assets for deck buttons.
- Confirm old `modding/decks.json` can still load.
