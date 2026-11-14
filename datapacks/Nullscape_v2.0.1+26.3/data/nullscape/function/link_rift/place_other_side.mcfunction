# @s = rift marker of rift being linked to the current location
# at center of block of gateway to replace
# run from nullscape:link_rift/position_other_side

execute store success score #loaded nullscape.temp if loaded ~ ~ ~
# if its not loaded, then its for sure not forceloaded, so we can blindly forceload
execute if score #loaded nullscape.temp matches 0 run forceload add ~ ~

execute unless block ~ ~ ~ end_gateway unless block ~ ~1 ~ bedrock run return fail

# delete bedrock
fill ~-1 ~-2 ~-1 ~1 ~2 ~1 air replace bedrock strict

# set other blocks
execute store result score #random nullscape.temp run random value 1..12

execute if score #random nullscape.temp matches 1 run place template nullscape:rift/return_shadow1 ~-2 ~-1 ~-2
execute if score #random nullscape.temp matches 2 run place template nullscape:rift/return_shadow1 ~2 ~-1 ~-2 clockwise_90
execute if score #random nullscape.temp matches 3 run place template nullscape:rift/return_shadow1 ~2 ~-1 ~2 180
execute if score #random nullscape.temp matches 4 run place template nullscape:rift/return_shadow1 ~-2 ~-1 ~2 counterclockwise_90

execute if score #random nullscape.temp matches 5 run place template nullscape:rift/return_shadow2_small ~-3 ~-1 ~-3
execute if score #random nullscape.temp matches 6 run place template nullscape:rift/return_shadow2_small ~3 ~-1 ~-3 clockwise_90
execute if score #random nullscape.temp matches 7 run place template nullscape:rift/return_shadow2_small ~3 ~-1 ~3 180
execute if score #random nullscape.temp matches 8 run place template nullscape:rift/return_shadow2_small ~-3 ~-1 ~3 counterclockwise_90

execute if score #random nullscape.temp matches 9 run place template nullscape:rift/return_shadow2_medium ~-3 ~-1 ~-3
execute if score #random nullscape.temp matches 10 run place template nullscape:rift/return_shadow2_medium ~3 ~-1 ~-3 clockwise_90
execute if score #random nullscape.temp matches 11 run place template nullscape:rift/return_shadow2_medium ~3 ~-1 ~3 180
execute if score #random nullscape.temp matches 12 run place template nullscape:rift/return_shadow2_medium ~-3 ~-1 ~3 counterclockwise_90

execute as @e[type=marker,tag=nullscape.rift_return,distance=..3] run data modify entity @s data.nullscape.exit_portal set from block ~ ~ ~ exit_portal
execute as @e[type=marker,tag=nullscape.rift_return,distance=..3] at @s run data modify block ~ ~ ~ exit_portal set from entity @s data.nullscape.exit_portal
kill @e[type=marker,tag=nullscape.rift_return,distance=..3]

# unforceload
execute if score #loaded nullscape.temp matches 0 run forceload remove ~ ~
