package net.runelite.client.plugins.ztob.rooms.Bloat;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.plugins.ztob.RoomOverlay;
import net.runelite.client.plugins.ztob.TheatreConfig;

public class BloatOverlay extends RoomOverlay
{
	@Inject
	private Bloat bloat;

	@Inject
	protected BloatOverlay(TheatreConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (config.bloatIndicator() && bloat.isBloatActive())
        {
            renderPoly(graphics, bloat.getBloatStateColor(), bloat.getBloatTilePoly(), 2);
        }

        return null;
	}
}
