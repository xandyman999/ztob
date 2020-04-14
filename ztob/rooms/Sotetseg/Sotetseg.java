package net.runelite.client.plugins.ztob.rooms.Sotetseg;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.party.messages.TilePing;
import net.runelite.client.plugins.ztob.Room;
import net.runelite.client.plugins.ztob.TheatreConfig;
import net.runelite.client.plugins.ztob.TheatrePlugin;
import net.runelite.client.ui.overlay.infobox.AnimatedInfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import net.runelite.client.ws.PartyService;
import net.runelite.client.ws.WSClient;

@Slf4j
public class Sotetseg extends Room
{
	static final int SOTETSEG_MAGE_ORB = 1606;
	static final int SOTETSEG_RANGE_ORB = 1607;
	static final int SOTETSEG_BIG_AOE_ORB = 1604;
	private static final int GROUNDOBJECT_ID_REDMAZE = 33035;
	private static final int GROUNDOBJECT_ID_BLACKMAZE = 33034;
	private static final int GROUNDOBJECT_ID_GREYMAZE = 33033;
	private static final int OVERWORLD_REGION_ID = 13123;
	private static final int UNDERWORLD_REGION_ID = 13379;
	@Getter
	private static final Point swMazeSquareOverWorld = new Point(9, 22);
	@Getter
	private static final Point swMazeSquareUnderWorld = new Point(42, 31);

	private boolean bigOrbPresent = false;
	private AnimatedInfoBox animatedInfoBox = null;
	private static Clip clip;

	static BufferedImage TACTICAL_NUKE_OVERHEAD;
	private static BufferedImage TACTICAL_NUKE_SHEET;

	@Getter
	private boolean sotetActive;
	private NPC sotetsegNPC;
	private int overWorldRegionID = -1;
	@Getter
	private boolean wasInUnderWorld = false;
	private boolean mazeTrigger = false;
	@Getter
	private LinkedHashSet<Point> redTiles = new LinkedHashSet<>();
	@Getter
	private HashSet<Point> greenTiles = new HashSet<>();

	@Inject
	private Client client;
	@Inject
	private TheatrePlugin plugin; //DO NOT USE just here for the counter constructor
	@Inject
	private InfoBoxManager infoBoxManager;
	@Inject
	private SotetsegOverlay sotetsegOverlay;
	@Inject
	private WSClient wsClient;
	@Inject
	private PartyService party;
	@Inject
	private ScheduledExecutorService executor;

	@Inject
	protected Sotetseg(TheatrePlugin plugin, TheatreConfig config)
	{
		super(plugin, config);
	}

	@Override
	public void init()
	{
		TACTICAL_NUKE_SHEET = ImageUtil.getResourceStreamFromClass(TheatrePlugin.class, "nuke_spritesheet.png");
		TACTICAL_NUKE_OVERHEAD = ImageUtil.getResourceStreamFromClass(TheatrePlugin.class, "Tactical_Nuke_Care_Package_Icon_MW2.png");

		try {
			AudioInputStream stream;
			AudioFormat format;
			DataLine.Info info;

			stream = AudioSystem.getAudioInputStream(new BufferedInputStream(TheatrePlugin.class.getResourceAsStream("mw2_tactical_nuke.wav")));
			format = stream.getFormat();
			info = new DataLine.Info(Clip.class, format);
			clip = (Clip) AudioSystem.getLine(info);
			clip.open(stream);
			FloatControl control = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
			if (control != null)
			{
				control.setValue(20f * (float) Math.log10(config.sotetsetAttacksSoundVolume() / 100.0f));
			}
		}
		catch (Exception e) {
			clip = null;
		}
	}

	@Override
	public void load()
	{
		overlayManager.add(sotetsegOverlay);
	}

