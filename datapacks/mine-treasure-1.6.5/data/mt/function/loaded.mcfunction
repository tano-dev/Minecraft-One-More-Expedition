# Loads only for the first time

execute store result score $difficulty mt.var run difficulty

function mt:settings/progression/high

function mt:settings/frequency/low

data modify storage mt:settings 3 set value {type:"minecraft:number_range",key:"3",width:200,label:{text:"Loot Table Rolls"},start:5,end:50,step:1,initial:20}

function mt:settings/despawn/standard

data modify storage mt:settings 5 set value {type:"minecraft:boolean",key:"5",label:{text:"Disable Structure Spawning Items"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 6 set value {type:"minecraft:boolean",key:"6",label:{text:"Disable Global Legendary/Mythical Sound"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 7 set value {type:"minecraft:boolean",key:"7",label:{text:"Disable Common Treasures"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 8 set value {type:"minecraft:boolean",key:"8",label:{text:"Disable Rare Treasures"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 9 set value {type:"minecraft:boolean",key:"9",label:{text:"Disable Epic Treasures"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 10 set value {type:"minecraft:boolean",key:"10",label:{text:"Disable Legendary Treasures"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 11 set value {type:"minecraft:boolean",key:"11",label:{text:"Disable Mythical Treasures"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 12 set value {type:"minecraft:boolean",key:"12",label:{text:"Disable Runes"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 13 set value {type:"minecraft:boolean",key:"13",label:{text:"Disable Detonation Mines"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 14 set value {type:"minecraft:boolean",key:"14",label:{text:"Disable Silk Touch Treasures"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 15 set value {type:"minecraft:boolean",key:"15",label:{text:"Disable Bedrock/Reinforced Deepslate"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 16 set value {type:"minecraft:boolean",key:"16",label:{text:"Disable Giga Drill Effect"},initial:0b,on_true:"1",on_false:"0"}

data modify storage mt:settings 17 set value {type:"minecraft:boolean",key:"17",label:{text:"Vanilla Mode",type:"text",initial:0b,on_true:"1",on_false:"0"}}

scoreboard players set $vanilla_mode mt.var 0

scoreboard players set $progression mt.var 3
scoreboard players set $frequency mt.var 1

scoreboard players set #load165 mt.var 1
