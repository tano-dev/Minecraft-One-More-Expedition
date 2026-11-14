

tag @s add in.checked

tag @s add in.wither_plus

particle large_smoke ~ ~ ~ 0.0 0.0 0.0 0.4 150 force
particle reverse_portal ~ ~ ~ 0.0 0.0 0.0 2 150 force
playsound entity.wither.spawn hostile @a[distance=..75] ~ ~ ~ 1 0.5
attribute @s scale base set 1.3
attribute @s max_health base set 600
data modify entity @s Health set value 600
data modify entity @s DeathLootTable set value "incendium:artifact/patron/warping_witherblade"



advancement grant @a[distance=..50] only incendium:misc/summon_wither_plus