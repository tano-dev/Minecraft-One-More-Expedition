advancement revoke @s only nova_structures:charts/using
advancement revoke @s only nova_structures:charts/unfolding
schedule clear nova_structures:charts/precaution

#if somehow player changed chart from hand in 1 tick, fails function(failstate1)
execute unless items entity @s weapon.* *[minecraft:custom_model_data={strings:["dnt:chart"]}] run return fail

#if player uses chart in a dimension thats doesnt corresponding to chart, fails function(failstate2)
execute if items entity @s weapon.offhand *[minecraft:custom_model_data={strings:["dnt:chart"]}] unless items entity @s weapon.mainhand *[minecraft:custom_model_data={strings:["dnt:chart"]}] if predicate nova_structures:chart_dimension_check2 run return fail
execute if items entity @s weapon.mainhand *[minecraft:custom_model_data={strings:["dnt:chart"]}] if predicate nova_structures:chart_dimension_check run return fail

#if everything is correct, generates chart and plays sounds
execute if items entity @s weapon.offhand *[minecraft:custom_model_data={strings:["dnt:chart"]}] unless items entity @s weapon.mainhand *[minecraft:custom_model_data={strings:["dnt:chart"]}] run loot replace entity @s weapon.offhand loot nova_structures:charts/unfolding/offhand
execute if items entity @s weapon.mainhand *[minecraft:custom_model_data={strings:["dnt:chart"]}] run loot replace entity @s weapon.mainhand loot nova_structures:charts/unfolding/mainhand

playsound minecraft:item.book.page_turn player @s