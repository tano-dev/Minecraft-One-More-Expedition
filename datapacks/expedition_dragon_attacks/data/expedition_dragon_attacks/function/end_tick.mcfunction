# Context: in the_end, positioned 0 80 0

# keep the pivot marker True Ending's dive uses
execute if score 20tick trueEnding_clock matches 1 unless entity @e[type=marker,tag=trueEnding_pivot,distance=..10] run summon marker ~ ~ ~ {Tags:["trueEnding_pivot"]}

# stage ambience (1:1 from True Ending tick)
execute if score ambience trueEnding_settings matches 1 if entity @p[distance=..128] unless entity @e[type=ender_dragon,distance=..256] run function true_ending:ambience/a_main_no_dragon
execute if entity @p[distance=..128] if entity @e[type=ender_dragon,distance=..256] run function true_ending:ambience/a_main

# Stellarity's dragon drives the copied attacks
execute as @e[type=ender_dragon,tag=stellarity.ender_dragon,distance=..256] at @s run function expedition_dragon_attacks:dragon

# attack / ambience projectile markers (1:1 from True Ending tick)
execute as @e[type=marker,tag=trueEnding_dragonparticle] at @s run function true_ending:ambience/dragon_entity
execute as @e[type=marker,tag=trueEnding_ultrafireball] at @s run function true_ending:boss/ultra_fireball
execute as @e[type=marker,tag=trueEnding_shockwave] at @s run function true_ending:boss/shockwave/root
execute as @e[type=marker,tag=trueEnding_shockwave2] at @s run function true_ending:boss/shockwave/root2
execute as @e[type=marker,tag=trueEnding_pad] at @s run function true_ending:boss/shockwave/pad
