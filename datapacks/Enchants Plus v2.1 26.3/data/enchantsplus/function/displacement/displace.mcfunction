execute if entity @s[type=ender_dragon] run return fail
particle minecraft:reverse_portal ~ ~1 ~ 0.2 0.4 0.2 0.01 50
playsound minecraft:entity.enderman.teleport master @a[distance=..8]
tag @s add ep.displace
execute as @p[nbt={SelectedItem:{components:{"minecraft:enchantments":{"enchantsplus:displacement_curse":1}}}}] at @s positioned ^ ^ ^-1 run spreadplayers ~ ~ 0 3 under 3 false @e[tag=ep.displace,limit=1,sort=nearest]
tag @s remove ep.displace
