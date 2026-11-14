# from: entity/mob/init
# @s: sentry

data modify entity @s CustomName set value {translate:"incendium.mob.pipeline.sentry.name",fallback:"Pipeline Sentry","color":"yellow"}


team join in.noname @s

tag @s add in.ticking_entity
tag @s add in.checked

data modify entity @s DeathLootTable set value 'incendium:entity/sentry'
data modify entity @s drop_chances set value {head:0.0f, chest:0.0f, legs:0.0f, feet:0.0f, mainhand:0.0f, offhand:0.0f}
attribute @s scale base set 1.5



execute store result score @s in.sentry_totems run random value 4..8
data modify entity @s[scores={in.sentry_totems=4}] equipment.mainhand set value {id:totem_of_undying,count:4}
data modify entity @s[scores={in.sentry_totems=5}] equipment.mainhand set value {id:totem_of_undying,count:5}
data modify entity @s[scores={in.sentry_totems=6}] equipment.mainhand set value {id:totem_of_undying,count:6}
data modify entity @s[scores={in.sentry_totems=7}] equipment.mainhand set value {id:totem_of_undying,count:7}
data modify entity @s[scores={in.sentry_totems=8}] equipment.mainhand set value {id:totem_of_undying,count:8}

# "Lives" particles

