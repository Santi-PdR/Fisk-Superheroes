# Sonic waves

Canary's sustained ability emits expanding, short-lived waves. They affect targets
during the first 11 ticks; their remaining four ticks are a visual fade. When a
wave permits glass breaking, the port can also break Forge-tagged glass blocks and
panes, with the original distance-dependent chance. The wave's persisted
`BreakGlass` flag, `powers.allowGriefing`, `powers.allowSonicWaveGriefing` and
Minecraft's `mobGriefing` gamerule must all allow it. `mobGriefing` is a
pre-existing port restriction, not a requirement of the 1.7.10 implementation.

Block changes additionally require a server-player owner, a loaded chunk, the
world border, player edit permission, nonnegative hardness and a non-canceled
Forge break event. Non-player or unresolved owners cannot break blocks because
the port does not have an identity for claim checks. The current Forge glass tags
may differ from the original material classification.

Waves shatter nearby icicle projectiles and dislodge thrown shields during their
active phase. Dislodged shields become exactly one item with their original data;
if the item spawn is canceled, the projectile is retained. The port does not
reproduce any item duplication from the old removal path. Icicle
shattering uses the original impact sound event when the external sound pack is
loaded, with approximate snowflake particles. Waves push living and non-living
entities along their actual movement, scaled by size and age as in the original;
a minimum mass prevents infinite impulses for zero-sized entities. The wave
particles approximate the original visual.
