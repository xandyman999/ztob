package net.runelite.client.plugins.ztob.rooms.Sotetseg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.ztob.RoomOverlay;
import net.runelite.client.plugins.ztob.TheatreConfig;
import net.runelite.client.ui.overlay.OverlayUtil;

public class SotetsegOverlay extends RoomOverlay
{
	@Inject
	private Sotetseg sotetseg;
	@Inject
	private SkillIconManager iconManager;

	@Inject
	protected SotetsegOverlay(TheatreConfig config)
	{
		super(config);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (sotetseg.isSotetActive())
        {
            if (config.sotetsegMaze())
            {
            	int counter = 1;
                for (Point p : sotetseg.getRedTiles())
				{
					WorldPoint wp = sotetseg.worldPointFromMazePoint(p);
					drawTile(graphics, wp, Color.WHITE, 1, 255, 0);
					LocalPoint lp = LocalPoint.fromWorld(client, wp);
					if (lp != null && !sotetseg.isWasInUnderWorld())
					{
						Point textPoint = Perspective.getCanvasTextLocation(client, graphics, lp, String.valueOf(counter), 0);
						if (textPoint != null)
						{
							OverlayUtil.renderTextLocation(graphics, textPoint, String.valueOf(counter), Color.WHITE);
						}
					}
					counter++;
				}

				for (Point p : sotetseg.getGreenTiles())
				{
					WorldPoint wp = sotetseg.worldPointFromMazePoint(p);
					drawTile(graphics, wp, Color.GREEN, 1, 255, 0);
				}
            }

			if (config.sotetsetAttacks() || config.sotetsetAttacks1())
			{
				for (Projectile p : client.getProjectiles())
				{
					int id = p.getId();
					int x = (int)p.getX();
					int y = (int)p.getY();
					int z = (int)p.getZ();
					Point point = Perspective.localToCanvas(
						client, new LocalPoint(x, y), 0,
						Perspective.getTileHeight(client, new LocalPoint(x, y), p.getFloor()) - z);
					if (point == null)
					{
						continue;
					}
					if (id == Sotetseg.SOTETSEG_MAGE_ORB && config.sotetsetAttacks())
					{
						BufferedImage icon = iconManager.getSkillImage(Skill.MAGIC);
						point = new Point(point.getX() - icon.getWidth() / 2, point.getY() - 30);
						OverlayUtil.renderImageLocation(graphics, point, icon);
					}
					if (id == Sotetseg.SOTETSEG_RANGE_ORB && config.sotetsetAttacks())
					{
						BufferedImage icon = iconManager.getSkillImage(Skill.RANGED);
						point = new Point(point.getX() - icon.getWidth() / 2, point.getY() - 30);
						OverlayUtil.renderImageLocation(graphics, point, icon);
					}
					if (id == Sotetseg.SOTETSEG_BIG_AOE_ORB && config.sotetsetAttacks1())
					{
						point = new Point(point.getX() - Sotetseg.TACTICAL_NUKE_OVERHEAD.getWidth() / 2, point.getY() - 60);
						OverlayUtil.renderImageLocation(graphics, point, Sotetseg.TACTICAL_NUKE_OVERHEAD);
					}
				}
			}
        }
		return null;
	}
}
