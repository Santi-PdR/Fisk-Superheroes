# Cactus spikes

Señor Cactus can fire an aimed eleven-spike burst and a radial 72-spike ring.
The configured profile defaults to 1.5 damage with CACTUS and SHARP components
and reduced knockback. Spikes can also come from a cactus trick arrow. Each
projectile lasts at most 200 ticks, has 0.0175 gravity and is consumed on a block
impact or accepted entity hit. A rejected entity hit leaves it in flight; half
of entity contacts clear the target's hurt immunity first, as in the original.

The projectile uses the legacy `fiskheroes.spike` damage message and a distinct
projectile damage type with 0.3 food exhaustion, credited to the owner and with
the spike as direct source. It does not bypass armor, potion effects or
enchantments. Ownerless cactus-arrow spikes retain the projectile as their direct
source without inventing an attacker. Other components of the projectile's
movement and rendering are still port approximations and have not been exhaustively
compared with 1.7.10.

Server GameTests exercise projectile count, hunger, profile persistence, rejected
and accepted hits, ownerless attacks, damage attribution and armor reduction. A
client/dedicated-server smoke test checks aiming, actual target damage, the ring's
72 accepted spawns and projectile rendering. The ring count is recorded at spawn:
counting surviving entities later is unreliable because fast spikes can collide,
despawn or be removed before a console command executes.
