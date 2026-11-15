scoreboard players set @s ep.mined_wheat 0
execute unless predicate enchantsplus:harvest run return 0
execute as @e[type=item,distance=..8,limit=1,sort=nearest,nbt={Age:0s,Item:{id:"minecraft:wheat_seeds"}}] at @s if block ~ ~ ~ #minecraft:air if block ~ ~-1 ~ minecraft:farmland run function enchantsplus:harvest/plant_wheat