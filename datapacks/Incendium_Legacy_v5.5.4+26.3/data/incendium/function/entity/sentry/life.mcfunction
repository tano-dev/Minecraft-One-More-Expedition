# from: entity/sentry/main
# @s: Sentry "life" marker

execute store result score @s in.sentry_totems run data get entity @s equipment.mainhand.count

execute store result entity @s Rotation[1] float 1 run scoreboard players get $sentry_lives in.dummy

# update rotation as well
execute if entity @s[scores={in.sentry_totems=1..}] rotated as @s positioned ^ ^0 ^1.2 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force
execute if entity @s[scores={in.sentry_totems=2..}] rotated as @s positioned ^ ^.808 ^.808 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force
execute if entity @s[scores={in.sentry_totems=3..}] rotated as @s positioned ^ ^1.2 ^0 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force
execute if entity @s[scores={in.sentry_totems=4..}] rotated as @s positioned ^ ^.808 ^-.808 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force
execute if entity @s[scores={in.sentry_totems=5..}] rotated as @s positioned ^ ^0 ^-1.2 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force
execute if entity @s[scores={in.sentry_totems=6..}] rotated as @s positioned ^ ^-.808 ^-.808 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force
execute if entity @s[scores={in.sentry_totems=7..}] rotated as @s positioned ^ ^-1.2 ^0 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force
execute if entity @s[scores={in.sentry_totems=8..}] rotated as @s positioned ^ ^-.808 ^.808 run particle minecraft:dust{color:[0.5, 1.0, 1.0], scale:1.25} ~ ~-.5 ~ 0 0 0 0 1 force



execute if entity @p[distance=..40,tag=in.valid_player] run rotate @s facing entity @p[distance=..40,tag=in.valid_player]