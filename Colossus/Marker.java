import java.awt.*;

/**
 * Class Marker implements the GUI for a legion marker.
 * @version $Id$
 * @author David Ripton
 */

public final class Marker extends Chit
{
    private Legion legion;
    private static Font font;
    private static Font oldFont;
    private static int fontHeight;
    private static int fontWidth;


    public Marker(int scale, String id, Container container, Legion legion)
    {
        super(scale, id, container);
        this.legion = legion;
        setBackground(Color.black);
    }


    public void setLegion(Legion legion)
    {
        this.legion = legion;
    }


    public Legion getLegion()
    {
        return legion;
    }


    /** Show the height of the legion. */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (legion == null)
        {
            return;
        }

        String height = Integer.toString(legion.getHeight());
        Rectangle rect = getBounds();

        // Construct a font 1.5 times the size of the current font.
        if (font == null)
        {
            oldFont = g.getFont();
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            font = new Font(name, style, (3 * size) >> 1);
            g.setFont(font);
            FontMetrics fontMetrics = g.getFontMetrics();
            // XXX getAscent() seems to return too large a number
            // Test this 80% fudge factor on multiple platforms.
            fontHeight = 4 * fontMetrics.getAscent() / 5;
            fontWidth = fontMetrics.stringWidth(height);
        }
        else
        {
            g.setFont(font);
        }

        int x = rect.x + ((rect.width * 3) >> 2) - (fontWidth >> 1);
        int y = rect.y + rect.height * 2 / 3 + fontHeight / 2;

        // Provide a high-contrast background for the number.
        g.setColor(Color.white);
        g.fillRect(x, y - fontHeight, fontWidth, fontHeight);

        // Show height in black.
        g.setColor(Color.black);
        g.drawString(height, x, y);

        // Restore the font.
        g.setFont(oldFont);

        // Draw a one-pixel-wide black border around the outside
        // edge of the marker.
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }


    public static void main(String [] args)
    {
        Chit.main(args);
    }
}
