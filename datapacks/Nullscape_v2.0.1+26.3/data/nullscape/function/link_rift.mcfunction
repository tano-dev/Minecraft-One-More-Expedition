# @s = nullscape.rift_gateway marker in gateway block that has exit_portal data
# at @s
# run from nullscape:tick

# store pos
data modify entity @s data.nullscape.exit_portal set from block ~ ~ ~ exit_portal
data modify entity @s data.nullscape.exit.x set from entity @s data.nullscape.exit_portal[0]
data modify entity @s data.nullscape.exit.y set from entity @s data.nullscape.exit_portal[1]
data modify entity @s data.nullscape.exit.z set from entity @s data.nullscape.exit_portal[2]
function nullscape:link_rift/position_other_side with entity @s data.nullscape.exit
# set surrounding gateways
execute at @e[type=minecraft:marker,tag=nullscape.rift_gateway,distance=0.5..10] run data modify block ~ ~ ~ exit_portal set from entity @s data.nullscape.exit_portal
# cleanup, should also kill @s
kill @e[type=minecraft:marker,tag=nullscape.rift_gateway,distance=..10]
