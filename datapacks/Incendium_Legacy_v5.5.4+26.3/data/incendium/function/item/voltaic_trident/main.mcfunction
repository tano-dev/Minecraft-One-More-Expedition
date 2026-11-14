# from: entity/other/main
# @s: trident


# malfunction
execute if entity @s[tag=in.live] if block ~ ~ ~ water run function incendium:item/voltaic_trident/explode

# on dmg
execute if entity @s[tag=in.voltaic_trident,tag=in.live,tag=!in.dealt_damage,nbt={DealtDamage:1b}] run function incendium:item/voltaic_trident/damage

particle minecraft:electric_spark ^ ^ ^ 0 0 0 0 1 force
