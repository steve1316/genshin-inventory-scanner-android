package com.steve1316.genshin_inventory_scanner_android.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.steve1316.genshin_inventory_scanner_android.MainActivity.loggerTag
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
				commit()
			}
		} catch (e: Exception) {
		}
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
}