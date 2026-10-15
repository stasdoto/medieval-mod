package com.stasdoto.medieval.construct;

import java.util.Random;

import net.minecraft.world.item.DyeColor;

import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.settle.BKind;
import com.stasdoto.medieval.settle.Layout;
import com.stasdoto.medieval.settle.Site;

/** Chooses and draws the building for a plot, in the way of the people who built it. */
public final class Styles {
	private Styles() {
	}

	public static Bp build(Layout.Lot lot, Site site) {
		return build(lot.kind, site, lot.culture != null ? lot.culture : site.culture, lot.w, lot.d, lot.wealth, lot.seed);
	}

	public static Bp build(BKind kind, Site site, long seed) {
		return build(kind, site, site.culture, kind.w, kind.d, kind == BKind.HOUSE_RICH ? 2 : 1, seed);
	}

	static Bp build(BKind kind, Site site, Culture c, int w, int d, int wealth, long seed) {
		Random r = new Random(seed);
		Palette p = Palette.of(c, r);
		Bp b = new Bp(w, d, seed);
		try {
			draw(b, p, r, kind, site, c, w, d, wealth);
		} catch (RuntimeException e) {
			if (ERRORS.incrementAndGet() <= 20) {
				com.stasdoto.medieval.Medieval.LOGGER.error("[medieval] building {} ({}) {}x{} failed", kind, c, w, d, e);
			}
			return new Bp(w, d, seed);
		}
		return b;
	}

	private static final java.util.concurrent.atomic.AtomicInteger ERRORS = new java.util.concurrent.atomic.AtomicInteger();

	private static void draw(Bp b, Palette p, Random r, BKind kind, Site site, Culture c, int w, int d, int wealth) {
		boolean islam = c == Culture.ANDALUSI || c == Culture.ARAB || c == Culture.PERSIAN;
		switch (kind) {
			case HOMESTEAD -> Homes.village(b, p, c);
			case HOUSE -> Homes.town(b, p, c, Math.min(wealth, 1));
			case HOUSE_RICH -> Homes.town(b, p, c, 2);
			case SMITHY -> Civic.smithy(b, p, c == Culture.LATIN || c == Culture.BYZANTINE || c == Culture.CAUCASUS || islam);
			case TAVERN -> Civic.tavern(b, p, c);
			case GRANARY -> Civic.granary(b, p);
			case STALL -> Civic.stall(b, r);
			case WELL -> Civic.well(b, p);
			case CELLS -> Civic.cells(b, p);
			case YURT -> Houses.yurt(b, w / 2, d / 2, r.nextInt(4) == 0 ? DyeColor.BROWN : r.nextInt(5) == 0 ? DyeColor.LIGHT_GRAY : DyeColor.WHITE);
			case CHURCH_SMALL -> {
				switch (c) {
					case RUS, BALT_FINN -> Holy.woodenChurch(b, p);
					case NORSE -> Holy.stave(b, p);
					case BYZANTINE, CAUCASUS -> Holy.crossDomed(b, p, false, false);
					default -> Holy.romanesque(b, p, false);
				}
			}
			case CHURCH -> {
				switch (c) {
					case RUS, BALT_FINN -> {
						if (site.kind.urban() || r.nextInt(4) == 0) {
							Holy.crossDomed(b, p, false, true);
						} else {
							Holy.woodenChurch(b, p);
						}
					}
					case NORSE -> Holy.stave(b, p);
					case BYZANTINE, CAUCASUS -> Holy.crossDomed(b, p, false, false);
					default -> Holy.romanesque(b, p, false);
				}
			}
			case CATHEDRAL -> {
				switch (c) {
					case RUS, BALT_FINN -> Holy.crossDomed(b, p, true, true);
					case BYZANTINE, CAUCASUS -> Holy.crossDomed(b, p, true, false);
					default -> Holy.romanesque(b, p, true);
				}
			}
			case MOSQUE -> Holy.mosque(b, p, c);
			case HALL -> {
				switch (c) {
					case RUS, BALT_FINN, WEST_SLAV, MAGYAR -> Civic.terem(b, p);
					case NORSE, CELTIC -> Houses.longhouse(b, p, 3, 2, b.w - 4, b.d - 3);
					case ANDALUSI, ARAB, PERSIAN, STEPPE -> Civic.palace(b, p);
					default -> Civic.stoneHall(b, p);
				}
			}
			case KEEP -> Forts.keep(b, p, c, switch (c) {
				case LATIN -> r.nextInt(4) == 0 ? 1 : 0;
				case CELTIC -> 1;
				case RUS, WEST_SLAV, MAGYAR, BALT_FINN, NORSE, STEPPE -> site.kind.ordinal() >= Site.Kind.GREAT_CITY.ordinal() && r.nextBoolean() ? 1 : 2;
				default -> 3;
			});
			case SHED -> Crafts.shed(b, p, c);
			case POTTERY -> Crafts.pottery(b, p, c);
			case TANNERY -> Crafts.tannery(b, p, c);
			case BAKERY -> Crafts.bakery(b, p, c);
			case BREWERY -> Crafts.brewery(b, p, c);
			case WEAVER -> Crafts.weaver(b, p, c);
			case CARPENTER -> Crafts.carpenter(b, p, c);
			case BLOOMERY -> Crafts.bloomery(b, p, c);
			case KILN -> Crafts.kiln(b, p, c);
			case CHARCOAL -> Crafts.charcoal(b, p, c);
			case APIARY -> Crafts.apiary(b, p, c);
			case STABLE -> Crafts.stable(b, p, c);
			case WAREHOUSE -> Crafts.warehouse(b, p, c);
			case BATHHOUSE -> Public.bathhouse(b, p, c);
			case CARAVANSERAI -> Public.caravanserai(b, p, c);
			case BARRACKS -> Public.barracks(b, p, c);
			case TOWER -> Public.tower(b, p, c);
			case SHRINE -> Public.shrine(b, p, c);
			case CEMETERY -> Public.cemetery(b, p, c);
			case GARDEN -> Public.garden(b, p, c);
			case WATERMILL -> Shore.watermill(b, p, c);
			case NORIA -> Shore.noria(b, p, c);
			case WINDMILL -> Shore.windmill(b, p, c);
			case FISHERY -> Shore.fishery(b, p, c);
			case PIER -> Shore.pier(b, p, c);
			case BOATYARD -> Shore.boatyard(b, p, c);
			case MOTTE -> Forts.motte(b, p, c);
			case LONGHOUSE -> Forts.longhouse(b, p, c);
		}
	}
}
