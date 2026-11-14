


# decrease lifetime
scoreboard players remove @s[scores={in.lifetime=1..}] in.lifetime 1

# particles
execute if entity @s[predicate=incendium:random/15] run particle lava ~ ~ ~ 0 0 0 0 1 force
execute if entity @s[scores={in.lifetime=..3}] run particle lava ~ ~ ~ .25 .15 .25 .05 1 force

# lower damage after time
execute if entity @s[scores={in.lifetime=3}] run data modify entity @s damage set value 7d
execute if entity @s[scores={in.lifetime=0}] run data modify entity @s damage set value 4d


# start deleting chance after a while
kill @s[scores={in.lifetime=..0},predicate=incendium:random/10]

# loop
execute at @s as @s run schedule function incendium:item/multiplex_crossbow/arrow/loop 1t append
