# As the idle dragon (same triggers and odds as True Ending's a_main)

# below 2/3 health: freeze, roar and fire 10 ultra fireballs (once)
execute if entity @s[tag=!trueEnding_halfhealth,tag=!trueEnding_inattack] if score @s trueEnding_health_percent matches ..666 run function expedition_dragon_attacks:start_half

# below 1/3 health: first dragon laser (once)
execute if entity @s[tag=!trueEnding_quarterhealth,tag=!trueEnding_inattack] if score @s trueEnding_health_percent matches ..333 run function expedition_dragon_attacks:start_quarter

# ultra fireball chain: every minute, 20%, needs a survival player nearby, below 2/3 health
execute if score 1min trueEnding_clock matches 1 if entity @s[tag=trueEnding_halfhealth,tag=!trueEnding_inattack] if entity @p[distance=..128,gamemode=!spectator,gamemode=!creative] if predicate true_ending:chance/20_percent run function expedition_dragon_attacks:start_ultra

# dragon laser: every second, 20%, below 1/3 health, not while its breath clouds still linger
execute if score 20tick trueEnding_clock matches 1 if entity @s[tag=!trueEnding_inattack] if score @s trueEnding_health_percent matches ..333 if predicate true_ending:chance/20_percent unless entity @e[distance=..128,type=area_effect_cloud,tag=trueEnding_dragonbreath_quarter] run function expedition_dragon_attacks:start_laser

# shockwave dive: every second, 6%, any health
execute if score 20tick trueEnding_clock matches 1 if entity @s[tag=!trueEnding_inattack] if predicate true_ending:chance/6_percent run function expedition_dragon_attacks:start_dive
