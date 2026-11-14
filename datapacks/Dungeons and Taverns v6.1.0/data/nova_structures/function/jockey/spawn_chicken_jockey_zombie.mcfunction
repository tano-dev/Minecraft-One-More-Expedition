data modify entity @s IsBaby set value 1b
data modify entity @s PersistenceRequired set value 1b
data modify entity @s DeathLootTable set value "nova_structures:entities/chicken_jockey_zombie"
attribute @s minecraft:spawn_reinforcements modifier add minecraft:leader_zombie_bonus 0.625 add_value
attribute @s minecraft:max_health modifier add minecraft:leader_zombie_bonus 2.5 add_multiplied_total
data modify entity @s Health set value 50.0f