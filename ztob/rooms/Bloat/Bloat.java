package net.runelite.client.plugins.ztob.rooms.Bloat;

import java.awt.Color;
import java.awt.Polygon;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.NpcID;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.ztob.Room;
import net.runelite.client.plugins.ztob.TheatreConfig;
import net.runelite.client.plugins.ztob.TheatrePlugin;

public class Bloat extends Room
{
	@Getter
	private boolean bloatActive;
	private NPC bloatNPC;
	private int bloatDownCount = 0;
	private int bloatState = 0;

	@Inject
	private BloatOverlay bloatOverlay;
	@Inject
	private Client client;

	@Inject
	protected Bloat(TheatrePlugin plugin, TheatreConfig config)
	{
		super(plugin, config);
	}

	@Override
	public void load()
	{
		overlayManager.add(bloatOverlay);
	}

	@Override
	public void unload()
	{
		overlayManager.remove(bloatOverlay);

		bloatDownCount = 0;
		bloatState = 0;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.PESTILENT_BLOAT:
				bloatActive = true;
				bloatNPC = npc;
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.PESTILENT_BLOAT:
				bloatActive = false;
				bloatNPC = null;
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (bloatActive)
		{
			bloatDownCount++;

			if (bloatNPC.getAnimation() == -1) //1 = up; 2 = down; 3 = warn;
			{
				bloatDownCount = 0;
				if (bloatNPC.getHealth() == 0)
				{
					bloatState = 2;
				}
				else
				{
					bloatState = 1;
				}
			}
			else
			{
				if (25 < bloatDownCount && bloatDownCount < 35)
				{
					bloatState = 3;
				}
				else if (bloatDownCount < 26)
				{
					bloatState = 2;
				}
				else if (bloatNPC.getModelHeight() == 568)
				{
					bloatState = 2;
				}
				else
				{
					bloatState = 1;
				}
			}
		}
	}

	Polygon getBloatTilePoly()
	{
		if (bloatNPC == null)
		{
			return null;
		}

		int size = 1;
		NPCComposition composition = bloatNPC.getTransformedComposition();
		if (composition != null)
		{
			size = composition.getSize();
		}

		LocalPoint lp = null;

		switch (bloatState)
		{
			case 1:
				lp = bloatNPC.getLocalLocation();

				if (lp == null)
				{
					return null;
				}

				return Perspective.getCanvasTileAreaPoly(client, lp, size, true);
			case 2:
			case 3:
				lp = LocalPoint.fromWorld(client, bloatNPC.getWorldLocation());

				if (lp == null)
				{
					return null;
				}

				return Perspective.getCanvasTileAreaPoly(client, lp, size, false);
		}

		return null;
	}

	Color getBloatStateColor()
	{
		Color col = Color.CYAN;
		switch (bloatState)
		{
			case 2:
				col = Color.MAGENTA;
				break;
			case 3:
				col = Color.RED;
				break;
		}
		return col;
	}
}
