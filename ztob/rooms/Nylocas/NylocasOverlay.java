package net.runelite.client.plugins.ztob.rooms.Nylocas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.ztob.RoomOverlay;
import net.runelite.client.plugins.ztob.TheatreConfig;
import net.runelite.client.ui.overlay.OverlayUtil;

public class NylocasOverlay extends RoomOverlay
{
	@Inject
	private Nylocas nylocas;

	@Inject
	protected NylocasOverlay(TheatreConfig config)
	{
		super(config);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (nylocas.isNyloActive())
        {
            if (config.nyloPillars())
            {
                Map<NPC, Integer> pillars = nylocas.getNylocasPillars();
                for (NPC npc : pillars.keySet()) {
                    final int health = pillars.get(npc);
                    final String healthStr = String.valueOf(health) + "%";
                    WorldPoint p = npc.getWorldLocation();
                    LocalPoint lp = LocalPoint.fromWorld(client, p.getX() + 1, p.getY() + 1);
                    final double rMod = 130.0 * health / 100.0;
                    final double gMod = 255.0 * health / 100.0;
                    final double bMod = 125.0 * health / 100.0;
                    final Color c = new Color((int) (255 - rMod), (int) (0 + gMod), (int) (0 + bMod));
                    if (lp != null)
                    {
                        Point canvasPoint = Perspective.localToCanvas(client, lp, client.getPlane(),
                                                                      65);
                        renderTextLocation(graphics, healthStr, c, canvasPoint);
                    }
                }
            }

            if (config.nyloBlasts() || config.nyloTimeAlive() || config.getHighlightMageNylo() || config.getHighlightMeleeNylo() || config.getHighlightRangeNylo())
            {
                final Map<NPC, Integer> npcMap = nylocas.getNylocasNpcs();
                for (NPC npc : npcMap.keySet())
                {
                    if (config.nyloAggressiveOverlay() && nylocas.getAggressiveNylocas().contains(npc) && !npc.isDead())
                    {
                        LocalPoint lp = npc.getLocalLocation();
                        if (lp != null)
                        {
                            Polygon poly = Perspective.getCanvasTileAreaPoly(client, lp, npc.getComposition().getSize(), -25);
                            renderPoly(graphics, Color.RED, poly, 1);
                        }
                    }

                    int ticksLeft = npcMap.get(npc);
                    if (ticksLeft > -1)
                    {
                        if (config.nyloTimeAlive() && !npc.isDead())
                        {
                            int ticksAlive = 52 - ticksLeft;
                            Point textLocation = npc.getCanvasTextLocation(graphics, String.valueOf(ticksAlive), 60);
                            if (textLocation != null)
                            {
                                OverlayUtil.renderTextLocation(graphics, textLocation, String.valueOf(ticksAlive), Color.WHITE);
                            }
                        }

                        if (config.nyloBlasts() && ticksLeft <= 6) {
                            LocalPoint lp = npc.getLocalLocation();
                            if (lp != null)
                            {
                                renderPoly(graphics, Color.YELLOW, Perspective.getCanvasTileAreaPoly(client, lp, npc.getComposition().getSize(), -15), 1);
                            }
                        }
                    }

                    String name = npc.getName();

                    if (config.nyloOverlay() && !npc.isDead())
                    {
                        LocalPoint lp = npc.getLocalLocation();
                        if (lp != null)
                        {
                            if (config.getHighlightMeleeNylo() && name.equals("Nylocas Ischyros"))
                            {
                                renderPoly(graphics, new Color(255, 188, 188), Perspective.getCanvasTileAreaPoly(client, lp, npc.getComposition().getSize()), 1);
                            }
                            else if (config.getHighlightRangeNylo() && name.equals("Nylocas Toxobolos"))
                            {
                                renderPoly(graphics, Color.GREEN, Perspective.getCanvasTileAreaPoly(client, lp, npc.getComposition().getSize()), 1);
                            }
                            else if (config.getHighlightMageNylo() && name.equals("Nylocas Hagios"))
                            {
                                renderPoly(graphics, Color.CYAN, Perspective.getCanvasTileAreaPoly(client, lp, npc.getComposition().getSize()), 1);
                            }
                        }
                    }
                }
            }
        }
		return null;
	}
}
