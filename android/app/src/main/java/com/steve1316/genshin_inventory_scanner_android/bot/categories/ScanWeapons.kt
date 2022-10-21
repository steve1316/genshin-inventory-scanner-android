package com.steve1316.genshin_inventory_scanner_android.bot.categories

import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game
import com.steve1316.genshin_inventory_scanner_android.data.Weapon
import com.steve1316.genshin_inventory_scanner_android.utils.BotService
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

	private var firstSearchComplete = false
	private var firstSearchMaxSearches = 21
	private var subsequentSearchMaxSearches = 7

	private var weaponList: ArrayList<Weapon> = arrayListOf()

	private fun printInitialInfo(): String {
		var startWith5Stars = false
		var startWith4Stars = false
		var message = if (game.configData.scan5StarWeapons) {
			startWith5Stars = true
			"5* Weapons"
		} else if (game.configData.scan4StarWeapons) {
			startWith4Stars = true
			"4* Weapons"
		} else {
			"3* Weapons"
		}

		if (startWith5Stars) {
			if (game.configData.scan4StarWeapons) {
				message += ", 4* Weapons"
			}

			if (game.configData.scan3StarWeapons) {
				message += ", 3* Weapons"
			}
		} else if (startWith4Stars) {
			if (game.configData.scan3StarWeapons) {
				message += ", 3* Weapons"
			}
		}

		return message
	}

	private fun isSearchDone(): Boolean {
		return (search5StarComplete && search4StarComplete && search3StarComplete)
	}

	private fun validateRarity(rarity: String): Boolean {
		return (game.configData.scan5StarWeapons && !search5StarComplete && (rarity == "5")) || (game.configData.scan4StarWeapons && !search4StarComplete && (rarity == "4")) ||
				(game.configData.scan3StarWeapons && !search3StarComplete && (rarity == "3"))
	}

	private fun currentSearchCompleted() {
		if (game.configData.scan5StarWeapons && !search5StarComplete) {
			game.printToLog("[SCAN_WEAPONS] Finished scanning for 5* Weapons...", tag, isWarning = true)
			search5StarComplete = true
		} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
			game.printToLog("[SCAN_WEAPONS] Finished scanning for 4* Weapons...", tag, isWarning = true)
			search4StarComplete = true
		} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
			game.printToLog("[SCAN_WEAPONS] Finished scanning for 3* Weapons...", tag, isWarning = true)
			search3StarComplete = true
		}
	}

	private fun checkIfSearchCompleted(): Boolean {
		val region = if (enableFullRegionSearch) {
			intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight)
		} else {
			intArrayOf(0, MPS.displayHeight - (MPS.displayHeight / 3), MPS.displayWidth, MPS.displayHeight / 3)
		}

		return if (game.configData.scan5StarWeapons && !search5StarComplete && (game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_2", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_1", region = region, customConfidence = 0.95).size != 0)
		) {
			game.printToLog("[SCAN_WEAPONS] Search for 5* Weapons has been completed.", tag, isWarning = true)
			search5StarComplete = true
			true
		} else if (game.configData.scan4StarWeapons && !search4StarComplete && (game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_2", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_1", region = region, customConfidence = 0.95).size != 0)
		) {
			game.printToLog("[SCAN_WEAPONS] Search for 4* Weapons has been completed.", tag, isWarning = true)
			search4StarComplete = true
			true
		} else if (game.configData.scan3StarWeapons && !search3StarComplete && (game.imageUtils.findAll("artifact_level_2", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_1", region = region, customConfidence = 0.95).size != 0)
		) {
			game.printToLog("[SCAN_WEAPONS] Search for 3* Weapons has been completed.", tag, isWarning = true)
			search3StarComplete = true
			true
		} else {
			false
		}
	}

	private fun search(): ArrayList<Point> {
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
			game.printToLog("[SCAN_WEAPONS] Search came up with no matches.", tag, isError = true)
			arrayListOf()
		}
	}

	private fun searchCleanupFirstTime(locationSize: Int) {
		// Cover the case where the first search found all the bot needed.
		val booleanArray: BooleanArray = booleanArrayOf(game.configData.scan5StarWeapons, game.configData.scan4StarWeapons, game.configData.scan3StarWeapons)
		if (locationSize != 0 && locationSize < firstSearchMaxSearches && booleanArray.count { it } == 1) {
			game.printToLog("[SCAN_WEAPONS] First search is less than the max in the full region scan for the only rarity selected.", tag, isWarning = true)
			search5StarComplete = true
			search4StarComplete = true
			search3StarComplete = true
			enableFullRegionSearch = false
			enableSingleRowSearch = false
			firstSearchComplete = true
			return
		}

		// Cover the case where the first search found all the bot needed and was at the maximum.
		else if (locationSize != 0 && locationSize == firstSearchMaxSearches) {
			game.printToLog("[SCAN_WEAPONS] First search is the max in the full region scan.", tag, isWarning = true)
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			firstSearchComplete = true
			game.scanUtils.scrollFirstRow()
		}

		// Cover the case where the first search found less than the maximum.
		else if (locationSize != 0 && locationSize < firstSearchMaxSearches) {
			game.printToLog("[SCAN_WEAPONS] First search less than the max in the full region scan.", tag, isWarning = true)
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			firstSearchComplete = true
			game.scanUtils.scrollFirstRow()
		}

		// Cover the case where the first search found nothing.
		else if (locationSize == 0) {
			game.printToLog("[SCAN_WEAPONS] First search found no matches in the full region scan.", tag, isWarning = true)
			enableFullRegionSearch = false
			enableSingleRowSearch = false
			if (!checkIfSearchCompleted()) {
				firstSearchComplete = false
			}
		}
	}

	private fun searchCleanupSubsequent(locationSize: Int) {
		// Cover the case where the number of matches found in the full region search was the maximum or less than.
		if (locationSize != 0 && locationSize <= subsequentSearchMaxSearches && enableFullRegionSearch && !enableSingleRowSearch) {
			game.printToLog("[SCAN_WEAPONS] Subsequent search less than the max in the full region scan.", tag, isWarning = true)
			enableFullRegionSearch = false
			enableSingleRowSearch = true
		}

		// Cover the case where the number of matches in the single row search was the entire row.
		else if (locationSize != 0 && locationSize == subsequentSearchMaxSearches && !enableFullRegionSearch && enableSingleRowSearch) {
			game.printToLog("[SCAN_WEAPONS] Subsequent search is the max in the row scan.", tag, isWarning = true)
			game.scanUtils.scrollSubsequentRow()
		}

		// Cover the case where matches found in single row search was not the maximum. End this rarity search and prep for the next rarity search.
		else if (locationSize != 0 && locationSize < subsequentSearchMaxSearches && !enableFullRegionSearch && enableSingleRowSearch) {
			game.printToLog("[SCAN_WEAPONS] Subsequent search less than the max in the row scan.", tag, isWarning = true)
			currentSearchCompleted()
		}

		// Cover the case where no matches found in either full region or single row search.
		else if (locationSize == 0) {
			game.printToLog("[SCAN_WEAPONS] Subsequent search found no matches in the row scan.", tag, isWarning = true)
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			checkIfSearchCompleted()
		}
	}

	fun start() {
		game.printToLog("**************************************", tag)
		game.printToLog("[SCAN_WEAPONS] WEAPON SCAN STARTING...", tag)
		game.printToLog("[SCAN_WEAPONS] ${printInitialInfo()}", tag)
		game.printToLog("**************************************", tag)

		// Reset the scroll view.
		game.scanUtils.resetScrollScreen()

		while (!isSearchDone()) {
			if (!BotService.isRunning) throw InterruptedException("Stopping the bot and breaking out of the loop due to the Stop button being pressed")

			val locations: ArrayList<Point> = search()
			game.printToLog("[SCAN_WEAPONS] Found ${locations.size} locations: $locations.", tag, isWarning = true)

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
							game.printToLog(
								"[ERROR] Weapon failed to scan: (Name: $weaponName, Level: $weaponLevel, Ascension: $weaponAscensionLevel, " +
										"Refinement: $weaponRefinementLevel, Equipped By: $weaponEquippedBy, Locked: $weaponLocked)\n", tag, isError = true
							)
						}
					}
				}
			}

			if (!firstSearchComplete) {
				searchCleanupFirstTime(locations.size)
			} else {
				searchCleanupSubsequent(locations.size)
			}

			if (debugMode) {
				game.printToLog(
					"[SCAN_WEAPONS][DEBUG] enableFullRegionSearch: $enableFullRegionSearch | enableSingleRowSearch: $enableSingleRowSearch | firstSearchComplete: $firstSearchComplete",
					tag,
					isWarning = true
				)
				game.printToLog(
					"[SCAN_WEAPONS][DEBUG] search5StarComplete: $search5StarComplete | search4StarComplete: $search4StarComplete | search3StarComplete: $search3StarComplete",
					tag,
					isWarning = true
				)
			}
		}

		game.printToLog("[SCAN_WEAPONS] Weapon scan completed with ${weaponList.size} scanned.", tag)
	}
}