package com.steve1316.genshin_inventory_scanner_android.data

import kotlin.properties.Delegates

// Details from https://frzyc.github.io/genshin-optimizer/#/doc

class CharacterData {
	lateinit var key: String
	var level by Delegates.notNull<Int>()
	var constellation by Delegates.notNull<Int>()
	var ascension by Delegates.notNull<Int>()
	lateinit var talent: Talent

	inner class Talent {
		var auto by Delegates.notNull<Int>()
		var skill by Delegates.notNull<Int>()
		var burst by Delegates.notNull<Int>()

		override fun toString(): String {
			return "(Auto: $auto, Skill: $skill, Burst: $burst)"
		}
	}

	override fun toString(): String {
		return "Name: $key, Level: $level, Ascension Level: $ascension, Constellation Level: $constellation, Talents: $talent"
	}
}