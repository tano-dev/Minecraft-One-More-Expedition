# set home pos and radius from stored info
$execute as @s at @s run data merge entity @s {home_pos:$(Pos),home_radius:$(r)} 
$execute as @s[type=vex] at @s run data merge entity @s {bound_pos:$(Pos)} 



scoreboard players reset @s in.radius_home
