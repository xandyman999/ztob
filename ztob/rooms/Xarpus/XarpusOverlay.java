package net.runelite.client.plugins.ztob.rooms.Xarpus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Point;
import net.runelite.client.plugins.ztob.RoomOverlay;
import net.runelite.client.plugins.ztob.TheatreConfig;

public class XarpusOverlay extends RoomOverlay
{
	@Inject
	private Xarpus xarpus;

	@Inject
	protected XarpusOverlay(TheatreConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (xarpus.isXarpusActive())
        {
            NPC boss = xarpus.getXarpusNPC();

            if ((config.xarpusTick1() && boss.getId() == NpcID.XARPUS_8340)
				|| (config.xarpusTick2() && boss.getId() == NpcID.XARPUS_8341))
            {
                int tick = xarpus.getXarpusTicksUntilAttack();
                final String ticksLeftStr = String.valueOf(tick);
                Point canvasPoint = boss.getCanvasTextLocation(graphics, ticksLeftStr, 130);
                renderTextLocation(graphics, ticksLeftStr, Color.WHITE, canvasPoint);
            }

            if (config.xarpusExhumed() && boss.getId() == NpcID.XARPUS_8339)
            {
                for (GroundObject o : xarpus.getXarpusExhumeds().keySet())
                {
                    Polygon poly = o.getCanvasTilePoly();
                    if (poly != null)
                    {
                        graphics.setColor(new Color(0, 255, 0, 130));
                        graphics.setStroke(new BasicStroke(1));
                        graphics.draw(poly);
                    }
                }
            }
        }
		return null;
	}
}
