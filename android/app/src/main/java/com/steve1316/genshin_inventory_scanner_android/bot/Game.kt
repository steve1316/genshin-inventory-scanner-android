package com.steve1316.genshin_inventory_scanner_android.bot

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.steve1316.genshin_inventory_scanner_android.BuildConfig
import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.StartModule
import com.steve1316.genshin_inventory_scanner_android.bot.categories.ScanArtifacts
import com.steve1316.genshin_inventory_scanner_android.bot.categories.ScanCharacters
import com.steve1316.genshin_inventory_scanner_android.bot.categories.ScanMaterials
import com.steve1316.genshin_inventory_scanner_android.bot.categories.ScanWeapons
import com.steve1316.genshin_inventory_scanner_android.data.Artifact
import com.steve1316.genshin_inventory_scanner_android.data.CharacterData
import com.steve1316.genshin_inventory_scanner_android.data.ConfigData
import com.steve1316.genshin_inventory_scanner_android.data.Weapon
import com.steve1316.genshin_inventory_scanner_android.utils.ImageUtils
import com.steve1316.genshin_inventory_scanner_android.utils.MessageLog
import com.steve1316.genshin_inventory_scanner_android.utils.MyAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opencv.core.Point
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService as MPS

/**
 * Main driver for bot activity and navigation.
 */
class Game(private val myContext: Context) {
	private val tag: String = "${loggerTag}Game"

	private val startTime: Long = System.currentTimeMillis()

	val configData: ConfigData = ConfigData(myContext)
	val imageUtils: ImageUtils = ImageUtils(myContext, this)
	val gestureUtils: MyAccessibilityService = MyAccessibilityService.getInstance()
	val scanUtils: Scan = Scan(this)

