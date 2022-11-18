package com.steve1316.genshin_inventory_scanner_android.bot.categories

import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game
import com.steve1316.genshin_inventory_scanner_android.data.CharacterData
import com.steve1316.genshin_inventory_scanner_android.utils.BotService
import org.opencv.core.Point

class ScanCharacters(private val game: Game) {
	private val tag: String = "${MainActivity.loggerTag}ScanCharacters"
	private val debugMode = game.configData.debugMode

	private var searchComplete = false

	// This grid is comprised of each row followed by the columns in the inner ArrayLists. For 1080p, the x's are spaced out by 175px and the y's are spaced out by 220px except for the last row.
	// Use the starting location to offset it by these for the location of each grid element on the screen.
	private val gridOffsetRow1: ArrayList<Point> = arrayListOf(
		Point(40.0, 175.0), Point(215.0, 175.0), Point(390.0, 175.0)
	)
	private val gridOffsetRow2: ArrayList<Point> = arrayListOf(
		Point(40.0, 395.0), Point(215.0, 395.0), Point(390.0, 395.0)
	)
	private val gridOffsetRow3: ArrayList<Point> = arrayListOf(
		Point(40.0, 615.0), Point(215.0, 615.0), Point(390.0, 615.0)
	)
	private val gridOffsetRow4: ArrayList<Point> = arrayListOf(
		Point(40.0, 835.0), Point(215.0, 835.0), Point(390.0, 835.0)
	)

	private lateinit var characterStartingLocation: Point

	private var firstSearchComplete = false

	private val testSingleSearch = game.configData.enableTestSingleSearch && game.configData.testSearchCharacter

	/**
	 * Perform initial setup by navigating to the Character grid.
	 *
	 * @return True if the Character grid is shown successfully.
	 */
	private fun initialSetup(): Boolean {
		game.wait(0.5)

		if (game.imageUtils.findImage("character_attributes_selected", tries = 1, suppressError = !game.configData.debugMode) != null ||
			game.findAndPress("character_attributes", tries = 1, suppressError = !game.configData.debugMode)
		) {
			return if (testSingleSearch) {
				true
			} else {
				game.findAndPress("character_grid")
			}
		}

		if (game.findAndPress("exit_inventory", tries = 1)) {
			game.wait(1.0)
			game.findAndPress("character")
			if (!testSingleSearch && game.findAndPress("character_grid")) return true
			else if (testSingleSearch) return true
		} else {
			if (!testSingleSearch && game.imageUtils.findImage("character_grid_starting_location", tries = 1) != null) return true
			else if (!testSingleSearch && game.findAndPress("character_grid", tries = 1)) return true
			else if (testSingleSearch) return true
		}

		return false
	}

	/**
	 * Compile the list of grid offsets for the current search.
	 *
	 * @return List of grid offsets.
	 */
	private fun search(): ArrayList<Point> {
		// If this is a subsequent scan, return only the grid offsets of the last row.
		return if (!firstSearchComplete) (gridOffsetRow1 + gridOffsetRow2 + gridOffsetRow3 + gridOffsetRow4) as ArrayList<Point>
		else gridOffsetRow4
	}

