
# 3. Grant Resistance for 3 seconds (60 ticks)
execute as @p[distance=..0.1] run effect give @s minecraft:resistance 3 0 true



# 5. Visual feedback: heart particles at teleport location
execute at @e[type=ender_pearl,tag=endershot,nbt={inGround:1b},limit=1,sort=nearest] run particle minecraft:heart ~ ~1.2 ~ 0.3 0.3 0.3 0.05 5

# 6. Sound feedback: level-up sound for the player
execute as @p[distance=..0.1] run playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.5 1.5
