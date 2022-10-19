package com.steve1316.genshin_inventory_scanner_android.bot.categories

import com.steve1316.genshin_inventory_scanner_android.MainActivity
import com.steve1316.genshin_inventory_scanner_android.bot.Game
import com.steve1316.genshin_inventory_scanner_android.data.Artifact
import org.opencv.core.Point
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService as MPS

class ScanArtifacts(private val game: Game) {
	private val tag: String = "${MainActivity.loggerTag}ScanArtifacts"
	private val debugMode = game.configData.debugMode

	private var firstRun = true
	private var firstSearchCompleted = false
	private var search5StarComplete = false
	private var search4StarComplete = false
	private var search3StarComplete = false

	private var previousRow1: ArrayList<Point> = arrayListOf(Point(1.0, 1.0))
	private var previousRow2: ArrayList<Point> = arrayListOf(Point(2.0, 2.0))
	private var alternateRow: Boolean = false

	private var artifactList: ArrayList<Artifact> = arrayListOf()

	fun search(): ArrayList<Point> {
		val region = if (firstRun) {
			intArrayOf(0, 0, MPS.displayWidth - (MPS.displayWidth / 3), MPS.displayHeight)
		} else {
			intArrayOf(0, MPS.displayHeight - (MPS.displayHeight / 3), MPS.displayWidth, MPS.displayHeight / 3)
		}

		return if (game.configData.scan5StarArtifacts && !search5StarComplete) {
			game.imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.7)
		} else if (game.configData.scan4StarArtifacts && !search4StarComplete) {
			game.imageUtils.findAll("artifact_level_4", region = region, customConfidence = 0.7)
		} else if (game.configData.scan3StarArtifacts && !search3StarComplete) {
			game.imageUtils.findAll("artifact_level_3", region = region, customConfidence = 0.7)
		} else {
			arrayListOf()
		}
	}

	private fun validateSearchCheck(locations: ArrayList<Point>) {
		if (game.configData.scan5StarArtifacts && !search5StarComplete && locations.size == 0) {
			search5StarComplete = true
		} else if (game.configData.scan4StarArtifacts && !search4StarComplete && locations.size == 0) {
			search4StarComplete = true
		} else if (game.configData.scan3StarArtifacts && !search3StarComplete && locations.size == 0) {
			search3StarComplete = true
		}

		if (locations.size != 0) firstSearchCompleted = true

		if (firstSearchCompleted) {
			if (alternateRow) previousRow1 = locations
			else previousRow2 = locations
		}

		alternateRow = !alternateRow
	}

	private fun isSearchDone(): Boolean {
		return previousRow1 != previousRow2
	}

	private fun printInitialInfo(): String {
		var message = if (game.configData.scan5StarArtifacts) {
			"5* Artifacts"
		} else if (game.configData.scan4StarArtifacts) {
			"4* Artifacts"
		} else {
			"3* Artifacts"
		}

		message += if (game.configData.scan4StarArtifacts) ", 4* Artifacts" else ""
		message += if (game.configData.scan3StarArtifacts) ", 3* Artifacts" else ""

		return message
	}

	/**
	 * Starts the process to scan artifacts in the inventory.
	 *
	 */
	fun start() {
		if (game.imageUtils.findImage("category_selected_artifacts", tries = 2) == null && !game.findAndPress("category_unselected_artifacts", tries = 2)) {
			game.printToLog("[ERROR] Unable to start Artifact scan.", tag, isError = true)
			return
		}

		game.printToLog("**************************************", tag)
		game.printToLog("[SCAN_ARTIFACTS] ARTIFACT SCAN STARTING...", tag)
		game.printToLog("[SCAN_ARTIFACTS] ${printInitialInfo()}", tag)
		game.printToLog("**************************************", tag)

		// Reset the scroll view.
		game.scanUtils.resetScrollScreen()

		// Collect the locations of all artifacts whose image asset can be found.
		while (isSearchDone()) {
			val locations: ArrayList<Point> = search()
			validateSearchCheck(locations)

			if (locations.isNotEmpty()) {
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

				if (debugMode) game.printToLog("[DEBUG] ${locations.size} Artifacts: $locations", tag, isWarning = true)
				game.printToLog("[DEBUG] ${locations.size} Artifacts: $locations", tag, isWarning = true)

				// Now scan each artifact in each location.
				locations.forEach {
					// Select the artifact.
					game.gestureUtils.tap(it.x, it.y, "artifact_level_5")

					val artifactName = game.scanUtils.getArtifactName()
					val (artifactSetName, artifactType) = game.scanUtils.getArtifactSetNameAndType(artifactName)

					val artifactLevel = game.scanUtils.getArtifactLevel()
					val artifactRarity = game.scanUtils.getArtifactRarity()

					val equipped = game.scanUtils.getEquippedBy()
					val locked = game.scanUtils.getLocked()

					game.printToLog("DEBUG: $artifactName, $artifactSetName, $artifactType, $artifactLevel, $artifactRarity, $equipped, $locked", tag, isWarning = true)

					val artifactMainStat = game.scanUtils.getArtifactMainStat(artifactType, artifactLevel.toInt()).first
					val artifactSubStats: MutableMap<String, String> = game.scanUtils.getArtifactSubStats()

					val artifactObject = Artifact().apply {
						setKey = artifactSetName
						slotKey = artifactType
						level = artifactLevel.toInt()
						rarity = artifactRarity.toInt()
						mainStatKey = artifactMainStat
						location = equipped
						lock = locked
						substats = artifactSubStats
					}

					artifactList.add(artifactObject)

					game.printToLog("[SCAN_ARTIFACTS] Artifact scanned: $artifactObject\n", tag)
				}
			}

			// Different scrolling behavior based on whether this is the first run.
			if (firstRun) {
				game.scanUtils.scrollFirstRow()
				firstRun = false
			} else if (locations.size != 0 || !firstSearchCompleted) {
				game.scanUtils.scrollSubsequentRow()
			}
		}

		game.printToLog("[SCAN_ARTIFACTS] Artifact scan completed with ${artifactList.size} scanned.", tag)
	}
}