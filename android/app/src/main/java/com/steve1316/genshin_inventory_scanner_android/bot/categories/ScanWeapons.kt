package com.steve1316.genshin_inventory_scanner_android.bot.categories

import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game
import org.opencv.core.Point
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService as MPS

class ScanWeapons(private val game: Game) {
	private val tag: String = "${MainActivity.loggerTag}ScanWeapons"
	private val debugMode = game.configData.debugMode

	private var firstRun = true

	/**
	 * Starts the process to scan weapons in the inventory.
	 *
	 */
	fun start() {
		if (game.imageUtils.findImage("category_selected_weapons", tries = 2) == null && !game.findAndPress("category_unselected_weapons", tries = 2)) {
			game.printToLog("[ERROR] Unable to start Weapon scan.", tag, isError = true)
			return
		}

		// Reset the scroll view.
		game.scanUtils.resetScrollScreen()

		// Collect the locations of all weapons whose "Lv." image asset can be found.
		var tries = 10
		while (tries > 0) {
			val locations: ArrayList<Point> = if (firstRun) {
				game.imageUtils.findAll("item_level", region = intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight))
			} else {
				// Shrink the search area to the bottom half of the screen since we are now searching only for the last row that appears on the screen.
				game.imageUtils.findAll("item_level", region = intArrayOf(0, MPS.displayHeight - (MPS.displayHeight / 3), MPS.displayWidth, MPS.displayHeight / 3))
			}

			// For every subsequent search, only search for the very last row as every other row above has been processed already.
			if (!firstRun) {
				// Now iterate through the array backwards and keep only the locations from the last row.
				val reversedLocations = locations.reversed()
				locations.clear()
				val finalYCoordinate = reversedLocations[0].y
				reversedLocations.forEach { location ->
					if (location.y == finalYCoordinate) {
						locations.add(location)
					}
				}

				// Now revert the order of the new locations array.
				locations.reverse()
			}

			if (debugMode) game.printToLog("[DEBUG] ${locations.size} Weapons: $locations", tag)

			// Now scan each weapon in each location.
			locations.forEach { location ->
				// Select the weapon.
				game.gestureUtils.tap(location.x, location.y, "item_level")
				game.printToLog("[SCAN_WEAPONS] ${game.scanUtils.getWeaponNameAndRarity()} - ${game.scanUtils.getWeaponLevel()} - ${game.scanUtils.getRefinementLevel()}", tag)
			}

			// Different scrolling behavior based on whether this is the first run.
			if (firstRun) {
				game.scanUtils.scrollFirstRow()
				firstRun = false
			} else {
				game.scanUtils.scrollSubsequentRow()
			}

			tries -= 1
		}
	}
}