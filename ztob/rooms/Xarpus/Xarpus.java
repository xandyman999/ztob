package net.runelite.client.plugins.ztob.rooms.Xarpus;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.poison.PoisonPlugin;
import net.runelite.client.plugins.ztob.Room;
import net.runelite.client.plugins.ztob.TheatreConfig;
import net.runelite.client.plugins.ztob.TheatrePlugin;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

public class Xarpus extends Room
{
	private static BufferedImage EXHUMED_COUNT_ICON;
	private static final int GROUNDOBJECT_ID_EXHUMED = 32743;

	@Inject
	private XarpusOverlay xarpusOverlay;
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private TheatrePlugin p; //DO NOT USE just here for the counter constructor

	private Counter counter;
	@Getter
	private boolean xarpusActive;
	private boolean xarpusStare;
	@Getter
	private final Map<GroundObject, Integer> xarpusExhumeds = new HashMap<>();
	@Getter
	private int xarpusTicksUntilAttack;
	@Getter
	private NPC xarpusNPC;

	@Inject
	protected Xarpus(TheatrePlugin plugin, TheatreConfig config)
	{
		super(plugin, config);
	}

	@Override
	public void init()
	{
		EXHUMED_COUNT_ICON = ImageUtil.resizeCanvas(ImageUtil.getResourceStreamFromClass(PoisonPlugin.class, "1067-POISON.png"), 26, 26);
	}

	@Override
	public void load()
	{
		overlayManager.add(xarpusOverlay);
	}

	@Override
	public void unload()
	{
		overlayManager.remove(xarpusOverlay);

		infoBoxManager.removeInfoBox(counter);
		counter = null;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.XARPUS:
			case NpcID.XARPUS_8339:
			case NpcID.XARPUS_8340:
			case NpcID.XARPUS_8341:
				xarpusActive = true;
				xarpusNPC = npc;
				xarpusStare = false;
				xarpusTicksUntilAttack = 9;
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned) {
		NPC npc = npcDespawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.XARPUS:
			case NpcID.XARPUS_8339:
			case NpcID.XARPUS_8340:
			case NpcID.XARPUS_8341:
				xarpusActive = false;
				xarpusNPC = null;
				xarpusStare = false;
				xarpusTicksUntilAttack = 9;
				xarpusExhumeds.clear();
				infoBoxManager.removeInfoBox(counter);
				counter = null;
				break;
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (xarpusActive)
		{
			GroundObject o = event.getGroundObject();
			if (o.getId() == GROUNDOBJECT_ID_EXHUMED)
			{
				if (counter == null)
				{
					counter = new Counter(EXHUMED_COUNT_ICON, p, 1);
					infoBoxManager.addInfoBox(counter);
				}
				else
				{
					counter.setCount(counter.getCount() + 1);
				}

				xarpusExhumeds.put(o, 11);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (xarpusActive)
		{
			for (Iterator<GroundObject> it = xarpusExhumeds.keySet().iterator(); it.hasNext();)
			{
				GroundObject key = it.next();
				xarpusExhumeds.replace(key, xarpusExhumeds.get(key) - 1);
				if (xarpusExhumeds.get(key) < 0)
				{
					it.remove();
				}
			}
			if (xarpusNPC.getOverheadText() != null && !xarpusStare)
			{
				xarpusStare = true;
				xarpusTicksUntilAttack = 9;
			}
			if (xarpusStare)
			{
				xarpusTicksUntilAttack--;
				if (xarpusTicksUntilAttack <= 0)
				{
					xarpusTicksUntilAttack = 8;
				}
			}
			else if (xarpusNPC.getId() == NpcID.XARPUS_8340)
			{
				xarpusTicksUntilAttack--;

				if (xarpusTicksUntilAttack <= 0)
				{
					xarpusTicksUntilAttack = 4;
				}
			}
		}
	}
}
