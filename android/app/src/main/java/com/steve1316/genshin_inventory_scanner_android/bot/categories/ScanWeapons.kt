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
	private var firstSearchLessThanMax = false
	private var firstSearchMaxSearches = 21
	private var subsequentSearchMaxSearches = 7
	private var subsequentSearchScrollOnce = false
	private var previousRow: ArrayList<Weapon> = arrayListOf()
	private var currentRow: ArrayList<Weapon> = arrayListOf()

	private val testSingleSearch = game.configData.enableTestSingleSearch && game.configData.testSearchWeapon

	/**
	 * Prints the initial information before starting the overall search process.
	 *
	 * @return The formatted string containing the initial information.
	 */
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

	/**
	 * Checks if the search process can be ended.
	 *
	 * @return True when all search queries have been completed.
	 */
	private fun isSearchDone(): Boolean {
		return (search5StarComplete && search4StarComplete && search3StarComplete)
	}

	/**
	 * Validates the rarity of the weapon.
	 *
	 * @param rarity The scanned rarity from the OCR.
	 * @return True if the rarity from the OCR is valid.
	 */
	private fun validateRarity(rarity: String): Boolean {
		return (game.configData.scan5StarWeapons && !search5StarComplete && (rarity == "5")) || (game.configData.scan4StarWeapons && !search4StarComplete && (rarity == "4")) ||
				(game.configData.scan3StarWeapons && !search3StarComplete && (rarity == "3"))
	}

	/**
	 * Marks the current search for the current rarity as completed.
	 *
	 */
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

	/**
	 * An additional check for if the current search has been completed which is determined by the existence of the other rarities. Reset the weapon ascension level in the search query if so.
	 *
	 * @return True if the search for the current rarity has been completed.
	 */
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
			currentSearchCompleted()
			game.scanUtils.resetWeaponAscensionLevel()
			true
		} else if (game.configData.scan4StarWeapons && !search4StarComplete && (game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_2", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_1", region = region, customConfidence = 0.95).size != 0)
		) {
			game.printToLog("[SCAN_WEAPONS] Search for 4* Weapons has been completed.", tag, isWarning = true)
			currentSearchCompleted()
			game.scanUtils.resetWeaponAscensionLevel()
			true
		} else if (game.configData.scan3StarWeapons && !search3StarComplete && (game.imageUtils.findAll("artifact_level_2", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_1", region = region, customConfidence = 0.95).size != 0)
		) {
			game.printToLog("[SCAN_WEAPONS] Search for 3* Weapons has been completed.", tag, isWarning = true)
			currentSearchCompleted()
			game.scanUtils.resetWeaponAscensionLevel()
			true
		} else {
			false
		}
	}

	/**
	 * Performs a search query via a full region scan or row scan.
	 *
	 * @return List of found matches for the current rarity.
	 */
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

	/**
	 * Perform cleanup for first time search with special logic for edge cases.
	 *
	 * @param locationSize Number of matches found from the search query.
	 */
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

		// Cover the case where the first search found less than the maximum. End the search for this rarity and move on to the next. The next scan will be a full scan again but without scrolling.
		else if (locationSize != 0 && locationSize < firstSearchMaxSearches && !firstSearchLessThanMax) {
			game.printToLog("[SCAN_WEAPONS] First search less than the max in the full region scan. Next scan will be a full region scan again.", tag, isWarning = true)
			enableFullRegionSearch = true
			enableSingleRowSearch = false
			firstSearchComplete = false
			firstSearchLessThanMax = true
			checkIfSearchCompleted()
		}

		// Cover the case where the "first" search found less than the maximum. Switch to row scanning for every search after this.
		else if (locationSize != 0 && locationSize < firstSearchMaxSearches && firstSearchLessThanMax) {
			game.printToLog("[SCAN_WEAPONS] \"First\" search less than the max in the full region scan. Switching to row scan.", tag, isWarning = true)
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

	/**
	 * Perform cleanup for subsequent searches with special logic for edge cases.
	 *
	 * @param locationSize Number of matches found from the search query.
	 */
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

			// Check if the previous row was scanned again.
			if (previousRow.size == 7 && currentRow.size == 7) {
				val validChecklist: ArrayList<Boolean> = arrayListOf()
				var index = 0
				while (index <= 6) {
					if (previousRow[index] == currentRow[index]) validChecklist.add(true)
					index++
				}

				// If the current row was already found in the previous, mark this search as complete.
				if (validChecklist.size == 7) {
					previousRow.clear()
					currentSearchCompleted()
					return
				}
			}

			previousRow = currentRow.map { it.clone() } as ArrayList<Weapon>
			currentRow.clear()
			game.scanUtils.scrollSubsequentRow()
		}

		// Cover the case where matches found in single row search was not the maximum. End this rarity search and prep for the next rarity search.
		else if (locationSize != 0 && locationSize < subsequentSearchMaxSearches && !enableFullRegionSearch && enableSingleRowSearch) {
			game.printToLog("[SCAN_WEAPONS] Subsequent search less than the max in the row scan.", tag, isWarning = true)
			subsequentSearchScrollOnce = if (!subsequentSearchScrollOnce && checkIfSearchCompleted()) {
				true
			} else {
				game.scanUtils.scrollSubsequentRow()
				false
			}
		}

		// Cover the case where no matches found in either full region or single row search.
		else if (locationSize == 0) {
			game.printToLog("[SCAN_WEAPONS] Subsequent search found no matches in the row scan.", tag, isWarning = true)
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			checkIfSearchCompleted()
		}
	}

	/**
	 * Starts the search process and process through all search queries.
	 *
	 * @return List of scanned weapons.
	 */
	fun start(): ArrayList<Weapon> {
		if (game.imageUtils.findImage("category_selected_weapons", tries = 2) == null && !game.findAndPress("category_unselected_weapons", tries = 2)) {
			game.printToLog("[ERROR] Could not make the category active and thus could not start the Weapon scan...", tag, isError = true)
			return arrayListOf()
		}

		game.scanUtils.setBackpackLocation()

		val weaponList: ArrayList<Weapon> = arrayListOf()

		// Reset the scroll view or perform a test single search.
		if (!testSingleSearch) {
			game.printToLog("**************************************", tag)
			game.printToLog("[SCAN_WEAPONS] WEAPON SCAN STARTING...", tag)
			game.printToLog("[SCAN_WEAPONS] ${printInitialInfo()}", tag)
			game.printToLog("**************************************", tag)

			game.scanUtils.resetScrollScreen()
		} else {
			game.printToLog("**************************************", tag)
			game.printToLog("[SCAN_SINGLE] TESTING SINGLE SEARCH...", tag)
			game.printToLog("**************************************", tag)

			val (weaponName, _) = game.scanUtils.getWeaponNameAndRarity()
			val (weaponLevel, weaponAscensionLevel) = game.scanUtils.getWeaponLevelAndAscension()
			val weaponRefinementLevel = game.scanUtils.getRefinementLevel()
			val weaponEquippedBy = game.scanUtils.getEquippedBy()
			val weaponLocked = game.scanUtils.getLocked()

			try {
				val weaponObject = Weapon().apply {
					key = weaponName
					level = weaponLevel
					ascension = weaponAscensionLevel
					refinement = weaponRefinementLevel
					location = weaponEquippedBy
					lock = weaponLocked
				}

				weaponList.add(weaponObject)

				game.printToLog("[SCAN_SINGLE] Weapon scanned: $weaponObject\n", tag)
			} catch (e: Exception) {
				game.printToLog(
					"[ERROR] Weapon failed to scan: (Name: $weaponName, Level: $weaponLevel, Ascension: $weaponAscensionLevel, " +
							"Refinement: $weaponRefinementLevel, Equipped By: $weaponEquippedBy, Locked: $weaponLocked)\n", tag, isError = true
				)
			}

			return weaponList
		}

		while (!isSearchDone()) {
			if (!BotService.isRunning) throw InterruptedException("Stopping the bot and breaking out of the loop due to the Stop button being pressed")

			val locations: ArrayList<Point> = search()

			game.printToLog("[SCAN_WEAPONS] Found ${locations.size} weapon locations: $locations.\n", tag, isWarning = true)

			if (locations.isNotEmpty()) {
				// For every subsequent search, only search for the very last row as every other row above has been processed already.
				if (!enableFullRegionSearch) {
					// Now iterate through the array backwards and keep only the locations from the last row.
					val reversedLocations = locations.reversed()
					locations.clear()
					val finalYCoordinate = reversedLocations[0].y
					reversedLocations.forEach { location ->
						if (location.y == finalYCoordinate || location.y + 1 == finalYCoordinate || location.y - 1 == finalYCoordinate) {
							locations.add(location)
						}
					}

					// Now revert the order of the new locations array.
					locations.reverse()
				}

				// Now scan each weapon in each location.
				locations.forEach {
					if (!BotService.isRunning) throw InterruptedException("Stopping the bot and breaking out of the loop due to the Stop button being pressed")

					// Select the weapon.
					game.gestureUtils.tap(it.x, it.y, "item_level")

					// Apply the conditional if the user wanted to scan only the locked ones.
					val weaponLocked = game.scanUtils.getLocked()
					if (!game.configData.scanOnlyLockedWeapons || (game.configData.scanOnlyLockedWeapons && weaponLocked)) {
						val (weaponName, weaponRarity) = game.scanUtils.getWeaponNameAndRarity()
						if (validateRarity(weaponRarity)) {
							val (weaponLevel, weaponAscensionLevel) = game.scanUtils.getWeaponLevelAndAscension()
							val weaponRefinementLevel = game.scanUtils.getRefinementLevel()
							val weaponEquippedBy = game.scanUtils.getEquippedBy()

							try {
								val weaponObject = Weapon().apply {
									key = weaponName
									level = weaponLevel
									ascension = weaponAscensionLevel
									refinement = weaponRefinementLevel
									location = weaponEquippedBy
									lock = weaponLocked
								}

								weaponList.add(weaponObject)
								if (enableSingleRowSearch) currentRow.add(weaponObject)

								game.printToLog("[SCAN_WEAPONS] Weapon scanned: $weaponObject\n", tag)
							} catch (e: Exception) {
								game.printToLog(
									"[ERROR] Weapon failed to scan: (Name: $weaponName, Level: $weaponLevel, Ascension: $weaponAscensionLevel, " +
											"Refinement: $weaponRefinementLevel, Equipped By: $weaponEquippedBy, Locked: $weaponLocked)\n", tag, isError = true
								)
							}
						}
					} else {
						game.printToLog("[SCAN_WEAPONS] Skipping weapon as it is not marked as locked.\n", tag)
					}
				}

				// Recover the scroll level if needed.
				if (firstSearchComplete) game.scanUtils.scrollRecovery(locations[0].y)
			}

			if (!firstSearchComplete) {
				searchCleanupFirstTime(locations.size)
			} else {
				searchCleanupSubsequent(locations.size)
			}

			if (debugMode) {
				game.printToLog(
					"[SCAN_WEAPONS][DEBUG] enableFullRegionSearch: $enableFullRegionSearch | enableSingleRowSearch: $enableSingleRowSearch | firstSearchComplete: $firstSearchComplete",
					tag, isWarning = true
				)
				game.printToLog(
					"[SCAN_WEAPONS][DEBUG] search5StarComplete: $search5StarComplete | search4StarComplete: $search4StarComplete | search3StarComplete: $search3StarComplete",
					tag, isWarning = true
				)
			}
		}

		game.printToLog("\n**************************************", tag)
		game.printToLog("[SCAN_WEAPONS] Weapon scan completed with ${weaponList.size} scanned.", tag)
		game.printToLog("**************************************", tag)

		return weaponList
	}
}