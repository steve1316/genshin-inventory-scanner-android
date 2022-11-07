package com.steve1316.genshin_inventory_scanner_android.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
import com.steve1316.genshin_inventory_scanner_android.data.Data
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class JSONParser {
	/**
	 * Initialize settings from the JSON file.
	 *
	 * @param myContext The application context.
	 */
	fun initializeSettings(myContext: Context) {
		Log.d(loggerTag, "Loading settings from JSON file...")

		// Grab the JSON object from the file.
		val jString = File(myContext.getExternalFilesDir(null), "settings.json").bufferedReader().use { it.readText() }
		val jObj = JSONObject(jString)

		//////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////

		val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(myContext)

		try {
			val weaponsObj = jObj.getJSONObject("weapons")
			sharedPreferences.edit {
				putBoolean("enableScanWeapons", weaponsObj.getBoolean("enableScanWeapons"))
				putBoolean("scan5StarWeapons", weaponsObj.getBoolean("scan5StarWeapons"))
				putBoolean("scan4StarWeapons", weaponsObj.getBoolean("scan4StarWeapons"))
				putBoolean("scan3StarWeapons", weaponsObj.getBoolean("scan3StarWeapons"))
				commit()
			}
		} catch (e: Exception) {
		}

		try {
			val artifactsObj = jObj.getJSONObject("artifacts")
			sharedPreferences.edit {
				putBoolean("enableScanArtifacts", artifactsObj.getBoolean("enableScanArtifacts"))
				putBoolean("scan5StarArtifacts", artifactsObj.getBoolean("scan5StarArtifacts"))
				putBoolean("scan4StarArtifacts", artifactsObj.getBoolean("scan4StarArtifacts"))
				putBoolean("scan3StarArtifacts", artifactsObj.getBoolean("scan3StarArtifacts"))
				commit()
			}
		} catch (e: Exception) {
		}

		try {
			val materialsObj = jObj.getJSONObject("materials")
			sharedPreferences.edit {
				putBoolean("enableScanMaterials", materialsObj.getBoolean("enableScanMaterials"))
				putBoolean("enableScanCharacterDevelopmentItems", materialsObj.getBoolean("enableScanCharacterDevelopmentItems"))
				commit()
			}
		} catch (e: Exception) {
		}

		try {
			val charactersObj = jObj.getJSONObject("characters")
			sharedPreferences.edit {
				putBoolean("enableScanCharacters", charactersObj.getBoolean("enableScanCharacters"))
				putString("travelerName", charactersObj.getString("travelerName"))
				commit()
			}
		} catch (e: Exception) {
		}

		try {
			val miscObj = jObj.getJSONObject("misc")
			sharedPreferences.edit {
				putBoolean("debugMode", miscObj.getBoolean("debugMode"))
				putBoolean("enableTestSingleSearch", miscObj.getBoolean("enableTestSingleSearch"))
				putBoolean("testSearchWeapon", miscObj.getBoolean("testSearchWeapon"))
				putBoolean("testSearchArtifact", miscObj.getBoolean("testSearchArtifact"))
				putBoolean("testSearchMaterial", miscObj.getBoolean("testSearchMaterial"))
				putBoolean("testSearchCharacter", miscObj.getBoolean("testSearchCharacter"))
				commit()
			}
		} catch (e: Exception) {
		}

		// Now load in the data from all of the JSON files.
		loadData(myContext)
	}

	/**
	 * Convert JSONArray to ArrayList object.
	 *
	 * @param jsonArray The JSONArray object to be converted.
	 * @return The converted ArrayList object.
	 */
	private fun toArrayList(jsonArray: JSONArray): ArrayList<String> {
		val newArrayList: ArrayList<String> = arrayListOf()

		var i = 0
		while (i < jsonArray.length()) {
			newArrayList.add(jsonArray.get(i) as String)
			i++
		}

		return newArrayList
	}

	/**
	 * Load in the data from the JSON files in the /assets/data/ folder.
	 *
	 * @param myContext The application context.
	 */
	private fun loadData(myContext: Context) {
		// Load data for artifact sets.
		val artifactString = myContext.assets?.open("data/artifacts.json")?.bufferedReader().use { it?.readText() } ?: throw Exception("Could not load map data from the artifacts.json file.")
		val artifactJSONArray = JSONArray(artifactString)
		Data.artifactSets.clear()
		for (i in 0 until artifactJSONArray.length()) {
			val artifactSet = artifactJSONArray.getJSONObject(i)
			Data.artifactSets[artifactSet["name"] as String] = mutableMapOf()

			Data.artifactSets[artifactSet["name"] as String]?.put("flower", artifactSet.getString("flower"))
			Data.artifactSets[artifactSet["name"] as String]?.put("plume", artifactSet.getString("plume"))
			Data.artifactSets[artifactSet["name"] as String]?.put("sands", artifactSet.getString("sands"))
			Data.artifactSets[artifactSet["name"] as String]?.put("goblet", artifactSet.getString("goblet"))
			Data.artifactSets[artifactSet["name"] as String]?.put("circlet", artifactSet.getString("circlet"))
		}

		Log.d(loggerTag, "Loaded in data for ${Data.artifactSets.size} artifact sets.")

		// Load data for character development items.
		val characterDevelopmentItemsString = myContext.assets?.open("data/characterdevelopmentitems.json")?.bufferedReader().use { it?.readText() }
			?: throw Exception("Could not load map data from the characterdevelopmentitems.json file.")
		val characterDevelopmentItemsJSONArray = JSONArray(characterDevelopmentItemsString)
		Data.characterDevelopmentItems.clear()
		for (i in 0 until characterDevelopmentItemsJSONArray.length()) {
			val characterDevelopmentItem = characterDevelopmentItemsJSONArray.getJSONObject(i)
			Data.characterDevelopmentItems.add(characterDevelopmentItem["name"] as String)
		}

		Log.d(loggerTag, "Loaded in data for ${Data.characterDevelopmentItems.size} Character Development Items.")

		// Load data for materials.
		val materialString = myContext.assets?.open("data/materials.json")?.bufferedReader().use { it?.readText() } ?: throw Exception("Could not load map data from the materials.json file.")
		val materialJSONArray = JSONArray(materialString)
		Data.materials.clear()
		for (i in 0 until materialJSONArray.length()) {
			val material = materialJSONArray.getJSONObject(i)
			Data.materials.add(material["name"] as String)
		}

		Log.d(loggerTag, "Loaded in data for ${Data.materials.size} materials.")

		// Load data for weapons.
		val weaponString = myContext.assets?.open("data/weapons.json")?.bufferedReader().use { it?.readText() } ?: throw Exception("Could not load map data from the weapons.json file.")
		val weaponJSONArray = JSONArray(weaponString)
		Data.weapons.clear()
		for (i in 0 until weaponJSONArray.length()) {
			val weapon = weaponJSONArray.getJSONObject(i)
			Data.weapons[weapon["name"] as String] = weapon["rarity"] as String
		}

		Log.d(loggerTag, "Loaded in data for ${Data.weapons.size} weapons.")

		// Load data for characters.
		val characterString = myContext.assets?.open("data/characters.json")?.bufferedReader().use { it?.readText() } ?: throw Exception("Could not load map data from the characters.json file.")
		val characterJSONArray = JSONArray(characterString)
		Data.characters.clear()
		for (i in 0 until characterJSONArray.length()) {
			val character = characterJSONArray.getJSONObject(i)
			Data.characters.add(character["name"] as String)
		}

		Log.d(loggerTag, "Loaded in data for ${Data.characters.size} characters.")
	}
}