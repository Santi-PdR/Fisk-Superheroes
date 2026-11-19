# Sonic waves

Canary's sustained ability emits expanding, short-lived waves. They start 0.2 blocks
below eye height, without inheriting the wearer's motion. The normalized aim is
perturbed by independent Gaussian spread on all three axes as in the original;
it is not renormalized afterward, so its speed is close to, but not exactly, 1.
The default profile deals 7 SOUND damage, with 0.025 knockback and glass breaking;
pack overrides can also set the damage to zero. By default the cry runs while the
key is held; packs can set `isToggle` to switch it on and off with separate presses.
Pack-provided `SHOOT` and `SHOOT_STOP` cues start and stop with the cry, including
loop cleanup when changing suits. A newly tracking listener receives active
loops subject to the pack's sound conditions and listener policy. Disabling the
last enabled sonic card stops the cry and its loop; re-enabling it requires a new
activation. The bundled Canary power does not itself specify these cues.

Waves affect targets during the first 11 ticks; their remaining four ticks are a visual fade. When a
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

The wave uses its own wearer-attributed SOUND damage source, tagged as a
projectile like the original. Unlike the Warden's sonic boom, it does not bypass
armor, enchantments or effects. Its center passes non-colliding plants and stops
at blocks with a collision shape, using modern projectile collision rules.

Waves shatter nearby icicle projectiles and dislodge thrown shields during their
active phase. Dislodged shields become exactly one item with their original data;
if the item spawn is canceled, the projectile is retained. The port does not
reproduce any item duplication from the old removal path. Icicle
shattering uses the original impact sound event when the external sound pack is
loaded, with approximate snowflake particles. Waves push living and non-living
entities along their actual movement, scaled by size and age as in the original;
a minimum mass prevents infinite impulses for zero-sized entities. The client
renders a white, translucent ring with a cylindrical outer wall and an annular
face, following the original radius, thickness, length and opacity; the previous
Minecraft sonic-boom particles have been removed.

Server GameTests cover held and toggled controls, suit removal, profile defaults
and overrides, protected glass and panes, plant and wall collisions, SOUND damage
attribution and armor reduction, active versus fading effects,
directional item impulse, icicle shattering, and shield item preservation when a
spawn is canceled. A dedicated-server test with a real Black Canary client checks
ability input, living-target damage, item impulse, glass-pane breakage, icicle
shattering and one shield drop retaining its data. It also verifies that the
renderer runs and brightens pixels against a dark background, retaining screenshots
for inspection. Runner-only synthetic sound fixtures check loop start, release,
suit-switch cleanup, late multiplayer tracking and dynamic modifier disable. They
do not ship with the mod or verify subjective audio. The projectile probes in
the single-client test are summoned.
