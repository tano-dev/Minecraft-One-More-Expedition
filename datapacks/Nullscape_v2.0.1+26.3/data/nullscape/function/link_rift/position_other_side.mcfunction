# @s = nullscape.rift_gateway marker in gateway block that has exit_portal data
# at @s
# run from nullscape:link_rift
# with $x : int 
# with $y : int
# with $z : int

$execute positioned $(x) $(y) $(z) positioned ~ ~0.5 ~ run function nullscape:link_rift/place_other_side
