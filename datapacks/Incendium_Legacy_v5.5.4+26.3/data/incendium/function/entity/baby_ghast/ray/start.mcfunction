scoreboard players set #distance in.fire_laser 0
data modify storage incendium:storage Ghast set from entity @s UUID
data remove storage incendium:storage Owner
data modify storage incendium:storage Owner set from entity @s data.owner
function incendium:entity/baby_ghast/ray/iter
