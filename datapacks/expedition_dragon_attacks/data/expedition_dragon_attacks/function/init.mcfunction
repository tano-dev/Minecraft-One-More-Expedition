# Once per dragon (as the dragon)
tag @s add dragon_attacks.init
tag @s remove trueEnding_inattack
tag @s remove trueEnding_inattack_doubledive
scoreboard players set @s trueEnding_bosstime 218
execute in minecraft:the_end positioned 0 80 0 unless entity @e[type=marker,tag=trueEnding_pivot,distance=..10] run summon marker ~ ~ ~ {Tags:["trueEnding_pivot"]}
