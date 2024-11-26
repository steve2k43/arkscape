package net.nocturne.game.npc.others;

import java.util.concurrent.TimeUnit;

import net.nocturne.executor.GameExecutorManager;
import net.nocturne.game.Animation;
import net.nocturne.game.Entity;
import net.nocturne.game.WorldTile;
import net.nocturne.game.npc.NPC;
import net.nocturne.game.npc.combat.NPCCombatDefinitions;
import net.nocturne.game.player.Player;
import net.nocturne.game.tasks.WorldTask;
import net.nocturne.game.tasks.WorldTasksManager;
import net.nocturne.utils.Logger;
import net.nocturne.utils.Utils;

@SuppressWarnings("serial")
public class LivingRock extends NPC {

	private Entity source;
	private long deathTime;

	public LivingRock(int id, WorldTile tile, int mapAreaNameHash,
			boolean canBeAttackFromOutOfArea, boolean spawned) {
		super(id, tile, mapAreaNameHash, canBeAttackFromOutOfArea, spawned);
	}

	@Override
	public void sendDeath(final Entity source) {
		final NPCCombatDefinitions defs = getCombatDefinitions();
		resetWalkSteps();
		getCombat().removeTarget();
		setNextAnimation(null);
		WorldTasksManager.schedule(new WorldTask() {
			int loop;

			@Override
			public void run() {
				if (loop == 0) {
					setNextAnimation(new Animation(defs.getDeathEmote()));
				} else if (loop >= defs.getDeathDelay()) {
					giveXP();
					drop();
					reset();
					transformIntoRemains(source);
					stop();
				}
				loop++;
			}
		}, 0, 1);
	}

	public void transformIntoRemains(Entity source) {
		this.source = source;
		deathTime = Utils.currentTimeMillis();
		final int remainsId = getId() + 5;
		setNextNPCTransformation(remainsId);
		setRandomWalk(0);
		GameExecutorManager.slowExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					if (remainsId == getId())
						takeRemains();
				} catch (Throwable e) {
					Logger.handle(e);
				}
			}
		}, 3, TimeUnit.MINUTES);

	}

	public boolean canMine(Player player) {
		return Utils.currentTimeMillis() - deathTime > 60000
				|| player == source;
	}

	public void takeRemains() {
		setNPC(getId() - 5);
		setLocation(getRespawnTile());
		setRandomWalk(NORMAL_WALK);
		finish();
		if (!isSpawned())
			setRespawnTask();
	}

}
