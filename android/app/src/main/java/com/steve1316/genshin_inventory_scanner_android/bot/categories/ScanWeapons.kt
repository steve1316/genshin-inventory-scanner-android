package com.steve1316.genshin_inventory_scanner_android.bot.categories

import android.util.Log
import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game
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
	private var skipScroll = false

	private var firstSearchComplete = false
	private var firstSearchMaxSearches = 21
	private var subsequentSearchMaxSearches = 7

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

	private fun currentSearchCompleted() {
		if (game.configData.scan5StarWeapons && !search5StarComplete) {
			game.printToLog("[SCAN_WEAPONS] Finished scanning for 5* Weapons...", tag)
			search5StarComplete = true
		} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
			game.printToLog("[SCAN_WEAPONS] Finished scanning for 4* Weapons...", tag)
			search4StarComplete = true
		} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
			game.printToLog("[SCAN_WEAPONS] Finished scanning for 3* Weapons...", tag)
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
			search5StarComplete = true
			true
		} else if (game.configData.scan4StarWeapons && !search4StarComplete && (game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_2", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_1", region = region, customConfidence = 0.95).size != 0)
		) {
			search4StarComplete = true
			true
		} else if (game.configData.scan3StarWeapons && !search3StarComplete && (game.imageUtils.findAll("artifact_level_2", region = region, customConfidence = 0.95).size != 0 ||
					game.imageUtils.findAll("artifact_level_1", region = region, customConfidence = 0.95).size != 0)
		) {
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
			Log.w(tag, "Searching 5 stars.")
			return game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95)
		} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
			Log.w(tag, "Searching 4 stars.")
			return game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95)
		} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
			Log.w(tag, "Searching 3 stars.")
			return game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95)
		} else {
			Log.e(tag, "Returning nothing at all.")
			arrayListOf()
		}
	}

	private fun searchCleanupFirstTime(locationSize: Int) {
		// Cover the case where the first search found all the bot needed.
		val booleanArray: BooleanArray = booleanArrayOf(game.configData.scan5StarWeapons, game.configData.scan4StarWeapons, game.configData.scan3StarWeapons)
		if (locationSize != 0 && locationSize < firstSearchMaxSearches && booleanArray.count { it } == 1) {
			Log.w(tag, "SEARCH: first search less than max in full region for the only rarity")
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
			Log.w(tag, "SEARCH: first search is max in full region")
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			firstSearchComplete = true
			game.scanUtils.scrollFirstRow()
		}

		// Cover the case where the first search found less than the maximum.
		else if (locationSize != 0 && locationSize < firstSearchMaxSearches) {
			Log.w(tag, "SEARCH: first search less than max in full region")
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			skipScroll = true
			firstSearchComplete = true
			game.scanUtils.scrollFirstRow()
		}

		// Cover the case where the first search found nothing.
		else if (locationSize == 0) {
			Log.w(tag, "SEARCH: first search found nothing in full region")
			enableFullRegionSearch = false
			enableSingleRowSearch = false
			skipScroll = true
			if (!checkIfSearchCompleted()) {
				firstSearchComplete = false
			}
		}
	}

	private fun searchCleanupSubsequent(locationSize: Int) {
		// Cover the case where the number of matches found in the full region search was the maximum or less than.
		if (locationSize != 0 && locationSize <= subsequentSearchMaxSearches && enableFullRegionSearch && !enableSingleRowSearch) {
			Log.w(tag, "SEARCH: subsequent search <= max in full region")
			enableFullRegionSearch = false
			enableSingleRowSearch = true
			skipScroll = false
		}

		// Cover the case where the number of matches in the single row search was the entire row.
		else if (locationSize != 0 && locationSize == subsequentSearchMaxSearches && !enableFullRegionSearch && enableSingleRowSearch) {
			Log.w(tag, "SEARCH: subsequent search is max in row")
			skipScroll = false
			game.scanUtils.scrollSubsequentRow()
		}

		// Cover the case where matches found in single row search was not the maximum.
		else if (locationSize != 0 && locationSize < subsequentSearchMaxSearches && !enableFullRegionSearch && enableSingleRowSearch) {
			// End this rarity search and prep for the next rarity search.
			Log.w(tag, "SEARCH: subsequent search is less than max in row")
			currentSearchCompleted()
			skipScroll = true
		}

		// Cover the case where no matches found in either full region or single row search.
		else if (locationSize == 0) {
			Log.w(tag, "SEARCH: subsequent search found none in row")
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

		var locations: ArrayList<Point> = arrayListOf()

		while (!isSearchDone()) {
			if (!BotService.isRunning) {
				throw Exception()
			}

			locations = search()
			Log.w(tag, "Found locations: $locations")

			// Main Body


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
//					game.gestureUtils.tap(it.x, it.y, "item_level")

//					val (weaponName, weaponRarity) = game.scanUtils.getWeaponNameAndRarity()
//					if (validateRarity(weaponRarity)) {
//						val (weaponLevel, weaponAscensionLevel) = game.scanUtils.getWeaponLevelAndAscension()
//						val weaponRefinementLevel = game.scanUtils.getRefinementLevel()
//						val weaponEquippedBy = game.scanUtils.getEquippedBy()
//						val weaponLocked = game.scanUtils.getLocked()
//
//						try {
//							val weaponObject = Weapon().apply {
//								key = weaponName
//								level = weaponLevel.toInt()
//								ascension = weaponAscensionLevel.toInt()
//								refinement = weaponRefinementLevel.toInt()
//								location = weaponEquippedBy
//								lock = weaponLocked
//							}
//
//							weaponList.add(weaponObject)
//
//							game.printToLog("[SCAN_WEAPONS] Weapon scanned: $weaponObject\n", tag)
//						} catch (e: Exception) {
//							game.printToLog(
//								"[ERROR] Weapon failed to scan: (Name: $weaponName, Level: $weaponLevel, Ascension: $weaponAscensionLevel, " +
//										"Refinement: $weaponRefinementLevel, Equipped By: $weaponEquippedBy, Locked: $weaponLocked)\n", tag, isError = true
//							)
//						}
//					}
				}
			}


			// End of Main Body
			if (!firstSearchComplete) {
				searchCleanupFirstTime(locations.size)
			} else {
				searchCleanupSubsequent(locations.size)
			}

			Log.w(tag, "enableFullRegionSearch: $enableFullRegionSearch | enableSingleRowSearch: $enableSingleRowSearch | skipScroll: $skipScroll | firstSearchComplete: $firstSearchComplete")
			Log.w(tag, "search5StarComplete: $search5StarComplete | search4StarComplete: $search4StarComplete | search3StarComplete: $search3StarComplete")
		}
	}

