# @s: ghast sentry


tag @s add in.checked

data modify entity @s CustomName set value {translate:"incendium.mob.reactor.sentry.name",fallback:"Reactor Sentry"}
attribute @s minecraft:max_health base set 40
attribute @s minecraft:scale base set 0.8
attribute @s minecraft:armor base set 10
attribute @s minecraft:follow_range base set 32
data modify entity @s Health set value 40
data modify entity @s ExplosionPower set value 2
data merge entity @s {equipment:{mainhand:{id:"totem_of_undying", count:4}}}
data modify entity @s DeathLootTable set value 'incendium:entity/sentry'
data modify entity @s drop_chances set value {feet:0.0,legs:0.0,chest:0.0,head:0.0,mainhand:0.0,offhand:0.0,body:0.0,}

team join in.noname @s

# set home pos
scoreboard players set @s in.radius_home 20
function incendium:entity/home_pos/get_home

