#check if sprint key has been pressed, then tag the ghast as dnt_harness_sprinting
execute on controller if predicate {type:"minecraft:entity_properties",entity:"this",predicate:{"minecraft:type_specific/player":{input:{sprint:1b}}}} run execute on vehicle run tag @s add dnt_harness_sprinting

#remove dnt_harness_sprinting tag from ghast if forward key is not being held down or if no player is riding the ghast
execute on controller unless predicate {type:"minecraft:entity_properties",entity:"this",predicate:{"minecraft:type_specific/player":{input:{forward:1b}}}} run execute on vehicle run tag @s remove dnt_harness_sprinting
execute unless predicate {type:"minecraft:entity_properties",entity:"this",predicate:{passenger:{entity_type:"minecraft:player"}}} run tag @s remove dnt_harness_sprinting

#modify player fov depending on dnt_harness_sprinting state
execute as @s[tag=dnt_harness_sprinting] run execute on passengers run attribute @s movement_speed modifier add nova_structures:boost_player_fov 0.1 add_multiplied_base
execute as @s[tag=!dnt_harness_sprinting] run execute on passengers run attribute @s movement_speed modifier remove nova_structures:boost_player_fov