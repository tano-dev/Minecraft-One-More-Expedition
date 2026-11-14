# from: player/on_block
# @s: player being hit

# gives wither to nearby entities
execute as @e[type=!#incendium:other,distance=0.001..4.5] run effect give @s wither 10 0

# shield cooldown
scoreboard players set @s in.cd_shield 120

# audiovisuals
playsound minecraft:entity.evoker.cast_spell player @a[distance=..16] ~ ~ ~ 1 0.75 0.5
summon area_effect_cloud ~ ~0.5 ~ {custom_particle:{type:dust, color:[0,0,0],scale:2},WaitTime:0,Radius:0.5f,RadiusPerTick:0.8f,Duration:8}
summon area_effect_cloud ~ ~0.8 ~ {custom_particle:{type:smoke},WaitTime:0,Radius:0.5f,RadiusPerTick:0.8f,Duration:8}
execute as @e[type=!#incendium:other,distance=0.001..4.5] at @s run particle raid_omen ~ ~1 ~ .1 .1 .1 .1 10 force
execute as @e[type=!#incendium:other,distance=0.001..4.5] at @s run particle smoke ~ ~1.6 ~ .1 .1 .1 .1 15 force
