schedule clear wither:wither/bedrock/death/deadbedrock1t
schedule clear wither:wither/bedrock/death/deadbedrock2t
schedule clear wither:wither/bedrock/death/deadbedrock3t
schedule clear wither:wither/bedrock/death/deadbedrock4t
schedule clear wither:wither/bedrock/death/deadbedrock5t

# The enrage resistance would block the final blow
execute as @e[type=minecraft:wither,tag=bedrockdeath] run effect clear @s minecraft:resistance
execute as @e[type=minecraft:wither,tag=bedrockdeath] run data merge entity @s {Invulnerable:0b,Silent:0b,Glowing:0b,Health:1f}
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] run playsound minecraft:entity.dragon_fireball.explode hostile @a ~ ~ ~ 1 .1
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] run playsound minecraft:entity.dragon_fireball.explode hostile @a ~ ~ ~ 1 .5
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] run playsound minecraft:entity.dragon_fireball.explode hostile @a ~ ~ ~ 1 .9
# Credited to the nearest player so the kill counts (trophy, stats, advancements)
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] as @p run damage @e[type=minecraft:wither,tag=bedrockdeath,limit=1] 10000 minecraft:player_attack by @s
# The Wither bursts in a final explosion
execute at @e[type=minecraft:armor_stand,tag=wither_stasis,limit=1] if data storage wither:options {toggledestruction:Enabled} run summon minecraft:creeper ~ ~ ~ {Team:"Wither",Silent:1b,Invulnerable:1b,CustomNameVisible:0b,ExplosionRadius:7b,Fuse:1,ignited:1b,CustomName:"the Wither's Wrath"}
execute at @e[type=minecraft:armor_stand,tag=wither_stasis,limit=1] run particle minecraft:explosion_emitter ~ ~1 ~ 1 1 1 0 4 normal
kill @e[type=minecraft:armor_stand,tag=wither_stasis]
