# from: enchantment/technical/hefty_pickaxe
# @s: player holding pickaxe

# Set score based on nether height

execute if predicate incendium:location/nether_96 run scoreboard players set $mining_speed in.dummy 6
execute if predicate incendium:location/nether_64 run scoreboard players set $mining_speed in.dummy 10
execute if predicate incendium:location/nether_32 run scoreboard players set $mining_speed in.dummy 25
execute if predicate incendium:location/nether_16 run scoreboard players set $mining_speed in.dummy 38
execute if predicate incendium:location/nether_0 run scoreboard players set $mining_speed in.dummy 55

execute unless predicate incendium:dimension/nether run scoreboard players set $mining_speed in.dummy 0

# Modify pickaxe
item modify entity @s weapon.mainhand incendium:hefty_pickaxe_update
