package com.steve1316.genshin_inventory_scanner_android.data

import kotlin.properties.Delegates

// Details from https://frzyc.github.io/genshin-optimizer/#/doc

class Weapon : Cloneable {
	lateinit var key: String
	var level by Delegates.notNull<Int>()
	var ascension by Delegates.notNull<Int>()
	var refinement by Delegates.notNull<Int>()
	lateinit var location: String
	var lock by Delegates.notNull<Boolean>()

	override fun toString(): String {
		return "Name: $key, Level: $level, Ascension: $ascension, Refinement: $refinement, Equipped By: $location, Locked: $lock"
	}

	override fun equals(other: Any?): Boolean {
		val weapon: Weapon = other as Weapon
		return key == weapon.key && level == weapon.level && ascension == weapon.ascension && refinement == weapon.refinement && location == weapon.location && lock == weapon.lock
	}

	override fun hashCode(): Int {
		var result = key.hashCode()
		result = 31 * result + location.hashCode()
		return result
	}

	public override fun clone(): Any {
		val newWeapon: Weapon = super.clone() as Weapon

		newWeapon.key = key
		newWeapon.level = level
		newWeapon.ascension = ascension
		newWeapon.refinement = refinement
		newWeapon.location = location
		newWeapon.lock = lock

		return newWeapon
	}
}