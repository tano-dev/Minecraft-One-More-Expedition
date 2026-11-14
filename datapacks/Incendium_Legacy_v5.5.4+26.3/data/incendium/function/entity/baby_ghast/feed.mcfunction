# from (adv): technical/feed_ghastling
# @s: player

execute positioned ^ ^ ^2 as @n[type=ghast,tag=in.baby_ghast,distance=..5] at @s run function incendium:entity/baby_ghast/eat

advancement revoke @s only incendium:technical/feed_ghastling
