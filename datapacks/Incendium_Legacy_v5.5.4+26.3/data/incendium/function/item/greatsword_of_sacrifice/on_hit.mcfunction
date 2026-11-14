# from: player/event/on_hit_or_kill
# @s: player hitting


execute store result score @s in.temp_health run data get entity @s Health

# scales on health
execute if score @s in.temp_health matches 1..10 run damage @s 2 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 11..20 run damage @s 4 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 21..30 run damage @s 6 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 31..40 run damage @s 8 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 41..50 run damage @s 10 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 51..60 run damage @s 12 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 61..70 run damage @s 14 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 71..80 run damage @s 16 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 81..90 run damage @s 18 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 91..100 run damage @s 20 incendium:sacrifice by @s from @s
execute if score @s in.temp_health matches 101.. run damage @s 25 incendium:sacrifice by @s from @s




scoreboard players reset @s in.temp_health

particle soul ~ ~1 ~ 0 0 0 0.1 70 force
playsound minecraft:entity.husk.converted_to_zombie player @a[distance=..16] ~ ~ ~ 2 1.5 0
