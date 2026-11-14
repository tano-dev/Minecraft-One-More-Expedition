# from (adv): incendium:technical/inventory/firestorm


clear @s crossbow[custom_data~{incendium:{item:'firestorm'}}, repair_cost=0] 1
loot give @s loot incendium:artifact/weapon/firestorm

playsound minecraft:entity.item.break master @s ~ ~ ~ 1 1
playsound minecraft:entity.item.break master @s ~ ~ ~ 1 1

advancement revoke @s only incendium:technical/inventory/firestorm

execute if entity @e[type=experience_orb,distance=..16] run return run kill @e[type=experience_orb,distance=..16]
experience add @s -100