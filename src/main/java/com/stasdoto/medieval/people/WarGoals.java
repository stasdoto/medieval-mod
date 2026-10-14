package com.stasdoto.medieval.people;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/** What warriors do: follow their lord, defend him, strike back, clear monsters, and fight in melee. */
public final class WarGoals {
	private WarGoals() {
	}

	static Player lord(PersonEntity p) {
		if (p.lord == null || !(p.level() instanceof ServerLevel sl)) {
			return null;
		}
		return sl.getPlayerByUUID(p.lord);
	}

	/** Walks after the lord, keeping a few steps away. */
	public static final class FollowLord extends Goal {
		private final PersonEntity p;
		private int repath;

		public FollowLord(PersonEntity p) {
			this.p = p;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			Player l = lord(p);
			return l != null && p.getTarget() == null && p.distanceToSqr(l) > 36;
		}

		@Override
		public boolean canContinueToUse() {
			Player l = lord(p);
			return l != null && p.getTarget() == null && p.distanceToSqr(l) > 9;
		}

		@Override
		public void tick() {
			Player l = lord(p);
			if (l == null) {
				return;
			}
			p.getLookControl().setLookAt(l);
			if (--repath <= 0) {
				repath = 10;
				if (p.distanceToSqr(l) > 48 * 48) {
					p.snapTo(l.getX() + p.getRandom().nextInt(5) - 2, l.getY(), l.getZ() + p.getRandom().nextInt(5) - 2, p.getYRot(), 0);
				} else {
					p.getNavigation().moveTo(l, p.distanceToSqr(l) > 144 ? 1.25 : 1.0);
				}
			}
		}
	}

	/** Melee: walk up and strike. */
	public static final class Fight extends Goal {
		private final PersonEntity p;
		private int cooldown;

		public Fight(PersonEntity p) {
			this.p = p;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = p.getTarget();
			return t != null && t.isAlive() && p.role() == Role.WARRIOR;
		}

		@Override
		public void tick() {
			LivingEntity t = p.getTarget();
			if (t == null) {
				return;
			}
			p.getLookControl().setLookAt(t, 30f, 30f);
			double d2 = p.distanceToSqr(t);
			if (d2 > 4.5) {
				p.getNavigation().moveTo(t, 1.25);
			} else {
				p.getNavigation().stop();
			}
			if (--cooldown <= 0 && d2 <= 5.0 && p.level() instanceof ServerLevel sl) {
				cooldown = 16;
				p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
				p.doHurtTarget(sl, t);
			}
		}
	}

	/** Attacks whoever attacks the lord, or whom the lord attacks. */
	public static final class DefendLord extends TargetGoal {
		private final PersonEntity p;

		public DefendLord(PersonEntity p) {
			super(p, false);
			this.p = p;
		}

		@Override
		public boolean canUse() {
			Player l = lord(p);
			if (l == null || p.role() != Role.WARRIOR) {
				return false;
			}
			LivingEntity a = l.getLastHurtByMob();
			if (a == null || !a.isAlive() || a == p) {
				a = l.getLastHurtMob();
			}
			if (a == null || !a.isAlive() || a == p || a instanceof Player || a instanceof PersonEntity pe && p.lord.equals(pe.lord)) {
				return false;
			}
			p.setTarget(a);
			return true;
		}
	}

	/** Strikes back at whoever hurt him (warriors only). */
	public static final class Retaliate extends TargetGoal {
		private final PersonEntity p;

		public Retaliate(PersonEntity p) {
			super(p, false);
			this.p = p;
		}

		@Override
		public boolean canUse() {
			LivingEntity a = p.getLastHurtByMob();
			if (a == null || !a.isAlive() || p.role() != Role.WARRIOR) {
				return false;
			}
			if (a instanceof Player pl && p.lord != null && p.lord.equals(pl.getUUID())) {
				return false;
			}
			p.setTarget(a);
			return true;
		}
	}

	/** Warriors clear monsters near them. */
	public static final class HuntMonsters extends TargetGoal {
		private final PersonEntity p;

		public HuntMonsters(PersonEntity p) {
			super(p, true);
			this.p = p;
		}

		@Override
		public boolean canUse() {
			if (p.role() != Role.WARRIOR || p.tickCount % 10 != 0) {
				return false;
			}
			Monster best = null;
			double bd = 20 * 20;
			for (Monster m : p.level().getEntitiesOfClass(Monster.class, new AABB(p.blockPosition()).inflate(20))) {
				double d = p.distanceToSqr(m);
				if (d < bd && m.isAlive()) {
					bd = d;
					best = m;
				}
			}
			if (best == null) {
				return false;
			}
			p.setTarget(best);
			return true;
		}
	}
}
