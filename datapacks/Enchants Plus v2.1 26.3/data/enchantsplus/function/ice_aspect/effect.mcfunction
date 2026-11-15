# cancel if ender dragon or wither
execute if entity @s[type=ender_dragon] run return fail
execute if entity @s[type=wither] run return fail


# particls and sounds and freezing 
particle block{block_state:"minecraft:ice"} ~ ~1 ~ 0.2 0.5 0.2 0 8 normal
playsound minecraft:block.glass.break block @a ~ ~ ~ 1 1
effect give @s minecraft:slowness 3 9 true


execute unless entity @s[type=#enchantsplus:large] run summon block_display ~ ~ ~ {transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[1.5f,3f,1.5f],translation:[-0.75f,0f,-0.75f]},block_state:{id:ice},Tags:["ep.frozen"]}
execute as @s[type=#enchantsplus:large] run summon block_display ~ ~ ~ {transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[3f,3f,3f],translation:[-1.5f,0f,-1.5f]},block_state:{id:ice},Tags:["ep.frozen"]}
tag @s add ep.entity.frozen
data merge entity @s {NoAI:true}