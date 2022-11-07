package com.steve1316.genshin_inventory_scanner_android.data

import kotlin.properties.Delegates

// Details from https://frzyc.github.io/genshin-optimizer/#/doc

class GOOD {
	lateinit var format: String
	var version by Delegates.notNull<Double>()
	lateinit var source: String
	lateinit var characters: ArrayList<CharacterData>
	lateinit var artifacts: ArrayList<Artifact>
	lateinit var weapons: ArrayList<Weapon>
	lateinit var materials: Map<String, Int>
}