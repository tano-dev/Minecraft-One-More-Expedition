execute unless entity @s[nbt=!{SelectedItem:{}}] positioned ~ ~1 ~ if block ^ ^ ^2 #enchantsplus:air run summon item ^ ^ ^2 {Item:{id:"minecraft:acacia_button"},PickupDelay:60,Tags:["ep.drop"]}
execute unless entity @s[nbt=!{SelectedItem:{}}] positioned ~ ~1 ~ unless block ^ ^ ^2 #enchantsplus:air run summon item ~ ~ ~ {Item:{id:"minecraft:oak_button"},PickupDelay:60,Tags:["ep.drop"]}
data modify entity @e[type=item,sort=nearest,limit=1,tag=ep.drop] Item set from entity @s SelectedItem
execute as @e[type=item,sort=nearest,limit=1,tag=ep.drop] run tag @s remove ep.drop
item replace entity @s weapon.mainhand with air

playsound minecraft:entity.item.pickup master @s ~ ~ ~