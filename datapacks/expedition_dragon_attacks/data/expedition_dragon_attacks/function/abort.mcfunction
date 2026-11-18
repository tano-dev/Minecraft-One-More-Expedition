# Stellarity started the death sequence: stop any running attack and hand the dragon back
tag @s add dragon_attacks.dying
tag @s remove trueEnding_inattack
tag @s remove trueEnding_inattack_doubledive
scoreboard players set @s trueEnding_bosstime 219
data merge entity @s {NoAI:0b,Silent:0b}