	/**
	 * Starts the search process and process through all search queries.
	 *
	 * @return List of Characters scanned.
	 */
	fun start(): ArrayList<CharacterData> {
		if (!initialSetup()) {
			game.printToLog("[ERROR] Could not make the category active and thus could not start the Character scan...", tag, isError = true)
			return arrayListOf()
		}

		val characterList: ArrayList<CharacterData> = arrayListOf()
		val listOfCharacterNames: ArrayList<String> = arrayListOf()

		// Reset the scroll view or perform a test single search.
		if (!testSingleSearch) {
			game.printToLog("**************************************", tag)
			game.printToLog("[SCAN_CHARACTERS] CHARACTER SCAN STARTING...", tag)
			game.printToLog("**************************************", tag)

			characterStartingLocation = game.imageUtils.findImage("character_grid_starting_location") ?: throw Exception("[ERROR] Could not find the starting location to begin the Character scan...")
		} else {
			game.printToLog("**************************************", tag)
			game.printToLog("[SCAN_SINGLE] TESTING SINGLE SEARCH...", tag)
			game.printToLog("**************************************", tag)

			var characterName = ""
			var characterLevel = 1
			var characterAscensionLevel = 0
			var characterConstellationLevel = 0
			var characterTalentLevels = arrayListOf<Int>()

			try {
				val exitLocation = game.imageUtils.findImage("exit_inventory") ?: throw Exception("Could not find the Exit button for location offset.")

				characterName = game.scanUtils.getCharacterName(exitLocation)
				if (listOfCharacterNames.contains(characterName)) {
					searchComplete = true
				} else {
					characterLevel = game.scanUtils.getCharacterLevel(exitLocation)
					characterAscensionLevel = game.scanUtils.getCharacterAscensionLevel()
					characterConstellationLevel = game.scanUtils.getCharacterConstellationLevel()
					characterTalentLevels = game.scanUtils.getCharacterTalentLevels(characterName, characterConstellationLevel)
				}

				val newCharacter = CharacterData().apply {
					key = characterName
					level = characterLevel
					constellation = characterConstellationLevel
					ascension = characterAscensionLevel
					talent = Talent().apply {
						auto = characterTalentLevels[0]
						skill = characterTalentLevels[1]
						burst = characterTalentLevels[2]
					}
				}

				characterList.add(newCharacter)

				game.printToLog("[SCAN_CHARACTERS] Character scanned: $newCharacter\n", tag)
			} catch (e: Exception) {
				game.printToLog(
					"[ERROR] Failed to scan: (Name: $characterName, Ascension Level: $characterAscensionLevel, Constellations Level: $characterConstellationLevel, " +
							"Talent Levels: $characterTalentLevels) with the following error: ${e.message}\n", tag, isError = true
				)
			}

			return characterList
		}

		while (!searchComplete) {
			if (!BotService.isRunning) throw InterruptedException("Stopping the bot and breaking out of the loop due to the Stop button being pressed")

			val locations: ArrayList<Point> = search()

			locations.forEach {
				if (!searchComplete) {
					if (!BotService.isRunning) throw InterruptedException("Stopping the bot and breaking out of the loop due to the Stop button being pressed")

					// Select the Character by using the starting location and the grid offset.
					game.gestureUtils.tap(characterStartingLocation.x + it.x, characterStartingLocation.y + it.y, "item_level")
					game.wait(0.10)
					if (!game.findAndPress("character_confirm", tries = 2)) game.findAndPress("character_level_up", tries = 2)

					game.wait(1.0)

					var characterName = ""
					var characterLevel = 1
					var characterAscensionLevel = 0
					var characterConstellationLevel = 0
					var characterTalentLevels = arrayListOf<Int>()

					try {
						val exitLocation = game.imageUtils.findImage("exit_inventory") ?: throw Exception("Could not find the Exit button for location offset.")

						characterName = game.scanUtils.getCharacterName(exitLocation)
						if (characterName != "" && listOfCharacterNames.contains(characterName)) {
							game.printToLog("[SCAN_CHARACTERS] $characterName was already scanned. Ending the scan now...", tag)
							searchComplete = true
						} else {
							characterLevel = game.scanUtils.getCharacterLevel(exitLocation)
							characterAscensionLevel = game.scanUtils.getCharacterAscensionLevel()
							characterConstellationLevel = game.scanUtils.getCharacterConstellationLevel()
							characterTalentLevels = game.scanUtils.getCharacterTalentLevels(characterName, characterConstellationLevel)
						}

						val newCharacter = CharacterData().apply {
							key = characterName
							level = characterLevel
							constellation = characterConstellationLevel
							ascension = characterAscensionLevel
							talent = Talent().apply {
								auto = characterTalentLevels[0]
								skill = characterTalentLevels[1]
								burst = characterTalentLevels[2]
							}
						}

						characterList.add(newCharacter)
						listOfCharacterNames.add(newCharacter.key)

						game.printToLog("[SCAN_CHARACTERS] Character scanned: $newCharacter\n", tag)
					} catch (e: Exception) {
						game.printToLog(
							"[ERROR] Failed to scan: (Name: $characterName, Ascension Level: $characterAscensionLevel, Constellations Level: $characterConstellationLevel, " +
									"Talent Levels: $characterTalentLevels) with the following error: ${e.message}\n", tag, isError = true
						)
					}

					game.findAndPress("character_grid")
					game.wait(1.0)
				}
			}

			// Recover the scroll level if needed.
			game.scanUtils.scrollCharacterRecovery(locations[0].y)

			if (!firstSearchComplete && !searchComplete) {
				game.scanUtils.scrollFirstCharacterRow()
				firstSearchComplete = true
			} else if (!searchComplete) {
				game.scanUtils.scrollSubsequentCharacterRow()
			}
		}

		game.printToLog("[SCAN_CHARACTERS] Scan completed with ${characterList.size} scanned.", tag)

		return characterList
	}
}