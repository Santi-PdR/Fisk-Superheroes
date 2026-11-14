# Suit Augmentor

The Suit Augmentor upgrades attributes on a suit's **core piece** (normally its chestpiece).
The available attributes and their starting and maximum levels come from the hero pack's
`data/augments/<hero>.json`, so different suits have different upgrade paths.

1. Craft and place the Suit Augmentor. Put the core piece into its upper slot.
2. Place the upgrade item requested by the attribute's tooltip into the lower slot.
3. Click the attribute icon. The server consumes one item, raises that attribute by one level,
   and stores the result on the suit core. The item tooltip displays installed upgrades.

There are five upgrade items: iron (I), gold (II), gold-titanium (III), vibranium (IV), and
dwarf-star alloy (V). A level *n* attribute requires the corresponding item to reach *n + 1*.
Each higher-tier item is crafted from six of its metal, two of the previous upgrade, and one
Tutridium gem. The iron item uses six iron and one gem, without previous upgrades:

```text
I I I    I I I
  T      U T U
I I I    I I I
```

The left pattern is the iron upgrade; the right pattern is for tiers II–V. `I` is the appropriate
metal ingot, `T` a Tutridium gem, and `U` the previous tier of upgrade.

The machine itself uses this pattern:

```text
D T D
G A G
S B S
```

`D` is dwarf-star alloy, `T` a Tutridium gem, `G` gold-titanium ingots, `A` an anvil,
`S` stone, and `B` a gold-titanium block. The recipes unlock in the recipe book as you obtain
the relevant materials and the previous tier of upgrade.

The core retains its upgrades when it is removed or changed between compatible suit iterations.
They only apply while the core belongs to the active hero, and a profile that revokes augments
(such as inactive Mk50 nanites) temporarily disables them. Existing suits without upgrades keep
their previous behavior.

The core's integer `Upgrades` tag uses the original six four-bit level slots, in the order of
attributes in the hero's augment JSON. The modern port uses separate item IDs for the five
upgrade types instead of 1.7.10 item metadata. The interface uses the original artwork but does
not yet reproduce its animated button effects. Damage-reduction upgrades act on the port's
existing armor protection, so the complete tier-protection system is not yet identical to 1.7.10.
