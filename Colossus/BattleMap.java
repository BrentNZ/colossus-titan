import java.awt.*;
import java.awt.event.*;

/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleMap extends Frame implements MouseListener,
    MouseMotionListener, WindowListener, AdjustmentListener,
    ActionListener
{
    public static final double SQRT3 = Math.sqrt(3.0);
    private Hex[][] h = new Hex[6][6];
    private Chit[] chits = new Chit[24];
    private int tracking;
    final private boolean[][] show =
    {
        {false,false,true,true,true,false},
        {false,true,true,true,true,false},
        {false,true,true,true,true,true},
        {true,true,true,true,true,true},
        {false,true,true,true,true,true},
        {false,true,true,true,true,false}
    };
    private Rectangle rectClip = new Rectangle();
    private Image offImage;
    private Graphics gBack;
    private Dimension offDimension;
    private boolean needToClear;
    private MediaTracker tracker;
    private boolean imagesLoaded;

    private int scale;
    private int cx;
    private int cy;
    Dimension preferredSize;
    Scrollbar vert;
    Scrollbar horz; 
    int tx = 0;
    int ty = 0;
    MenuBar mb;
    Menu m1;
    MenuItem mi1_1, mi1_2;


    public BattleMap()
    {
        super("BattleMap");

        scale = 25;
        cx = 3 * scale;
        cy = 3 * scale;
        preferredSize = new Dimension(28 * scale, 28 * scale);
        
        setSize(preferredSize);
        setBackground(java.awt.Color.white);
        setVisible(true);
        addWindowListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        
        tracking = -1;
        needToClear = false;
        imagesLoaded = false;

        horz = new Scrollbar(Scrollbar.HORIZONTAL);
        vert = new Scrollbar(Scrollbar.VERTICAL);
        add("East", vert);
        add("South", horz);
        
        mb = new MenuBar();
        m1 = new Menu("Size", false);
        mb.add(m1);
        mi1_1 = new MenuItem("Shrink");
        m1.add(mi1_1);
        mi1_2 = new MenuItem("Grow");
        m1.add(mi1_2);
        setMenuBar(mb);
        mi1_1.addActionListener(this);
        mi1_2.addActionListener(this);
        vert.addAdjustmentListener(this);
        horz.addAdjustmentListener(this);

        pack();
        validate();

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j] = new Hex
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + i % 2) *
                        SQRT3 * scale), scale);
                }
            }
        }

        tracker = new MediaTracker(this);

        chits[0] = new Chit(100, 100, 60, "images/Angel.gif", this);
        chits[1] = new Chit(120, 120, 60, "images/Archangel.gif", this);
        chits[2] = new Chit(140, 140, 60, "images/Behemoth.gif", this);
        chits[3] = new Chit(160, 160, 60, "images/Centaur.gif", this);
        chits[4] = new Chit(180, 180, 60, "images/Colossus.gif", this);
        chits[5] = new Chit(200, 200, 60, "images/Cyclops.gif", this);
        chits[6] = new Chit(220, 220, 60, "images/Gargoyle.gif", this);
        chits[7] = new Chit(240, 240, 60, "images/Giant.gif", this);
        chits[8] = new Chit(260, 260, 60, "images/Gorgon.gif", this);
        chits[9] = new Chit(280, 280, 60, "images/Griffon.gif", this);
        chits[10] = new Chit(300, 300, 60, "images/Guardian.gif", this);
        chits[11] = new Chit(320, 320, 60, "images/Hydra.gif", this);
        chits[12] = new Chit(340, 340, 60, "images/Lion.gif", this);
        chits[13] = new Chit(360, 360, 60, "images/Minotaur.gif", this);
        chits[14] = new Chit(380, 380, 60, "images/Ogre.gif", this);
        chits[15] = new Chit(400, 400, 60, "images/Ranger.gif", this);
        chits[16] = new Chit(420, 420, 60, "images/Serpent.gif", this);
        chits[17] = new Chit(440, 440, 60, "images/Titan.gif", this);
        chits[18] = new Chit(460, 460, 60, "images/Troll.gif", this);
        chits[19] = new Chit(480, 480, 60, "images/Unicorn.gif", this);
        chits[20] = new Chit(500, 500, 60, "images/Warbear.gif", this);
        chits[21] = new Chit(520, 520, 60, "images/Warlock.gif", this);
        chits[22] = new Chit(540, 540, 60, "images/Dragon.gif", this);
        chits[23] = new Chit(560, 560, 60, "images/Wyvern.gif", this);

        for (int i = 0; i < chits.length; i++)
        {
            tracker.addImage(chits[i].image, 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            System.out.println("waitForAll was interrupted");
        }
        imagesLoaded = true;

        repaint();
    }


    void rescale(int scale)
    {
        this.scale = scale;
        cx = 3 * scale;
        cy = 3 * scale; 
        preferredSize.width = 28 * scale;
        preferredSize.height = 28 * scale;
        setSize(preferredSize);
        
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    h[i][j].rescale
                        ((int) Math.round(cx + 3 * i * scale),
                        (int) Math.round(cy + (2 * j + i % 2) *
                        SQRT3 * scale), scale);
                }
            }
        }

        validate();
        repaint();
    }


    public void mouseDragged(MouseEvent e)
    {
        if (tracking != -1)
        {
            Point point = e.getPoint();
            point.x += tx;
            point.y += ty;

            Rectangle clip = new Rectangle(chits[tracking].getBounds());
            chits[tracking].setLocation(point);
            clip.add(chits[tracking].getBounds());
            needToClear = true;
            repaint(clip.x - tx, clip.y - ty, clip.width, clip.height);
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        tracking = -1;
    }
    
    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        point.x += tx;
        point.y += ty;

        for (int i=0; i < chits.length; i++)
        {
            if (chits[i].select(point))
            {
                tracking = 0;

                // Don't swap if it's already on top.
                if (i != 0)
                {
                    Chit tmpchit = chits[i];
                    for (int j = i; j > 0; j--)
                    {
                        chits[j] = chits[j - 1];
                    }
                    chits[0] = tmpchit;
                    Rectangle clip = new Rectangle(chits[0].getBounds());
                    repaint(clip.x - tx, clip.y - ty, clip.width, clip.height);
                }
                return;
            }
        }

        // No hits on chits, so check map.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && h[i][j].select(point))
                {
                    Rectangle clip = new Rectangle(h[i][j].getBounds());
                    repaint(clip.x - tx, clip.y - ty, clip.width, clip.height);
                    return;
                }
            }
        }
    }

    public void mouseMoved(MouseEvent e)
    {
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        System.exit(0);
    }

    public void windowDeactivated(WindowEvent e)
    {
    }
    
    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }


    public void adjustmentValueChanged(AdjustmentEvent e)
    {
        // Horizontal or vertical?
        if (e.getAdjustable() == horz)
        {
            tx = e.getValue();
        }
        else
        {
            ty = e.getValue();
        }

        repaint();
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Shrink")
        {
            rescale(scale - 1);
        }
        else  // "Grow"
        {
            rescale(scale + 1);            
        }
    }


    public void paint(Graphics g)
    {
        g.translate(-tx, -ty);

        if (!imagesLoaded)
        {
            return;
        }

        rectClip = g.getClipBounds();

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(g);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = chits.length - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                chits[i].paint(g);
            }
        }
    }

    public void update(Graphics g)
    {
        g.translate(-tx, -ty);

        Dimension d = getSize();
        rectClip = g.getClipBounds();
        
        // Create the back buffer only if we don't have a good one.
        if (gBack == null || d.width != offDimension.width || 
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(d.width, d.height);
            gBack = offImage.getGraphics();
        }

        // Clear the background only when chits are dragged.
        if (needToClear)
        {
            gBack.setColor(getBackground());
            gBack.fillRect(rectClip.x, rectClip.y, rectClip.width, 
                rectClip.height);
            gBack.setColor(getForeground());
            needToClear = false;
        }

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j] && rectClip.intersects(h[i][j].getBounds()))
                {
                    h[i][j].paint(gBack);
                }
            }
        }

        // Draw chits from back to front.
        for (int i = chits.length - 1; i >= 0; i--)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                chits[i].paint(gBack);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }

    
    public Dimension getMinimumSize()
    {
        return new Dimension(100, 100);
    }

    public Dimension getPreferredSize()
    {
        return preferredSize;
    }


    public static void main(String args[])
    {
        BattleMap battlemap = new BattleMap();
    }


}


