schedule function medieval_nether:main_2s 2s

execute in minecraft:the_nether as @e[type=#medieval_nether:structure_banned,distance=0..] at @s as @s[y=0,dy=126.5,nbt=!{PersistenceRequired:1b}] unless entity @a[distance=..60] run tp @s ~ -70 ~