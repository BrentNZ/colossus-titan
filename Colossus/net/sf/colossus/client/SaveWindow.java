package net.sf.colossus.client;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import net.sf.colossus.util.Options;


/** 
 *  Saves window position and size.
 *  @version $Id$
 *  @author David Ripton 
 */
public final class SaveWindow
{
    private IOptions options;
    private String name;
    private final Dimension screen =
            Toolkit.getDefaultToolkit().getScreenSize();

    public SaveWindow(IOptions options, String name)
    {
        this.options = options;
        this.name = name;
    }

    public Dimension loadSize()
    {
        int x = options.getIntOption(name + Options.sizeX);
        int y = options.getIntOption(name + Options.sizeY);
        Dimension size = null;
        if (x > 0 && y > 0)
        {
            size = new Dimension(x, y);
        }
        return size;
    }

    public void saveSize(final Dimension size)
    {
        options.setOption(name + Options.sizeX, (int)size.getWidth());
        options.setOption(name + Options.sizeY, (int)size.getHeight());
    }

    public Point loadLocation()
    {
        int x = options.getIntOption(name + Options.locX);
        int y = options.getIntOption(name + Options.locY);
        Point location = null;
        if (x >= 0 && y >= 0 && x < screen.width && y < screen.height)
        {
            location = new Point(x, y);
        }
        return location;
    }

    public void saveLocation(final Point location)
    {
        options.setOption(name + Options.locX, location.x);
        options.setOption(name + Options.locY, location.y);
    }
}

