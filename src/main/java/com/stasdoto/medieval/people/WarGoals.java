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

	/** Garrisons and the lord's warriors go for raiders attacking their settlement or their lord. */
	public static final class HuntRaiders extends TargetGoal {
		private final PersonEntity p;

		public HuntRaiders(PersonEntity p) {
			super(p, false);
			this.p = p;
		}

		@Override
		public boolean canUse() {
			if (p.role() != Role.WARRIOR || !p.enemyOf.isEmpty() || p.tickCount % 10 != 0) {
				return false;
			}
			PersonEntity best = null;
			double bd = 48 * 48;
			for (PersonEntity e : p.level().getEntitiesOfClass(PersonEntity.class, new AABB(p.blockPosition()).inflate(48))) {
				if (e.enemyOf.isEmpty() || !e.isAlive()) {
					continue;
				}
				boolean mine = e.raidOf.equals(p.settlement) || p.lord != null && e.enemyOf.equals(p.lord.toString());
				double d = p.distanceToSqr(e);
				if (mine && d < bd) {
					bd = d;
					best = e;
				}
			}
			if (best == null) {
				return false;
			}
			p.setTarget(best);
			return true;
		}
	}

	/** Raiders attack the lord, his warriors and the people of the settlement they raid. */
	public static final class RaiderTargets extends TargetGoal {
		private final PersonEntity p;

		public RaiderTargets(PersonEntity p) {
			super(p, false);
			this.p = p;
		}

		@Override
		public boolean canUse() {
			if (p.enemyOf.isEmpty() || p.tickCount % 10 != 0) {
				return false;
			}
			LivingEntity best = null;
			double bd = 32 * 32;
			for (Player pl : p.level().players()) {
				if (pl.getUUID().toString().equals(p.enemyOf) && !pl.isCreative() && !pl.isSpectator()) {
					double d = p.distanceToSqr(pl);
					if (d < bd) {
						bd = d;
						best = pl;
					}
				}
			}
			for (PersonEntity e : p.level().getEntitiesOfClass(PersonEntity.class, new AABB(p.blockPosition()).inflate(32))) {
				if (!e.isAlive() || !e.enemyOf.isEmpty()) {
					continue;
				}
				boolean foe = e.lord != null && e.lord.toString().equals(p.enemyOf) || e.settlement.equals(p.raidOf);
				double d = p.distanceToSqr(e);
				if (foe && d < bd) {
					bd = d;
					best = e;
				}
			}
			if (best == null) {
				return false;
			}
			p.setTarget(best);
			return true;
		}
	}

	/** Raiders march on the settlement when nobody is in reach. */
	public static final class March extends Goal {
		private final PersonEntity p;
		private int repath;

		public March(PersonEntity p) {
			this.p = p;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !p.enemyOf.isEmpty() && p.getTarget() == null && p.raidTarget != net.minecraft.core.BlockPos.ZERO
					&& p.distanceToSqr(p.raidTarget.getX(), p.raidTarget.getY(), p.raidTarget.getZ()) > 64;
		}

		@Override
		public void tick() {
			if (--repath <= 0) {
				repath = 20;
				p.getNavigation().moveTo(p.raidTarget.getX() + 0.5, p.raidTarget.getY(), p.raidTarget.getZ() + 0.5, 1.15);
			}
		}
	}
}
