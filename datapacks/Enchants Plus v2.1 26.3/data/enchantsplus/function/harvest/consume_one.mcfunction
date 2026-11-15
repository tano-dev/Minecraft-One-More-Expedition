execute store result score #itemcount ep.global run data get entity @s Item.count
scoreboard players remove #itemcount ep.global 1
execute if score #itemcount ep.global matches ..0 run return run kill @s
execute store result entity @s Item.count int 1 run scoreboard players get #itemcount ep.global
