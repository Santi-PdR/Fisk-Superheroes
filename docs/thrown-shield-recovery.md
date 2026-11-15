# Thrown shield recovery

The original `EntityThrownShield` has its own bounce and electromagnetic return rules; the port's existing projectile is simpler and always homes after impact or a long flight. This change does not claim to implement the original ricochets, magnetic variants, throw controls, or rendering.

The port previously used the same 200-tick age both to start a long-flight return and to abort it. A shield that had not hit anything therefore dropped on the next tick rather than getting a chance to reach its owner. Outbound and return timeouts are now separate. The return timer is saved with the projectile and defaults to zero for older saves. Homing movement is capped at the remaining distance to avoid overshooting. If a non-player owner is assigned by another mod, the item is dropped instead of discarded. The alternate shield request packet also retains the item if entity spawning fails.

Regression GameTests exercise long-flight recovery with item data, the saved return deadline and timed-out drop, and non-player owners. These are server-side tests; the shield's existing controls and visuals are unchanged.
