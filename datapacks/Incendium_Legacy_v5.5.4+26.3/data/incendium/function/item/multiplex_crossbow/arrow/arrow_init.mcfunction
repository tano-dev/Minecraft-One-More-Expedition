

# run in.checked tag and set lifetime
tag @s add in.checked
scoreboard players set @s in.lifetime 8

# set their data
data modify entity @s damage set value 11d
data modify entity @s life set value 1195
data modify entity @s pickup set value false
data modify entity @s weapon.id set value crossbow


# run the motion setup using random rotation using a macro
tag @s add in.in_motion 
$execute on origin at @s rotated ~$(xz) ~$(y) positioned 0.0 0.0 0.0 run tp @n[tag=in.multiplex_arrow,tag=in.checked,tag=in.in_motion,type=#minecraft:arrows] ^ ^ ^1.5
data modify storage incendium:temp motion set from entity @s Pos
tp ~ ~ ~
data modify entity @s Motion set from storage incendium:temp motion
tag @s remove in.in_motion

# run their temp clock
function incendium:item/multiplex_crossbow/arrow/main