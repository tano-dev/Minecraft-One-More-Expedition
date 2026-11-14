# Init scoreboards
scoreboard objectives add tr.first dummy
scoreboard objectives add nullscape.temp dummy
scoreboard objectives add nullscape.sculk_broken minecraft.mined:minecraft.sculk

# Schedule (wait for Terralith + Incendium)
schedule function nullscape:wait_for_player 30t replace
