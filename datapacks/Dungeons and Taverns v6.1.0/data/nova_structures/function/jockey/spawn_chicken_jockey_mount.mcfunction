tag @s add dnt_chicken_jockey
data modify entity @s IsChickenJockey set value 1b
data modify entity @s PersistenceRequired set value 1b
execute summon zombie run execute at @s run ride @s mount @n[tag=dnt_chicken_jockey]
execute on controller run function nova_structures:jockey/spawn_chicken_jockey_zombie
tag @s remove dnt_chicken_jockey