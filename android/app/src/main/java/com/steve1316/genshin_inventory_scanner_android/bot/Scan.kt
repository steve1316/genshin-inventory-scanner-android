package com.steve1316.genshin_inventory_scanner_android.bot

import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.data.Artifact
import com.steve1316.genshin_inventory_scanner_android.data.Data
import net.ricecode.similarity.JaroWinklerStrategy
import net.ricecode.similarity.StringSimilarityServiceImpl
import org.opencv.core.Point
import java.text.DecimalFormat
import java.util.*
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService as MPS

class Scan(private val game: Game) {
	private val tag: String = "${loggerTag}Game"
	private val debugMode = game.configData.debugMode

	var backpackLocation: Point? = null

	private var failAttempts = 5
	private var maxAscensionLevel = 6

	private var scrollTapDelay = 0.0
	private var scrollAttempts = 0
	private var characterScrollAttempts = 0
	private var characterScrollDiff = 0f
	private var characterScrollDuration = 0L
	fun setBackpackLocation() {
		if (backpackLocation == null) {
			backpackLocation = game.imageUtils.findImage("backpack", tries = 1)!!
		}
	}

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

		game.gestureUtils.swipe(900f, 800f, 900f, 650f)

		val delay = if (0.85 + scrollTapDelay == 1.00) {
			scrollTapDelay = 0.0
			0.85
		} else {
			0.85 + scrollTapDelay
		}

		if (scrollAttempts % 10 == 0) scrollTapDelay += 0.01
		scrollAttempts += 1

