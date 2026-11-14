scoreboard players add $distance in.fire_laser 1
execute if predicate incendium:random/70 run particle minecraft:dust{color:[1.0, 1.0, 1.0], scale:1.0} ~ ~ ~ 0.01 0.01 0.01 0.0 1 force
execute if score #hit in.fire_laser matches 0 if score $distance in.fire_laser matches ..40 positioned ^ ^ ^0.1 rotated ~ ~ run function incendium:item/voltaic_trident/dna/ray
