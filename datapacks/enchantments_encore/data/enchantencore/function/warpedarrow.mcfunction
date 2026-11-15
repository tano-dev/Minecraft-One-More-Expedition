# endershot.mcfunction
# Spawns a custom ender_pearl, transfers ownership, launches it,
# then teleports, heals, and grants resistance after impact.

# 1. Detect player holding an ender pearl within 3 blocks and summon custom pearl
execute at @s if entity @n[type=minecraft:player,nbt={Inventory:[{id:"minecraft:ender_pearl"}]},distance=..3] run summon ender_pearl ~ ~ ~ {Tags:["endershot"]}

# 2. Tag the pearl with its shooter’s UUID for later matching
execute as @e[type=ender_pearl,tag=endershot,limit=1,sort=nearest] at @s run data modify entity @s Owner set from entity @p[distance=..3] UUID

# 3. Remove one ender pearl from the player’s inventory
execute at @s run clear @p[distance=..3] minecraft:ender_pearl 1

# 4. Launch the custom pearl with the same motion as the last arrow shot
execute as @e[type=ender_pearl,tag=endershot,limit=1,sort=nearest] at @s run data modify entity @s Motion set from entity @e[type=arrow,distance=..3,limit=1,sort=nearest] Motion

# 5. When the custom pearl impacts, teleport the owner, heal, and give resistance
execute at @s if entity @p[distance=..3,nbt={Inventory:[{id:"minecraft:ender_pearl"}]},limit=1,sort=nearest] run effect give @p[distance=..3,limit=1,sort=nearest] minecraft:absorption 5 0 true
execute at @s if entity @p[distance=..3,nbt={Inventory:[{id:"minecraft:ender_pearl"}]},limit=1,sort=nearest] run effect give @p[limit=1,sort=nearest,distance=..3] minecraft:regeneration 2 1 true
# Clean up the pearl after impact
execute as @e[type=ender_pearl,tag=endershot,nbt={inGround:1b}] at @s run kill @s
