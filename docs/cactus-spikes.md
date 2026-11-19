# Cactus spikes

Señor Cactus can fire an aimed eleven-spike burst and a radial 72-spike ring.
The configured profile defaults to 1.5 damage with CACTUS and SHARP components
and reduced knockback. Spikes can also come from a cactus trick arrow. Each
projectile lasts at most 200 ticks, has 0.0175 gravity and is consumed on a block
impact or accepted entity hit. A rejected entity hit leaves it in flight; half
of entity contacts clear the target's hurt immunity first, as in the original.
Aimed and ring launches use the legacy 0.16-block sideways offset, eye height minus
0.1, Gaussian spread and speed 4.5; they do not inherit the wearer's movement.
The ring uses each shot's radial yaw to calculate its offset, without changing
the player's actual rotation. The extra ten aimed spikes add the original uniform
spread. The ring uses the original bow sound event and randomized pitch; like other
Fisk sound events it requires the external sound pack.

The projectile uses the legacy `fiskheroes.spike` damage message and a distinct
projectile damage type with 0.3 food exhaustion, credited to the owner and with
the spike as direct source. It does not bypass armor, potion effects or
enchantments. Ownerless cactus-arrow spikes retain the projectile as their direct
source without inventing an attacker. Collision behavior and rendering use modern
Minecraft facilities and have not been exhaustively compared with 1.7.10.

Server GameTests exercise projectile count, hunger, launch offsets, Gaussian spread,
grounded and airborne shooter motion, profile persistence, rejected and accepted
hits, ownerless attacks, damage attribution and armor reduction. A
client/dedicated-server smoke test checks aiming, actual target damage, the ring's
72 accepted spawns and projectile rendering. The ring count is recorded at spawn:
counting surviving entities later is unreliable because fast spikes can collide,
despawn or be removed before a console command executes.

The cactus trick arrow bursts after a block or entity impact, including a nonliving
entity or a target that rejects damage. It creates 20–21 spikes and a non-terrain
explosion of radius 1.999. The original resets previous position at the start of
its tick: an entity impact therefore gives the spikes only uniform ±0.2 motion,
while a wall impact first moves the arrow 0.05 blocks backwards, yielding an
approximately backward unit vector plus that spread. These are initial velocities;
the subsequent explosion and modern collision physics can change them.

For spike collisions, the thrower is ignored for the first four flight ticks,
then becomes a valid target on tick five even if the projectile is still nearby.
Unlike modern vanilla's owner-vehicle exclusion, other collidable entities,
including the thrower's vehicle, are not covered by this grace period. The flight
age restarts after a reload, as the old throwable's unsaved air counter did.
