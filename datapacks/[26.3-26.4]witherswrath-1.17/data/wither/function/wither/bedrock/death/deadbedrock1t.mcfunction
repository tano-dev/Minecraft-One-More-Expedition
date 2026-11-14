function wither:wither/bedrock/death/deadbedrockscale
execute as @e[type=minecraft:wither,tag=bedrockdeath] at @e[type=minecraft:armor_stand,tag=wither_stasis,limit=1] run tp @s ~0.12 ~ ~
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] run particle minecraft:flash{color:[1,1,1,1]} ~ ~1.5 ~ 0 0 0 0 1 force
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] run playsound minecraft:entity.parrot.imitate.wither hostile @a ~ ~ ~ 1 .8

schedule function wither:wither/bedrock/death/deadbedrock2t 1t
