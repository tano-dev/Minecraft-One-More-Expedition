scoreboard players set #hit in.fire_laser 0
tag @s add laser
execute at @s run summon minecraft:firework_rocket ~ ~1 ~ {Silent:true,Motion:[0.0,0.0,0.0],ShotAtAngle:true,LifeTime:0,FireworksItem:{id:"firework_rocket",count:1,components:{fireworks:{flight_duration:1,explosions:[{shape:'burst',has_twinkle:false,has_trail:false,colors:[39679]}]}}},Tags:["in.sentrys_wrath_firework"]}
execute if entity @e[type=blaze,tag=in.sentry,distance=..5] run advancement grant @p[distance=..30,tag=in.self] only incendium:ash_barrens/duel_of_the_fates

execute at @s positioned ~ ~1 ~ if score #hit in.fire_laser matches 0 if predicate incendium:random/85 if score #distance2 in.fire_laser matches 0..250 positioned ^ ^ ^0.2 facing entity @e[type=#incendium:mobs_no_player,tag=!laser,distance=..10,sort=nearest,limit=1] eyes if block ~ ~ ~ #incendium:airs run function incendium:item/sentrys_wrath/short_lightning/ray
