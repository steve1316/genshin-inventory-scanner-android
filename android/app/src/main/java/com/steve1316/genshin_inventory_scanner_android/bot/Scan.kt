package com.steve1316.genshin_inventory_scanner_android.bot

import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.data.Artifact
import com.steve1316.genshin_inventory_scanner_android.data.Data
import org.opencv.core.Point
import java.util.*
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService as MPS

class Scan(private val game: Game) {
	private val tag: String = "${loggerTag}Game"
	private val debugMode = game.configData.debugMode

	private var failAttempts = 5
	private var maxAscensionLevel = 6
	private var scrollDiff = 0L
	private var scrollDuration = 0L

	/**
	 * Resets the scroll level of the current category back to the top.
	 *
	 * @param pressReorder Defaults to true if scroll can be reset by pressing on the Reorder button.
	 */
	fun resetScrollScreen(pressReorder: Boolean = true) {
		if (pressReorder) {
			game.printToLog("\n[SCAN] Now resetting the scroll level for this category by pressing the Reorder button...", tag)
			game.findAndPress("reorder")
			game.findAndPress("reorder")
			game.wait(1.0)
			game.printToLog("[SCAN] Finished resetting scroll level.", tag)
		} else {
			game.printToLog("\n[SCAN] Now resetting the scroll level for this category...", tag)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(1.0)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(1.0)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(1.0)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(1.0)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.printToLog("[SCAN] Finished resetting scroll level.", tag)
		}
	}

	/**
	 * Reset flags to their default values. Used when transitioning to a new search.
	 *
	 */
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
		game.gestureUtils.swipe(900f, 800f, 900f, 695f - scrollDiff, duration = 200L + scrollDuration)
		game.wait(1.0)

		// Increment the difference of the scrolling and the duration that it takes. Scroll momentum will require a reset after 2 scrolls to make sure that scroll motion stays consistent.
		scrollDiff += 8L
		scrollDuration += 10L
		if (scrollDiff % 16L == 0L) {
			scrollDiff = 0L
			scrollDuration = 0L
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
						game.printToLog("[SCAN] Exceeded 5 fail attempts. Setting search ascension level to: $weaponAscensionLevel", tag, isWarning = true)
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
	 * Resets the internal weapon ascension level for searches. Happens when a rarity changes during a search.
	 *
	 */
	fun resetWeaponAscensionLevel() {
		maxAscensionLevel = 6
		failAttempts = 5
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

	fun getArtifactName(): String {
		var tries = 3
		var thresholdDiff = 0.0
		while (tries > 0) {
			val artifactName = game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, customThreshold = 195.0 - thresholdDiff)
			val formattedName = toPascalCase(artifactName)

			Data.artifactSets.forEach { artifactSet ->
				artifactSet.value.forEach { artifactType ->
					if (artifactType.value == formattedName) {
						return formattedName
					}
				}
			}

			thresholdDiff += 5.0

			tries -= 1
		}
		val artifactName = game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, customThreshold = 195.0)
		return toPascalCase(artifactName)
	}

	fun getArtifactSetNameAndType(artifactName: String): Pair<String, String> {
		Data.artifactSets.forEach { artifactSet ->
			artifactSet.value.forEach { artifactType ->
				if (artifactType.value == artifactName) {
					return Pair(artifactSet.key, artifactType.key)
				}
			}
		}

		game.printToLog("[SCAN] Failed to find the corresponding set to the artifact: $artifactName", tag, isError = true)
		return Pair("", "")
	}

	fun getArtifactLevel(): String {
		return game.imageUtils.findTextTesseract((game.backpackLocation.x + 1490).toInt(), (game.backpackLocation.y + 475).toInt(), 66, 33, customThreshold = 170.0, reuseSourceBitmap = true)
	}

