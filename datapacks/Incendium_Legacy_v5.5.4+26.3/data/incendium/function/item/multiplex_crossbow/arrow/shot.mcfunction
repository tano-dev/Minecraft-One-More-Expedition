
# mark the arrow
tag @s add in.multiplex_arrow

# sounds
stopsound @a[distance=..24] * item.crossbow.shoot
playsound minecraft:entity.generic.explode player @a[distance=..24] ~ ~ ~ 2 2 1

# summon arrows and chance for more
function incendium:item/multiplex_crossbow/start_summon_arrow
function incendium:item/multiplex_crossbow/start_summon_arrow
function incendium:item/multiplex_crossbow/start_summon_arrow
function incendium:item/multiplex_crossbow/start_summon_arrow
execute if predicate incendium:random/80 run function incendium:item/multiplex_crossbow/start_summon_arrow
execute if predicate incendium:random/80 run function incendium:item/multiplex_crossbow/start_summon_arrow
execute if predicate incendium:random/80 run function incendium:item/multiplex_crossbow/start_summon_arrow
execute if predicate incendium:random/80 run function incendium:item/multiplex_crossbow/start_summon_arrow
execute if predicate incendium:random/80 run function incendium:item/multiplex_crossbow/start_summon_arrow
execute if predicate incendium:random/80 run function incendium:item/multiplex_crossbow/start_summon_arrow
execute if predicate incendium:random/80 run function incendium:item/multiplex_crossbow/start_summon_arrow

# init start arrow
execute store result storage incendium:temp xz double 1 run random value -18..18
execute store result storage incendium:temp y double 1 run random value -18..9
function incendium:item/multiplex_crossbow/arrow/arrow_init with storage incendium:temp
