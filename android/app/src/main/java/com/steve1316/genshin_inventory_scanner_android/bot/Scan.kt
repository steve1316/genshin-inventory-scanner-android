package com.steve1316.genshin_inventory_scanner_android.bot

import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.data.Data

class Scan(private val game: Game) {
	private val tag: String = "${loggerTag}Game"

	private var weaponName: String = ""
	private var weaponRarity: String = ""
	private var weaponLevel: String = ""
	private var weaponAscensionLevel: Int = 0

	fun scrollScreen() {
		game.gestureUtils.swipe(900f, 800f, 900f, 400f)
	}

	fun getWeaponName(): String {
		weaponName = game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, customThreshold = 170.0)

		// Determine rarity.
		if (!Data.weapons[weaponName].isNullOrBlank()) {
			weaponRarity = Data.weapons[weaponName] as String
			game.printToLog("[SCAN] Rarity is: $weaponRarity", tag)
		}

		return weaponName
	}

	fun getWeaponLevel(): String {
		// First determine ascension level. Weapons are usually sorted from highest level to lowest.
		var i = 6
		while (i >= 0) {
			if (game.imageUtils.findImage("weapon_ascension_$i", tries = 1) != null) {
				weaponAscensionLevel = i
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

				return weaponLevel
			}

			i--
		}

		game.printToLog("[SCAN] Could not determine weapon level. Returning default value...", tag, isWarning = true)
		return "1"
	}

	fun getRefinementLevel(): String {
		return game.imageUtils.findTextTesseract((game.backpackLocation.x + 1490).toInt(), (game.backpackLocation.y + 530).toInt(), 35, 35, customThreshold = 170.0, reuseSourceBitmap = true)
	}
}