	@Override
	public void unload()
	{
		overlayManager.remove(sotetsegOverlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged change)
	{
		if (change.getKey().equals("SotetsegAttacksSoundsVolume"))
		{
			if (clip != null)
			{
				FloatControl control = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
				if (control != null)
				{
					control.setValue(20f * (float) Math.log10(config.sotetsetAttacksSoundVolume() / 100.0f));
				}
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.SOTETSEG:
			case NpcID.SOTETSEG_8388:
				sotetActive = true;
				sotetsegNPC = npc;
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned) {
		NPC npc = npcDespawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.SOTETSEG:
			case NpcID.SOTETSEG_8388:
				if (client.getPlane() != 3)
				{
					sotetActive = false;
					sotetsegNPC = null;
					mazeTrigger = false;
				}
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (sotetActive)
		{
			if (sotetsegNPC != null && sotetsegNPC.getId() == NpcID.SOTETSEG_8388)
			{
				if (!redTiles.isEmpty())
				{
					redTiles.clear();
				}

				if (!greenTiles.isEmpty())
				{
					greenTiles.clear();
				}

				if (isInOverWorld())
				{
					wasInUnderWorld = false;
					overWorldRegionID = client.getLocalPlayer().getWorldLocation().getRegionID();
					mazeTrigger = true;
				}
			}

			if (!redTiles.isEmpty() && wasInUnderWorld && config.sotetsegMazeDiscord())
			{
				for (Point p : redTiles)
				{
					WorldPoint wp = worldPointFromMazePoint(p);
					TilePing tilePing = new TilePing(wp);
					if (party != null && party.getLocalMember() != null)
					{
						tilePing.setMemberId(party.getLocalMember().getMemberId());
						wsClient.send(tilePing);
					}
				}
			}
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (sotetActive)
		{
			GroundObject o = event.getGroundObject();

			if (o.getId() == GROUNDOBJECT_ID_REDMAZE)
			{
				Tile t = event.getTile();
				WorldPoint p = WorldPoint.fromLocal(client, t.getLocalLocation());
				Point point = new Point(p.getRegionX(), p.getRegionY());
				if (isInOverWorld())
				{
					redTiles.add(new Point(point.getX() - swMazeSquareOverWorld.getX(), point.getY() - swMazeSquareOverWorld.getY()));
				}
				if (isInUnderWorld())
				{
					redTiles.add(new Point(point.getX() - swMazeSquareUnderWorld.getX(), point.getY() - swMazeSquareUnderWorld.getY()));
					wasInUnderWorld = true;

					//#COMMUNICATION SET MAZE
					if (MazeCommunication.isMazeComplete(redTiles))
					{
						int[] seed = MazeCommunication.getMazeSeed(redTiles);
						if (seed[0] != 0)
						{
							String name = MazeCommunication.unfuckName(client.getLocalPlayer().getName());
							executor.execute(() ->
							{
								try
								{
									MazeCommunication.setMazeLayourSeedTask(seed, name);
									log.debug("Setting task to seed " + seed[0] + " " + seed[1]);
								}
								catch (Exception ex)
								{
									log.debug("unable to submit maze seed", ex);
								}
							});
						}
						else
						{
							log.debug("Seed for task invalid " + seed[0] + " " + seed[1]);
						}
					}
				}
			}
			//#COMMUNICATION ASK FOR MAZE
			if (o.getId() == GROUNDOBJECT_ID_BLACKMAZE && isInOverWorld() && mazeTrigger && !wasInUnderWorld)
			{
				mazeTrigger = false;
				HashSet<String> raiders = new HashSet<>();
				Map<Integer, Object> varcmap = client.getVarcMap();
				for (int i = 330; i < 335; i++)
				{
					if (varcmap.containsKey(i))
					{
						String name = varcmap.get(i).toString();
						if (name != null && !name.equals(""))
						{
							raiders.add(MazeCommunication.unfuckName(name));
						}
					}
				}

				if (raiders.size() > 0)
				{
					HashSet<String> playerNameSet = new HashSet<>();
					client.getPlayers().forEach(p -> playerNameSet.add(MazeCommunication.unfuckName(p.getName())));
					for (final String s : raiders)
					{
						log.debug(Arrays.toString(playerNameSet.toArray()));
						if (!playerNameSet.contains(s))
						{
							log.debug("Chosen player " + s + " request 1");
							final ScheduledFuture<Boolean> scheduledFuture = executor.schedule(()-> requestMazeLayout(s), 200, TimeUnit.MILLISECONDS);
							executor.schedule(()->
						    {
						    	try
								{
									if (!scheduledFuture.get())
									{
										log.debug("Maze request 2");
										requestMazeLayout(s);
									}
								}
								catch (Exception ex)
								{
									log.debug("Second maze retrieval ran into an error", ex);
								}
							}, 800, TimeUnit.MILLISECONDS);
							break;
						}
					}
				}
			}
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (sotetActive && config.sotetsetAttacks1())
		{
			boolean foundBigOrb = false;
			for (Projectile p : client.getProjectiles())
			{
				if (p.getId() == SOTETSEG_BIG_AOE_ORB)
				{
					foundBigOrb = true;
					if (!bigOrbPresent)
					{
						animatedInfoBox = new AnimatedInfoBox(
							TACTICAL_NUKE_SHEET, plugin, p,
							new Rectangle(32, 32), 32, 5);
						infoBoxManager.addInfoBox(animatedInfoBox);

						if (clip != null && config.sotetsetAttacksSound())
						{
							clip.setFramePosition(0);
							clip.start();
						}
					}
					break;
				}
			}
			bigOrbPresent = foundBigOrb;
		}
		if (!bigOrbPresent)
		{
			infoBoxManager.removeInfoBox(animatedInfoBox);
		}
	}

	WorldPoint worldPointFromMazePoint(Point mazePoint)
	{
		if (overWorldRegionID == -1)
		{
			return WorldPoint.fromRegion(
				client.getLocalPlayer().getWorldLocation().getRegionID(), mazePoint.getX() + Sotetseg.getSwMazeSquareOverWorld().getX(),
				mazePoint.getY() + Sotetseg.getSwMazeSquareOverWorld().getY(), 0);
		}
		return WorldPoint.fromRegion(
			overWorldRegionID, mazePoint.getX() + Sotetseg.getSwMazeSquareOverWorld().getX(),
			mazePoint.getY() + Sotetseg.getSwMazeSquareOverWorld().getY(), 0);
	}

	private boolean isInOverWorld()
	{
		return client.getMapRegions().length > 0 && client.getMapRegions()[0] == OVERWORLD_REGION_ID;
	}

	private boolean isInUnderWorld()
	{
		return client.getMapRegions().length > 0 && client.getMapRegions()[0] == UNDERWORLD_REGION_ID;
	}

	private boolean requestMazeLayout(final String name)
	{
		try
		{
			int seed[] = MazeCommunication.getMazeLayourSeedTask(name);
			HashSet<Point> points = new HashSet<>();
			if (seed != null)
			{
				points = MazeCommunication.pointSetFromSeed(seed);
				greenTiles.addAll(points);
				if (points.size() > 0)
				{
					return true;
				}
			}
			log.debug("Setting maze from " + name + " to " + points);
		}
		catch (IOException ex)
		{
			log.debug("unable to retrieve maze seed for " + name, ex);
		}
		return false;
	}
}
