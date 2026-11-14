scoreboard players set @s nullscape.sculk_broken 0

execute unless predicate {type:"minecraft:location_check",predicate:{dimension:"minecraft:the_end"}} run return 0
execute unless predicate {type:"minecraft:location_check",predicate:{biomes:"nullscape:infestation"}} run return 0

kill @e[type=minecraft:experience_orb,distance=..15,tag=!nullscape.tagged_exp]
