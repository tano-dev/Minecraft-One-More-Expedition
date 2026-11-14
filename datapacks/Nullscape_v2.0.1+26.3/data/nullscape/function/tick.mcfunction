# run from #minecraft:tick

# rift gateway linking
execute in minecraft:the_end as @e[type=minecraft:marker,tag=nullscape.rift_gateway,x=0] at @s if data block ~ ~ ~ exit_portal run function nullscape:link_rift

# sculk xp tracking
execute as @a[scores={nullscape.sculk_broken=1..}] at @s run function nullscape:broke_sculk
execute in minecraft:the_end run tag @e[type=minecraft:experience_orb,tag=!nullscape.tagged_exp,x=0] add nullscape.tagged_exp
