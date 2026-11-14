# from: player/1s
# @s: player


execute if entity @s[predicate=incendium:structure/forbidden_castle] unless entity @e[type=piglin_brute,tag=in.castle,distance=..160] run advancement grant @s only incendium:ash_barrens/conquer_castle


