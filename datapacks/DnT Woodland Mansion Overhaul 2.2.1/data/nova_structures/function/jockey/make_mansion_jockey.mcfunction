tag @s add dnt_mansion_jockey
data modify entity @s IsChickenJockey set value 1b
data modify entity @s PersistenceRequired set value 1b
execute summon zombie run execute at @s run ride @s mount @n[tag=dnt_mansion_jockey]
execute on controller run function nova_structures:jockey/make_zombie_baby
tag @s remove dnt_mansion_jockey