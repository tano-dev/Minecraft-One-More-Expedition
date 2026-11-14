execute positioned ~-0.05 ~-0.05 ~-0.05 as @n[type=fireball,tag=!in.reflected,dx=0] if entity @s[dx=0] run return run function incendium:entity/baby_ghast/ray/hit
scoreboard players add #distance in.fire_laser 1

execute as @s if predicate incendium:random/50 run particle minecraft:flame ~ ~ ~ 0.01 0.01 0.01 0.0 1 force

execute if score #distance in.fire_laser matches ..250 positioned ^ ^ ^0.2 if block ~ ~ ~ #incendium:airs rotated ~ ~ run function incendium:entity/baby_ghast/ray/iter