particle minecraft:splash ~ ~2 ~ 0.8 0.8 0.8 0 25 normal
playsound minecraft:ambient.underwater.loop.additions neutral @a ~ ~ ~ 0.1 2
data modify entity @s active_effects append value {id:"minecraft:regeneration",amplifier:6,duration:1,show_particles:0b}