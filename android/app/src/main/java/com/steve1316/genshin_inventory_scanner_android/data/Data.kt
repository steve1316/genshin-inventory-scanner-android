package com.steve1316.genshin_inventory_scanner_android.data

class Data {
	companion object {
		val artifactSets: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
		val characterDevelopmentItems: ArrayList<String> = arrayListOf()
		val materials: ArrayList<String> = arrayListOf()
		val weapons: MutableMap<String, String> = mutableMapOf()
		val characters: ArrayList<String> = arrayListOf()
	}
}