# breeze burst
execute as @e[type=minecraft:arrow,tag=enchantsplus.bb,nbt={inGround:true}] at @s run function enchantsplus:breezeburst/bb_wc

# gluttony
execute at @a[nbt={SelectedItem:{components:{"minecraft:enchantments":{"enchantsplus:gluttony":1}}}}] as @e[type=item,distance=..2.5,nbt={Age:0s}] run function enchantsplus:gluttony/gluttonize

execute at @a[nbt={SelectedItem:{components:{"minecraft:enchantments":{"enchantsplus:gluttony":2}}}}] as @e[type=item,distance=..9,nbt={Age:0s}] run function enchantsplus:gluttony/gluttonize

execute at @a[nbt={SelectedItem:{components:{"minecraft:enchantments":{"enchantsplus:gluttony":3}}}}] as @e[type=item,distance=..9,nbt={Age:0s}] run function enchantsplus:gluttony/gluttonize
execute as @a[nbt={SelectedItem:{components:{"minecraft:enchantments":{"enchantsplus:gluttony":3}}}}] at @s run tp @e[type=experience_orb,distance=..6,nbt={Age:0s}] @s

# luminosity
#tag @e[type=minecraft:marker,tag=ep.light.mark] add ep.expiredlight
#tag @e[type=minecraft:marker,tag=ep.light.mark2] add ep.expiredlight
#tag @e[type=minecraft:marker,tag=ep.light.mark1] add ep.expiredlight


#execute as @e[nbt={equipment:{chest:{components:{"minecraft:enchantments":{"enchantsplus:luminosity":3}}}}}] at @s positioned ~ ~1 ~ run function enchantsplus:luminosity/light
#execute as @e[nbt={equipment:{chest:{components:{"minecraft:enchantments":{"enchantsplus:luminosity":2}}}}}] at @s positioned ~ ~1 ~ run function enchantsplus:luminosity/light2
#execute as @e[nbt={equipment:{chest:{components:{"minecraft:enchantments":{"enchantsplus:luminosity":1}}}}}] at @s positioned ~ ~1 ~ run function enchantsplus:luminosity/light1

#execute as @e[type=minecraft:marker,tag=ep.expiredlight] at @s run function enchantsplus:luminosity/del
# precision

execute as @e[type=arrow,tag=ep.ng1,nbt={inGround:true}] run function enchantsplus:precision/normalize
execute as @e[type=arrow,tag=ep.ng2,nbt={inGround:true}] run function enchantsplus:precision/normalize

execute as @e[type=arrow,tag=ep.ng1] if predicate enchantsplus:notsoslow run function enchantsplus:precision/normalize
execute as @e[type=arrow,tag=ep.ng2] if predicate enchantsplus:slow run function enchantsplus:precision/normalize

# ice aspect
execute as @e[type=block_display,tag=ep.frozen] at @s unless entity @e[nbt={active_effects:[{id:"minecraft:slowness",amplifier:9b}]},distance=..2] run kill @s
execute as @e[type=!player,nbt={NoAI:true},tag=ep.entity.frozen] unless entity @s[nbt={active_effects:[{id:"minecraft:slowness",amplifier:9b}]}] at @s run function enchantsplus:ice_aspect/unfreeze
execute as @e[type=armor_stand,tag=ep.entity.frozen] unless entity @s[nbt={active_effects:[{id:"minecraft:slowness",amplifier:9b}]}] at @s run function enchantsplus:ice_aspect/unfreeze

execute as @a[tag=ep.entity.frozen] at @s at @n[type=block_display,tag=ep.frozen] rotated as @s run tp @s ~ ~ ~
execute as @a[tag=ep.entity.frozen] unless entity @s[nbt={active_effects:[{id:"minecraft:slowness",amplifier:9b}]}] at @s run function enchantsplus:ice_aspect/unfreeze
execute as @a[tag=ep.entity.frozen] if predicate enchantsplus:onfire at @s run function enchantsplus:ice_aspect/extinguish

# harvest
execute as @a[scores={ep.mined_wheat=1..}] at @s run function enchantsplus:harvest/wheat
execute as @a[scores={ep.mined_carrot=1..}] at @s run function enchantsplus:harvest/carrot
execute as @a[scores={ep.mined_potato=1..}] at @s run function enchantsplus:harvest/potato
execute as @a[scores={ep.mined_beet=1..}] at @s run function enchantsplus:harvest/beetroot
execute as @a[scores={ep.mined_wart=1..}] at @s run function enchantsplus:harvest/nether_wart
