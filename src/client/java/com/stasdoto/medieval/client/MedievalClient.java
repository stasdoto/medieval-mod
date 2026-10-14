package com.stasdoto.medieval.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

import com.stasdoto.medieval.people.People;

public class MedievalClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(People.PERSON, PersonRenderer::new);
	}
}
