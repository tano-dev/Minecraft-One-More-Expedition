# Two extra death cries during the sequence; the real death sound is the third, like Bedrock Edition
execute at @e[type=minecraft:wither,tag=bedrockdeath,limit=1] run playsound minecraft:entity.wither.death hostile @a[distance=..64] ~ ~ ~ 2 1
