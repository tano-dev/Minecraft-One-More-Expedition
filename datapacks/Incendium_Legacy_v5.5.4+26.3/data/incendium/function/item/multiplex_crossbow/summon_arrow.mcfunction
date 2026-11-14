
# macro to summon multplex arrows
$summon $(arrow) ~ ~ ~ {Owner:$(owner),PierceLevel:$(pierce),Rotation:$(rotation),crit:$(crit),item:$(item),Tags:["in.multiplex_arrow"]}

# init arrows and set some random numbers used to offset the roation later
execute store result storage incendium:temp xz double 1 run random value -18..18
execute store result storage incendium:temp y double 1 run random value -18..9
execute as @e[type=#minecraft:arrows,tag=in.multiplex_arrow,tag=!in.checked,distance=..8] run function incendium:item/multiplex_crossbow/arrow/arrow_init with storage incendium:temp
