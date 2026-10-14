package com.stasdoto.medieval.realm;

/** Goods of the 11th century economy. {@code price} is the usual price in silver pennies (денарии / куны / дирхемы). */
public enum Res {
	FOOD("хлеб и снедь", 1),
	WOOD("лес и дрова", 1),
	STONE("камень", 2),
	IRON("железо", 6),
	TOOLS("орудия", 10),
	WEAPONS("оружие", 25),
	ARMOR("доспехи", 60),
	CLOTH("сукно и полотно", 5),
	HIDES("кожи", 4),
	FURS("меха", 15),
	SALT("соль", 4),
	HONEY("мёд и воск", 6),
	HORSES("кони", 80);

	public final String title;
	public final int price;

	Res(String title, int price) {
		this.title = title;
		this.price = price;
	}
}
