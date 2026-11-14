# from: entity/player/main
# @s: player wearing helm
#   OR
# from: entity/other/main
# @s: armor_stand wearing helm

execute if predicate incendium:tick/5 run function incendium:item/scouts_battle_helm/update

## Particles
scoreboard players add @s in.scouts_helmet 10
scoreboard players set @s[scores={in.scouts_helmet=360..}] in.scouts_helmet 0
execute store result storage incendium:temp scouts_helmet double 1 run scoreboard players get @s in.scouts_helmet 

# uses marker for rotation but displays particle at player position
execute if predicate incendium:is/normal unless entity @s[tag=in.invis] unless entity @s[gamemode=spectator] run execute anchored eyes positioned ^ ^ ^ run function incendium:item/scouts_battle_helm/helper with storage incendium:temp
