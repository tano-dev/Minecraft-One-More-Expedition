scoreboard players set #hit in.fire_laser 0
tag @s add laser
execute at @s run summon minecraft:firework_rocket ~ ~1 ~ {Silent:true,Motion:[0.0,0.0,0.0],ShotAtAngle:true,LifeTime:0,FireworksItem:{id:"firework_rocket",count:1,components:{fireworks:{flight_duration:1,explosions:[{shape:'burst',has_twinkle:false,has_trail:false,colors:[39679]}]}}},Tags:["in.sentrys_wrath_firework"]}
execute if predicate incendium:random/50 run data merge entity @s[nbt={Fire:-20s}] {Fire:80s}
