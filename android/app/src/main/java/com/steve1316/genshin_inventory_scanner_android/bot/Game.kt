package com.steve1316.genshin_inventory_scanner_android.bot

import android.content.Context
import android.util.Log
import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.StartModule
import com.steve1316.genshin_inventory_scanner_android.data.ConfigData
import com.steve1316.genshin_inventory_scanner_android.utils.ImageUtils
import com.steve1316.genshin_inventory_scanner_android.utils.MediaProjectionService
import com.steve1316.genshin_inventory_scanner_android.utils.MessageLog
import com.steve1316.genshin_inventory_scanner_android.utils.MyAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
	 * Perform an initialization check at the start.
	 *
	 * @return True if the required image asset is found on the screen.
	 */
	private fun initializationCheck(): Boolean {
		printToLog("[INIT] Performing an initialization check...")
		return (imageUtils.findImage("backpack") != null)
	}

	/**
	 * Bot will begin automation here.
	 *
	 * @return
	 */
	fun start() {
		val startTime: Long = System.currentTimeMillis()

		landscapeCheck()

		if (initializationCheck()) {
			val loc = imageUtils.findImage("backpack")!!
			printToLog(imageUtils.findTextTesseract((loc.x + 1480).toInt(), (loc.y + 97).toInt(), 550, 55, customThreshold = 170.0))
		} else {
			throw Exception("Unable to detect if the bot is at the Inventory screen.")
		}

		val endTime: Long = System.currentTimeMillis()
		val runTime: Long = endTime - startTime
		printToLog("Total Runtime: ${runTime}ms")
	}
}