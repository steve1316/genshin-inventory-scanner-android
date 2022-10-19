package com.steve1316.genshin_inventory_scanner_android.bot.categories

import android.util.Log
import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game
import com.steve1316.genshin_inventory_scanner_android.data.Weapon
import org.opencv.core.Point
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService as MPS

class ScanWeapons(private val game: Game) {
	private val tag: String = "${MainActivity.loggerTag}ScanWeapons"
	private val debugMode = game.configData.debugMode

	private var enableFullRegionSearch = true
	private var enableSingleRowSearch = false
	private var search5StarComplete = !game.configData.scan5StarWeapons
	private var search4StarComplete = !game.configData.scan4StarWeapons
	private var search3StarComplete = !game.configData.scan3StarWeapons
	private var skipScroll = false

	private var firstSearchComplete = false
	private var firstSearchMaxSearches = 21
	private var subsequentSearchMaxSearches = 7

	private var weaponList: ArrayList<Weapon> = arrayListOf()

	fun search(): ArrayList<Point> {
		val region = if (enableFullRegionSearch) {
			intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight)
		} else {
			intArrayOf(0, MPS.displayHeight - (MPS.displayHeight / 3), MPS.displayWidth, MPS.displayHeight / 3)
		}

		return if (game.configData.scan5StarWeapons && !search5StarComplete) {
			return game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95)
		} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
			return game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95)
		} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
			return game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95)
		} else {
			Log.e(tag, "Returning nothing at all.")
			arrayListOf()
		}
	}

	private fun validateRarity(rarity: String): Boolean {
		return (game.configData.scan5StarWeapons && !search5StarComplete && (rarity == "5")) || (game.configData.scan4StarWeapons && !search4StarComplete && (rarity == "4")) ||
				(game.configData.scan3StarWeapons && !search3StarComplete && (rarity == "3"))
	}

	private fun validateSearchCompletion(locations: ArrayList<Point>): Boolean {
		// If no locations were found, mark that rarity search as complete.
		if (locations.size == 0) {
			if (game.configData.scan5StarWeapons && !search5StarComplete) {
				game.printToLog("[SCAN_WEAPONS] Finished scanning for 5* Weapons...", tag)
				search5StarComplete = true
			} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
				game.printToLog("[SCAN_WEAPONS] Finished scanning for 4* Weapons...", tag)
				search4StarComplete = true
			} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
				game.printToLog("[SCAN_WEAPONS] Finished scanning for 3* Weapons...", tag)
				search3StarComplete = true
			} else {
				return false
			}
		}

		return true
	}

	private fun checkMore(locations: ArrayList<Point>) {
		if (enableFullRegionSearch && !enableSingleRowSearch && locations.size < firstSearchMaxSearches) {
			// If the amount of locations for the current rarity found was less than the expected maximum for the full region search, then that means that this search has been completed early.
			// Turn off scrolling and allow for full region search again for the next rarity search to start.
			skipScroll = !firstSearchComplete

			if (!firstSearchComplete) {
				if (game.configData.scan5StarWeapons && !search5StarComplete) {
					game.printToLog("[SCAN_WEAPONS] 5* Weapons seem to have been all scanned from the full region...", tag)
					search5StarComplete = true
				} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
					game.printToLog("[SCAN_WEAPONS] 4* Weapons seem to have been all scanned from the full region...", tag)
					search4StarComplete = true
				} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
					game.printToLog("[SCAN_WEAPONS] 3* Weapons seem to have been all scanned from the full region...", tag)
					search3StarComplete = true
				}

				firstSearchComplete = true
			}
		} else if (!enableFullRegionSearch && enableSingleRowSearch && locations.size < subsequentSearchMaxSearches) {
			// Similarly, if the amount of locations was less than the expected maximum for the single row search, then end the search, turn off scrolling but do not re-enable full region search.
			skipScroll = false

			if (game.configData.scan5StarWeapons && !search5StarComplete) {
				game.printToLog("[SCAN_WEAPONS] 5* Weapons seem to have been all scanned from the last row...", tag)
				search5StarComplete = true
			} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
				game.printToLog("[SCAN_WEAPONS ] 4* Weapons seem to have been all scanned from the last row...", tag)
				search4StarComplete = true
			} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
				game.printToLog("[SCAN_WEAPONS] 3* Weapons seem to have been all scanned from the last row...", tag)
				search3StarComplete = true
			}
		} else {
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			skipScroll = false
		}
	}

	private fun isSearchDone(): Boolean {
		return (search5StarComplete && game.configData.scan5StarWeapons) && (search4StarComplete && game.configData.scan4StarWeapons) &&
				(search3StarComplete && game.configData.scan3StarWeapons)
	}

	private fun printInitialInfo(): String {
		var message = if (game.configData.scan5StarWeapons) {
			"5* Weapons"
		} else if (game.configData.scan4StarWeapons) {
			"4* Weapons"
		} else if (game.configData.scan3StarWeapons) {
			"3* Weapons"
		} else {
			"1* Weapons"
		}

		message += if (game.configData.scan4StarWeapons) ", 4* Weapons" else ""
		message += if (game.configData.scan3StarWeapons) ", 3* Weapons" else ""

		return message
	}

	/**
	 * Starts the process to scan weapons in the inventory.
	 *
	 */
	fun start() {
		if (game.imageUtils.findImage("category_selected_weapons", tries = 2) == null && !game.findAndPress("category_unselected_weapons", tries = 2)) {
			game.printToLog("[ERROR] Unable to start Weapon scan.", tag, isError = true)
			return
		}

		game.printToLog("**************************************", tag)
		game.printToLog("[SCAN_WEAPONS] WEAPON SCAN STARTING...", tag)
		game.printToLog("[SCAN_WEAPONS] ${printInitialInfo()}", tag)
		game.printToLog("**************************************", tag)

		// Reset the scroll view.
		game.scanUtils.resetScrollScreen()

		// Collect the locations of all weapons whose "Lv." image asset can be found.
		while (!isSearchDone()) {
			val locations = search()
			if (!validateSearchCompletion(locations)) {
				break
			}

			if (locations.isNotEmpty()) {
				// For every subsequent search, only search for the very last row as every other row above has been processed already.
				if (!enableFullRegionSearch) {
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

				// Now scan each weapon in each location.
				locations.forEach {
					// Select the weapon.
					game.gestureUtils.tap(it.x, it.y, "item_level")

					val (weaponName, weaponRarity) = game.scanUtils.getWeaponNameAndRarity()
					if (validateRarity(weaponRarity)) {
						val (weaponLevel, weaponAscensionLevel) = game.scanUtils.getWeaponLevelAndAscension()
						val weaponRefinementLevel = game.scanUtils.getRefinementLevel()
						val weaponEquippedBy = game.scanUtils.getEquippedBy()
						val weaponLocked = game.scanUtils.getLocked()

						try {
							val weaponObject = Weapon().apply {
								key = weaponName
								level = weaponLevel.toInt()
								ascension = weaponAscensionLevel.toInt()
								refinement = weaponRefinementLevel.toInt()
								location = weaponEquippedBy
								lock = weaponLocked
							}

							weaponList.add(weaponObject)

							game.printToLog("[SCAN_WEAPONS] Weapon scanned: $weaponObject\n", tag)
						} catch (e: Exception) {
							game.printToLog("[ERROR] Weapon failed to scan: (Name: $weaponName, Level: $weaponLevel, Ascension: $weaponAscensionLevel, " +
									"Refinement: $weaponRefinementLevel, Equipped By: $weaponEquippedBy, Locked: $weaponLocked)\n", tag, isError = true)
						}
					}
				}
			}

			checkMore(locations)

			// Different scrolling behavior based on whether this is the first run.
			if (!skipScroll && enableFullRegionSearch) {
				game.scanUtils.scrollFirstRow()
				enableFullRegionSearch = false
			} else if (!skipScroll && locations.size != 0 || enableSingleRowSearch) {
				game.scanUtils.scrollSubsequentRow()
			}

			firstSearchComplete = true
		}

		game.printToLog("[SCAN_WEAPONS] Weapon scan completed with ${weaponList.size} scanned.", tag)
	}
}