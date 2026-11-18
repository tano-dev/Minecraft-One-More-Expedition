# Objectives True Ending's copied attack functions rely on
scoreboard objectives add trueEnding_bosstime dummy
scoreboard objectives add trueEnding_clock dummy
scoreboard objectives add trueEnding_count dummy
scoreboard objectives add trueEnding_dragonphase dummy
scoreboard objectives add trueEnding_health_percent dummy
scoreboard objectives add trueEnding_y dummy
scoreboard objectives add trueEnding_constants dummy
scoreboard objectives add trueEnding_settings dummy
scoreboard objectives add trueEnding_storage dummy
scoreboard players set 10 trueEnding_constants 10

# True Ending defaults (ambience: 0 = off, 1 = full, 2 = particles only)
execute unless score ambience trueEnding_settings matches -2147483648.. run scoreboard players set ambience trueEnding_settings 1
execute unless score pearlbreaking trueEnding_settings matches -2147483648.. run scoreboard players set pearlbreaking trueEnding_settings 1
execute unless score crystal_beacon trueEnding_settings matches -2147483648.. run scoreboard players set crystal_beacon trueEnding_settings 1
