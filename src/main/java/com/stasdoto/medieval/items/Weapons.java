package com.stasdoto.medieval.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;

import com.stasdoto.medieval.Medieval;

/**
 * Weapons of the 11th century. Damage and speed follow the weapon: long heavy blades and axes hit hard and slowly,
 * sabres and spears are quicker.
 */
public final class Weapons {
	private Weapons() {
	}

	public static final List<Item> ALL = new ArrayList<>();

	public static final Item CAROLINGIAN_SWORD = register("carolingian_sword", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 3.5f, -2.5f));
	public static final Item NORMAN_SWORD = register("norman_sword", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 4.0f, -2.6f));
	public static final Item SABRE = register("sabre", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 2.5f, -2.1f));
	public static final Item BEARDED_AXE = register("bearded_axe", Item::new, new Item.Properties().axe(ToolMaterial.IRON, 5.5f, -3.0f));
	public static final Item DANE_AXE = register("dane_axe", Item::new, new Item.Properties().axe(ToolMaterial.IRON, 8.0f, -3.3f));
	public static final Item MACE = register("mace", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 4.5f, -2.9f));
	public static final Item SHESTOPER = register("shestoper", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 5.0f, -2.9f));
	public static final Item FLAIL = register("flail", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 5.5f, -3.2f));
	public static final Item SPEAR = register("spear", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 3.0f, -2.2f));
	public static final Item SULITSA = register("sulitsa", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 2.0f, -2.0f));
	public static final Item LANCE = register("lance", Item::new, new Item.Properties().sword(ToolMaterial.IRON, 6.0f, -3.4f));
	/** The silver grivna (a bar of silver, the big money of Rus): worth 5 emeralds in the settlements' markets. */
	public static final Item GRIVNA = register("grivna", Item::new, new Item.Properties().stacksTo(64));

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Medieval.id("main"));
	public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(CAROLINGIAN_SWORD))
			.title(Component.translatable("itemGroup.medieval"))
			.displayItems((params, output) -> {
				for (Item i : ALL) {
					output.accept(i);
				}
			})
			.build();

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Medieval.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
		ALL.add(item);
		return item;
	}

	/** A weapon a warrior of this people would carry. */
	public static Item forCulture(com.stasdoto.medieval.history.Culture c, java.util.Random r) {
		Item[] pick = switch (c) {
			case RUS, BALT_FINN -> new Item[]{CAROLINGIAN_SWORD, BEARDED_AXE, SPEAR, SPEAR, SHESTOPER, SULITSA};
			case NORSE -> new Item[]{DANE_AXE, BEARDED_AXE, CAROLINGIAN_SWORD, SPEAR};
			case STEPPE, PERSIAN -> new Item[]{SABRE, SABRE, SPEAR, MACE};
			case LATIN, CELTIC -> new Item[]{NORMAN_SWORD, NORMAN_SWORD, LANCE, MACE, SPEAR, FLAIL};
			case WEST_SLAV, MAGYAR -> new Item[]{CAROLINGIAN_SWORD, SABRE, BEARDED_AXE, SPEAR};
			case BYZANTINE, CAUCASUS -> new Item[]{SPEAR, NORMAN_SWORD, MACE, SABRE};
			case ANDALUSI, ARAB -> new Item[]{SABRE, SPEAR, CAROLINGIAN_SWORD};
		};
		return pick[r.nextInt(pick.length)];
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
	}
}