		game.wait(delay)
		game.gestureUtils.tap(900.0, 800.0, "artifact_level_5")
		game.wait(0.5)
	}

	fun scrollRowSlightly() {
		game.printToLog("\n[SCAN] Scrolling subsequent row moderately up...", tag, isError = true)
		game.gestureUtils.swipe(900f, 800f, 900f, 740f)
		game.wait(0.5)
	}

	fun scrollFirstCharacterRow() {
		game.printToLog("\n[SCAN] Scrolling the character row down...", tag)
		game.gestureUtils.swipe(200f, 800f, 200f, 600f)

		scrollTapDelay = 0.0

		game.wait(0.5)
	}

	fun scrollSubsequentCharacterRow() {
		game.gestureUtils.swipe(200f, 800f, 200f, 640f - characterScrollDiff, duration = 500L - characterScrollDuration)

		if (characterScrollAttempts % 6 == 0) {
			characterScrollDiff = 0f
			characterScrollDuration = 0L
		} else if (characterScrollAttempts % 2 == 0) {
			characterScrollDiff += 10f
			characterScrollDuration += 10L
		}

		characterScrollAttempts += 1
		game.wait(0.5)
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
			val weaponName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 97).toInt(), 550, 55, customThreshold = 195.0 - thresholdDiff)
			if (debugMode) game.printToLog("[DEBUG] Scanned the weapon name: $weaponName", tag)

			val formattedWeaponName = toPascalCase(weaponName)

			var correctWeaponName = "empty"
			Data.weapons.forEach { weapon ->
				if (weapon.key == formattedWeaponName) {
					resultWeaponName = formattedWeaponName
					correctWeaponName = formattedWeaponName
				}
			}

			if (formattedWeaponName == correctWeaponName) break

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
						game.imageUtils.findTextTesseract(
							(backpackLocation!!.x + 1535).toInt(),
							(backpackLocation!!.y + 480).toInt(),
							53,
							30,
							customThreshold = 200.0,
							reuseSourceBitmap = true,
							detectDigitsOnly = true
						)
					}
					5 -> {
						game.imageUtils.findTextTesseract(
							(backpackLocation!!.x + 1535).toInt(),
							(backpackLocation!!.y + 480).toInt(),
							53,
							30,
							customThreshold = 200.0,
							reuseSourceBitmap = true,
							detectDigitsOnly = true
						)
					}
					4 -> {
						game.imageUtils.findTextTesseract(
							(backpackLocation!!.x + 1535).toInt(),
							(backpackLocation!!.y + 480).toInt(),
							53,
							30,
							customThreshold = 200.0,
							reuseSourceBitmap = true,
							detectDigitsOnly = true
						)
					}
					3 -> {
						game.imageUtils.findTextTesseract(
							(backpackLocation!!.x + 1535).toInt(),
							(backpackLocation!!.y + 480).toInt(),
							53,
							30,
							customThreshold = 110.0,
							reuseSourceBitmap = true,
							detectDigitsOnly = true
						)
					}
					2 -> {
						game.imageUtils.findTextTesseract(
							(backpackLocation!!.x + 1535).toInt(),
							(backpackLocation!!.y + 480).toInt(),
							53,
							30,
							customThreshold = 180.0,
							reuseSourceBitmap = true,
							detectDigitsOnly = true
						)
					}
					1 -> {
						game.imageUtils.findTextTesseract(
							(backpackLocation!!.x + 1535).toInt(),
							(backpackLocation!!.y + 480).toInt(),
							53,
							30,
							customThreshold = 200.0,
							reuseSourceBitmap = true,
							detectDigitsOnly = true
						)
					}
					else -> {
						game.imageUtils.findTextTesseract(
							(backpackLocation!!.x + 1535).toInt(),
							(backpackLocation!!.y + 480).toInt(),
							53,
							30,
							customThreshold = 200.0,
							reuseSourceBitmap = true,
							detectDigitsOnly = true
						)
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
		return game.imageUtils.findTextTesseract(
			(backpackLocation!!.x + 1490).toInt(),
			(backpackLocation!!.y + 530).toInt(),
			35,
			35,
			customThreshold = 170.0,
			reuseSourceBitmap = true,
			detectDigitsOnly = true
		)
	}

	/**
	 * Detects whether or not the item is equipped and by who.
	 *
	 * @return The Character's name that the item is equipped to or an empty string if none.
	 */
	fun getEquippedBy(): String {
		return if (game.imageUtils.findImage("equipped", tries = 1, region = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)) != null) {
			val result = game.imageUtils.findTextTesseract(
				(backpackLocation!!.x + 1705).toInt(), (backpackLocation!!.y + 815).toInt(), 360, 40, customThreshold = 170.0,
				reuseSourceBitmap = true
			)
			toPascalCase(result.replace("|", "").trim())
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
			val artifactName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 90).toInt(), 560, 65, customThreshold = 195.0 - thresholdDiff)
			if (debugMode) game.printToLog("[DEBUG] Scanned the artifact name: $artifactName", tag)

			val formattedName = toPascalCase(artifactName)

			Data.artifactSets.forEach { artifactSet ->
				artifactSet.value.forEach { artifactType ->
					if (artifactType.value == formattedName) {
						return formattedName
					}
				}
			}

			thresholdDiff += 10.0

			tries -= 1
		}

		game.printToLog("[SCAN] Failed to match artifact name to any in the database. Forcing the result through now...", tag, isError = true)
		val artifactName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 97).toInt(), 550, 55)
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
		return game.imageUtils.findTextTesseract(
			(backpackLocation!!.x + 1490).toInt(),
			(backpackLocation!!.y + 475).toInt(),
			66,
			33,
			customThreshold = 170.0,
			reuseSourceBitmap = true,
			detectDigitsOnly = true
		)
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
		substatLocations.forEach { location ->
			val substat = game.imageUtils.findTextTesseract((location.x + 25).toInt(), (location.y - 20).toInt(), 390, 45, customThreshold = 190.0, reuseSourceBitmap = true)

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

			// Remove all letters that got mixed into the substat value.
			formattedSubstat[1] = formattedSubstat[1].filter { if (it != '.') it.isDigit() else true }

			substats.add(Artifact.Companion.Substat(key = formattedSubstat[0], value = formattedSubstat[1]))
		}

		return substats
	}

	fun getMaterialName(): String {
		var tries = 3
		var thresholdDiff = 0.0
		while (tries > 0) {
			val materialName = game.imageUtils.findTextTesseract((game.backpackLocation.x + 1480).toInt(), (game.backpackLocation.y + 97).toInt(), 550, 55, customThreshold = 180.0 - thresholdDiff)
			if (debugMode) game.printToLog("[DEBUG] Scanned the material name: $materialName", tag)

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
		val itemName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 97).toInt(), 550, 55, reuseSourceBitmap = true)
		return toPascalCase(itemName)
	}

	fun getMaterialAmountFirstTime(location: Point): Int {
		val offset = Point(-75.0, 70.0)
		val regionWidth = 140
		val regionHeight = 35

		val result = game.imageUtils.findTextTesseract(
			(location.x + offset.x).toInt(),
			(location.y + offset.y).toInt(),
			regionWidth,
			regionHeight,
			customThreshold = 145.0,
			reuseSourceBitmap = true,
			detectDigitsOnly = true
		)
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

		val result = game.imageUtils.findTextTesseract((location.x - 65).toInt(), (location.y + 80).toInt(), 130, 25, customThreshold = 145.0, reuseSourceBitmap = true, detectDigitsOnly = true)
		return try {
			result.toInt()
		} catch (e: Exception) {
			game.printToLog("[SCAN] Failed to convert the material amount of $result to an integer. Returning 0 for now...", tag, isWarning = true)
			0
		}
	}

	fun getCharacterName(exitLocation: Point): String {
		val service = StringSimilarityServiceImpl(JaroWinklerStrategy())
		val decimalFormat = DecimalFormat("#.###")
		val textSimilarityConfidence = 0.85

		var tries = 5
		var thresholdDiff = 0.0
		while (tries > 0) {
			val name = game.imageUtils.findTextTesseract((exitLocation.x - 475).toInt(), (exitLocation.y + 100).toInt(), 420, 55, customThreshold = 195.0 - thresholdDiff)
			if (true) game.printToLog("[DEBUG] Scanned the character name: $name", tag)

			val formattedCharacterName = toPascalCase(name)

			if (toPascalCase(game.configData.travelerName) == formattedCharacterName) {
				return formattedCharacterName
			} else {
				Data.characters.forEach {
					val score = decimalFormat.format(service.score(it, formattedCharacterName)).toDouble()
					if (score >= textSimilarityConfidence) {
						return it
					}
				}
			}

			thresholdDiff += 5.0
			tries -= 1
		}

		game.printToLog("[SCAN] Failed to match Character name with the ones in the database.", tag, isError = true)

		return ""
	}

	fun getCharacterLevel(exitLocation: Point): Int {
		var level = game.imageUtils.findTextTesseract((exitLocation.x - 375).toInt(), (exitLocation.y + 200).toInt(), 65, 40, customThreshold = 170.0, detectDigitsOnly = true)
		level = level.filter { if (it != '.') it.isDigit() else true }
		return try {
			level.toInt()
		} catch (e: Exception) {
			game.printToLog("[SCAN] Failed to convert the Character level of $level to an integer. Returning 1 for now...", tag, isWarning = true)
			1
		}
	}

	fun getCharacterAscensionLevel(): Int {
		return game.imageUtils.findAll("character_ascension_star", region = intArrayOf(MPS.displayWidth - (MPS.displayWidth / 3), 0, MPS.displayWidth / 3, MPS.displayHeight), customConfidence = 0.8)
			.size
	}

	fun getCharacterConstellationLevel(): Int {
		if (!game.findAndPress("character_constellation", tries = 2) && game.imageUtils.findImage("character_constellation_selected", tries = 2) == null) {
			game.printToLog("[SCAN] Failed to go to the Constellations page. Returning 0 for now...", tag, isError = true)
			return 0
		}

		game.wait(1.0)

		return 6 - game.imageUtils.findAll("character_constellation_locked", region = intArrayOf(MPS.displayWidth - (MPS.displayWidth / 3), 0, MPS.displayWidth / 3, MPS.displayHeight)).size
	}

	fun getCharacterTalentLevels(characterName: String, characterConstellationLevel: Int = 0): ArrayList<Int> {
		if (!game.findAndPress("character_talents", tries = 2) && game.imageUtils.findImage("character_talents_selected", tries = 2) == null) {
			game.printToLog("[SCAN] Failed to go to the Talents page. Returning 1's for now...", tag, isError = true)
			return arrayListOf(1, 1, 1)
		}

		val hasConstellation3 = (characterConstellationLevel == 3)
		val hasConstellation5 = (characterConstellationLevel >= 5)

		game.wait(1.0)

		// Cover edge cases where certain characters have a special dash that is positioned in between the skill and the burst.
		val characterExceptions: ArrayList<String> = arrayListOf("KamisatoAyaka", "Mona")

		val result = arrayListOf<Int>()
		var index = 1

		var levelLocations = game.imageUtils.findAll(
			"character_talents_level", region = intArrayOf(MPS.displayWidth - (MPS.displayWidth / 3), 0, MPS.displayWidth / 3, MPS.displayHeight),
			customConfidence = 0.7
		)

		if (levelLocations.size < 3) {
			var levelTries = 5
			var confidenceDiff = 0.5
			while (levelTries > 0) {
				levelLocations = game.imageUtils.findAll(
					"character_talents_level", region = intArrayOf(MPS.displayWidth - (MPS.displayWidth / 3), 0, MPS.displayWidth / 3, MPS.displayHeight),
					customConfidence = 0.7 - confidenceDiff
				)

				if (levelLocations.size >= 3) {
					break
				}

				confidenceDiff += 0.1
				levelTries -= 1
			}
		}

		levelLocations.forEach { location ->
			if (index == 1 || index == 2 || (index == 3 && !characterExceptions.contains(characterName)) || (index == 4 && characterExceptions.contains(characterName))) {
				var resultLevel = 0
				var tries = 3
				var thresholdDiff = 0.0

				while (tries > 0) {
					var level = game.imageUtils.findTextTesseract((location.x + 25).toInt(), (location.y - 15).toInt(), 40, 35, customThreshold = 180.0 - thresholdDiff, detectDigitsOnly = true)
					level = level.filter { if (it != '.') it.isDigit() else true }

					// Cover edge cases after scanning here.
					if (level == "0") {
						game.printToLog("[SCAN] Detected value of \"0\". Changing it to \"10\".", tag, isWarning = true)
						level = "10"
					}

					resultLevel = try {
						if (level.toInt() > 13) {
							game.printToLog("[SCAN] Detected value greater than 13. Changing it to 10...", tag, isWarning = true)
							10
						}
						else level.toInt()
					} catch (e: Exception) {
						// For talents boosted by constellation, the number 11 would be assumed if scan fails.
						if (tries == 1 && (hasConstellation3 || hasConstellation5)) {
							game.printToLog("[SCAN] Failed to scan for Talent $index. However, talent has been boosted by constellation so changing it to \"11\" by default...", tag, isWarning = true)
							11
						} else {
							game.printToLog("[SCAN] Failed to convert Talent $index level of $level to an integer. Trying again for $tries more tries...", tag, isWarning = true)
							tries -= 1
							0
						}
					}

					if (resultLevel != 0) {
						break
					} else if (tries <= 0) {
						resultLevel = 1
						break
					}

					game.wait(0.10)
					thresholdDiff += 10.0
				}

				result.add(resultLevel)
			}

			index += 1
		}

		return result
	}
}