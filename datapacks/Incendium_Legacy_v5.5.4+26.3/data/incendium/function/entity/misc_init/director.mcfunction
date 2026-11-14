# from: entity/init
# @s: withered director


tag @s add in.checked
data modify entity @s CustomName set value {translate:"incendium.mob.reactor.director.name",fallback:"Radiation Director"}
attribute @s minecraft:max_health base set 40
data modify entity @s Health set value 40

data modify entity @s equipment.body set value {id:"totem_of_undying",count:1}
loot replace entity @s armor.head loot incendium:artifact/tool/hazmat_helm
loot replace entity @s armor.chest loot incendium:artifact/tool/hazmat_chest
loot replace entity @s armor.legs loot incendium:artifact/tool/hazmat_legs
loot replace entity @s armor.feet loot incendium:artifact/tool/hazmat_boots
data modify entity @s equipment.mainhand set value {id:"minecraft:copper_axe",count:1, components:{enchantments:{"minecraft:sharpness":1}}}
data modify entity @s drop_chances set value {feet:0.05,legs:0.05,chest:0.05,head:0.05,mainhand:0.0,offhand:0.0,body:0.0,}

team join in.noname @s

# set home pos
scoreboard players set @s in.radius_home 5
function incendium:entity/home_pos/get_home
