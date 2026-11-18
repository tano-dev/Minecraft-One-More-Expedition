# Clocks (True Ending drives these with schedule/TimerQueue; plain counters are cheaper)
scoreboard players add 1min trueEnding_clock 1
scoreboard players add 20tick trueEnding_clock 1
scoreboard players add 10tick trueEnding_clock 1
execute if score 1min trueEnding_clock matches 1200.. run scoreboard players set 1min trueEnding_clock 0
execute if score 20tick trueEnding_clock matches 20.. run scoreboard players set 20tick trueEnding_clock 0
execute if score 10tick trueEnding_clock matches 10.. run scoreboard players set 10tick trueEnding_clock 0

# Everything below only runs while a player is in The End near the arena (one cheap player-only check otherwise)
execute in minecraft:the_end positioned 0 80 0 if entity @p[distance=..256] run function expedition_dragon_attacks:end_tick
