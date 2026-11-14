

# grab information for creating the other arrows
data modify entity @s[type=!spectral_arrow] data.arrow set value arrow
data modify entity @s[type=spectral_arrow] data.arrow set value spectral_arrow
data modify entity @s data.item set from entity @s item

data modify storage incendium:temp owner set from entity @s Owner
data modify storage incendium:temp arrow set from entity @s data.arrow
data modify storage incendium:temp rotation set from entity @s Rotation
data modify storage incendium:temp item set from entity @s data.item
data modify storage incendium:temp pierce set value 0
data modify storage incendium:temp pierce set from entity @s PierceLevel
data modify storage incendium:temp crit set value 0
data modify storage incendium:temp crit set from entity @s crit


# run a macro to summon arrows with data mataching the orgin arrow
function incendium:item/multiplex_crossbow/summon_arrow with storage incendium:temp
