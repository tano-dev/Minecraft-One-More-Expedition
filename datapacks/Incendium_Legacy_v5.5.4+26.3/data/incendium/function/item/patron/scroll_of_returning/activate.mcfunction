# from: ./use
# @s: player

tag @s add in.self


tellraw @s {translate:"incendium.item.returning_scroll.system.whoosh",fallback:"Whoosh! A familiar place awaits.", "italic": true, "color": "#D393F5"}

# particles before tping
particle minecraft:large_smoke ~ ~ ~ 0 0 0 0.1 50
particle minecraft:dust_color_transition{from_color:[0.5f,0.2f,0.66f], scale:4.0, to_color:[1.0f,0.5f,0.0f]} ~ ~ ~ 0.25 0.5 0.25 0.35 25

execute in minecraft:overworld run tp @s @e[type=marker,tag=in.world_spawn,limit=1]

execute at @s run particle minecraft:large_smoke ~ ~ ~ 0 0 0 0.1 50

# random cond to not crumble
execute store result score $rand in.dummy if predicate incendium:random/10
function incendium:item/patron/scroll_of_returning/crumble

tag @s remove in.self
