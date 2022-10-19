package com.steve1316.genshin_inventory_scanner_android.bot

import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.data.Data
import java.util.*
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService as MPS

class Scan(private val game: Game) {
	private val tag: String = "${loggerTag}Game"
	private val debugMode = game.configData.debugMode

	private var failAttempts = 5
	private var maxAscensionLevel = 6
	private var scrollDiff = 0L

	/**
	 * Resets the scroll level of the current category back to the top.
	 *
	 */
	fun resetScrollScreen() {
		game.printToLog("\n[SCAN] Now resetting the scroll level for this category...", tag)
		game.findAndPress("reorder")
		game.findAndPress("reorder")
		game.wait(1.0)
		game.printToLog("[SCAN] Finished resetting scroll level.", tag)
	}

	fun reset() {
		failAttempts = 5
		maxAscensionLevel = 6
		scrollDiff = 0L
	}

	/**
	 * Since the very next row is halfway visible, scroll down only halfway. This needs to run before scrollSubsequentRow().
	 *
	 */
	fun scrollFirstRow() {
		game.printToLog("\n[SCAN] Scrolling the first row down...", tag)
		reset()
		game.gestureUtils.swipe(900f, 800f, 900f, 750f, duration = 200L)
		game.wait(1.0)
	}

	/**
	 * Scrolls one row down for every subsequent row after the first call of scrollFirstRow().
	 *
	 */
	fun scrollSubsequentRow() {
		game.printToLog("\n[SCAN] Scrolling subsequent row down...", tag)
		game.gestureUtils.swipe(900f, 800f, 900f, 695f - scrollDiff, duration = 200L)
		game.wait(1.0)

		// Increment the difference. Scroll momentum will require a reset after 4 scrolls to make sure that scroll motion stays consistent.
		scrollDiff += 8L
		if (scrollDiff % 24L == 0L) {
			scrollDiff = 0L
		}
	}

	/**
	 * Converts a string to Pascal Case.
	 *
	 * @param str Unformatted string.
	 * @return String in Pascal Case.
	 */
	private fun toPascalCase(str: String): String {
		val cleanedString = str.replace("'", "").replace("’", "").replace("-", " ").replace("(", "")
			.replace(")", "").replace(":", "")
		val split = cleanedString.split(" ")
		val result = arrayListOf<String>()
		split.forEach { word ->
			result.add(word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
		}

		return result.joinToString("")
	}

	/**
	 * Detects the weapon's name and rarity.
	 *
	 * @return Weapon Name and rarity.
	 */
	fun getWeaponNameAndRarity(): Pair<String, String> {
		var tries = 3
		var thresholdDiff = 0.0
		var resultWeaponName = ""
		while (tries > 0) {
			val weaponName = game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, customThreshold = 195.0 - thresholdDiff)
			val formattedWeaponName = toPascalCase(weaponName)

			Data.weapons.forEach { weapon ->
				if (weapon.key == formattedWeaponName) {
					resultWeaponName = formattedWeaponName
				}
			}

			thresholdDiff += 5.0

			tries -= 1
		}

		var weaponRarity = ""

		// Determine rarity.
		if (!Data.weapons[resultWeaponName].isNullOrBlank()) weaponRarity = Data.weapons[resultWeaponName] as String

		if (debugMode) game.printToLog("[DEBUG] Weapon is: $resultWeaponName, Rarity is: $weaponRarity", tag)
		return Pair(resultWeaponName, weaponRarity)
	}

	/**
	 * Detects the weapon's level and ascension.
	 *
	 * @return Weapon Level and Ascension.
	 */
	fun getWeaponLevelAndAscension(): Pair<String, String> {
		// First determine ascension level. Weapons are usually sorted from highest level to lowest.
		val weaponLevel: String
		val weaponAscensionLevel: String
		var i = maxAscensionLevel
		var failedOnce = false
		while (i >= 0) {
			if (game.imageUtils.findImage("weapon_ascension_$i", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
				weaponAscensionLevel = i.toString()
				weaponLevel = when (i) {
					6 -> {
						game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 53, 30, customThreshold = 200.0, reuseSourceBitmap = true)
					}
					5 -> {
						game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 53, 30, customThreshold = 200.0, reuseSourceBitmap = true)
					}
					4 -> {
						game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 53, 30, customThreshold = 200.0, reuseSourceBitmap = true)
					}
					3 -> {
						game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 53, 30, customThreshold = 110.0, reuseSourceBitmap = true)
					}
					2 -> {
						game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 53, 30, customThreshold = 180.0, reuseSourceBitmap = true)
					}
					1 -> {
						game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 53, 30, customThreshold = 200.0, reuseSourceBitmap = true)
					}
					else -> {
						game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 53, 30, customThreshold = 200.0, reuseSourceBitmap = true)
					}
				}

				if (debugMode) game.printToLog("[DEBUG] Weapon Level: $weaponLevel, Ascension Level: $weaponAscensionLevel", tag)

				if (failedOnce) {
					failAttempts -= 1
					if (failAttempts < 0) {
						maxAscensionLevel = i
						failAttempts = 5
					}
				} else {
					failAttempts = 5
				}

				return Pair(weaponLevel, weaponAscensionLevel)
			} else {
				failedOnce = true
			}

			i -= 1
		}

		game.printToLog("[SCAN] Could not determine weapon level. Returning default value...", tag, isWarning = true)
		failAttempts = 5
		return Pair("1", "0")
	}

	/**
	 * Detects the weapon's refinement level.
	 *
	 * @return Weapon Refinement Level
	 */
	fun getRefinementLevel(): String {
		return game.imageUtils.findTextTesseract((game.backpackLocation.x + 1490).toInt(), (game.backpackLocation.y + 530).toInt(), 35, 35, customThreshold = 170.0, reuseSourceBitmap = true)
	}

	/**
	 * Detects whether or not the item is equipped and by who.
	 *
	 * @return The Character's name that the item is equipped to or an empty string if none.
	 */
	fun getEquippedBy(): String {
		return if (game.imageUtils.findImage("equipped", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
			val result = game.imageUtils.findTextTesseract(
				(game.backpackLocation.x + 1705).toInt(), (game.backpackLocation.y + 815).toInt(), 360, 40, customThreshold = 170.0,
				reuseSourceBitmap = true
			)
			result.replace("|", "").trim()
		} else {
			""
		}
	}

	/**
	 * Detects whether or not the item is locked.
	 *
	 * @return True if the item is locked.
	 */
	fun getLocked(): Boolean {
		return game.imageUtils.findImage("lock", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null
	}
}