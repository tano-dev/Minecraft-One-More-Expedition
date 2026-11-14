scoreboard players set #hit in.fire_laser 0
tag @s add laser
summon minecraft:firework_rocket ~ ~1 ~ {Silent:true,Motion:[0.0,0.0,0.0],ShotAtAngle:true,LifeTime:0,FireworksItem:{id:"firework_rocket",count:1,components:{fireworks:{flight_duration:1,explosions:[{shape:'burst',has_twinkle:false,has_trail:false,colors:[16725028]},{shape:'burst',has_twinkle:false,has_trail:false,colors:[16733476]},{shape:'burst',has_twinkle:false,has_trail:false,colors:[16742692]},{shape:'burst',has_twinkle:false,has_trail:false,colors:[16751908]},{shape:'burst',has_twinkle:false,has_trail:false,colors:[16759040]},{shape:'burst',has_twinkle:false,has_trail:false,colors:[16765184]}]}}}}

execute at @s positioned ~ ~1 ~ if score #hit in.fire_laser matches 0 if predicate incendium:random/87 if score #distance2 in.fire_laser matches 0..1200 positioned ^ ^ ^0.2 facing entity @e[type=#incendium:mobs,tag=!laser,distance=..10,sort=nearest,limit=1] eyes if block ~ ~ ~ #incendium:airs run function incendium:item/firestorm/ray/iter
execute if predicate incendium:random/50 run data merge entity @s {Fire:80s}
