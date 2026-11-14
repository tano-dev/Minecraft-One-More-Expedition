# from (adv): incendium:technical/inventory/sentrys_wrath


clear @s crossbow[custom_data={incendium:{item:'sentrys_wrath'}}, repair_cost=0] 1
loot give @s loot incendium:artifact/weapon/sentrys_wrath

playsound minecraft:entity.item.break master @s ~ ~ ~ 1 1
playsound minecraft:entity.item.break master @s ~ ~ ~ 1 1

advancement revoke @s only incendium:technical/inventory/sentrys_wrath

execute if entity @e[type=experience_orb,distance=..16] run return run kill @e[type=experience_orb,distance=..16]
experience add @s -100