package com.stasdoto.medieval.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

import com.stasdoto.medieval.history.Culture;
import com.stasdoto.medieval.people.PersonEntity;
import com.stasdoto.medieval.people.Role;

/** Draws the people with the player model and their painted skins; children are smaller. */
public class PersonRenderer extends HumanoidMobRenderer<PersonEntity, AvatarRenderState, PlayerModel> {
	static final RenderStateDataKey<Identifier> SKIN = RenderStateDataKey.create(() -> "medieval:person_skin");

	public PersonRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PlayerModel(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public void extractRenderState(PersonEntity p, AvatarRenderState s, float partialTick) {
		super.extractRenderState(p, s, partialTick);
		Identifier skin = Skins.texture(p.look(), p.culture(), p.role(), p.female());
		s.setData(SKIN, skin);
		s.skin = Skins.skin(skin);
		s.isBaby = p.role() == Role.CHILD;
	}

	@Override
	protected void scale(AvatarRenderState s, PoseStack poseStack) {
		super.scale(s, poseStack);
		if (s.isBaby) {
			poseStack.scale(0.62f, 0.62f, 0.62f);
		}
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState s) {
		Identifier id = s.getData(SKIN);
		return id != null ? id : Skins.texture(0, Culture.RUS, Role.PEASANT, false);
	}
}
