summon item ~ ~ ~ {Item:{id:"minecraft:oak_button"},PickupDelay:60,Tags:["ep.drop"]}
data modify entity @e[type=item,sort=nearest,limit=1,tag=ep.drop] Item set from entity @s SelectedItem
execute as @e[type=item,sort=nearest,limit=1,tag=ep.drop] run tag @s remove ep.drop
item replace entity @s weapon.mainhand with air

playsound minecraft:entity.item.pickup master @s ~ ~ ~