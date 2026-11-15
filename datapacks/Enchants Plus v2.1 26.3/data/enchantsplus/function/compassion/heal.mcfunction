execute store result score #cc.pulses ep.global run random value 1..2

execute if entity @s[type=#minecraft:undead] run scoreboard players set #cc.per ep.global 6
execute unless entity @s[type=#minecraft:undead] run scoreboard players set #cc.per ep.global 4

execute if entity @s[type=#minecraft:undead] run effect give @s minecraft:instant_damage 1 0 true
execute unless entity @s[type=#minecraft:undead] run effect give @s minecraft:instant_health 1 0 true
execute if score #cc.pulses ep.global matches 2 if entity @s[type=#minecraft:undead] run effect give @s minecraft:instant_damage 1 0 true
execute if score #cc.pulses ep.global matches 2 unless entity @s[type=#minecraft:undead] run effect give @s minecraft:instant_health 1 0 true

scoreboard players operation #cc.heal ep.global = #cc.pulses ep.global
scoreboard players operation #cc.heal ep.global *= #cc.per ep.global
execute store result storage enchantsplus:tmp hp int 1 run scoreboard players get #cc.heal ep.global
function enchantsplus:compassion/text with storage enchantsplus:tmp

playsound minecraft:entity.player.levelup player @a ~ ~ ~ 0.4 1.9