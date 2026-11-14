# Ran as/at the fight Wither at 30 HP or less in Bedrock mode: it freezes in place, swells and bursts
tag @s add bedrockdeath
scoreboard players set #bscale wither.stats 1000
# HighDPS: the final blow must not count as an enrage hit
tag @s add HighDPS
playsound minecraft:entity.wither.death hostile @a[distance=..64] ~ ~ ~ 2 1
data merge entity @s {Silent:1b,Glowing:1b,NoGravity:1b,Invulnerable:1b}
attribute @s minecraft:max_health base set 1000
data modify entity @s Health set value 1f
summon minecraft:armor_stand ~ ~ ~ {Tags:["wither_stasis"],Invisible:1b,Invulnerable:1b,Marker:1b}

schedule clear wither:wither/phase2/dashes/dash
schedule clear wither:wither/phase2/charge/chargepre
schedule clear wither:wither/phase1/homing/homingpre

schedule function wither:wither/bedrock/death/deadbedrock1t 1t
schedule function wither:wither/bedrock/death/deadbedrockcry 70t append
schedule function wither:wither/bedrock/death/deadbedrockcry 145t append
schedule function wither:wither/bedrock/death/deadbedrockendloop 221t