	fun getArtifactMainStat(artifactType: String, artifactLevel: Int): Pair<String, String> {
		return when (artifactType) {
			"flower" -> {
				Pair("hp", Artifact.hpStats[artifactLevel].toString())
			}
			"plume" -> {
				Pair("atk", Artifact.atkStats[artifactLevel].toString())
			}
			"sands" -> {
				if (game.imageUtils.findImage("artifact_stat_hp_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("hp_", Artifact.hp_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_atk_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("atk_", Artifact.atk_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_def_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("def_", Artifact.def_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_eleMas", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("eleMas", Artifact.eleMasStats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_enerRech_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("enerRech_", Artifact.enerRech_Stats[artifactLevel].toString())
				} else {
					game.printToLog("[SCAN] Failed to detect the main stat for this Sands artifact.", tag, isError = true)
					Pair("", "")
				}
			}
			"goblet" -> {
				if (game.imageUtils.findImage("artifact_stat_hp_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("hp_", Artifact.hp_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_atk_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("atk_", Artifact.atk_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_def_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("def_", Artifact.def_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_eleMas", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("eleMas", Artifact.eleMasStats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_anemo_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("anemo_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_geo_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("geo_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_electro_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("electro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_hydro_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("hydro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_pyro_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("pyro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_cryo_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("cryo_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_dendro_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("dendro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_physical_dmg_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("physical_dmg_", Artifact.physical_dmg_Stats[artifactLevel].toString())
				} else {
					game.printToLog("[SCAN] Failed to detect the main stat for this Goblet artifact.", tag, isError = true)
					Pair("", "")
				}
			}
			"circlet" -> {
				if (game.imageUtils.findImage("artifact_stat_hp_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("hp_", Artifact.hp_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_atk_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("atk_", Artifact.atk_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_def_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("def_", Artifact.def_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_eleMas", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("eleMas", Artifact.eleMasStats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_critRate_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("critRate_", Artifact.critRate_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_critDMG_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("critDMG_", Artifact.critDMG_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_heal_", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
					Pair("heal_", Artifact.heal_Stats[artifactLevel].toString())
				} else {
					game.printToLog("[SCAN] Failed to detect the main stat for this Circlet artifact.", tag, isError = true)
					Pair("", "")
				}
			}
			else -> {
				game.printToLog("[SCAN] Invalid artifact type was passed in. Skipping main stat detection...", tag, isError = true)
				Pair("", "")
			}
		}
	}

	fun getArtifactSubStats(): ArrayList<Artifact.Companion.Substat> {
		val substats: ArrayList<Artifact.Companion.Substat> = arrayListOf()

		val substatLocations = game.imageUtils.findAll("artifact_substat", region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight))
		substatLocations.forEach {
			val substat = game.imageUtils.findTextTesseract((it.x + 25).toInt(), (it.y - 20).toInt(), 390, 45, customThreshold = 190.0, reuseSourceBitmap = true)

			val formattedSubstat = substat.split("+") as ArrayList<String>
			formattedSubstat[0] = formattedSubstat[0].uppercase(Locale.ROOT)

			if (formattedSubstat[0] == "ATK" || formattedSubstat[0] == "HP" || formattedSubstat[0] == "DEF") {
				if (formattedSubstat[1].contains("%")) formattedSubstat[0] = formattedSubstat[0].lowercase(Locale.ROOT) + "_"
				else formattedSubstat[0] = formattedSubstat[0].lowercase(Locale.ROOT)
			} else if (formattedSubstat[0] == "ELEMENTAL MASTERY") {
				formattedSubstat[0] = "eleMas"
			} else if (formattedSubstat[0] == "ENERGY RECHARGE") {
				formattedSubstat[0] = "enerRech" + "_"
			} else if (formattedSubstat[0] == "CRIT RATE") {
				formattedSubstat[0] = "critRate" + "_"
			} else if (formattedSubstat[0] == "CRIT DMG") {
				formattedSubstat[0] = "critDMG" + "_"
			} else {
				formattedSubstat[0] = formattedSubstat[0].lowercase(Locale.ROOT)
			}

			// Cover edge cases here.
			if (formattedSubstat[1] == "1") {
				game.printToLog("[SCAN] Detected value of \"1\". Changing it to \"11\".", tag, isWarning = true)
				formattedSubstat[1] = "11"
			}

			substats.add(Artifact.Companion.Substat(key = formattedSubstat[0], value = formattedSubstat[1]))
		}

		return substats
	}

	fun getMaterialName(): String {
		var tries = 3
		var thresholdDiff = 0.0
		while (tries > 0) {
			val materialName = game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, customThreshold = 180.0 - thresholdDiff)
			val formattedName = toPascalCase(materialName)

			Data.materials.forEach { material ->
				if (material == formattedName) {
					return formattedName
				}
			}

			Data.characterDevelopmentItems.forEach { characterDevelopmentItem ->
				if (characterDevelopmentItem == formattedName) {
					return formattedName
				}
			}

			thresholdDiff += 5.0

			tries -= 1
		}

		game.printToLog("[SCAN] Failed to match material name to any in the database. Forcing the result through now...", tag, isError = true)
		val itemName = game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, reuseSourceBitmap = true)
		return toPascalCase(itemName)
	}

	fun getMaterialAmountFirstTime(location: Point): Int {
		val offset = Point(-75.0, 70.0)
		val regionWidth = 140
		val regionHeight = 35

		val result = game.imageUtils.findTextTesseract((location.x + offset.x).toInt(), (location.y + offset.y).toInt(), regionWidth, regionHeight, customThreshold = 145.0, reuseSourceBitmap = true)
		return try {
			result.toInt()
		} catch (e: Exception) {
			game.printToLog("[SCAN] Failed to convert the material amount of $result to an integer. Returning 0 for now...", tag, isError = true)
			0
		}
	}

	fun getMaterialAmountSubsequent(): Int {
		val location = game.imageUtils.findMaterialLocation()

		if (location == null) {
			game.printToLog("Failed to find cropped and resized image of material. Returning 0 for now...", tag, isError = true)
			return 0
		}

		val result = game.imageUtils.findTextTesseract((location.x - 65).toInt(), (location.y + 80).toInt(), 130, 25, customThreshold = 145.0, reuseSourceBitmap = true)
		return try {
			result.toInt()
		} catch (e: Exception) {
			game.printToLog("[SCAN] Failed to convert the material amount of $result to an integer. Returning 0 for now...", tag, isWarning = true)
			0
		}
	}
}