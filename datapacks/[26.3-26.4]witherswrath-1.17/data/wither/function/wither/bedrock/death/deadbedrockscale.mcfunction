# Swells a tiny bit every tick so the growth looks smooth (1.0 to about 1.44 over the sequence)
scoreboard players add #bscale wither.stats 2
execute store result storage wither:fight bscale.s double 0.001 run scoreboard players get #bscale wither.stats
function wither:wither/bedrock/death/deadbedrockscale_set with storage wither:fight bscale
