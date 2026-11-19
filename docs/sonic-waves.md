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

Some original interactions remain unported: the wave does not currently shatter
icicle projectiles or dislodge thrown shields, and its knockback treatment for
non-living entities differs. The particles also approximate the original visual.
