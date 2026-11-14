# from ./fire
# @s: arrow being shot by player

data merge entity @s {pickup:0b,PierceLevel:4b,damage:3.0d,crit:0b}

execute store result entity @s Motion[0] double 0.001 run data get entity @s Motion[0] 410
execute store result entity @s Motion[1] double 0.001 run data get entity @s Motion[1] 410
execute store result entity @s Motion[2] double 0.001 run data get entity @s Motion[2] 410

data modify entity @s NoGravity set value 1b


# tags
tag @s add in.checked
tag @s add in.cluster
tag @s add in.ticking_entity
tag @s add in.air_toggle
