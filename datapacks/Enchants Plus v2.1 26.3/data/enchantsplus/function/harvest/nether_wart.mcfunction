scoreboard players set @s ep.mined_wart 0
execute unless predicate enchantsplus:harvest run return 0
execute as @e[type=item,distance=..8,limit=1,sort=nearest,nbt={Age:0s,Item:{id:"minecraft:nether_wart"}}] at @s if block ~ ~ ~ #minecraft:air if block ~ ~-1 ~ minecraft:soul_sand run function enchantsplus:harvest/plant_nether_wart