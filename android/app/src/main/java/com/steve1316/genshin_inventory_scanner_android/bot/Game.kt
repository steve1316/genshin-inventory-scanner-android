package com.steve1316.genshin_inventory_scanner_android.bot

import android.content.Context
import android.os.Build
import android.util.Log
import com.steve1316.genshin_inventory_scanner_android.BuildConfig
import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.StartModule
import com.steve1316.genshin_inventory_scanner_android.bot.categories.ScanArtifacts
import com.steve1316.genshin_inventory_scanner_android.bot.categories.ScanMaterials
import com.steve1316.genshin_inventory_scanner_android.bot.categories.ScanWeapons
import com.steve1316.genshin_inventory_scanner_android.data.Artifact
import com.steve1316.genshin_inventory_scanner_android.data.ConfigData
import com.steve1316.genshin_inventory_scanner_android.data.Weapon
import com.steve1316.genshin_inventory_scanner_android.utils.ImageUtils
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService
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

	lateinit var backpackLocation: Point

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
		if (MediaProjectionService.displayHeight > MediaProjectionService.displayWidth) {
			Log.d(tag, "Virtual display is not correct. Recreating it now...")
			MediaProjectionService.forceGenerateVirtualDisplay(myContext)
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
	 * Perform an initialization check at the start.
	 *
	 * @return True if the required image asset is found on the screen.
	 */
	private fun initializationCheck(): Boolean {
		printToLog("[INIT] Performing an initialization check...")
		return (imageUtils.findImage("backpack") != null)
	}


	/**
	 * Collect all of the scanned information into a JSON file in GOOD format.
	 *
	 * @param weapons List of scanned weapons.
	 * @param artifacts List of scanned artifacts.
	 * @param materials List of scanned materials/character development items.
	 */
	private fun compileIntoGOOD(weapons: ArrayList<Weapon>, artifacts: ArrayList<Artifact>, materials: MutableMap<String, Int>) {
		printToLog("\n[INFO] Saving data into a JSON file in GOOD format now...")

		// Generate a path to the root of the application folder in the Internal Storage.
		val path = File(myContext.getExternalFilesDir(null)?.absolutePath ?: throw Exception("Could not generate a path to the folder to save the file in GOOD format."))

		// Generate the file name.
		val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val current = LocalDateTime.now()
			val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			"GOOD @ ${current.format(formatter)}"
		} else {
			val current = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
			val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
			"GOOD @ ${current.format(sdf)}"
		}

		// Collect the weapon list into the JSON file.
		val fileContent = """{
	"format": "GOOD",
	"version": 2,
	"source": "Genshin Inventory Scanner Android v${BuildConfig.VERSION_NAME}",
	"characters": [],
	"artifacts": [],
	"weapons": [],
	"materials": {}
}"""

		// Now save the file.
		val file = File(path, "$fileName.json")
		if (!file.exists()) {
			file.createNewFile()
			file.printWriter().use { out ->
				out.print(fileContent)
			}

			printToLog("\n[INFO] Data saved into $fileName.json")
		}
	}

	/**
	 * Bot will begin automation here.
	 *
	 */
	fun start() {
		val startTime: Long = System.currentTimeMillis()

		landscapeCheck()

		wait(2.0)

		if (initializationCheck()) {
			backpackLocation = imageUtils.findImage("backpack") ?: throw Exception("Bot needs to start at the Inventory screen.")

			var weapons: ArrayList<Weapon> = arrayListOf()
			var artifacts: ArrayList<Artifact> = arrayListOf()
			var materials: MutableMap<String, Int> = mutableMapOf()

			// Begin scanning logic based on current settings.

			if ((configData.enableScanWeapons && !configData.enableTestSingleSearch) || (configData.enableTestSingleSearch && configData.testSearchWeapon)) {
				val scanWeapons = ScanWeapons(this)
				weapons = scanWeapons.start()
			}

			if ((configData.enableScanArtifacts && !configData.enableTestSingleSearch) || (configData.enableTestSingleSearch && configData.testSearchArtifact)) {
				val scanArtifacts = ScanArtifacts(this)
				artifacts = scanArtifacts.start()
			}

			if (configData.enableScanMaterials || configData.enableScanCharacterDevelopmentItems || (configData.enableTestSingleSearch && configData.testSearchMaterial)) {
				val scanMaterials = ScanMaterials(this)
				materials = scanMaterials.start()
			}

			// Compile all of the data acquired into a file in GOOD format.
			compileIntoGOOD(weapons, artifacts, materials)
		} else {
			throw Exception("Unable to detect if the bot is at the Inventory screen.")
		}

		val endTime: Long = System.currentTimeMillis()
		val runTime: Long = endTime - startTime
		printToLog("Total Runtime: ${runTime}ms")
	}
}