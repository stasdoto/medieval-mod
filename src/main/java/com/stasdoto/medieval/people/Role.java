package com.stasdoto.medieval.people;

/** What a person of the settlement does. */
public enum Role {
	PEASANT("пахарь", "пахарка"),
	CRAFTSMAN("ремесленник", "мастерица"),
	SMITH("кузнец", "кузнечиха"),
	MERCHANT("купец", "торговка"),
	PRIEST("священник", "монахиня"),
	MONK("монах", "монахиня"),
	WARRIOR("воин", "воительница"),
	NOBLE("боярин", "боярыня"),
	CHILD("мальчик", "девочка"),
	HERDER("пастух", "пастушка");

	public final String male, female;

	Role(String male, String female) {
		this.male = male;
		this.female = female;
	}
}
