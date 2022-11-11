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

	// Material Scan
	val enableScanMaterials: Boolean
	val enableScanCharacterDevelopmentItems: Boolean

	// Character Scan
	val enableScanCharacters: Boolean
	val travelerName: String

	// Misc
	val enableTestSingleSearch: Boolean
	val testSearchWeapon: Boolean
	val testSearchArtifact: Boolean
	val testSearchMaterial: Boolean
	val testSearchCharacter: Boolean
	val testScrollRows: Boolean
	val testScrollCharacterRows: Boolean

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

		// Material Scan settings.
		enableScanMaterials = sharedPreferences.getBoolean("enableScanMaterials", false)
		enableScanCharacterDevelopmentItems = sharedPreferences.getBoolean("enableScanCharacterDevelopmentItems", false)

		// Character Scan settings.
		enableScanCharacters = sharedPreferences.getBoolean("enableScanCharacters", false)
		travelerName = sharedPreferences.getString("travelerName", "").toString()

		// Android-specific settings.
		enableDelayTap = sharedPreferences.getBoolean("enableDelayTap", false)
		delayTapMilliseconds = sharedPreferences.getInt("delayTapMilliseconds", 1000)
		confidence = sharedPreferences.getFloat("confidence", 0.8f).toDouble() / 100.0
		confidenceAll = sharedPreferences.getFloat("confidenceAll", 0.8f).toDouble() / 100.0
		customScale = sharedPreferences.getFloat("customScale", 1.0f).toDouble()

		// Misc
		enableTestSingleSearch = sharedPreferences.getBoolean("enableTestSingleSearch", false)
		testSearchWeapon = sharedPreferences.getBoolean("testSearchWeapon", false)
		testSearchArtifact = sharedPreferences.getBoolean("testSearchArtifact", false)
		testSearchMaterial = sharedPreferences.getBoolean("testSearchMaterial", false)
		testSearchCharacter = sharedPreferences.getBoolean("testSearchCharacter", false)
		testScrollRows = sharedPreferences.getBoolean("testScrollRows", false)
		testScrollCharacterRows = sharedPreferences.getBoolean("testScrollCharacterRows", false)

		Log.d(tag, "Successfully loaded settings from SharedPreferences to memory.")
	}
}