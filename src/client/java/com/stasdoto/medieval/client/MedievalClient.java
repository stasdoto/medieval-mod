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
			long win = mc.getWindow().handle();
			boolean m = org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_M) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
			boolean n = org.lwjgl.glfw.GLFW.glfwGetKey(win, org.lwjgl.glfw.GLFW.GLFW_KEY_N) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
			if (m && !mDown) {
				if (mc.screen == null) {
					mc.setScreen(new WorldMapScreen());
				} else if (mc.screen instanceof WorldMapScreen) {
					mc.setScreen(null);
				}
			}
			if (n && !nDown && mc.screen == null) {
				MiniMap.visible = !MiniMap.visible;
			}
			mDown = m;
			nDown = n;
		});
	}
}
