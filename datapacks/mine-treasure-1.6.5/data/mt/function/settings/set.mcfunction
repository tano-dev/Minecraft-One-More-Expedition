$scoreboard players set $progression mt.var $(1)

execute if score $progression mt.var matches 1 run function mt:settings/progression/minimal

execute if score $progression mt.var matches 2 run function mt:settings/progression/low

execute if score $progression mt.var matches 3 run function mt:settings/progression/standard

execute if score $progression mt.var matches 4 run function mt:settings/progression/high

execute if score $progression mt.var matches 5 run function mt:settings/progression/extreme

$scoreboard players set $frequency mt.var $(2)

execute if score $frequency mt.var matches 1 run function mt:settings/frequency/minimal

execute if score $frequency mt.var matches 2 run function mt:settings/frequency/low

execute if score $frequency mt.var matches 3 run function mt:settings/frequency/standard

execute if score $frequency mt.var matches 4 run function mt:settings/frequency/high

execute if score $frequency mt.var matches 5 run function mt:settings/frequency/extreme

$data modify storage mt:settings 3.initial set value $(3)

$scoreboard players set $despawn mt.var $(4)

execute if score $despawn mt.var matches 1 run function mt:settings/despawn/sloth

execute if score $despawn mt.var matches 2 run function mt:settings/despawn/slow

execute if score $despawn mt.var matches 3 run function mt:settings/despawn/standard

execute if score $despawn mt.var matches 4 run function mt:settings/despawn/fast

execute if score $despawn mt.var matches 5 run function mt:settings/despawn/hypersonic

$data modify storage mt:settings 5.initial set value $(5)
$scoreboard players set #disable_build mt.constant $(5)

$data modify storage mt:settings 6.initial set value $(6)
$scoreboard players set #disable_global mt.constant $(6)

$data modify storage mt:settings 7.initial set value $(7)
$scoreboard players set #disable_common mt.constant $(7)

$data modify storage mt:settings 8.initial set value $(8)
$scoreboard players set #disable_rare mt.constant $(8)

$data modify storage mt:settings 9.initial set value $(9)
$scoreboard players set #disable_epic mt.constant $(9)

$data modify storage mt:settings 10.initial set value $(10)
$scoreboard players set #disable_legendary mt.constant $(10)

$data modify storage mt:settings 11.initial set value $(11)
$scoreboard players set #disable_mythical mt.constant $(11)
$data modify storage mt:settings 17.initial set value $(11)
$scoreboard players set $vanilla_mode mt.var $(11)

$data modify storage mt:settings 12.initial set value $(12)
$scoreboard players set #disable_rune mt.constant $(12)

$data modify storage mt:settings 13.initial set value $(13)
$scoreboard players set #disable_deto_mine mt.constant $(13)

$data modify storage mt:settings 14.initial set value $(14)
$scoreboard players set #disable_silk_touch mt.constant $(14)

$data modify storage mt:settings 15.initial set value $(15)
$scoreboard players set #disable_indestructible mt.constant $(15)

$data modify storage mt:settings 16.initial set value $(16)
$scoreboard players set #disable_giga_drill mt.constant $(16)

$data modify storage mt:settings 17.initial set value $(17)
$scoreboard players set $vanilla_mode mt.var $(17)

execute if score $vanilla_mode mt.var matches 1 run data modify storage mt:settings 11.initial set value 1
execute if score $vanilla_mode mt.var matches 1 run scoreboard players set #disable_mythical mt.constant 1
