# Suit protection

An active hero suit gives the tier-based damage reduction used by Fisk's Superheroes 1.7.10.
At the original default settings, tier 1 reduces eligible damage by 75%, tier 7 by 94.8%,
and tier 10 by 97.6%. The curve is `round((100 - (100 - baseline) * factor / factor^tier) * 10) / 1000`.
The Forge common config exposes `suits.damageReductionBaseline` (percent, default 75) and
`suits.damageReductionFactor` (default 1.3). Tier 0 means no tier protection.

The hero pack may change a wearer's tier dynamically: for example, inactive Mk50 nanites have
no tier protection, while giant Ant-Man uses a higher tier. A damage-reduction augment on the
active suit core improves the tier's reduction; profiles that revoke augments disable the upgrade,
not the underlying tier. The armor bar in the survival HUD shows this reduction. Vanilla armor
points from other items and enchantments still work; superhero pieces themselves contribute no
vanilla armor points, avoiding a second reduction from the same suit.

Most armor-bypassing damage, including fall, drowning, suffocation and the void, bypasses suit
protection. As in the original, burning, lava and direct magic receive half protection, and
indirect magic receives full protection even though it bypasses vanilla armor. Other powers and
fall resistance may independently cancel or change damage. Martian fire weakness halves the
remaining suit protection against fire, after these source-specific rules. A typed hit counts as
fire when its `FIRE` fraction is at least 0.5, matching the original extended damage source.
Nearby fire, lava or a
burning entity also causes nausea, slowness and weakness. Area scans run every five ticks, while
a burning wearer is checked immediately.

The `requireFullSuit` policy still controls whether a partial suit activates its hero's powers and
protection; when disabled, an active partial suit receives the hero's full tier protection. This
port policy is more permissive than the original tracker, which required the suit's full armor signature. Suit durability scaling is not implemented: ported suit pieces are currently
unbreakable. The HUD currently shows the base protection rather than a source-specific value; it
does not yet reproduce the original metal-skin heat coloring.
