advancement revoke @s only enchantsplus:kickback
execute unless predicate enchantsplus:kickback/one run return fail


execute if predicate enchantsplus:kickback/one run scoreboard players set #lvl ep.kickback 1
execute if predicate enchantsplus:kickback/two run scoreboard players set #lvl ep.kickback 2
execute if predicate enchantsplus:kickback/three run scoreboard players set #lvl ep.kickback 3

scoreboard players operation #str ep.kickback = #lvl ep.kickback
scoreboard players operation #str ep.kickback *= #250 ep.const
scoreboard players add #str ep.kickback 300

tag @s add ep.kb_player
execute on attacker if entity @s[distance=..8] run function enchantsplus:kickback/push
tag @s remove ep.kb_player
