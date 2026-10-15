package com.stasdoto.medieval.people;

import java.util.Random;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;

import com.stasdoto.medieval.history.Culture;

/** Arms and armour of the warriors by people and rank: militia in leather, the retinue in mail, knights in plate-reinforced mail on horses. */
public final class Gear {
	private Gear() {
	}

	public static void arm(PersonEntity p, Culture c, boolean elite, Random r) {
		int rank = elite ? 2 : r.nextInt(3) == 0 ? 0 : 1; // 0 militia, 1 retinue, 2 elite
		boolean east = c == Culture.STEPPE || c == Culture.ARAB || c == Culture.PERSIAN || c == Culture.ANDALUSI;
		Item head, chest, legs, feet;
		switch (rank) {
			case 0 -> {
				head = r.nextBoolean() ? Items.LEATHER_HELMET : null;
				chest = Items.LEATHER_CHESTPLATE;
				legs = r.nextBoolean() ? Items.LEATHER_LEGGINGS : null;
				feet = Items.LEATHER_BOOTS;
			}
			case 1 -> {
				head = east && r.nextBoolean() ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET;
				chest = Items.CHAINMAIL_CHESTPLATE;
				legs = r.nextBoolean() ? Items.CHAINMAIL_LEGGINGS : Items.LEATHER_LEGGINGS;
				feet = Items.LEATHER_BOOTS;
			}
			default -> {
				head = Items.IRON_HELMET;
				chest = r.nextInt(3) == 0 ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE;
				legs = Items.CHAINMAIL_LEGGINGS;
				feet = Items.IRON_BOOTS;
			}
		}
		set(p, EquipmentSlot.HEAD, head);
		set(p, EquipmentSlot.CHEST, chest);
		set(p, EquipmentSlot.LEGS, legs);
		set(p, EquipmentSlot.FEET, feet);
		// Shields: round or kite-shaped, most warriors carry one; archers of the steppe do not.
		if (c != Culture.STEPPE && r.nextInt(4) != 0) {
			set(p, EquipmentSlot.OFFHAND, Items.SHIELD);
		} else if (c == Culture.STEPPE || r.nextInt(5) == 0) {
			set(p, EquipmentSlot.OFFHAND, null);
			if (r.nextBoolean()) {
				set(p, EquipmentSlot.MAINHAND, Items.BOW);
			}
		}
	}

	private static void set(PersonEntity p, EquipmentSlot slot, Item item) {
		if (item != null) {
			p.setItemSlot(slot, new ItemStack(item));
		}
	}

	/** A knight or a nobleman's rider on his horse. */
	public static void mount(WorldGenLevel level, PersonEntity p, Random r) {
		var h = net.minecraft.world.entity.EntityTypes.HORSE.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (h == null) {
			return;
		}
		h.snapTo(p.getX(), p.getY(), p.getZ(), r.nextFloat() * 360f, 0f);
		h.setPersistenceRequired();
		level.addFreshEntity(h);
		p.startRiding(h);
	}
}
