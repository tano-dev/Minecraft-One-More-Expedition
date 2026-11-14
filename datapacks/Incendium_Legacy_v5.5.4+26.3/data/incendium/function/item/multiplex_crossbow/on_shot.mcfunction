# from (adv): technical/multiplex_crossbow

execute as @n[type=#arrows,tag=!in.checked,distance=..5] positioned as @s run function incendium:item/multiplex_crossbow/arrow/shot
execute as @e[type=#arrows,tag=!in.checked,distance=..5] run tag @s add in.checked
advancement revoke @s only incendium:technical/multiplex_crossbow
