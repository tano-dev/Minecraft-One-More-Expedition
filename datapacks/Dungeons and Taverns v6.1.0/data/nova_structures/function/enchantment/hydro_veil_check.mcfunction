execute store result score @s dnt_mob_health run data get entity @s Health 99999
execute store result score @s dnt_mob_max_health run attribute @s minecraft:max_health get 99999
execute if score @s dnt_mob_health < @s dnt_mob_max_health run function nova_structures:enchantment/hydro_veil_heal
scoreboard players reset @s dnt_mob_health
scoreboard players reset @s dnt_mob_max_health