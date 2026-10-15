package com.stasdoto.medieval.people;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.WorldGenLevel;

import com.stasdoto.medieval.Medieval;
import com.stasdoto.medieval.settle.BKind;
import com.stasdoto.medieval.settle.Site;

/** Registration of the people and who lives in which building. */
public final class People {
	private People() {
	}

	private static final ResourceKey<EntityType<?>> PERSON_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Medieval.id("person"));
	public static final EntityType<PersonEntity> PERSON = Registry.register(BuiltInRegistries.ENTITY_TYPE, PERSON_KEY,
			EntityType.Builder.<PersonEntity>of(PersonEntity::new, MobCategory.CREATURE).sized(0.6f, 1.8f).eyeHeight(1.62f)
					.clientTrackingRange(8).build(PERSON_KEY));

	public static void init() {
		FabricDefaultAttributeRegistry.register(PERSON, PersonEntity.createAttributes());
	}

	/** Where a person works by day. */
	static BlockPos workPlace(Site site, com.stasdoto.medieval.settle.Layout layout, Role role, BKind kind, BlockPos door, java.util.Random r) {
		switch (role) {
			case PEASANT, HERDER -> {
				if (layout != null && layout.fieldOuter > layout.fieldInner) {
					double a = r.nextDouble() * Math.PI * 2;
					double d = layout.fieldInner + 10 + r.nextDouble() * Math.max(10, (layout.fieldOuter - layout.fieldInner) * 0.6);
					int x = site.x + (int) (Math.cos(a) * d), z = site.z + (int) (Math.sin(a) * d);
					com.stasdoto.medieval.earth.Terrain.Col c = com.stasdoto.medieval.earth.Terrain.column(x, z);
					if (c.waterTop == com.stasdoto.medieval.earth.Terrain.NO_WATER) {
						return new BlockPos(x, c.top() + 1, z);
					}
				}
				return door;
			}
			case MERCHANT -> {
				if (layout != null && !layout.squares.isEmpty()) {
					var q = layout.squares.get(r.nextInt(layout.squares.size()));
					int x = (q.x0 + q.x1) / 2 + r.nextInt(9) - 4, z = (q.z0 + q.z1) / 2 + r.nextInt(9) - 4;
					return new BlockPos(x, com.stasdoto.medieval.earth.Terrain.column(x, z).top() + 1, z);
				}
				return door;
			}
			case WARRIOR, GUARD -> {
				if (layout != null && !layout.walls.isEmpty()) {
					var w = layout.walls.get(r.nextInt(layout.walls.size()));
					double a = r.nextDouble() * Math.PI * 2;
					double rr = w.radiusAt(a) - 5;
					int x = w.cx + (int) (Math.cos(a) * rr), z = w.cz + (int) (Math.sin(a) * rr);
					return new BlockPos(x, com.stasdoto.medieval.earth.Terrain.column(x, z).top() + 1, z);
				}
				return door;
			}
			default -> {
				return door;
			}
		}
	}

	/** Farm animals for a yard or a camp. */
	static void animals(WorldGenLevel level, Site site, BKind kind, BlockPos at, java.util.Random r) {
		java.util.List<net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> types = new java.util.ArrayList<>();
		if (kind == BKind.HOMESTEAD) {
			types.add(net.minecraft.world.entity.EntityTypes.CHICKEN);
			types.add(net.minecraft.world.entity.EntityTypes.CHICKEN);
			if (r.nextBoolean()) {
				types.add(site.culture == com.stasdoto.medieval.history.Culture.ARAB || site.culture == com.stasdoto.medieval.history.Culture.ANDALUSI
						|| site.culture == com.stasdoto.medieval.history.Culture.PERSIAN ? net.minecraft.world.entity.EntityTypes.SHEEP
						: r.nextBoolean() ? net.minecraft.world.entity.EntityTypes.PIG : net.minecraft.world.entity.EntityTypes.COW);
			}
			if (r.nextInt(3) == 0) {
				types.add(net.minecraft.world.entity.EntityTypes.SHEEP);
			}
		} else if (kind == BKind.YURT) {
			types.add(net.minecraft.world.entity.EntityTypes.HORSE);
			types.add(net.minecraft.world.entity.EntityTypes.SHEEP);
			types.add(net.minecraft.world.entity.EntityTypes.SHEEP);
		} else if (kind == BKind.HALL || kind == BKind.KEEP || kind == BKind.MOTTE) {
			types.add(net.minecraft.world.entity.EntityTypes.HORSE);
		} else if (kind == BKind.STABLE || kind == BKind.CARAVANSERAI) {
			types.add(net.minecraft.world.entity.EntityTypes.HORSE);
			types.add(net.minecraft.world.entity.EntityTypes.HORSE);
			if (r.nextBoolean()) {
				types.add(net.minecraft.world.entity.EntityTypes.DONKEY);
			}
		} else if (kind == BKind.APIARY) {
			types.add(net.minecraft.world.entity.EntityTypes.BEE);
			types.add(net.minecraft.world.entity.EntityTypes.BEE);
		} else if (kind == BKind.CHARCOAL || kind == BKind.SHED && r.nextInt(3) == 0) {
			types.add(net.minecraft.world.entity.EntityTypes.CHICKEN);
		}
		for (var t : types) {
			var m = t.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (m == null) {
				continue;
			}
			m.snapTo(at.getX() + 0.5 + r.nextInt(5) - 2, at.getY(), at.getZ() + 0.5 + r.nextInt(5) - 2, r.nextFloat() * 360f, 0f);
			m.setPersistenceRequired();
			level.addFreshEntity(m);
		}
	}

	/** The people living in a building, put in at world generation. */
	public static void populate(WorldGenLevel level, Site site, BKind kind, BlockPos door, long seed) {
		populate(level, site, kind, door, seed, null);
	}

	/** @param layout the settlement's plan, for the people's places of work (may be null) */
	public static void populate(WorldGenLevel level, Site site, BKind kind, BlockPos door, long seed, com.stasdoto.medieval.settle.Layout layout) {
		java.util.Random r = new java.util.Random(seed ^ 0x51ED2701L);
		Role[] who = switch (kind) {
			case HOMESTEAD -> site.kind == Site.Kind.CAMP ? new Role[]{Role.HERDER, Role.HERDER, Role.CHILD}
					: r.nextInt(3) == 0 ? new Role[]{Role.PEASANT, Role.PEASANT, Role.CHILD, Role.CHILD} : new Role[]{Role.PEASANT, Role.PEASANT, Role.CHILD};
			case YURT -> new Role[]{Role.HERDER, Role.HERDER, Role.CHILD};
			case HOUSE -> new Role[]{Role.CRAFTSMAN, r.nextBoolean() ? Role.CRAFTSMAN : Role.MERCHANT, Role.CHILD};
			case HOUSE_RICH -> new Role[]{Role.MERCHANT, Role.NOBLE, Role.CHILD};
			case SMITHY, BLOOMERY -> new Role[]{Role.SMITH, r.nextBoolean() ? Role.SMITH : Role.CHILD};
			case TAVERN -> new Role[]{Role.INNKEEPER, Role.INNKEEPER, Role.MERCHANT};
			case STALL -> new Role[]{Role.MERCHANT};
			case CHURCH_SMALL, CHURCH, MOSQUE -> new Role[]{Role.PRIEST};
			case CATHEDRAL -> new Role[]{Role.PRIEST, Role.MONK, Role.MONK};
			case CELLS -> new Role[]{Role.MONK, Role.MONK};
			case HALL -> new Role[]{Role.NOBLE, Role.NOBLE, Role.WARRIOR, Role.WARRIOR};
			case KEEP, MOTTE -> new Role[]{Role.WARRIOR, Role.WARRIOR, Role.WARRIOR};
			case BARRACKS -> new Role[]{Role.GUARD, Role.GUARD, Role.WARRIOR, Role.WARRIOR};
			case TOWER -> new Role[]{Role.NOBLE, Role.GUARD};
			case LONGHOUSE -> new Role[]{Role.WARRIOR, Role.WARRIOR, Role.PEASANT, Role.CHILD};
			case WATERMILL, NORIA, WINDMILL -> new Role[]{Role.MILLER, Role.MILLER};
			case POTTERY, KILN -> new Role[]{Role.POTTER, Role.POTTER};
			case TANNERY -> new Role[]{Role.TANNER, Role.TANNER};
			case BAKERY -> new Role[]{Role.BAKER, Role.BAKER, Role.CHILD};
			case BREWERY -> new Role[]{Role.BREWER, Role.BREWER};
			case WEAVER -> new Role[]{Role.WEAVER, Role.WEAVER};
			case CARPENTER, BOATYARD -> new Role[]{Role.CARPENTER, Role.CARPENTER};
			case FISHERY, PIER -> new Role[]{Role.FISHER, Role.FISHER};
			case APIARY -> new Role[]{Role.BEEKEEPER};
			case CHARCOAL -> new Role[]{Role.CHARCOALER};
			case STABLE -> new Role[]{Role.GROOM};
			case WAREHOUSE, CARAVANSERAI -> new Role[]{Role.MERCHANT, Role.MERCHANT, Role.GUARD};
			case BATHHOUSE -> new Role[]{Role.CRAFTSMAN};
			default -> new Role[0];
		};
		animals(level, site, kind, door, r);
		for (int i = 0; i < who.length; i++) {
			Role role = who[i];
			boolean female = switch (role) {
				case WARRIOR, GUARD, PRIEST, MONK -> false;
				default -> i == 1 || role == Role.CHILD && r.nextBoolean();
			};
			PersonEntity p = PERSON.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (p == null) {
				continue;
			}
			int look = r.nextInt();
			p.setup(look, site.culture, role, female, door, site.name, Names.of(site.culture, female, look));
			p.work = workPlace(site, layout, role, kind, door, r);
			p.snapTo(door.getX() + 0.5 + r.nextInt(3) - 1, door.getY(), door.getZ() + 0.5 + r.nextInt(3) - 1, r.nextFloat() * 360f, 0f);
			if (role == Role.WARRIOR || role == Role.GUARD) {
				p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
						new net.minecraft.world.item.ItemStack(com.stasdoto.medieval.items.Weapons.forCulture(site.culture, r)));
				Gear.arm(p, site.culture, kind == BKind.HALL || kind == BKind.KEEP || kind == BKind.MOTTE, r);
			}
			if (role == Role.WARRIOR && (kind == BKind.KEEP || kind == BKind.MOTTE || kind == BKind.HALL) && r.nextInt(3) == 0) {
				Gear.mount(level, p, r);
			}
			level.addFreshEntity(p);
		}
	}
}
