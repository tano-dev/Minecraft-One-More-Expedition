# grabs current pos and custom radius from mob
execute store result storage incendium:temp Home.r int 1 run scoreboard players get @s in.radius_home
data modify storage incendium:temp Home.Pos set from entity @s Pos
execute as @s run function incendium:entity/home_pos/set_home with storage incendium:temp Home