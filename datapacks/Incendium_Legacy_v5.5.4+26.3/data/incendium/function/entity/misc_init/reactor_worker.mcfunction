# @s: Reactor Workers

tag @s add in.checked
data modify entity @s CustomName set value {translate:"incendium.mob.reactor.worker.name",fallback:"Reactor Worker"}
team join in.noname @s
data modify entity @s equipment.head set value {id:"minecraft:copper_helmet",count:1,components:{"minecraft:trim":{pattern:"minecraft:bolt",material:"minecraft:netherite"},enchantments:{"minecraft:protection":1},"minecraft:enchantment_glint_override":false}}
execute store result score @s in.random run random value 1..3
execute if score @s in.random matches 1 run data modify entity @s equipment.mainhand set value {id:"minecraft:copper_spear",count:1, components:{enchantments:{"minecraft:sharpness":1}}}
execute if score @s in.random matches 2 run data modify entity @s equipment.mainhand set value {id:"minecraft:copper_sword",count:1, components:{enchantments:{"minecraft:sharpness":1}}}
execute if score @s in.random matches 1..2 run loot replace entity @s weapon.offhand loot incendium:artifact/shield/radiation_shield
execute if score @s in.random matches 3 run data modify entity @s equipment.mainhand set value {id:"minecraft:bow",count:1, components:{enchantments:{power:1}}}
data modify entity @s drop_chances set value {feet:0.0,legs:0.0,chest:0.0,head:0.01,mainhand:0.0,offhand:0.0,body:0.0,}



scoreboard players reset @s in.random

# set home pos
scoreboard players set @s in.radius_home 5
function incendium:entity/home_pos/get_home
