# from: player/on_block
# @s: player being hit

# gives poison to nearby entities
execute as @e[type=!#incendium:other,distance=0.001..4.5] run effect give @s poison 10 0

# shield cooldown
scoreboard players set @s in.cd_shield 120

# audiovisuals
playsound minecraft:entity.evoker.cast_spell player @a[distance=..16] ~ ~ ~ 1 0.75 0.5
summon area_effect_cloud ~ ~0.5 ~ {custom_particle:{type:dust, color:[0,1,0],scale:2},Radius:0.5f,RadiusPerTick:0.8f,Duration:8,WaitTime:0}
summon area_effect_cloud ~ ~0.8 ~ {custom_particle:{type:totem_of_undying},Radius:0.5f,RadiusPerTick:0.8f,Duration:8,WaitTime:0}
particle dust{color:[0,1,0],scale:2} ~ ~ ~ 4.5 4.5 4.5 1 10
execute as @e[type=!#incendium:other,distance=0.001..4.5] at @s run particle minecraft:glow_squid_ink ~ ~1 ~ .1 .1 .1 .1 10 force
execute as @e[type=!#incendium:other,distance=0.001..4.5] at @s run particle minecraft:copper_fire_flame ~ ~1.6 ~ .1 .1 .1 0.2 25 force
execute as @e[type=!#incendium:other,distance=0.001..4.5] at @s run particle minecraft:sneeze ~ ~1 ~ .1 .1 .1 .1 10 force
