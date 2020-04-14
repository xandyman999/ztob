package net.runelite.client.plugins.ztob.rooms.Verzik;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.Getter;
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
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class Verzik extends Room
{
	enum Phase
	{
		PHASE1,
		PHASE2,
		PHASE3
	}

	enum SpecialAttack
	{
		WEB_COOLDOWN,
		WEBS,
		YELLOWS,
		GREEN,
		NONE
	}

	private static final int NPC_ID_TORNADO = 8386;
	private static final int VERZIK_P1_MAGIC = 8109;
	private static final int VERZIK_P2_REG = 8114;
	private static final int VERZIK_P2_BOUNCE = 8116;
	private static final int VERZIK_ORGASM = 8117;
	private static final int p3_crab_attack_count = 5;
	private static final int p3_web_attack_count = 10;
	private static final int p3_yellow_attack_count = 15;
	private static final int p3_green_attack_count = 20;

	@Inject
	private VerzikOverlay verzikOverlay;

	@Getter
	private NPC verzikNPC;
	@Getter
	private boolean verzikActive;

	private List<NPC> verzikTornados = new ArrayList<>();
	@Getter
	private List<WorldPoint> verzikTornadoLocations = new ArrayList<>();
	@Getter
	private List<WorldPoint> verzikTornadoTrailingLocations = new ArrayList<>();
	// npc, (hpRatio, hp)

	@Getter
	private Map<NPC, Pair<Integer, Integer>> verzikReds = new HashMap<>();
	@Getter
	private HashSet<NPC> verzikAggros = new HashSet<>();

	@Getter
	private int verzikTicksUntilAttack = -1;
	@Getter
	private int verzikTotalTicksUntilAttack = 0;

	@Getter
	private boolean verzikEnraged = false;
	private boolean verzikFirstEnraged = false;
	@Getter
	private int verzikAttackCount;
	private Phase verzikPhase;

	private boolean verzikTickPaused = true;
	@Getter
	private SpecialAttack verzikSpecial = SpecialAttack.NONE;
	private int verzikLastAnimation = -1;

	@Inject
	private Verzik(TheatrePlugin plugin, TheatreConfig config)
	{
		super(plugin, config);
	}

	@Override
	public void load()
	{
		overlayManager.add(verzikOverlay);
	}

	@Override
	public void unload()
	{
		overlayManager.remove(verzikOverlay);
		verzikCleanup();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.WEB:
				if (verzikNPC != null && verzikNPC.getInteracting() == null)
				{
					verzikSpecial = SpecialAttack.WEBS;
				}
				break;
			case NpcID.NYLOCAS_ISCHYROS_8381:
			case NpcID.NYLOCAS_HAGIOS_8383:
			case NpcID.NYLOCAS_TOXOBOLOS_8382:
				verzikAggros.add(npc);
				break;
			case NpcID.NYLOCAS_MATOMENOS_8385:
				verzikReds.putIfAbsent(npc, new MutablePair<>(npc.getHealthRatio(), npc.getHealth()));
				break;
			case NPC_ID_TORNADO:
				verzikTornados.add(npc);
				if (!verzikEnraged)
				{
					verzikEnraged = true;
					verzikFirstEnraged = true;
				}
				break;
			case NpcID.VERZIK_VITUR_8369:
				verzikSpawn(npc);
				break;
			case NpcID.VERZIK_VITUR_8370:
				verzikPhase = Phase.PHASE1;
				verzikSpawn(npc);
				break;
			case NpcID.VERZIK_VITUR_8371:
				verzikSpawn(npc);
				break;
			case NpcID.VERZIK_VITUR_8372:
				verzikPhase = Phase.PHASE2;
				verzikSpawn(npc);
				break;
			case NpcID.VERZIK_VITUR_8373:
				verzikSpawn(npc);
				break;
			case NpcID.VERZIK_VITUR_8374:
				verzikPhase = Phase.PHASE3;
				verzikSpawn(npc);
				break;
			case NpcID.VERZIK_VITUR_8375:
				verzikSpawn(npc);
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		switch (npc.getId()) {
			case NpcID.NYLOCAS_ISCHYROS_8381:
			case NpcID.NYLOCAS_HAGIOS_8383:
			case NpcID.NYLOCAS_TOXOBOLOS_8382:
				verzikAggros.remove(npc);
				break;
			case NpcID.NYLOCAS_MATOMENOS_8385:
				verzikReds.remove(npc);
				break;
			case NPC_ID_TORNADO:
				verzikTornados.remove(npc);
				break;
			case NpcID.VERZIK_VITUR_8369:
				verzikCleanup();
				break;
			case NpcID.VERZIK_VITUR_8370:
				verzikCleanup();
				break;
			case NpcID.VERZIK_VITUR_8371:
				verzikCleanup();
				break;
			case NpcID.VERZIK_VITUR_8372:
				verzikCleanup();
				break;
			case NpcID.VERZIK_VITUR_8373:
				verzikCleanup();
				break;
			case NpcID.VERZIK_VITUR_8374:
				verzikCleanup();
				break;
			case NpcID.VERZIK_VITUR_8375:
				verzikCleanup();
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick eventuld )
	{
		if (verzikActive)
		{
			verzikTornadoTrailingLocations.clear();
			verzikTornadoTrailingLocations.addAll(verzikTornadoLocations);
			verzikTornadoLocations.clear();
			for (NPC nado : verzikTornados)
			{
				verzikTornadoLocations.add(nado.getWorldLocation());
			}

			Function<Integer, Integer> adjust_for_enrage = i -> isVerzikEnraged() ? i - 2 : i;

			if (verzikTickPaused)
			{
				switch (verzikNPC.getId())
				{
					case NpcID.VERZIK_VITUR_8370:
						verzikPhase = Phase.PHASE1;
						verzikAttackCount = 0;
						verzikTicksUntilAttack = 18;
						verzikTickPaused = false;
						break;
					case NpcID.VERZIK_VITUR_8372:
						verzikPhase = Phase.PHASE2;
						verzikAttackCount = 0;
						verzikTicksUntilAttack = 3;
						verzikTickPaused = false;
						break;
					case NpcID.VERZIK_VITUR_8374:
						verzikPhase = Phase.PHASE3;
						verzikAttackCount = 0;
						verzikTicksUntilAttack = 6;
						verzikTickPaused = false;
						break;
				}
			}
			else if (verzikSpecial == SpecialAttack.WEBS)
			{
				verzikTotalTicksUntilAttack++;

				if (verzikNPC.getInteracting() != null)
				{
					verzikSpecial = SpecialAttack.WEB_COOLDOWN;
					verzikAttackCount = 10;
					verzikTicksUntilAttack = 10;
					verzikFirstEnraged = false;
				}
			}
			else
			{
				verzikTicksUntilAttack = Math.max(0, verzikTicksUntilAttack - 1);
				verzikTotalTicksUntilAttack++;

				int animationID = verzikNPC.getAnimation();

				if (animationID > -1 && verzikPhase == Phase.PHASE1 && verzikTicksUntilAttack < 5 && animationID != verzikLastAnimation)
				{
					if (animationID == VERZIK_P1_MAGIC)
					{
						verzikTicksUntilAttack = 14;
						verzikAttackCount++;
					}
				}

				if (animationID > -1 && verzikPhase == Phase.PHASE2 && verzikTicksUntilAttack < 3 && animationID != verzikLastAnimation)
				{
					switch (animationID)
					{
						case VERZIK_P2_REG:
						case VERZIK_P2_BOUNCE:
							verzikTicksUntilAttack = 4;
							verzikAttackCount++;
							if (verzikAttackCount == 7)
							{
								verzikTicksUntilAttack = 8;
							}
							break;
						case VERZIK_ORGASM:
							verzikAttackCount = 0;
							verzikTicksUntilAttack = 12;
							break;
					}
				}

				verzikLastAnimation = animationID;

				if (verzikPhase == Phase.PHASE3)
				{
					verzikAttackCount = verzikAttackCount % p3_green_attack_count;

					if (verzikTicksUntilAttack <= 0)
					{
						verzikAttackCount++;

						// first 9 including crabs
						if (verzikAttackCount < p3_web_attack_count)
						{
							verzikSpecial = SpecialAttack.NONE;
							verzikTicksUntilAttack = adjust_for_enrage.apply(7);
						}
						// between webs and yellows
						else if (verzikAttackCount < p3_yellow_attack_count)
						{
							verzikSpecial = SpecialAttack.NONE;
							verzikTicksUntilAttack = adjust_for_enrage.apply(7);
						}
						// yellow cant attack
						else if (verzikAttackCount < p3_yellow_attack_count + 1)
						{
							verzikSpecial = SpecialAttack.YELLOWS;
							verzikTicksUntilAttack = 14 + 7;
						}
						// between yellow and green
						else if (verzikAttackCount < p3_green_attack_count)
						{
							verzikSpecial = SpecialAttack.NONE;
							verzikTicksUntilAttack = adjust_for_enrage.apply(7);
						}
						// ready for green
						else if (verzikAttackCount < p3_green_attack_count + 1)
						{
							verzikSpecial = SpecialAttack.GREEN;
							// 12 during purps?
							verzikTicksUntilAttack = 12;
						}
						else
						{
							verzikSpecial = SpecialAttack.NONE;
							verzikTicksUntilAttack = adjust_for_enrage.apply(7);
						}
					}

					if (verzikFirstEnraged)
					{
						verzikFirstEnraged = false;
						if (verzikSpecial != SpecialAttack.YELLOWS || verzikTicksUntilAttack <= 7)
						{
							verzikTicksUntilAttack = 5;
						}
					}
				}
			}
		}
	}

	Color verzikSpecialWarningColor()
	{
		Color col = Color.WHITE;
		if (verzikPhase != Phase.PHASE3)
		{
			return col;
		}
		switch (verzikAttackCount)
		{
			case Verzik.p3_crab_attack_count - 1:
				col = Color.MAGENTA;
				break;
			case Verzik.p3_web_attack_count - 1:
				col = Color.ORANGE;
				break;
			case Verzik.p3_yellow_attack_count - 1:
				col = Color.YELLOW;
				break;
			case Verzik.p3_green_attack_count - 1:
				col = Color.GREEN;
				break;
		}
		return col;
	}

	private void verzikSpawn(NPC npc)
	{
		verzikEnraged = false;
		verzikFirstEnraged = false;
		verzikTicksUntilAttack = 0;
		verzikAttackCount = 0;
		verzikNPC = npc;
		verzikActive = true;
		verzikTickPaused = true;
		verzikSpecial = SpecialAttack.NONE;
		verzikTotalTicksUntilAttack = 0;
		verzikLastAnimation = -1;
	}

	private void verzikCleanup()
	{
		verzikAggros.clear();
		verzikReds.clear();
		verzikTornadoLocations.clear();
		verzikTornadoTrailingLocations.clear();
		verzikEnraged = false;
		verzikFirstEnraged = false;
		verzikActive = false;
		verzikTornados.clear();
		verzikNPC = null;
		verzikPhase = null;
		verzikTickPaused = true;
		verzikSpecial = SpecialAttack.NONE;
		verzikTotalTicksUntilAttack = 0;
		verzikLastAnimation = -1;
	}
}
