package com.stasdoto.medieval.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.Identifier;

import com.stasdoto.medieval.people.People;

public class MedievalClient implements ClientModInitializer {
	private static boolean mDown, nDown;

	@Override
	public void onInitializeClient() {
		EntityRenderers.register(People.PERSON, PersonRenderer::new);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("medieval", "minimap"), MiniMap::draw);
		// M opens the big map, N hides or shows the minimap.
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			boolean m = com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_M);
			boolean n = com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_N);
			if (m && !mDown) {
				if (mc.gui.screen() == null) {
					mc.gui.setScreen(new WorldMapScreen());
				} else if (mc.gui.screen() instanceof WorldMapScreen) {
					mc.gui.setScreen(null);
				}
			}
			if (n && !nDown && mc.gui.screen() == null) {
				MiniMap.visible = !MiniMap.visible;
			}
			mDown = m;
			nDown = n;
		});
	}
}
