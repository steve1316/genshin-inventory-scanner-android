package com.steve1316.genshin_inventory_scanner_android.bot

class Scan(private val game: Game) {
	fun getName(): String {
		return game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, customThreshold = 170.0)
	}

	fun getLevel(): String {
		return game.imageUtils.findTextTesseract((game.backpackLocation.x + 1535).toInt(), (game.backpackLocation.y + 480).toInt(), 105, 30, customThreshold = 170.0)
	}

	fun getRefinementLevel(): String {
		return game.imageUtils.findTextTesseract((game.backpackLocation.x + 1490).toInt(), (game.backpackLocation.y + 530).toInt(), 35, 35, customThreshold = 170.0)
	}
}