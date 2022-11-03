package com.steve1316.genshin_inventory_scanner_android.bot.categories

import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game
import com.steve1316.genshin_inventory_scanner_android.utils.BotService
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService
import org.opencv.core.Point

class ScanMaterials(private val game: Game) {
	private val tag: String = "${MainActivity.loggerTag}ScanMaterials"
	private val debugMode = game.configData.debugMode

	private var searchComplete = false

	// This grid is comprised of each row followed by the columns in the inner ArrayLists. For 1080p, the x's are spaced out by 185px and the y's are spaced out by 220px.
	// Use the backpack image asset location to offset it by these for the location of each grid element on the screen.
	private val gridOffsetRow1: ArrayList<Point> = arrayListOf(
		Point(205.0, 175.0), Point(390.0, 175.0), Point(575.0, 175.0), Point(760.0, 175.0),
		Point(945.0, 175.0), Point(1130.0, 175.0), Point(1315.0, 175.0)
	)
	private val gridOffsetRow2: ArrayList<Point> = arrayListOf(
		Point(205.0, 395.0), Point(390.0, 395.0), Point(575.0, 395.0), Point(760.0, 395.0),
		Point(945.0, 395.0), Point(1130.0, 395.0), Point(1315.0, 395.0)
	)
	private val gridOffsetRow3: ArrayList<Point> = arrayListOf(
		Point(205.0, 615.0), Point(390.0, 615.0), Point(575.0, 615.0), Point(760.0, 615.0),
		Point(945.0, 615.0), Point(1130.0, 615.0), Point(1315.0, 615.0)
	)
	private val gridOffsetRow4: ArrayList<Point> = arrayListOf(
		Point(205.0, 735.0), Point(390.0, 735.0), Point(575.0, 735.0), Point(760.0, 735.0),
		Point(945.0, 735.0), Point(1130.0, 735.0), Point(1315.0, 735.0)
	)

	private var firstSearchComplete = false

	private val testSingleSearch = game.configData.enableTestSingleSearch && game.configData.testSearchMaterial

	/**
	 * Compile the list of grid offsets for the current search.
	 *
	 * @return List of grid offsets.
	 */
	private fun search(): ArrayList<Point> {
		// If this is a subsequent scan, return only the grid offsets of the last row.
		return if (!firstSearchComplete) (gridOffsetRow1 + gridOffsetRow2 + gridOffsetRow3) as ArrayList<Point>
		else gridOffsetRow4
	}

	/**
	 * Starts the search process and process through all search queries.
	 *
	 * @return List of materials along with their amounts scanned.
	 */
	fun start(): MutableMap<String, Int> {
		if (game.configData.enableScanMaterials && game.imageUtils.findImage("category_selected_materials", tries = 2) == null &&
			!game.findAndPress("category_unselected_materials", tries = 2) ||
			(game.configData.enableScanCharacterDevelopmentItems && game.imageUtils.findImage("category_selected_characterdevelopmentitems", tries = 2) == null &&
					!game.findAndPress("category_unselected_characterdevelopmentitems", tries = 2))
		) {
			game.printToLog("[ERROR] Could not make the category active and thus could not start the Material scan..", tag, isError = true)
			return mutableMapOf()
		}

		val materialList: MutableMap<String, Int> = mutableMapOf()

		// Reset the scroll view or perform a test single search.
		if (!testSingleSearch) {
			game.printToLog("**************************************", tag)
			game.printToLog("[SCAN_MATERIALS] MATERIAL SCAN STARTING...", tag)
			if (game.configData.enableScanMaterials) game.printToLog("[SCAN_MATERIALS] Materials", tag)
			else if (game.configData.enableScanCharacterDevelopmentItems) game.printToLog("[SCAN_MATERIALS] Character Development Items", tag)
			game.printToLog("**************************************", tag)

			game.scanUtils.resetScrollScreen()
		} else {
			game.printToLog("**************************************", tag)
			game.printToLog("[SCAN_MATERIALS] TESTING SINGLE SEARCH...", tag)
			game.printToLog("[SCAN_MATERIALS] Note: Material amount is not tested here.", tag)
			game.printToLog("**************************************", tag)

			val materialName = game.scanUtils.getMaterialName()
			game.printToLog("[SCAN_MATERIALS] Material scanned: $materialName\n", tag)

			return materialList
		}

		while (!searchComplete) {
			if (!BotService.isRunning) throw InterruptedException("Stopping the bot and breaking out of the loop due to the Stop button being pressed")

			val locations: ArrayList<Point> = search()

			locations.forEach {
				if (!searchComplete) {
					// Select the item by using the backpack location and the grid offset.
					game.gestureUtils.tap(game.backpackLocation.x + it.x, game.backpackLocation.y + it.y, "item_level")

					var materialName = ""
					var amount = 0

					val region = intArrayOf(MediaProjectionService.displayWidth / 2, 0, MediaProjectionService.displayWidth / 2, MediaProjectionService.displayHeight)
					if (game.imageUtils.findImage("cooking_ingredient", tries = 1, region = region, suppressError = true) == null) {
						try {
							materialName = game.scanUtils.getMaterialName()
							if (materialList.containsKey(materialName)) {
								searchComplete = true
							} else {
								amount = if (!firstSearchComplete) game.scanUtils.getMaterialAmountFirstTime(Point(game.backpackLocation.x + it.x, game.backpackLocation.y + it.y))
								else game.scanUtils.getMaterialAmountSubsequent()

								materialList[materialName] = amount
								game.printToLog("[SCAN_MATERIALS] Material scanned: x$amount $materialName\n", tag)
							}
						} catch (e: Exception) {
							game.printToLog(
								"[ERROR] Material failed to scan: (Name: $materialName, Amount: $amount)\n", tag, isError = true
							)
						}
					} else {
						game.printToLog("[SCAN_MATERIALS] Done\n", tag, isWarning = true)
						searchComplete = true
					}
				}
			}

			if (!firstSearchComplete && !searchComplete) {
				game.scanUtils.scrollFirstRow()
				firstSearchComplete = true
			} else if (!searchComplete) {
				game.scanUtils.scrollSubsequentRow()
			}
		}

		game.printToLog("[SCAN_MATERIALS] Material scan completed with ${materialList.size} scanned.", tag)

		return materialList
	}
}