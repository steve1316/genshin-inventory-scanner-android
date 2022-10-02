package com.steve1316.genshin_inventory_scanner_android.bot.categories

import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game

class ScanWeapons(private val game: Game) {
	private val tag: String = "${MainActivity.loggerTag}ScanWeapons"

	fun start() {
		if (game.imageUtils.findImage("category_selected_weapons") == null && !game.findAndPress("category_unselected_weapons")) {
			game.printToLog("[ERROR] Unable to start Weapon scan.", tag, isError = true)
			return
		}

		// Since the default state is a 7x3 grid on the screen, collect the locations of all weapons whose "Lv." can be read on the 7x3 grid.
		val locations = game.imageUtils.findAll("item_level")
		game.printToLog("${locations.size} Weapons: $locations", tag)

		game.gestureUtils.tap(locations[1].x, locations[1].y, "item_level")
		game.wait(0.10)
		game.printToLog("${game.scanUtils.getName()} - ${game.scanUtils.getLevel()} - ${game.scanUtils.getRefinementLevel()}")
	}
}