package com.steve1316.genshin_inventory_scanner_android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag

class ConfigData(myContext: Context) {
	private val tag = "${loggerTag}ConfigData"

	val debugMode: Boolean

	// Android
	val enableDelayTap: Boolean
	val delayTapMilliseconds: Int
	val confidence: Double
	val confidenceAll: Double
	val customScale: Double

	// Weapon Scan
	val enableScanWeapons: Boolean
	val scan5StarWeapons: Boolean
	val scan4StarWeapons: Boolean
	val scan3StarWeapons: Boolean

	// Artifact Scan
	val enableScanArtifacts: Boolean
	val scan5StarArtifacts: Boolean
	val scan4StarArtifacts: Boolean
	val scan3StarArtifacts: Boolean

	init {
		Log.d(tag, "Loading settings from SharedPreferences to memory...")

		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)

		debugMode = sharedPreferences.getBoolean("debugMode", false)

		// Weapon Scan settings.
		enableScanWeapons = sharedPreferences.getBoolean("enableScanWeapons", false)
		scan5StarWeapons = sharedPreferences.getBoolean("scan5StarWeapons", false)
		scan4StarWeapons = sharedPreferences.getBoolean("scan4StarWeapons", false)
		scan3StarWeapons = sharedPreferences.getBoolean("scan3StarWeapons", false)

		// Artifact Scan settings.
		enableScanArtifacts = sharedPreferences.getBoolean("enableScanArtifacts", false)
		scan5StarArtifacts = sharedPreferences.getBoolean("scan5StarArtifacts", false)
		scan4StarArtifacts = sharedPreferences.getBoolean("scan4StarArtifacts", false)
		scan3StarArtifacts = sharedPreferences.getBoolean("scan3StarArtifacts", false)

		// Android-specific settings.
		enableDelayTap = sharedPreferences.getBoolean("enableDelayTap", false)
		delayTapMilliseconds = sharedPreferences.getInt("delayTapMilliseconds", 1000)
		confidence = sharedPreferences.getFloat("confidence", 0.8f).toDouble() / 100.0
		confidenceAll = sharedPreferences.getFloat("confidenceAll", 0.8f).toDouble() / 100.0
		customScale = sharedPreferences.getFloat("customScale", 1.0f).toDouble()

		Log.d(tag, "Successfully loaded settings from SharedPreferences to memory.")
	}
}