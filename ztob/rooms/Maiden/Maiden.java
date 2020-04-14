package net.runelite.client.plugins.ztob.rooms.Maiden;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.ztob.Room;
import net.runelite.client.plugins.ztob.TheatreConfig;
import net.runelite.client.plugins.ztob.TheatrePlugin;

public class Maiden extends Room
{
	@Inject
	private Client client;
	@Inject
	private MaidenOverlay maidenOverlay;
	@Inject
	protected Maiden(TheatrePlugin plugin, TheatreConfig config)
	{
		super(plugin, config);
	}

	private static final int GRAPHICSOBJECT_ID_MAIDEN = 1579;

	@Getter
	private boolean maidenActive;
	@Getter
	private List<WorldPoint> maidenBloodSplatters = new ArrayList<>();
	private List<NPC> maidenSpawns = new ArrayList<>();
	@Getter
	private List<WorldPoint> maidenBloodSpawnLocations = new ArrayList<>();
	@Getter
	private List<WorldPoint> maidenBloodSpawnTrailingLocations = new ArrayList<>();

	@Getter
	private int ticksUntilAttack = 0;
	private int lastAnimationID = -1;
	@Getter
	private NPC maidenNPC;

	@Override
	public void load()
	{
		overlayManager.add(maidenOverlay);
	}

	@Override
	public void unload()
	{
		overlayManager.remove(maidenOverlay);

		maidenActive = false;
		maidenBloodSplatters.clear();
		maidenSpawns.clear();
		maidenBloodSpawnLocations.clear();
		maidenBloodSpawnTrailingLocations.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.THE_MAIDEN_OF_SUGADINTI:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8361:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8362:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8363:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8364:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8365:
				ticksUntilAttack = 10;
				maidenActive = true;
				maidenNPC = npc;
				break;
			case NpcID.BLOOD_SPAWN:
				maidenSpawns.add(npc);
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.THE_MAIDEN_OF_SUGADINTI:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8361:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8362:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8363:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8364:
			case NpcID.THE_MAIDEN_OF_SUGADINTI_8365:
				ticksUntilAttack = 0;
				maidenActive = false;
				maidenSpawns.clear();
				maidenNPC = null;
				break;
			case NpcID.BLOOD_SPAWN:
				maidenSpawns.remove(npc);
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!maidenActive)
		{
			return;
		}

		if (maidenNPC != null)
		{
			ticksUntilAttack--;
			if (lastAnimationID == -1 && maidenNPC.getAnimation() != lastAnimationID)
			{
				ticksUntilAttack = 10;
			}
			lastAnimationID = maidenNPC.getAnimation();
		}

		maidenBloodSplatters.clear();
		client.getGraphicsObjects().stream().filter(o -> o.getId() == GRAPHICSOBJECT_ID_MAIDEN).
			forEach(o -> maidenBloodSplatters.add(WorldPoint.fromLocal(client, o.getLocation())));

		maidenBloodSpawnTrailingLocations.clear();
		maidenBloodSpawnTrailingLocations.addAll(maidenBloodSpawnLocations);
		maidenBloodSpawnLocations.clear();

		maidenSpawns.forEach(s -> maidenBloodSpawnLocations.add(s.getWorldLocation()));
	}

	Color maidenSpecialWarningColor()
	{
		Color col = Color.GREEN;
		if (maidenNPC == null || maidenNPC.getInteracting() == null)
		{
			return col;
		}

		if (maidenNPC.getInteracting().getName().equals(client.getLocalPlayer().getName()))
		{
			return Color.ORANGE;
		}

		return col;
	}
}
