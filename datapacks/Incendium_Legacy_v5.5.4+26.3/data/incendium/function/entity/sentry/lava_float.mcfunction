
execute if entity @s[tag=!in.sentry_float] if block ~ ~-0.65 ~ minecraft:lava run tag @s add in.sentry_float
execute if entity @s[tag=in.sentry_float] unless block ~ ~-0.65 ~ minecraft:lava run attribute @s gravity base reset



execute if entity @s[tag=in.sentry_float] unless block ~ ~-0.65 ~ minecraft:lava run tag @s remove in.sentry_float
execute if entity @s[tag=!in.sentry_float] if block ~ ~-0.65 ~ minecraft:lava run attribute @s gravity base set 0
