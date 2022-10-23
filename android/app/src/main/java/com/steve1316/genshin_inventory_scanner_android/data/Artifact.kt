package com.steve1316.genshin_inventory_scanner_android.data

import kotlin.properties.Delegates

// Details from https://frzyc.github.io/genshin-optimizer/#/doc

class Artifact {
	lateinit var setKey: String
	lateinit var slotKey: String
	var level by Delegates.notNull<Int>()
	var rarity by Delegates.notNull<Int>()
	lateinit var mainStatKey: String
	lateinit var location: String
	var lock by Delegates.notNull<Boolean>()
	lateinit var substats: ArrayList<Substat>

	override fun toString(): String {
		return "Set: $setKey, Type: $slotKey, Level: $level, Rarity: $rarity, Main Stat: $mainStatKey, Equipped By: $location, Locked: $lock, Substats: $substats"
	}

	companion object {
		// Stats from https://genshin-impact.fandom.com/wiki/Artifact/Scaling

		data class Substat(val key: String, val value: String)

		val hpStats: MutableMap<Int, Int> = mutableMapOf(
			0 to 717,
			1 to 920,
			2 to 1123,
			3 to 1326,
			4 to 1530,
			5 to 1733,
			6 to 1936,
			7 to 2139,
			8 to 2342,
			9 to 2545,
			10 to 2749,
			11 to 2952,
			12 to 3155,
			13 to 3358,
			14 to 3561,
			15 to 3764,
			16 to 3967,
			17 to 4171,
			18 to 4374,
			19 to 4577,
			20 to 4780
		)

		val atkStats: MutableMap<Int, Int> = mutableMapOf(
			0 to 47,
			1 to 60,
			2 to 73,
			3 to 86,
			4 to 100,
			5 to 113,
			6 to 126,
			7 to 139,
			8 to 152,
			9 to 166,
			10 to 179,
			11 to 192,
			12 to 205,
			13 to 219,
			14 to 232,
			15 to 245,
			16 to 258,
			17 to 272,
			18 to 285,
			19 to 298,
			20 to 311
		)

		val hp_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 7.0,
			1 to 9.0,
			2 to 11.0,
			3 to 12.9,
			4 to 14.9,
			5 to 16.9,
			6 to 18.9,
			7 to 20.9,
			8 to 22.8,
			9 to 24.8,
			10 to 26.8,
			11 to 28.8,
			12 to 30.8,
			13 to 32.8,
			14 to 34.7,
			15 to 36.7,
			16 to 38.7,
			17 to 40.7,
			18 to 42.7,
			19 to 44.6,
			20 to 46.6
		)

		val atk_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 7.0,
			1 to 9.0,
			2 to 11.0,
			3 to 12.9,
			4 to 14.9,
			5 to 16.9,
			6 to 18.9,
			7 to 20.9,
			8 to 22.8,
			9 to 24.8,
			10 to 26.8,
			11 to 28.8,
			12 to 30.8,
			13 to 32.8,
			14 to 34.7,
			15 to 36.7,
			16 to 38.7,
			17 to 40.7,
			18 to 42.7,
			19 to 44.6,
			20 to 46.6
		)

		val def_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 8.7,
			1 to 11.2,
			2 to 13.7,
			3 to 16.2,
			4 to 18.6,
			5 to 21.1,
			6 to 23.6,
			7 to 26.1,
			8 to 28.6,
			9 to 31.0,
			10 to 33.5,
			11 to 36.0,
			12 to 38.5,
			13 to 40.9,
			14 to 43.4,
			15 to 45.9,
			16 to 48.4,
			17 to 50.8,
			18 to 53.3,
			19 to 55.8,
			20 to 58.3
		)

		val physical_dmg_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 8.7,
			1 to 11.2,
			2 to 13.7,
			3 to 16.2,
			4 to 18.6,
			5 to 21.1,
			6 to 23.6,
			7 to 26.1,
			8 to 28.6,
			9 to 31.0,
			10 to 33.5,
			11 to 36.0,
			12 to 38.5,
			13 to 40.9,
			14 to 43.4,
			15 to 45.9,
			16 to 48.4,
			17 to 50.8,
			18 to 53.3,
			19 to 55.8,
			20 to 58.3
		)

		// Shared between all of the elements.
		val elemental_dmg_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 7.0,
			1 to 9.0,
			2 to 11.0,
			3 to 12.9,
			4 to 14.9,
			5 to 16.9,
			6 to 18.9,
			7 to 20.9,
			8 to 22.8,
			9 to 24.8,
			10 to 26.8,
			11 to 28.8,
			12 to 30.8,
			13 to 32.8,
			14 to 34.7,
			15 to 36.7,
			16 to 38.7,
			17 to 40.7,
			18 to 42.7,
			19 to 44.6,
			20 to 46.6
		)

		val eleMasStats: MutableMap<Int, Double> = mutableMapOf(
			0 to 28.0,
			1 to 35.9,
			2 to 43.8,
			3 to 51.8,
			4 to 59.7,
			5 to 67.6,
			6 to 75.5,
			7 to 83.5,
			8 to 91.4,
			9 to 99.3,
			10 to 107.2,
			11 to 115.2,
			12 to 123.1,
			13 to 131.0,
			14 to 138.9,
			15 to 146.9,
			16 to 154.8,
			17 to 162.7,
			18 to 170.6,
			19 to 178.6,
			20 to 186.5
		)

		val enerRech_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 7.8,
			1 to 10.0,
			2 to 12.2,
			3 to 14.4,
			4 to 16.6,
			5 to 18.8,
			6 to 21.0,
			7 to 23.2,
			8 to 25.4,
			9 to 27.6,
			10 to 29.8,
			11 to 32.0,
			12 to 34.2,
			13 to 36.4,
			14 to 38.6,
			15 to 40.8,
			16 to 43.0,
			17 to 45.2,
			18 to 47.4,
			19 to 49.6,
			20 to 51.8
		)

		val critRate_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 4.7,
			1 to 6.0,
			2 to 7.3,
			3 to 8.6,
			4 to 9.9,
			5 to 11.3,
			6 to 12.6,
			7 to 13.9,
			8 to 15.2,
			9 to 16.6,
			10 to 17.9,
			11 to 19.2,
			12 to 20.5,
			13 to 21.8,
			14 to 23.2,
			15 to 24.5,
			16 to 25.8,
			17 to 27.1,
			18 to 28.4,
			19 to 29.8,
			20 to 31.1
		)

		val critDMG_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 9.3,
			1 to 12.0,
			2 to 14.6,
			3 to 17.3,
			4 to 19.9,
			5 to 22.5,
			6 to 25.2,
			7 to 27.8,
			8 to 30.5,
			9 to 33.1,
			10 to 35.7,
			11 to 38.4,
			12 to 41.0,
			13 to 43.7,
			14 to 46.3,
			15 to 49.0,
			16 to 51.6,
			17 to 54.2,
			18 to 56.9,
			19 to 59.6,
			20 to 62.2
		)

		val heal_Stats: MutableMap<Int, Double> = mutableMapOf(
			0 to 5.4,
			1 to 6.9,
			2 to 8.4,
			3 to 10.0,
			4 to 11.5,
			5 to 13.0,
			6 to 14.5,
			7 to 16.1,
			8 to 17.6,
			9 to 19.1,
			10 to 20.6,
			11 to 22.1,
			12 to 23.7,
			13 to 25.2,
			14 to 26.7,
			15 to 28.2,
			16 to 29.8,
			17 to 31.3,
			18 to 32.8,
			19 to 34.3,
			20 to 35.9
		)
	}
}