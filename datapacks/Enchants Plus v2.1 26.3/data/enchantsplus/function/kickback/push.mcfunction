playsound minecraft:entity.zombie.attack_wooden_door player @a ~ ~ ~ 1

execute at @s facing entity @a[tag=ep.kb_player,limit=1] feet rotated ~ 0 run summon marker ^ ^ ^-1 {Tags:["ep.kb"]}

execute if entity @s[type=player] at @s facing entity @a[tag=ep.kb_player,limit=1] feet rotated ~ 0 if block ^ ^ ^-1 #minecraft:air if block ^ ^1 ^-1 #minecraft:air run tp @s ^ ^ ^-1.5
execute if entity @s[type=player] run return run kill @e[type=marker,tag=ep.kb]

execute store result score #ax ep.kickback run data get entity @s Pos[0] 1000
execute store result score #az ep.kickback run data get entity @s Pos[2] 1000
execute store result score #mx ep.kickback run data get entity @e[type=marker,tag=ep.kb,limit=1] Pos[0] 1000
execute store result score #mz ep.kickback run data get entity @e[type=marker,tag=ep.kb,limit=1] Pos[2] 1000
scoreboard players operation #mx ep.kickback -= #ax ep.kickback
scoreboard players operation #mz ep.kickback -= #az ep.kickback
scoreboard players operation #mx ep.kickback *= #str ep.kickback
scoreboard players operation #mz ep.kickback *= #str ep.kickback
execute store result entity @s Motion[0] double 0.000001 run scoreboard players get #mx ep.kickback
execute store result entity @s Motion[2] double 0.000001 run scoreboard players get #mz ep.kickback
data modify entity @s Motion[1] set value 0.3d
kill @e[type=marker,tag=ep.kb]