/**
 * Class Hex describes one Battlemap hex.
 * @version $Id$
 * @author David Ripton
 */

class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    private boolean selected;
    private int[] xVertex = new int[6];
    private int[] yVertex = new int[6];
    private Polygon p;
    private Rectangle rectBound;


    Hex(int cx, int cy, int scale)
    {
        selected = false;

        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[4] = cx;
        yVertex[4] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[5] = cx - 1 * scale;
        yVertex[5] = cy + (int) Math.round(SQRT3 * scale);

        p = new Polygon(xVertex, yVertex, 6);
        // Add 1 to width and height because Java rectangles come up
        // one pixel short.
        rectBound = new Rectangle(xVertex[5], yVertex[0], xVertex[2] - 
                        xVertex[5] + 1, yVertex[3] - yVertex[0] + 1);
    }

    
    void rescale(int cx, int cy, int scale)
    {
        xVertex[0] = cx;
        yVertex[0] = cy;
        xVertex[1] = cx + 2 * scale;
        yVertex[1] = cy;
        xVertex[2] = cx + 3 * scale;
        yVertex[2] = cy + (int) Math.round(SQRT3 * scale);
        xVertex[3] = cx + 2 * scale;
        yVertex[3] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[4] = cx;
        yVertex[4] = cy + (int) Math.round(2 * SQRT3 * scale);
        xVertex[5] = cx - 1 * scale;
        yVertex[5] = cy + (int) Math.round(SQRT3 * scale);

        p.xpoints = xVertex;
        p.ypoints = yVertex;

        // Add 1 to width and height because Java rectangles come up
        // one pixel short.
        rectBound.x =  xVertex[5];
        rectBound.y =  yVertex[0];
        rectBound.width = xVertex[2] - xVertex[5] + 1;
        rectBound.height = yVertex[3] - yVertex[0] + 1;
    }


    public void paint(Graphics g)
    {
        if (selected)
        {
            g.setColor(java.awt.Color.red);
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
        else
        {
            g.setColor(java.awt.Color.white);
            g.fillPolygon(p);
            g.setColor(java.awt.Color.black);
            g.drawPolygon(p);
        }
    }


    boolean select(Point point)
    {
        if (p.contains(point))
        {
            selected = !selected;
            return true;
        }
        return false;
    }


    public Rectangle getBounds()
    {
        return rectBound;
    }

    public boolean contains(Point point)
    {
        return (p.contains(point));
    }
    
}