	/**
	 * Returns a formatted string of the elapsed time since the bot started as HH:MM:SS format.
	 *
	 * Source is from https://stackoverflow.com/questions/9027317/how-to-convert-milliseconds-to-hhmmss-format/9027379
	 *
	 * @return String of HH:MM:SS format of the elapsed time.
	 */
	private fun printTime(): String {
		val elapsedMillis: Long = System.currentTimeMillis() - startTime

		return String.format(
			"%02d:%02d:%02d",
			TimeUnit.MILLISECONDS.toHours(elapsedMillis),
			TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsedMillis)),
			TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedMillis))
		)
	}

	/**
	 * Print the specified message to debug console and then saves the message to the log.
	 *
	 * @param message Message to be saved.
	 * @param tag Distinguish between messages for where they came from. Defaults to Game's tag.
	 * @param isWarning Flag to determine whether to display log message in console as debug or warning.
	 * @param isError Flag to determine whether to display log message in console as debug or error.
	 */
	fun printToLog(message: String, tag: String = this.tag, isWarning: Boolean = false, isError: Boolean = false) {
		if (!isError && isWarning) {
			Log.w(tag, message)
		} else if (isError && !isWarning) {
			Log.e(tag, message)
		} else {
			Log.d(tag, message)
		}

		// Remove the newline prefix if needed and place it where it should be.
		val newMessage = if (message.startsWith("\n")) {
			"\n" + printTime() + " " + message.removePrefix("\n")
		} else {
			printTime() + " " + message
		}

		MessageLog.messageLog.add(newMessage)

		// Send the message to the frontend.
		StartModule.sendEvent("MessageLog", newMessage)
	}

	/**
	 * Wait the specified seconds to account for ping or loading.
	 *
	 * @param seconds Number of seconds to pause execution.
	 */
	fun wait(seconds: Double) {
		runBlocking {
			delay((seconds * 1000).toLong())
		}
	}

	/**
	 * Check rotation of the Virtual Display and if it is stuck in Portrait Mode, destroy and remake it.
	 *
	 */
	private fun landscapeCheck() {
		if (MPS.displayHeight > MPS.displayWidth) {
			Log.d(tag, "Virtual display is not correct. Recreating it now...")
			MPS.forceGenerateVirtualDisplay(myContext)
		} else {
			Log.d(tag, "Skipping recreation of Virtual Display as it is correct.")
		}
	}

	/**
	 * Finds and presses on the image asset.
	 *
	 * @param imageName Name of the button image file in the /assets/images/ folder.
	 * @param tries Number of tries to find the specified button. Defaults to 0 which will use ImageUtil's default.
	 * @param suppressError Whether or not to suppress saving error messages to the log in failing to find the button.
	 * @return True if the button was found and clicked. False otherwise.
	 */
	fun findAndPress(imageName: String, tries: Int = 5, suppressError: Boolean = false): Boolean {
		if (configData.debugMode) {
			printToLog("[DEBUG] Now attempting to find and click the \"$imageName\" image asset.")
		}

		val tempLocation: Point? = imageUtils.findImage(imageName, tries = tries, suppressError = suppressError)

		return if (tempLocation != null) {
			if (configData.enableDelayTap) {
				val newDelay: Double = ((configData.delayTapMilliseconds - 100)..(configData.delayTapMilliseconds + 100)).random().toDouble() / 1000
				if (configData.debugMode) printToLog("[DEBUG] Adding an additional delay of ${newDelay}s...")
				wait(newDelay)
			}

			gestureUtils.tap(tempLocation.x, tempLocation.y, imageName)
		} else {
			false
		}
	}

	/**
	 * Construct the weapon Json object array.
	 *
	 * @param weapons List of Weapon objects
	 * @return Array of Json objects.
	 */
	private fun constructWeaponArray(weapons: ArrayList<Weapon>): JsonArray {
		val result = JsonArray()

		weapons.forEach {
			val obj = JsonObject()
			obj.apply {
				addProperty("key", it.key)
				addProperty("level", it.level)
				addProperty("ascension", it.ascension)
				addProperty("refinement", it.refinement)
				addProperty("location", it.location)
				addProperty("lock", it.lock)
			}

			result.add(obj)
		}

		return result
	}

	/**
	 * Construct the artifact Json object array.
	 *
	 * @param artifacts List of Artifact objects
	 * @return Array of Json objects.
	 */
	private fun constructArtifactArray(artifacts: ArrayList<Artifact>): JsonArray {
		val result = JsonArray()

		artifacts.forEach {
			val obj = JsonObject()
			obj.apply {
				addProperty("setKey", it.setKey)
				addProperty("slotKey", it.slotKey)
				addProperty("level", it.level)
				addProperty("rarity", it.rarity)
				addProperty("mainStatKey", it.mainStatKey)
				addProperty("location", it.location)
				addProperty("lock", it.lock)
				add("substats", constructArtifactSubstatsArray(it.substats))
			}

			result.add(obj)
		}

		return result
	}

	/**
	 * Construct the artifact substat Json object array.
	 *
	 * @param substats List of Substat objects
	 * @return Array of Json objects.
	 */
	private fun constructArtifactSubstatsArray(substats: ArrayList<Artifact.Companion.Substat>): JsonArray {
		val result = JsonArray()

		substats.forEach {
			val obj = JsonObject()
			obj.apply {
				addProperty("key", it.key)
				addProperty("value", it.value.toDouble())
			}

			result.add(obj)
		}

		return result
	}

	/**
	 * Construct the material Json object
	 *
	 * @param materials Map of each material along with their amounts.
	 * @return Json object with each material paired with their amounts.
	 */
	private fun constructMaterialObject(materials: MutableMap<String, Int>): JsonObject {
		val result = JsonObject()

		materials.keys.forEach {
			result.addProperty(it, materials[it])
		}

		return result
	}

	/**
	 * Construct the character Json object array.
	 *
	 * @param characters List of Character objects.
	 * @return Array of Json objects.
	 */
	private fun constructCharacterArray(characters: ArrayList<CharacterData>): JsonArray {
		val result = JsonArray()

		characters.forEach {
			val obj = JsonObject()
			obj.apply {
				addProperty("key", it.key)
				addProperty("level", it.level)
				addProperty("constellation", it.constellation)
				addProperty("ascension", it.ascension)
				add("talent", JsonObject().apply {
					addProperty("auto", it.talent.auto)
					addProperty("skill", it.talent.skill)
					addProperty("burst", it.talent.burst)
				})
			}

			result.add(obj)
		}

		return result
	}

	/**
	 * Clean up the folder of the saved GOOD files if the amount of files inside is greater than the specified amount.
	 *
	 */
	private fun cleanFolder() {
		// Generate a path to the folder in the Internal Storage.
		val directory = File(myContext.getExternalFilesDir(null)?.absolutePath + "/good/")
		if (!directory.exists()) {
			directory.mkdirs()
		}

		// Delete all logs if the amount inside is greater than 50.
		val files = directory.listFiles()
		if (files != null && files.size > 50) {
			files.forEach { file ->
				file.delete()
			}
		}
	}

	/**
	 * Collect all of the scanned information into a JSON file in GOOD format.
	 *
	 * @param weapons List of scanned weapons.
	 * @param artifacts List of scanned artifacts.
	 * @param materials List of scanned materials/character development items.
	 * @param characters List of scanned characters.
	 */
	private fun compileIntoGOOD(weapons: ArrayList<Weapon>, artifacts: ArrayList<Artifact>, materials: MutableMap<String, Int>, characters: ArrayList<CharacterData>) {
		cleanFolder()

		printToLog("\n[INFO] Saving data into a JSON file in GOOD format now...")

		// Generate a path to the folder in the Internal Storage.
		val path = File(myContext.getExternalFilesDir(null)?.absolutePath + "/good/")
		if (!path.exists()) {
			path.mkdirs()
		}

		// Generate the file name.
		val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val current = LocalDateTime.now()
			val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss")
			"GOOD @ ${current.format(formatter)}"
		} else {
			val current = SimpleDateFormat("HH_mm_ss", Locale.getDefault()).format(Date())
			val sdf = SimpleDateFormat("yyyy-MM-dd HH_mm_ss", Locale.getDefault())
			"GOOD @ ${current.format(sdf)}"
		}

		// Construct the JSON object and collect all the data into it.
		val jsonObject = JsonObject().apply {
			addProperty("format", "GOOD")
			addProperty("version", "2")
			addProperty("source", "Genshin Inventory Scanner Android v${BuildConfig.VERSION_NAME}")
			add("characters", constructCharacterArray(characters))
			add("artifacts", constructArtifactArray(artifacts))
			add("weapons", constructWeaponArray(weapons))
			add("materials", constructMaterialObject(materials))
		}

		// Set up pretty printing via GSON.
		val gson = GsonBuilder().setPrettyPrinting().create()
		val prettyJSONString = gson.toJson(jsonObject)

		// Now save the file.
		val file = File(path, "$fileName.json")
		if (!file.exists()) {
			file.createNewFile()
			file.printWriter().use { out ->
				out.write(prettyJSONString)
			}

			printToLog("[INFO] Data saved into $path/$fileName.json\n")
		}
	}

	/**
	 * Bot will begin automation here.
	 *
	 */
	fun start() {
		val startTime: Long = System.currentTimeMillis()

		landscapeCheck()

		printToLog("[INFO] Device: ${MPS.displayHeight}x${MPS.displayWidth}")
		if (!configData.enableTestSingleSearch && !configData.testScrollRows && !configData.testScrollCharacterRows) {
			printToLog("[INFO] The following scans will be conducted:")
			printToLog(
				"[INFO] Weapons: ${configData.enableScanWeapons}, Artifacts: ${configData.enableScanArtifacts}, Materials: ${configData.enableScanMaterials}, " +
						"Character Development Items: ${configData.enableScanCharacterDevelopmentItems}, Characters: ${configData.enableScanCharacters}"
			)
		}

		wait(2.5)

		var weapons: ArrayList<Weapon> = arrayListOf()
		var artifacts: ArrayList<Artifact> = arrayListOf()
		var materials: MutableMap<String, Int> = mutableMapOf()
		var characters: ArrayList<CharacterData> = arrayListOf()

		// Begin scanning logic based on current settings.

		val scanWeapons = ScanWeapons(this)
		val scanArtifacts = ScanArtifacts(this)
		val scanMaterials = ScanMaterials(this)
		val scanCharacters = ScanCharacters(this)

		if (configData.testScrollRows) {
			scanUtils.scrollFirstRow()
			wait(0.5)
			var tries = 1
			while (tries <= 300) {
				scanUtils.scrollSubsequentRow()
				wait(0.5)
				val region = intArrayOf(0, MPS.displayHeight - (MPS.displayHeight / 3), MPS.displayWidth, MPS.displayHeight / 3)
				val locations = imageUtils.findAll("artifact_level_5", region = region, customConfidence = 0.95)
				printToLog("Locations found: $locations", tag, isWarning = true)
				scanUtils.scrollRecovery(locations[0].y)
				tries += 1
			}

			return
		} else if (configData.testScrollCharacterRows) {
			var tries = 1
			while (tries <= 300) {
				scanUtils.scrollSubsequentCharacterRow()
				wait(0.5)
				tries += 1
			}

			return
		}

		if ((configData.enableScanWeapons && !configData.enableTestSingleSearch) || (configData.enableTestSingleSearch && configData.testSearchWeapon)) {
			weapons = scanWeapons.start()
		}

		if ((configData.enableScanArtifacts && !configData.enableTestSingleSearch) || (configData.enableTestSingleSearch && configData.testSearchArtifact)) {
			artifacts = scanArtifacts.start()
		}

		if ((configData.enableScanMaterials && !configData.enableTestSingleSearch) || (configData.enableTestSingleSearch && configData.testSearchMaterial)) {
			materials = scanMaterials.start()
		}

		if ((configData.enableScanCharacterDevelopmentItems && !configData.enableTestSingleSearch)) {
			materials += scanMaterials.start(searchCharacterDevelopmentItems = true)
		}

		if ((configData.enableScanCharacters && !configData.enableTestSingleSearch) || (configData.enableTestSingleSearch && configData.testSearchCharacter)) {
			characters = scanCharacters.start()
		}

		// Compile all of the data acquired into a file in GOOD format.
		if (!configData.enableTestSingleSearch) compileIntoGOOD(weapons, artifacts, materials, characters)

		val endTime: Long = System.currentTimeMillis()
		val runTime: Long = endTime - startTime
		printToLog("Total Runtime: ${runTime}ms")
	}
}