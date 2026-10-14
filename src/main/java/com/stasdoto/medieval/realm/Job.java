package com.stasdoto.medieval.realm;

/** What the working people of a settlement are sent to do. */
public enum Job {
	FARMERS("пахари"),
	WOODCUTTERS("лесорубы"),
	QUARRYMEN("каменотёсы"),
	MINERS("рудокопы"),
	FISHERS("рыбаки"),
	HUNTERS("охотники и бортники"),
	WEAVERS("ткачи"),
	SMITHS("кузнецы"),
	HERDERS("скотоводы"),
	MERCHANTS("купцы"),
	BUILDERS("строители"),
	WARRIORS("воины");

	public final String title;

	Job(String title) {
		this.title = title;
	}
}
