# Phase 0: baseline invariants for safe refactoring

These invariants are intentionally explicit to support upcoming decomposition phases.

## Turn progression
1. `nextPlayer()` must:
   - increment `turnCount` by exactly 1,
   - reset `diceValue` to `0`,
   - reset `hasRolledThisTurn` to `false`,
   - reset `memeBankPlayerId` to `-1`,
   - set `currentPhase` to `PLAYING`.
2. Bankrupt players are skipped during `nextPlayer()` iteration.

## Auction lifecycle
1. `startAuction(cellId)` must:
   - set `isInAuction=true`,
   - set `auctionCellId=cellId`,
   - set `currentPhase=AUCTION`,
   - reset `auctionBids`,
   - set `currentAuctionTime=30`.
2. `endAuction()` must:
   - set `isInAuction=false`,
   - reset `auctionCellId`, `auctionStarterPlayerId`, `auctionCurrentPlayerId` to `-1`,
   - reset `auctionBids`,
   - restore `currentPhase=PLAYING`.

## Current baseline tests
- `GameStateBaselineTest` captures the above invariants and acts as pre-refactor safety net.
