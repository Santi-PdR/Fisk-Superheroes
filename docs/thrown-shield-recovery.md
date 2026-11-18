# Thrown shield recovery

The original `EntityThrownShield` has its own bounce and electromagnetic return rules; the port's existing projectile is simpler and always homes after impact or a long flight. The port does not yet implement the original ricochets or magnetic variants.

The port previously used the same 200-tick age both to start a long-flight return and to abort it. A shield that had not hit anything therefore dropped on the next tick rather than getting a chance to reach its owner. Outbound and return timeouts are now separate. The return timer is saved with the projectile and defaults to zero for older saves. Homing movement is capped at the remaining distance to avoid overshooting. If a non-player owner is assigned by another mod, the item is dropped instead of discarded. The alternate shield request packet also retains the item if entity spawning fails.

Regression GameTests exercise long-flight recovery with item data, the saved return deadline and timed-out drop, and non-player owners. These are server-side recovery tests.

## Throw input

The original item throws on a primary-attack swing while `SHIELD_THROW` is held; pressing the ability key by itself does not throw. The port now follows that two-input gesture for the default Captain America bindings. The client sends a request only on an attack press while the ability is held. The server rechecks the active suit, enabled modifier, held key, main-hand item and cooldown, and applies the cooldown only after a successful spawn. The original pack's `THROW` sound event is used instead of the trident cue; like other Fisk sounds, it requires the external sound pack.

## Projectile rendering

The original `RenderThrownShield` draws the shield mesh in flight orientation and spins it by 32 degrees per unit of velocity. The projectile now uses the existing shield geometry directly with interpolated yaw, pitch and spin instead of the generic camera-facing thrown-item renderer. Velocity-driven spin is tested server-side and the client smoke test uses a temporary diagnostic texture to verify rendered pixels. Suit-specific projectile liveries, electromagnetic model differences and enchantment glint are not yet reproduced.

## Blocking permission

As in the original, raising Captain America's shield requires an active hero granting `USE_SHIELD` and no shared shield cooldown. The gate runs on both client and server. When raised, the item uses the original 35-degree yaw and pitch coverage, 80 shared shield health, 100-tick cooldown, durability loss and seven-tick repeat-hit guard. The hit that depletes the shield passes through. Vanilla's wider blocking arc is disabled for this item only. Held shields wait 20 ticks before regenerating ten shield health per second; without an active or held shield, recovery starts immediately. Fall damage while looking down and flight-into-wall damage are also blockable as in the original. The port supports both hands; the original predates vanilla offhand items.

## Impact

The original uses an indirect projectile damage source attributed to the thrower and wears the shield on a successful hit. The port uses a tagged shield-specific projectile source with the original translated death-message key, applies one durability point (including Unbreaking and creative rules), and applies the shield's Fire Aspect on successful hits. The original's impact damage calculation includes the shield's enchantment bonus, suit weapon attributes, strength and weakness, a 1.5 multiplier and speed scaling capped at full damage; the shield also adds six melee attack damage when held in the main hand and wears on a successful melee hit. Knockback and immediate homing return still differ from the original bounce-dependent flight.
