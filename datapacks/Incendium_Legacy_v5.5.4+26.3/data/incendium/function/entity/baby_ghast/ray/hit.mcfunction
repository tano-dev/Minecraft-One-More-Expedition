
data modify entity @s Owner set from storage incendium:storage Ghast
tag @s add in.reflected
tag @s add in.in_motion


function incendium:entity/baby_ghast/ray/fireball_motion
data modify entity @s Motion set from storage incendium:storage Motion
data modify entity @s Owner set from storage incendium:storage Owner

tag @s remove in.in_motion


playsound minecraft:entity.dragon_fireball.explode neutral @a[distance=..50] ~ ~ ~ 1 1.6
particle minecraft:flame ~ ~ ~ 0 0 0 0.7 140 force