//	private var enableFullRegionSearch = true
//	private var enableSingleRowSearch = false
//	private var search5StarComplete = !game.configData.scan5StarWeapons
//	private var search4StarComplete = !game.configData.scan4StarWeapons
//	private var search3StarComplete = !game.configData.scan3StarWeapons
//	private var skipScroll = false
//
//	private var firstSearchComplete = false
//	private var firstSearchMaxSearches = 21
//	private var subsequentSearchMaxSearches = 7
//
//	private var weaponList: ArrayList<Weapon> = arrayListOf()
//
//	fun search(): ArrayList<Point> {
//		val region = if (enableFullRegionSearch) {
//			intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight)
//		} else {
//			intArrayOf(0, MPS.displayHeight - (MPS.displayHeight / 3), MPS.displayWidth, MPS.displayHeight / 3)
//		}
//
//		return if (game.configData.scan5StarWeapons && !search5StarComplete) {
//			return game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95)
//		} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
//			return game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95)
//		} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
//			return game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95)
//		} else {
//			Log.e(tag, "Returning nothing at all.")
//			arrayListOf()
//		}
//	}
//
//	private fun searchForOtherRarities(locations: ArrayList<Point>): Boolean {
//		if (locations.size == 0) {
//			val region = if (enableFullRegionSearch) {
//				intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight)
//			} else {
//				intArrayOf(0, MPS.displayHeight - (MPS.displayHeight / 3), MPS.displayWidth, MPS.displayHeight / 3)
//			}
//
//			return if (game.configData.scan5StarWeapons && !search5StarComplete && ((game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95).size != 0) ||
//				game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size != 0)) {
//				return true
//			} else if (game.configData.scan4StarWeapons && !search4StarComplete && ((game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95).size != 0) ||
//				game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size != 0)) {
//				return true
//			} else if (game.configData.scan3StarWeapons && !search3StarComplete && ((game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95).size != 0) ||
//				game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95).size != 0)) {
//				return true
//			} else {
//				Log.e(tag, "Returning no other rarities.")
//				false
//			}
//		}
//
//		Log.e(tag, "Returning no other rarities 2.")
//		return false
//	}
//
//	private fun validateRarity(rarity: String): Boolean {
//		return (game.configData.scan5StarWeapons && !search5StarComplete && (rarity == "5")) || (game.configData.scan4StarWeapons && !search4StarComplete && (rarity == "4")) ||
//				(game.configData.scan3StarWeapons && !search3StarComplete && (rarity == "3"))
//	}
//
//	private fun validateSearchCompletion(locations: ArrayList<Point>): Boolean {
//		// If no locations were found, mark that rarity search as complete.
//		if (locations.size == 0) {
//			if (!firstSearchComplete) {
//				return true
//			} else if (game.configData.scan5StarWeapons && !search5StarComplete) {
//				game.printToLog("[SCAN_WEAPONS] Finished scanning for 5* Weapons...", tag)
//				search5StarComplete = true
//			} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
//				game.printToLog("[SCAN_WEAPONS] Finished scanning for 4* Weapons...", tag)
//				search4StarComplete = true
//			} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
//				game.printToLog("[SCAN_WEAPONS] Finished scanning for 3* Weapons...", tag)
//				search3StarComplete = true
//			} else {
//				return false
//			}
//		}
//
//		return true
//	}
//
//	private fun finalValidationCheck(locations: ArrayList<Point>) {
//		if (enableFullRegionSearch && !enableSingleRowSearch && locations.size < firstSearchMaxSearches) {
//			// If the amount of locations for the current rarity found was less than the expected maximum for the full region search, then that means that this search has been completed early.
//			// Turn off scrolling and allow for full region search again for the next rarity search to start.
//			skipScroll = !firstSearchComplete
//
//			if (!firstSearchComplete) {
//				val region = intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight)
//				skipScroll = !((game.configData.scan5StarWeapons && !search5StarComplete && game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95).size == 0) ||
//						(game.configData.scan4StarWeapons && !search4StarComplete && game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95).size == 0) ||
//						(game.configData.scan3StarWeapons && !search3StarComplete && game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size == 0))
//
//				if (!searchForOtherRarities(locations)) {
//					if (game.configData.scan5StarWeapons && !search5StarComplete) {
//						game.printToLog("[SCAN_WEAPONS] 5* Weapons seem to have been all scanned from the full region...", tag)
//						search5StarComplete = true
//					} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
//						game.printToLog("[SCAN_WEAPONS] 4* Weapons seem to have been all scanned from the full region...", tag)
//						search4StarComplete = true
//					} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
//						game.printToLog("[SCAN_WEAPONS] 3* Weapons seem to have been all scanned from the full region...", tag)
//						search3StarComplete = true
//					}
//				}
//			}
//		} else if (!enableFullRegionSearch && enableSingleRowSearch && locations.size < subsequentSearchMaxSearches) {
//			// Similarly, if the amount of locations was less than the expected maximum for the single row search, then end the search, turn off scrolling but do not re-enable full region search.
//			skipScroll = false
//
//			if (!firstSearchComplete) {
//				val region = intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight)
//				skipScroll = !((game.configData.scan5StarWeapons && !search5StarComplete && game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95).size == 0) ||
//						(game.configData.scan4StarWeapons && !search4StarComplete && game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.95).size == 0) ||
//						(game.configData.scan3StarWeapons && !search3StarComplete && game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.95).size == 0))
//			}
//
//			if (!searchForOtherRarities(locations)) {
//				if (game.configData.scan5StarWeapons && !search5StarComplete) {
//					game.printToLog("[SCAN_WEAPONS] 5* Weapons seem to have been all scanned from the last row...", tag)
//					search5StarComplete = true
//				} else if (game.configData.scan4StarWeapons && !search4StarComplete) {
//					game.printToLog("[SCAN_WEAPONS ] 4* Weapons seem to have been all scanned from the last row...", tag)
//					search4StarComplete = true
//				} else if (game.configData.scan3StarWeapons && !search3StarComplete) {
//					game.printToLog("[SCAN_WEAPONS] 3* Weapons seem to have been all scanned from the last row...", tag)
//					search3StarComplete = true
//				}
//			}
//		} else if (locations.size != 0) {
//			Log.w(tag, "Validating okay")
//
//			enableFullRegionSearch = false
//			enableSingleRowSearch = true
//			skipScroll = false
//
//			firstSearchComplete = true
//		} else {
//			Log.w(tag, "Validating not okay")
//
//			enableFullRegionSearch = false
//			enableSingleRowSearch = true
//		}
//	}
//
//	private fun isSearchDone(): Boolean {
//		return (search5StarComplete && game.configData.scan5StarWeapons) && (search4StarComplete && game.configData.scan4StarWeapons) &&
//				(search3StarComplete && game.configData.scan3StarWeapons)
//	}
//
//	private fun printInitialInfo(): String {
//		var message = if (game.configData.scan5StarWeapons) {
//			"5* Weapons"
//		} else if (game.configData.scan4StarWeapons) {
//			"4* Weapons"
//		} else {
//			"3* Weapons"
//		}
//
//		val booleanArray: BooleanArray = booleanArrayOf(game.configData.scan5StarWeapons, game.configData.scan4StarWeapons, game.configData.scan3StarWeapons)
//		if (booleanArray.count { it } > 1) {
//			message += if (game.configData.scan4StarWeapons) ", 4* Weapons" else ""
//			message += if (game.configData.scan3StarWeapons) ", 3* Weapons" else ""
//		}
//
//		return message
//	}
//
//	/**
//	 * Starts the process to scan weapons in the inventory.
//	 *
//	 */
//	fun start() {
//		if (game.imageUtils.findImage("category_selected_weapons", tries = 2) == null && !game.findAndPress("category_unselected_weapons", tries = 2)) {
//			game.printToLog("[ERROR] Unable to start Weapon scan.", tag, isError = true)
//			return
//		}
//
//		game.printToLog("**************************************", tag)
//		game.printToLog("[SCAN_WEAPONS] WEAPON SCAN STARTING...", tag)
//		game.printToLog("[SCAN_WEAPONS] ${printInitialInfo()}", tag)
//		game.printToLog("**************************************", tag)
//
//		// Reset the scroll view.
//		game.scanUtils.resetScrollScreen()
//
//		// Collect the locations of all weapons whose "Lv." image asset can be found.
//		while (!isSearchDone()) {
//			if (!BotService.isRunning) {
//				throw Exception()
//			}
//
//			val locations = search()
//			Log.w(tag, "Locations searched: $locations")
//			if (!validateSearchCompletion(locations)) {
//				break
//			}
//
//			if (locations.isNotEmpty()) {
//				// For every subsequent search, only search for the very last row as every other row above has been processed already.
//				if (!enableFullRegionSearch) {
//					// Now iterate through the array backwards and keep only the locations from the last row.
//					val reversedLocations = locations.reversed()
//					locations.clear()
//					val finalYCoordinate = reversedLocations[0].y
//					reversedLocations.forEach { location ->
//						if (location.y == finalYCoordinate) {
//							locations.add(location)
//						}
//					}
//
//					// Now revert the order of the new locations array.
//					locations.reverse()
//				}
//
//				// Now scan each weapon in each location.
//				locations.forEach {
//					// Select the weapon.
//					game.gestureUtils.tap(it.x, it.y, "item_level")
//
//					val (weaponName, weaponRarity) = game.scanUtils.getWeaponNameAndRarity()
//					if (validateRarity(weaponRarity)) {
//						val (weaponLevel, weaponAscensionLevel) = game.scanUtils.getWeaponLevelAndAscension()
//						val weaponRefinementLevel = game.scanUtils.getRefinementLevel()
//						val weaponEquippedBy = game.scanUtils.getEquippedBy()
//						val weaponLocked = game.scanUtils.getLocked()
//
//						try {
//							val weaponObject = Weapon().apply {
//								key = weaponName
//								level = weaponLevel.toInt()
//								ascension = weaponAscensionLevel.toInt()
//								refinement = weaponRefinementLevel.toInt()
//								location = weaponEquippedBy
//								lock = weaponLocked
//							}
//
//							weaponList.add(weaponObject)
//
//							game.printToLog("[SCAN_WEAPONS] Weapon scanned: $weaponObject\n", tag)
//						} catch (e: Exception) {
//							game.printToLog(
//								"[ERROR] Weapon failed to scan: (Name: $weaponName, Level: $weaponLevel, Ascension: $weaponAscensionLevel, " +
//										"Refinement: $weaponRefinementLevel, Equipped By: $weaponEquippedBy, Locked: $weaponLocked)\n", tag, isError = true
//							)
//						}
//					}
//				}
//			}
//
//			finalValidationCheck(locations)
//
//			// Different scrolling behavior based on whether this is the first run.
//			if (!skipScroll && enableFullRegionSearch) {
//				game.scanUtils.scrollFirstRow()
//				enableFullRegionSearch = false
//			} else if (!skipScroll && locations.size != 0 || enableSingleRowSearch) {
//				game.scanUtils.scrollSubsequentRow()
//			}
//
//			Log.w(tag, "restarting search > enableFullRegionSearch: $enableFullRegionSearch | enableSingleRowSearch: $enableSingleRowSearch | skipScroll: $skipScroll | " +
//					"firstSearchComplete: $firstSearchComplete")
//			Log.w(tag, "restarting search > search5StarComplete: $search5StarComplete | search4StarComplete: $search4StarComplete | search3StarComplete: $search3StarComplete")
//		}
//
//		game.printToLog("[SCAN_WEAPONS] Weapon scan completed with ${weaponList.size} scanned.", tag)
//	}
}