package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class Marker implements the GUI for a legion marker.
 * @version $Id$
 * @author David Ripton
 */

final class Marker extends Chit
{
    private static final Logger LOGGER =
        Logger.getLogger(Marker.class.getName());
    
    private Font font;
    private int fontHeight;
    private int fontWidth;

    /** Construct a marker without a client.
        Use this constructor as a bit of documentation when
        explicitly not wanting a height drawn on the Marker. */
    Marker(int scale, String id)
    {
        this(scale, id, null);
    }

    /** Construct a marker with a client.
        By providing a client, a Marker will be adorned
        with the height of the stack, when that height
        is non-zero. A null client will prevent the height
        from being displayed. Sometimes (on the master board,
        for example) heights should be shown, and sometimes
        (in the engagement window, for example) they should
        be omitted. */
    Marker(int scale, String id, Client client)
    {
        super(scale, id);
        setBackground(Color.BLACK);
        this.client = client;

        if (getId().startsWith("Bk"))
        {
            setBorderColor(Color.white);
        }
    }

    /** Show the height of the legion. */
    public void paintComponent(Graphics g)
    {
        LOGGER.log(Level.FINEST, "Painting marker");
        super.paintComponent(g);

        if (client == null)
        {
            return; //no height labels wanted
        }

        int legionHeight = client.getLegionHeight(getId());
        if (legionHeight == 0)
        {
            return; //don't put a zero on top in the picker
        }
        String legionHeightString = Integer.toString(legionHeight);
        LOGGER.log(Level.FINEST, "Height is " + legionHeightString);

        // Construct a font 1.5 times the size of the current font.
        Font oldFont = g.getFont();
        if (font == null)
        {
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            font = new Font(name, style, 3 * size / 2);
            g.setFont(font);
            FontMetrics fontMetrics = g.getFontMetrics();
            // XXX getAscent() seems to return too large a number
            // Test this 80% fudge factor on multiple platforms.
            fontHeight = 4 * fontMetrics.getAscent() / 5;
            fontWidth = fontMetrics.stringWidth(legionHeightString);
            if(LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "New font set: " + font);
                LOGGER.log(Level.FINEST, "New font height: " + fontHeight);
                LOGGER.log(Level.FINEST, "New font width: " + fontWidth);
            }
        }
        else
        {
            g.setFont(font);
        }

        if(LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Our rectangle is: " + rect);
        }
        int x = rect.x + rect.width * 3 / 4 - fontWidth / 2;
        int y = rect.y + rect.height * 2 / 3 + fontHeight / 2;

        // Provide a high-contrast background for the number.
        g.setColor(Color.white);
        g.fillRect(x, y - fontHeight, fontWidth, fontHeight);

        // Show height in black.
        g.setColor(Color.black);
        g.drawString(legionHeightString, x, y);

        // Restore the font.
        g.setFont(oldFont);
    }
}