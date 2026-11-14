execute if predicate {predicate:{structures:"#nova_structures:nova_structures_all"},type:"minecraft:location_check"} run function nova_structures:structure_search/dnt/begin_search

execute if predicate {predicate:{structures:"#nova_structures:vanilla/all"},type:"minecraft:location_check"} run function nova_structures:structure_search/vanilla/begin_search

execute unless predicate {type:"minecraft:any_of",terms:[{type:"minecraft:location_check",predicate:{structures:"#nova_structures:nova_structures_all"}},{type:"minecraft:location_check",predicate:{structures:"#nova_structures:vanilla/all"}}]} run function nova_structures:structure_search_fail