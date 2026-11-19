
$rotate @s facing entity @e[distance=..48,nbt={UUID:$(UUID)},limit=1] eyes
tp @s ^ ^ ^1
execute if entity @s[tag=stellarity.radiant_fluffy_hammer] run tp @s ^ ^ ^1

$execute if entity @p[distance=..48,nbt={UUID:$(UUID)},limit=1,dx=1,dz=1,dy=2] at @p[distance=..48,nbt={UUID:$(UUID)},limit=1] run function stellarity:item/fluffy_hammer/despawn with entity @n[tag=stellarity.fluffy_hammer.display]
