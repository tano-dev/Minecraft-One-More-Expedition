# from: ./sched_take
# @s: baby ghast head (allay)

#Return item back to user
execute at @n[tag=in.self] if entity @a[limit=1,tag=in.self,gamemode=!creative] run summon minecraft:item ~ ~ ~ {Item:{id:"minecraft:fire_charge",count:1b},PickupDelay:1s,Tags:["in.return"]}
execute at @n[tag=in.self] if entity @a[limit=1,tag=in.self,gamemode=!creative] run data modify entity @n[type=item,distance=..1,tag=in.return] Item set from entity @n[type=allay,tag=in.baby_head,distance=..5] equipment.mainhand
#...but only if the item was accident/unintended
execute at @n[tag=in.self] if entity @n[type=item,distance=..1,tag=in.return,nbt={Item:{id:"minecraft:fire_charge",count:1}}] run kill @n[type=item,distance=..1,tag=in.return]

#Remove item from allay
execute as @n[tag=in.self] at @a[limit=1,tag=in.self] positioned ^ ^ ^2 as @n[type=allay,tag=in.baby_head,distance=..5] run item replace entity @s weapon with air

#Reset
advancement revoke @a[limit=1,tag=in.self] only incendium:technical/give_ghastling
tag @n[tag=in.self] remove in.self