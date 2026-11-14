# from: entity/mob/main
# @s: sentry

# Normal Particles
particle minecraft:electric_spark ~ ~0.85 ~ 0.1 0.25 0.1 0.15 1 normal @a[distance=..40]

# 5% of the time, start a ray to the nearest valid player
execute if entity @s[predicate=incendium:random/15,predicate=incendium:tick/20] anchored eyes positioned ^ ^ ^ facing entity @p[distance=..40,tag=in.valid_player] eyes run function incendium:entity/sentry/ray/start_ray

# "Lives" Particles
execute if entity @p[distance=..40,tag=in.valid_player] if predicate incendium:tick/10 anchored eyes positioned ^ ^ ^ run function incendium:entity/sentry/life



execute if predicate incendium:tick/4 run function incendium:entity/sentry/lava_float