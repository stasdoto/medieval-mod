package com.stasdoto.medieval.people;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.stasdoto.medieval.history.Culture;

/**
 * A person of the 11th century living in a settlement: dressed after his people and his work, with a name, a home and
 * a day - out to work in the morning, back home at dusk.
 */
public class PersonEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> DATA_LOOK = SynchedEntityData.defineId(PersonEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_CULTURE = SynchedEntityData.defineId(PersonEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ROLE = SynchedEntityData.defineId(PersonEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_FEMALE = SynchedEntityData.defineId(PersonEntity.class, EntityDataSerializers.BOOLEAN);

	public BlockPos home = BlockPos.ZERO;
	/** The player this warrior serves (follows and fights for), or null. */
	public java.util.UUID lord;
	public String settlement = "";
	public String name = "";

	public PersonEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation g) {
			g.setCanOpenDoors(true);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 32)
				.add(Attributes.ATTACK_DAMAGE, 3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_LOOK, 0);
		builder.define(DATA_CULTURE, 0);
		builder.define(DATA_ROLE, 0);
		builder.define(DATA_FEMALE, false);
	}

	public void setup(int look, Culture culture, Role role, boolean female, BlockPos home, String settlement, String name) {
		entityData.set(DATA_LOOK, look);
		entityData.set(DATA_CULTURE, culture.ordinal());
		entityData.set(DATA_ROLE, role.ordinal());
		entityData.set(DATA_FEMALE, female);
		this.home = home;
		this.settlement = settlement;
		this.name = name;
	}

	public int look() {
		return entityData.get(DATA_LOOK);
	}

	public Culture culture() {
		return Culture.values()[Math.floorMod(entityData.get(DATA_CULTURE), Culture.values().length)];
	}

	public Role role() {
		return Role.values()[Math.floorMod(entityData.get(DATA_ROLE), Role.values().length)];
	}

	public boolean female() {
		return entityData.get(DATA_FEMALE);
	}

	@Override
	public boolean isBaby() {
		return role() == Role.CHILD;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new WarGoals.Fight(this));
		goalSelector.addGoal(1, new OpenDoorGoal(this, true));
		goalSelector.addGoal(2, new WarGoals.FollowLord(this));
		goalSelector.addGoal(3, new DayGoal(this));
		targetSelector.addGoal(1, new WarGoals.DefendLord(this));
		targetSelector.addGoal(2, new WarGoals.Retaliate(this));
		targetSelector.addGoal(3, new WarGoals.HuntMonsters(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide() && tickCount % 20 == 0 && home != BlockPos.ZERO && distanceToSqr(home.getX(), home.getY(), home.getZ()) > 400 * 400) {
			// Wandered far away (pushed, chased): walk home.
			getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide()) {
			String who = female() ? role().female : role().male;
			String greet = switch (culture()) {
				case RUS, BALT_FINN -> "Здрав будь, путник!";
				case BYZANTINE -> "Мир тебе, странник.";
				case LATIN, CELTIC -> "Да хранит тебя Господь, путник.";
				case NORSE -> "Хейль, чужеземец!";
				case WEST_SLAV, MAGYAR -> "Слава Богу, гость!";
				case ANDALUSI, ARAB, PERSIAN -> "Мир тебе, путник.";
				case STEPPE -> "Чего ищешь в нашей степи?";
				case CAUCASUS -> "Добро пожаловать, гость!";
			};
			player.sendSystemMessage(Component.literal("<" + name + "> " + greet + " Я " + who + (settlement.isEmpty() ? "" : " из " + settlement) + "."));
			getNavigation().stop();
			getLookControl().setLookAt(player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("look", look());
		output.putInt("culture", entityData.get(DATA_CULTURE));
		output.putInt("role", entityData.get(DATA_ROLE));
		output.putBoolean("female", female());
		output.store("home", BlockPos.CODEC, home);
		output.putString("settlement", settlement);
		output.putString("name", name);
		if (lord != null) {
			output.store("lord", net.minecraft.core.UUIDUtil.CODEC, lord);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_LOOK, input.getIntOr("look", 0));
		entityData.set(DATA_CULTURE, input.getIntOr("culture", 0));
		entityData.set(DATA_ROLE, input.getIntOr("role", 0));
		entityData.set(DATA_FEMALE, input.getBooleanOr("female", false));
		home = input.read("home", BlockPos.CODEC).orElse(BlockPos.ZERO);
		settlement = input.getStringOr("settlement", "");
		name = input.getStringOr("name", "");
		lord = input.read("lord", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public Component getName() {
		return name.isEmpty() ? super.getName() : Component.literal(name);
	}
}
