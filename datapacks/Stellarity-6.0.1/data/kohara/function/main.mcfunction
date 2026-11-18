# Custom status effects
# kohara's Libs only handles loading, ticking and removing,
# the rest has to be set up manually
execute as @e[tag=kohara.status_effect.tick,type=!#kohara:invalid_targets] at @s run function #kohara:status_effects/tick

# Detecting Bow shots, thrown Ender Pearls, etc.
execute as @a run function kohara:as_players

execute as @e[type=minecraft:item_display,tag=kohara.particles,sort=nearest] at @s run function kohara:particles/settings/particle_main

execute as @e[type=minecraft:item,tag=!kohara.ticked] at @s run function kohara:items_tick_once/tick_once

execute as @e[tag=kohara.healed,type=!#kohara:invalid_targets] run function kohara:heal/reset_hp
