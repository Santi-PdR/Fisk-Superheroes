# Captain Cold's Cold Gun (1.20.1 port)

## Controls and server behavior

Wear a hero suit that grants `USE_COLD_GUN`, hold the main-hand Cold Gun and
hold the suit's **AIM** binding. The default Captain Cold suit binds AIM to
primary attack. Sneak and use the gun while not aiming to switch between beam
and cryo grenade; both modes require fully raised aim. A suit with a different
AIM binding may instead hold AIM and item use together.

The server validates the player, held stack, permission, aim, mode and
cooldown. Beam packets are only held-input heartbeats: the server performs at
most one raycast per tick and stops on release (or heartbeat timeout if the release packet is lost), suit/item
changes, death, or after approximately 611 use ticks. After full aim the beam
charges for 10 ticks, then grows over five ticks to a 32-block range. The
first collidable entity or block stops it. The default hit is 7 damage, with
Fisk's COLD profile and projectile/absolute-damage flags; the port models the
latter with the 1.20 damage-type tags that bypass potion and enchantment
reductions, but not armor. Vanilla hurt immunity still controls how often
health can actually decrease. The cryo ball uses the original default base
profile of 9 damage (60% COLD, 40% EXPLOSION). As in the original,
blast damage and knockback depend on distance and sampled hitbox exposure; partial
cover reduces the impact, and solid cover blocks it. Grenades have a 20-tick server-side per-player
cooldown; releasing the beam has a four-tick cooldown. Replacing an item stack
does not bypass these cooldowns.

Beam world changes require both `powers.allowGriefing` and
`powers.allowColdGunGriefing`, world-border and player edit permissions, plus
cancellable Forge break/placement events. The weapon-specific rule does not
change the separately implemented cryo burst. Placement uses
captured snapshots so a denied change is restored before neighbor notification.
Player-owned cryo bursts use the same protections; bursts without a server
player owner do not change blocks, because there is no identity for claim checks.
Protection plugins must support
Forge's standard block events to intercept these effects. The new beam-release
packet changes the networking protocol: server and clients must use this same
mod version.

## Known differences from 1.7.10

* The original layers a special, gradually thickening ice residue on surfaces;
  that block and its melt behavior are not yet ported. The present approximation
  freezes water, solidifies lava, removes adjacent fire and places snow where it
  can survive. Cryo bursts scale frozen ticks and slowing with distance, exposure,
  damage and COLD resistance, but this remains an approximation of temperature.
  Explosion impulse uses the modern vanilla Blast Protection calculation; it may
  differ from the old enchantment implementation.
* The original lowers an independently tracked body temperature by 0.1 per
  contact tick (with COLD resistance). The port approximates this with vanilla
  frozen ticks and a brief slowing effect, including contacts where damage was
  rejected. Frozen-tick gain compensates for vanilla thaw while the beam hits;
  it is not a recreation of the original temperature, HUD or speedster mechanics.
* `powers.coldGunRange` and `powers.coldGunDamage` set the global beam range and
  damage; they default to the original 32 blocks and 7 damage. The range is
  bounded to 0.01–256 blocks for server safety. Original per-suit rule
  overrides are not yet exposed. Recoil animation and ice-layer grief rules
  remain incomplete. The beam and hit particles approximate the original.

The Cold Gun beam uses its original start, stop and moving loop events from the
downloaded Fisk sound repository. The loop follows server-validated use, ramps
up over three ticks, and can be heard by tracking players within 64 blocks. The
original stereo recording is attenuated by distance in software because OpenAL
does not spatialize stereo; mono replacement recordings use native positioning.
The brief start clip is currently played at the starting position, rather than
following the player and cutting off on early release as in 1.7.10.
A client that begins tracking an active beam starts the loop too. As with other
Fisk sounds, the external sound pack must be available and loaded; the gameplay
does not depend on its download.
