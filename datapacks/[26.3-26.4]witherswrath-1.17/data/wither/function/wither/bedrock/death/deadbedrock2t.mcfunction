function wither:wither/bedrock/death/deadbedrockscale
execute as @e[type=minecraft:wither,tag=bedrockdeath] at @e[type=minecraft:armor_stand,tag=wither_stasis,limit=1] run tp @s ~-0.12 ~0.06 ~

schedule function wither:wither/bedrock/death/deadbedrock3t 1t
