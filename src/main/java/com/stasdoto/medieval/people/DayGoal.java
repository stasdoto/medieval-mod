package com.stasdoto.medieval.people;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * The day of a medieval person: at dawn out to his work (the field, the smithy, the market stall, the church, the
 * wall), busy there until evening, then home; at night indoors. Children play around the home.
 */
public class DayGoal extends Goal {
	private final PersonEntity p;
	private int cooldown;
	private BlockPos goal;

	public DayGoal(PersonEntity p) {
		this.p = p;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	private long time() {
		return p.level().getOverworldClockTime() % 24000L;
	}

	private boolean night() {
		long t = time();
		return t > 12500 && t < 23300;
	}

	/** Morning and evening: people are on the move; the middle of the day: at work. */
	private BlockPos wanted() {
		if (night() || p.work == BlockPos.ZERO || p.role() == Role.CHILD) {
			return p.home;
		}
		long t = time();
		return t < 11000 || t > 23300 ? p.work : p.home;
	}

	@Override
	public boolean canUse() {
		if (p.lord != null || !p.enemyOf.isEmpty() || p.home == BlockPos.ZERO || --cooldown > 0) {
			return false;
		}
		cooldown = 60 + p.getRandom().nextInt(80);
		BlockPos w = wanted();
		double range = w == p.work ? (p.role() == Role.PEASANT || p.role() == Role.HERDER ? 14 : 5) : night() ? 3 : 12;
		if (p.distanceToSqr(w.getX() + 0.5, w.getY(), w.getZ() + 0.5) > range * range) {
			goal = w;
			return true;
		}
		// At the place: a little walk about it (a peasant along his strip, a merchant round the stall).
		if (p.getRandom().nextInt(3) == 0 && !night()) {
			goal = w.offset(p.getRandom().nextInt(2 * (int) range + 1) - (int) range, 0, p.getRandom().nextInt(2 * (int) range + 1) - (int) range);
			return true;
		}
		return false;
	}

	@Override
	public void start() {
		double speed = night() ? 0.75 : 0.6;
		p.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, speed);
	}

	@Override
	public boolean canContinueToUse() {
		return !p.getNavigation().isDone();
	}
}
