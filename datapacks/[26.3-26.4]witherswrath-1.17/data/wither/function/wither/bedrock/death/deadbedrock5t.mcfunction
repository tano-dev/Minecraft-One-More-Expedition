function wither:wither/bedrock/death/deadbedrockscale
execute as @e[type=minecraft:wither,tag=bedrockdeath] at @e[type=minecraft:armor_stand,tag=wither_stasis,limit=1] run tp @s ~0.08 ~ ~0.08
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] run playsound minecraft:block.nether_sprouts.break ambient @a[distance=..40] ~ ~ ~ 1 .3

schedule function wither:wither/bedrock/death/deadbedrock1t 1t
