package com.stasdoto.medieval.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * The day of a medieval person: at dawn out to the fields, the workshop or the market, at dusk back home, at night
 * indoors. Children play around the home, warriors keep near it (their post).
 */
public class DayGoal extends Goal {
	private final PersonEntity p;
	private int cooldown;

	public DayGoal(PersonEntity p) {
		this.p = p;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	private boolean night() {
		long t = p.level().getOverworldClockTime() % 24000L;
		return t > 12800 && t < 23200;
	}

	@Override
	public boolean canUse() {
		if (--cooldown > 0 || p.home == BlockPos.ZERO) {
			return false;
		}
		cooldown = 80 + p.getRandom().nextInt(120);
		double d2 = p.distanceToSqr(p.home.getX() + 0.5, p.home.getY(), p.home.getZ() + 0.5);
		if (night()) {
			return d2 > 9;
		}
		Role r = p.role();
		double range = switch (r) {
			case PEASANT, HERDER -> 120;
			case MERCHANT, CRAFTSMAN -> 60;
			case CHILD -> 25;
			case WARRIOR, NOBLE -> 30;
			default -> 40;
		};
		return d2 > range * range;
	}

	@Override
	public void start() {
		if (night()) {
			p.getNavigation().moveTo(p.home.getX() + 0.5, p.home.getY(), p.home.getZ() + 0.5, 0.75);
			return;
		}
		// Back towards home ground, to a spot around it.
		int dx = p.getRandom().nextInt(41) - 20, dz = p.getRandom().nextInt(41) - 20;
		p.getNavigation().moveTo(p.home.getX() + dx + 0.5, p.home.getY(), p.home.getZ() + dz + 0.5, 0.7);
	}

	@Override
	public boolean canContinueToUse() {
		return !p.getNavigation().isDone();
	}
}
