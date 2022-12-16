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
	private val tag: String = "${loggerTag}Scan"
	private val debugMode = game.configData.debugMode

	var backpackLocation: Point? = null

	private var failAttempts = 5
	private var maxAscensionLevel = 6

	private var scrollAttempts = 1
	private var scrollDiff = 0f
	private var characterScrollAttempts = 1
	private var characterScrollDiff = 0f

	private val stringSimilarityService = StringSimilarityServiceImpl(JaroWinklerStrategy())
	private val decimalFormat = DecimalFormat("#.###")
	private val textSimilarityConfidence = 0.85

	private val regionRightSide = intArrayOf(MPS.displayWidth / 2, 0, MPS.displayWidth / 2, MPS.displayHeight)
	private val regionRightSideOneThird = intArrayOf(MPS.displayWidth - (MPS.displayWidth / 3), 0, MPS.displayWidth / 3, MPS.displayHeight)

	fun setBackpackLocation() {
		if (backpackLocation == null) {
			backpackLocation = game.imageUtils.findImage("backpack", tries = 1)!!
			backpackLocation!!.x -= (2400 - MPS.displayWidth)
		}
	}

	/**
	 * Resets the scroll level of the current category back to the top.
	 *
	 * @param pressReorder Defaults to true if scroll can be reset by pressing on the Reorder button.
	 */
	fun resetScrollScreen(pressReorder: Boolean = true) {
		if (pressReorder) {
			game.printToLog("\n[SCROLL] Now resetting the scroll level for this category by pressing the Reorder button...", tag)
			game.findAndPress("reorder")
			game.findAndPress("reorder")
			game.wait(1.0)
			game.printToLog("[SCAN] Finished resetting scroll level via reordering.\n", tag)
		} else {
			game.printToLog("\n[SCROLL] Now resetting the scroll level for this category...", tag)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(0.5)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(0.5)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(0.5)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.wait(0.5)
			game.gestureUtils.swipe(900f, 200f, 900f, 900f, duration = 200L)
			game.printToLog("[SCAN] Finished resetting scroll level via manual scrolling.\n", tag)
		}
	}

	/**
	 * Reset flags to their default values. Used when transitioning to a new search.
	 *
	 */
	fun reset() {
		game.printToLog("[SCAN] Resetting scroll and various flags back to default...\n", tag)
		failAttempts = 5
		maxAscensionLevel = 6

		scrollAttempts = 1
		scrollDiff = 0f
		characterScrollAttempts = 1
		characterScrollDiff = 0f
	}

	/**
	 * Since the very next row is halfway visible, scroll down only halfway. This needs to run before scrollSubsequentRow().
	 *
	 */
	fun scrollFirstRow() {
		game.printToLog("[SCROLL] Scrolling the first row down...", tag)
		reset()
		game.gestureUtils.swipe(900f, 800f, 900f, 800f - 90f, duration = 1000L)
		game.gestureUtils.tap(900.0, 800.0, "artifact_level_5")
	}

	/**
	 * Scrolls one row down for every subsequent row after the first call of scrollFirstRow().
	 *
	 */
	fun scrollSubsequentRow() {
		game.printToLog("[SCROLL] Scrolling subsequent row down...", tag)

		game.gestureUtils.swipe(900f, 800f, 900f, 800f - 250f - scrollDiff)
		game.gestureUtils.tap(900.0, 800.0, "artifact_level_5")

		if (scrollAttempts % 6 == 0) {
			// Alternate between even and odd increment levels by 10f or 0f to counteract the increasing acceleration caused by the scrolling.
			if (debugMode) game.printToLog("[DEBUG] Scrolling diff increased by 10f", tag)
			scrollDiff += 10f

			if (scrollDiff % 2f == 0f) {
				if (debugMode) game.printToLog("[DEBUG] Scrolling diff reset back to zero.", tag)
				scrollDiff = 0f
			}
		}

		scrollAttempts += 1
	}

	/**
	 * Attempt to counteract scroll acceleration by preemptively covering the difference between the current position and the maximum allowed.
	 *
	 * @param y The current position's y-coordinate.
	 */
	fun scrollRecovery(y: Double) {
		val maximumAllowed = 30f

		if (y.toFloat() <= 860f - maximumAllowed) {
			val recoveryAmount = 800f + (860f - (y - maximumAllowed).toFloat())
			game.printToLog("[SCROLL_RECOVERY] Resetting scroll level by ${recoveryAmount}px - 860.0px = ${recoveryAmount - 860f}px", tag)
			game.gestureUtils.swipe(900f, 860f, 900f, recoveryAmount)
			game.gestureUtils.tap(900.0, 800.0, "artifact_level_5")
		}
	}

	/**
	 * Scrolls one row down for the Character grid. Preempting this with a special first time scroll logic is not needed and will mess up the scroll acceleration later.
	 *
	 */
	fun scrollSubsequentCharacterRow() {
		game.printToLog("\n[SCROLL] Scrolling subsequent row down...", tag)

		game.gestureUtils.swipe(200f, 900f, 200f, 900f - 240f - scrollDiff)
		game.gestureUtils.tap(200.0, 900.0, "artifact_level_5")

		if (scrollAttempts % 6 == 0) {
			// Alternate between even and odd increment levels by 10f or 0f to counteract the increasing acceleration caused by the scrolling.
			if (debugMode) game.printToLog("[DEBUG] Scrolling diff increased by 10f", tag)
			scrollDiff += 10f

			if (scrollDiff % 2f == 0f) {
				if (debugMode) game.printToLog("[DEBUG] Scrolling diff reset back to zero.", tag)
				scrollDiff = 0f
			}
		}

		scrollAttempts += 1
	}

	/**
	 * Converts a string to Pascal Case.
	 *
	 * @param str Unformatted string.
	 * @return String in Pascal Case.
	 */
	private fun toPascalCase(str: String): String {
		val cleanedString = str.replace("'", "").replace("’", "").replace("-", " ").replace("(", "")
			.replace(")", "").replace(":", "").replace("\n", " ")
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
			val weaponName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 90).toInt(), 560, 65, customThreshold = 180.0 - thresholdDiff)
			if (debugMode) game.printToLog("[DEBUG] Scanned the weapon name: $weaponName", tag)

			val formattedWeaponName = toPascalCase(weaponName)

			var correctWeaponName = "empty"
			Data.weapons.forEach { weapon ->
				val score = decimalFormat.format(stringSimilarityService.score(weapon.key, formattedWeaponName)).toDouble()
				if (score >= textSimilarityConfidence) {
					resultWeaponName = formattedWeaponName
					correctWeaponName = formattedWeaponName
				}
			}

			if (formattedWeaponName == correctWeaponName) break

			thresholdDiff += 5.0
			tries -= 1
		}

		// Determine rarity.
		var weaponRarity = ""
		if (!Data.weapons[resultWeaponName].isNullOrBlank()) weaponRarity = Data.weapons[resultWeaponName] as String

		if (debugMode) game.printToLog("[DEBUG] Weapon is: $resultWeaponName, Rarity is: $weaponRarity", tag)
		return Pair(resultWeaponName, weaponRarity)
	}

	/**
	 * Detects the weapon's level and ascension.
	 *
	 * @return Weapon Level and Ascension.
	 */
	fun getWeaponLevelAndAscension(): Pair<Int, Int> {
		// First determine ascension level. Weapons are usually sorted from highest level to lowest.
		val weaponLevel: String
		val weaponAscensionLevel: String
		var i = maxAscensionLevel
		var failedOnce = false
		while (i >= 0) {
			if (game.imageUtils.findImage("weapon_ascension_$i", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
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

				return try {
					Pair(weaponLevel.toInt(), weaponAscensionLevel.toInt())
				} catch (e: Exception) {
					game.printToLog("[ERROR] Failed to convert weapon level of $weaponLevel to integer. Returning level 1 as default.", tag, isError = true)
					Pair(1, weaponAscensionLevel.toInt())
				}
			} else {
				failedOnce = true
			}

			i -= 1
		}

		game.printToLog("[ERROR] Could not determine weapon level through iteration of ascension levels. Returning level 1 as default.", tag, isError = true)
		failAttempts = 5
		return Pair(1, 0)
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
	fun getRefinementLevel(): Int {
		val result = game.imageUtils.findTextTesseract(
			(backpackLocation!!.x + 1490).toInt(), (backpackLocation!!.y + 530).toInt(), 35, 35, customThreshold = 180.0, reuseSourceBitmap = true, detectDigitsOnly = true
		)

		if (debugMode) game.printToLog("[DEBUG] Detected refinement level of $result.", tag)

		return try {
			val newResult = result.toInt()
			if (newResult < 0) {
				0
			} else if (newResult > 5) {
				5
			} else {
				newResult
			}
		} catch (e: Exception) {
			game.printToLog("[ERROR] Could not convert $result to integer. Returning ascension level of 0 as default.", tag, isError = true)
			0
		}
	}

	/**
	 * Detects whether or not the item is equipped and by who.
	 *
	 * @return The Character's name that the item is equipped to or an empty string if none.
	 */
	fun getEquippedBy(): String {
		return if (game.imageUtils.findImage("equipped", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
			val result = game.imageUtils.findTextTesseract(
				(backpackLocation!!.x + 1705).toInt(), (backpackLocation!!.y + 815).toInt(), 360, 40, customThreshold = 170.0,
				reuseSourceBitmap = true
			)
			toPascalCase(result.replace("|", "").trim())

			if (Data.characters.contains(result)) result else ""
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
		return game.imageUtils.findImage("lock", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null
	}

	/**
	 * Detects the name of the artifact, either matching it from the data or manually brute forcing it through.
	 *
	 * @return Name of the artifact.
	 */
	fun getArtifactName(): String {
		var tries = 3
		var thresholdDiff = 0.0
		while (tries > 0) {
			val artifactName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 90).toInt(), 560, 70, customThreshold = 180.0 - thresholdDiff)
				.replace("\n", "")
			if (debugMode) game.printToLog("[DEBUG] Scanned the artifact name: $artifactName", tag)

			val formattedName = toPascalCase(artifactName)

			Data.artifactSets.forEach { artifactSet ->
				artifactSet.value.forEach { artifactType ->
					// Cover for specific edge cases where artifact name spans overflows and wraps to two lines.
					if (artifactType.value == "InRemembranceOfViridescentFields") {
						val edgeScore = decimalFormat.format(stringSimilarityService.score("InRemembranceOfViridescentFields", formattedName)).toDouble()
						if (edgeScore >= 0.9) {
							return "InRemembranceOfViridescentFields"
						}
					}

					val score = decimalFormat.format(stringSimilarityService.score(artifactType.value, formattedName)).toDouble()
					if (score >= textSimilarityConfidence) {
						return formattedName
					}
				}
			}

			thresholdDiff += 10.0

			tries -= 1
		}

		game.printToLog("[WARNING] Failed to match artifact name to any in the database. Forcing the result through now...", tag, isWarning = true)
		val artifactName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 97).toInt(), 550, 55, customThreshold = 150.0)
		return toPascalCase(artifactName)
	}

	/**
	 * Fetches the artifact's set name and type depending on the passed in artifact name.
	 *
	 * @param artifactName Name of the artifact itself.
	 * @return Either a Pair object containing the set name and the type of the artifact or empty strings if the fetch failed.
	 */
	fun getArtifactSetNameAndType(artifactName: String): Pair<String, String> {
		Data.artifactSets.forEach { artifactSet ->
			artifactSet.value.forEach { artifactType ->
				if (artifactType.value == artifactName) {
					return Pair(artifactSet.key, artifactType.key)
				}
			}
		}

		game.printToLog("[ERROR] Failed to find the corresponding set to the artifact: $artifactName", tag, isError = true)
		return Pair("", "")
	}

	/**
	 * Detects the level of the artifact.
	 *
	 * @return Level of the artifact.
	 */
	fun getArtifactLevel(): Int {
		val result = game.imageUtils.findTextTesseract(
			(backpackLocation!!.x + 1490).toInt(),
			(backpackLocation!!.y + 475).toInt(),
			66,
			33,
			customThreshold = 180.0,
			reuseSourceBitmap = true,
			detectDigitsOnly = true
		)

		if (debugMode) game.printToLog("[DEBUG] Detected level of artifact is: $result.", tag)

		return try {
			val newResult = result.toInt()

			if (newResult > 20) {
				// Remove the first character in the case that the OCR reads the plus sign as a digit, like 4.
				result.substring(1).toInt()
			} else {
				newResult
			}
		} catch (e: Exception) {
			game.printToLog("[ERROR] Could not convert $result to integer. Returning level of 0 as default.", tag, isError = true)
			0
		}
	}

	/**
	 * Fetches the main stat of the artifact based on its type and level.
	 *
	 * @param artifactType The artifact's type: flower, plume, etc.
	 * @param artifactLevel The artifact's level.
	 * @return Either a Pair object containing what the main stat is along with its value or empty strings if the fetch failed.
	 */
	fun getArtifactMainStat(artifactType: String, artifactLevel: Int): Pair<String, String> {
		return when (artifactType) {
			"flower" -> {
				Pair("hp", Artifact.hpStats[artifactLevel].toString())
			}
			"plume" -> {
				Pair("atk", Artifact.atkStats[artifactLevel].toString())
			}
			"sands" -> {
				if (game.imageUtils.findImage("artifact_stat_hp_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("hp_", Artifact.hp_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_atk_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("atk_", Artifact.atk_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_def_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("def_", Artifact.def_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_eleMas", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("eleMas", Artifact.eleMasStats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_enerRech_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("enerRech_", Artifact.enerRech_Stats[artifactLevel].toString())
				} else {
					game.printToLog("[ERROR] Failed to detect the main stat for this Sands artifact.", tag, isError = true)
					Pair("", "")
				}
			}
			"goblet" -> {
				if (game.imageUtils.findImage("artifact_stat_hp_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("hp_", Artifact.hp_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_atk_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("atk_", Artifact.atk_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_def_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("def_", Artifact.def_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_eleMas", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("eleMas", Artifact.eleMasStats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_anemo_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("anemo_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_geo_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("geo_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_electro_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("electro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_hydro_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("hydro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_pyro_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("pyro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_cryo_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("cryo_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_dendro_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("dendro_dmg_", Artifact.elemental_dmg_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_physical_dmg_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("physical_dmg_", Artifact.physical_dmg_Stats[artifactLevel].toString())
				} else {
					game.printToLog("[ERROR] Failed to detect the main stat for this Goblet artifact.", tag, isError = true)
					Pair("", "")
				}
			}
			"circlet" -> {
				if (game.imageUtils.findImage("artifact_stat_hp_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("hp_", Artifact.hp_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_atk_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("atk_", Artifact.atk_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_def_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("def_", Artifact.def_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_eleMas", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("eleMas", Artifact.eleMasStats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_critRate_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("critRate_", Artifact.critRate_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_critDMG_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("critDMG_", Artifact.critDMG_Stats[artifactLevel].toString())
				} else if (game.imageUtils.findImage("artifact_stat_heal_", tries = 1, suppressError = !game.configData.debugMode, region = regionRightSide) != null) {
					Pair("heal_", Artifact.heal_Stats[artifactLevel].toString())
				} else {
					game.printToLog("[ERROR] Failed to detect the main stat for this Circlet artifact.", tag, isError = true)
					Pair("", "")
				}
			}
			else -> {
				game.printToLog("[ERROR] Invalid artifact type was passed in. Skipping main stat detection...", tag, isError = true)
				Pair("", "")
			}
		}
	}

	/**
	 * Detects the substat(s) of the artifact.
	 *
	 * @return List of what each substat is and its detected value represented as a Substat object.
	 */
	fun getArtifactSubStats(): ArrayList<Artifact.Companion.Substat> {
		val substats: ArrayList<Artifact.Companion.Substat> = arrayListOf()

		val substatLocations = game.imageUtils.findAll("artifact_substat", region = regionRightSide)
		val filteredSubstatLocations: ArrayList<Point> = substatLocations.filter { it.x in (substatLocations[0].x - 2)..(substatLocations[0].x + 2) } as ArrayList<Point>
		game.printToLog("[SCAN] Found ${substatLocations.size} substat locations: $substatLocations.", tag, isWarning = true)
		filteredSubstatLocations.forEach { location ->
			val substat = game.imageUtils.findTextTesseract((location.x + 25).toInt(), (location.y - 20).toInt(), 390, 45, customThreshold = 190.0, reuseSourceBitmap = true)
			if (substat != "" || substat.contains('+')) {
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
					game.printToLog("[WARNING] Detected value of \"1\". Changing it to \"11\".", tag, isWarning = true)
					formattedSubstat[1] = "11"
				}

				// Remove all letters that got mixed into the substat value.
				formattedSubstat[1] = formattedSubstat[1].filter { if (it != '.') it.isDigit() else true }

				substats.add(Artifact.Companion.Substat(key = formattedSubstat[0], value = formattedSubstat[1]))
			}
		}

		return substats
	}

	/**
	 * Detects the name of the material / character development item depending on what is on the screen.
	 *
	 * @return The detected name of the material / character development item or whatever the brute force method produced.
	 */
	fun getMaterialName(): String {
		var tries = 3
		var thresholdDiff = 0.0
		while (tries > 0) {
			val materialName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 97).toInt(), 550, 55, customThreshold = 180.0 - thresholdDiff)
			if (debugMode) game.printToLog("[DEBUG] Scanned the material name: $materialName", tag)

			val formattedName = toPascalCase(materialName)

			Data.materials.forEach { material ->
				val score = decimalFormat.format(stringSimilarityService.score(material, formattedName)).toDouble()
				if (score >= textSimilarityConfidence) {
					return formattedName
				}
			}

			Data.characterDevelopmentItems.forEach { characterDevelopmentItem ->
				val score = decimalFormat.format(stringSimilarityService.score(characterDevelopmentItem, formattedName)).toDouble()
				if (score >= textSimilarityConfidence) {
					return formattedName
				}
			}

			thresholdDiff += 5.0

			tries -= 1
		}

		game.printToLog("[WARNING] Failed to match material name to any in the database. Forcing the result through now...", tag, isWarning = true)
		val itemName = game.imageUtils.findTextTesseract((backpackLocation!!.x + 1480).toInt(), (backpackLocation!!.y + 97).toInt(), 550, 55, customThreshold = 150.0)
		return toPascalCase(itemName)
	}

	/**
	 * Detects the amount of the material / character development item using a location that is already offset beforehand. This is only usable on the first round of scans.
	 *
	 * @param location This is the location of the backpack image asset offset by the coordinates of the item location.
	 * @return The material / character development item amount as a Int or 1 if the detection / conversion to Int failed.
	 */
	fun getMaterialAmountFirstTime(location: Point): Int {
		val offset = Point(-75.0, 70.0)
		val regionWidth = 140
		val regionHeight = 35

		// Offset the given location for a second time in order to create a cropped region in order to read only the item amount.
		val result = game.imageUtils.findTextTesseract(
			(location.x + offset.x).toInt(),
			(location.y + offset.y).toInt(),
			regionWidth,
			regionHeight,
			customThreshold = 150.0,
			reuseSourceBitmap = true,
			detectDigitsOnly = true
		)
		return try {
			result.toInt()
		} catch (e: Exception) {
			game.printToLog("[ERROR] Failed to convert the material amount of $result to an integer. Returning 1 for now...", tag, isError = true)
			1
		}
	}

	/**
	 * Detects the amount of the material / character development item. This is used for every subsequent round of scans after getMaterialAmountFirstTime().
	 *
	 * @return The material / character development item amount as a Int or 1 if the detection / conversion to Int failed.
	 */
	fun getMaterialAmountSubsequent(): Int {
		val location = game.imageUtils.findMaterialLocation()

		if (location == null) {
			game.printToLog("[ERROR] Failed to find cropped and resized image of material. Returning 1 for now...", tag, isError = true)
			return 1
		}

		val result = game.imageUtils.findTextTesseract((location.x - 65).toInt(), (location.y + 80).toInt(), 130, 25, customThreshold = 150.0, reuseSourceBitmap = true, detectDigitsOnly = true)
		return try {
			result.toInt()
		} catch (e: Exception) {
			game.printToLog("[ERROR] Failed to convert the material amount of $result to an integer. Returning 1 for now...", tag, isError = true)
			1
		}
	}

	/**
	 * Detects the name of the Character.
	 *
	 * @param exitLocation The location of the exit image asset to be used for offset.
	 * @return The detected name of the Character based off of the data or an empty string if it failed to match in the data.
	 */
	fun getCharacterName(exitLocation: Point): String {
		var tries = 5
		var thresholdDiff = 0.0
		while (tries > 0) {
			val name = game.imageUtils.findTextTesseract((exitLocation.x - 475).toInt(), (exitLocation.y + 100).toInt(), 420, 55, customThreshold = 150.0 - thresholdDiff)
			if (debugMode) game.printToLog("[DEBUG] Scanned the character name: $name", tag)

			val formattedCharacterName = toPascalCase(name)

			if (toPascalCase(game.configData.travelerName) == formattedCharacterName) {
				return formattedCharacterName
			} else {
				Data.characters.forEach {
					val score = decimalFormat.format(stringSimilarityService.score(it, formattedCharacterName)).toDouble()

					// Handle edge cases here like those involving the characters "q" and "g".
					val scoreXingqiu = decimalFormat.format(stringSimilarityService.score("Xinggiu", formattedCharacterName)).toDouble()
					val scoreKeqing = decimalFormat.format(stringSimilarityService.score("Keging", formattedCharacterName)).toDouble()

					if (score >= 0.95) {
						return it
					} else if (scoreXingqiu >= 0.95) {
						game.printToLog("[SCAN] Encountered edge case for the character 'Xingqiu' so manually returning this character.", tag)
						return "Xingqiu"
					} else if (scoreKeqing >= 0.95) {
						game.printToLog("[SCAN] Encountered edge case for the character 'Keqing' so manually returning this character.", tag)
						return "Keqing"
					}
				}
			}

			thresholdDiff += 10.0
			tries -= 1
			game.wait(0.5)
		}

		game.printToLog("[ERROR] Failed to match Character name with the ones in the database. Returning an empty string...", tag, isError = true)

		return ""
	}

	/**
	 * Detects the level of the Character.
	 *
	 * @param exitLocation The location of the exit image asset to be used for offset.
	 * @return The level of the Character as a Int or 1 if the detection / conversion to Int failed.
	 */
	fun getCharacterLevel(exitLocation: Point): Int {
		var level = game.imageUtils.findTextTesseract((exitLocation.x - 375).toInt(), (exitLocation.y + 200).toInt(), 65, 40, customThreshold = 160.0, detectDigitsOnly = true)
		level = level.filter { if (it != '.') it.isDigit() else true }
		if (debugMode) game.printToLog("[DEBUG] Scanned the following text for the Character's level: $level", tag)
		return try {
			level.toInt()
		} catch (e: Exception) {
			game.printToLog("[ERROR] Failed to convert the Character level of $level to an integer. Returning 1 for now...", tag, isError = true)
			1
		}
	}

	/**
	 * Detects the Character's ascension level.
	 *
	 * @return Ascension level of the Character as a Int.
	 */
	fun getCharacterAscensionLevel(): Int {
		val ascensionProgressButton = game.imageUtils.findImage("character_ascension_progress", tries = 10)
		val result = if (ascensionProgressButton != null) {
			game.gestureUtils.tap(ascensionProgressButton.x, ascensionProgressButton.y, "character_ascension_progress")
			val scannedText = game.imageUtils.findTextTesseract((ascensionProgressButton.x - 810.0).toInt(), (ascensionProgressButton.y + 40.0).toInt(), 50, 35, customThreshold = 200.0,
				detectDigitsOnly = false)
			if (debugMode) game.printToLog("[DEBUG] Scanned the following text for the Character's ascension level: $scannedText", tag)
			try {
				when (scannedText.toInt()) {
					20 -> 0
					40 -> 1
					50 -> 2
					60 -> 3
					70 -> 4
					80 -> 5
					else -> 6
				}
			} catch (e: Exception) {
				game.printToLog("[SCAN] Could not parse ascension level from scanned text on the Ascension Progression screen so Character is most likely max ascended.", tag)
				6
			}
		} else {
			game.printToLog("[WARNING] Could not view the ascension progress for this Character. Returning 6 as the default ascension level.", tag, isWarning = true)
			6
		}

		game.findAndPress("exit_inventory")
		return result
	}

	/**
	 * Detects the Character's constellation level.
	 *
	 * @return Constellation level of the Character as a Int.
	 */
	fun getCharacterConstellationLevel(): Int {
		if (!game.findAndPress("character_constellation", tries = 2) && game.imageUtils.findImage("character_constellation_selected", tries = 2) == null) {
			game.printToLog("[ERROR] Failed to go to the Constellations page. Returning 0 for now...", tag, isError = true)
			return 0
		}

		game.wait(1.0)

		return 6 - game.imageUtils.findAll("character_constellation_locked", region = regionRightSideOneThird).size
	}

	/**
	 * Detects the Character's talent levels taking the given name and constellation level into consideration.
	 *
	 * @param characterName The Character's name that will be used for checking against edge cases to be handled separately.
	 * @param characterConstellationLevel The Character's constellation level to be considered in determining the correct talent skill.
	 * @return List of all 3 talent levels for the Character.
	 */
	fun getCharacterTalentLevels(characterName: String, characterConstellationLevel: Int = 0): ArrayList<Int> {
		if (!game.findAndPress("character_talents", tries = 2) && game.imageUtils.findImage("character_talents_selected", tries = 2) == null) {
			game.printToLog("[ERROR] Failed to go to the Talents page. Returning 1's for now...", tag, isError = true)
			return arrayListOf(1, 1, 1)
		}

		val hasConstellation3 = (characterConstellationLevel == 3)
		val hasConstellation5 = (characterConstellationLevel >= 5)

		game.wait(1.0)

		// Cover edge cases where certain characters have a special dash that is positioned in between the skill and the burst.
		val characterExceptions: ArrayList<String> = arrayListOf("KamisatoAyaka", "Mona")

		val result = arrayListOf<Int>()
		var index = 1

		var levelLocations = game.imageUtils.findAll("character_talents_level", region = regionRightSideOneThird, customConfidence = 0.7)

		// This makes sure that at least 3 talent levels will be processed: auto, skill and burst.
		if (levelLocations.size < 3) {
			var levelTries = 5
			var confidenceDiff = 0.5
			while (levelTries > 0) {
				levelLocations = game.imageUtils.findAll("character_talents_level", region = regionRightSideOneThird, customConfidence = 0.7 - confidenceDiff)

				if (levelLocations.size >= 3) {
					break
				}

				confidenceDiff += 0.1
				levelTries -= 1
			}
		}

		levelLocations.forEach { location ->
			// Process only the auto, skill and burst based on the current index and whether or not the Character matches one of the edge cases.
			if (index == 1 || index == 2 || (index == 3 && !characterExceptions.contains(characterName)) || (index == 4 && characterExceptions.contains(characterName))) {
				var resultLevel = 0
				var tries = 3
				var thresholdDiff = 0.0

				while (tries > 0) {
					var level = game.imageUtils.findTextTesseract((location.x + 25).toInt(), (location.y - 15).toInt(), 40, 35, customThreshold = 180.0 - thresholdDiff, detectDigitsOnly = true)
					level = level.filter { if (it != '.') it.isDigit() else true }

					// Cover edge cases after scanning here.
					if (level == "0") {
						game.printToLog("[WARNING] Detected talent level of \"0\". Changing it to \"10\".", tag, isWarning = true)
						level = "10"
					}

					// Handle exceptional cases where the talent level would be invalid or not based on the constellation level.
					resultLevel = try {
						if (level.toInt() > 13) {
							game.printToLog("[WARNING] Detected talent level of $level is greater than 13. Changing it to 10...", tag, isWarning = true)
							10
						} else level.toInt()
					} catch (e: Exception) {
						// For talents boosted by constellation, the number 11 would be assumed if scan fails.
						if (tries == 1 && (hasConstellation3 || hasConstellation5)) {
							game.printToLog(
								"[WARNING] Failed to scan for Talent $index. However, talent has been boosted by constellation so changing it to \"11\" by default...", tag, isWarning =
								true
							)
							11
						} else {
							game.printToLog("[WARNING] Failed to convert Talent $index level of $level to an integer. Trying again for $tries more tries...", tag, isWarning = true)
							tries -= 1
							0
						}
					}

					// Either a talent level not 0 or a talent level forcibly set to 1 if the process failed to process a valid level will be returned.
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