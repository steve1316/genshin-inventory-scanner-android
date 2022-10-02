package com.steve1316.genshin_inventory_scanner_android.data

import kotlin.properties.Delegates

// Details from https://frzyc.github.io/genshin-optimizer/#/doc

class Artifact {
	lateinit var setKey: String
	lateinit var slotKey: SlotKey
	var level by Delegates.notNull<Int>()
	var rarity by Delegates.notNull<Int>()
	lateinit var mainStatKey: StatKey
	lateinit var location: String
	var lock by Delegates.notNull<Boolean>()
	lateinit var substats: ArrayList<Substat>

	inner class Substat {
		lateinit var key: StatKey
		var value by Delegates.notNull<Double>()
	}

	enum class SlotKey {
		Flower, Plume, Sands, Goblet, Circlet
	}

	enum class StatKey {
		HP, HP_, ATK, ATK_, DEF, DEF_, ELEMAS, ENERRECH, HEAL_, CRITRATE_, CRITDMG_, PHYSICAL_DMG_, ANEMO_DMG_, GEO_DMG_, ELECTRO_DMG_, HYDRO_DMG_, PYRO_DMG_, CRYO_DMG_, DENDRO_DMG_
	}
}