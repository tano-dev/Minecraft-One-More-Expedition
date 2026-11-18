# As Stellarity's Ender Dragon, at the dragon

execute unless entity @s[tag=dragon_attacks.init] run function expedition_dragon_attacks:init

# Death sequence belongs to Stellarity
execute if entity @s[tag=stellarity.to_portal,tag=!dragon_attacks.dying] run function expedition_dragon_attacks:abort
execute if entity @s[tag=stellarity.to_portal] run return 0
execute if entity @s[tag=stellarity.at_portal] run return 0

# --- True Ending a_main housekeeping (1:1)
execute if score @s trueEnding_bosstime matches 0.. run scoreboard players add @s trueEnding_bosstime 1
execute store result score @s trueEnding_y run data get entity @s Pos[1]
execute store result score @s trueEnding_dragonphase run data get entity @s DragonPhase

# True Ending health is 0..1000, Stellarity's is 0..100
scoreboard players operation @s trueEnding_health_percent = @s stellarity.dragon.health_percent
scoreboard players operation @s trueEnding_health_percent *= 10 trueEnding_constants

# --- start a new attack only when idle AND Stellarity is circling/strafing and not chain-firing
execute if score @s trueEnding_bosstime matches 219..220 if entity @s[tag=!trueEnding_inattack] if score @s trueEnding_dragonphase matches 0..1 unless score @s stellarity.dragon.time_chainfiring matches 1.. run function expedition_dragon_attacks:try_start

# --- attack timelines (copied True Ending functions, 1:1)
execute if score @s trueEnding_bosstime matches 0..218 run function true_ending:boss/phase_ultra_fireball
execute if score @s trueEnding_bosstime matches 900..1999 run function true_ending:boss/dive/phase
execute if score @s trueEnding_bosstime matches 2000..2999 run function true_ending:boss/phase_laser

# idle hold
execute if score @s trueEnding_bosstime matches 220 run scoreboard players set @s trueEnding_bosstime 219
# safety: ranges True Ending uses for features that are not ported (totem / extra ultra / crystals destroyed)
execute if score @s trueEnding_bosstime matches 3000.. run tag @s remove trueEnding_inattack
execute if score @s trueEnding_bosstime matches 3000.. run scoreboard players set @s trueEnding_bosstime 219
