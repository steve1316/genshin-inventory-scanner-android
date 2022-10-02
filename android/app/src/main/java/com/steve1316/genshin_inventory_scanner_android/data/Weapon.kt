package com.steve1316.genshin_inventory_scanner_android.data

import kotlin.properties.Delegates

// Details from https://frzyc.github.io/genshin-optimizer/#/doc

class Weapon {
	lateinit var key: String
	var level by Delegates.notNull<Int>()
	var ascension by Delegates.notNull<Int>()
	var refinement by Delegates.notNull<Int>()
	lateinit var location: String
	var lock by Delegates.notNull<Boolean>()
}