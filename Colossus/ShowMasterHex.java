import java.awt.*;
import java.awt.event.*;

/**
 * Class ShowMasterHex displays the terrain type and recruits for a MasterHex.
 * @version $Id$
 * @author David Ripton
 */

class ShowMasterHex extends Dialog implements MouseListener, WindowListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private MasterHex hex;
    private Chit [] chits;
    private int numChits;
    private int scale = 60;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    public ShowMasterHex(Frame parentFrame, MasterHex hex, Point point)
    {
        super(parentFrame, hex.getTerrainName() + " Hex " + hex.getLabel(),
            false);

        this.hex = hex;
        numChits = hex.getNumRecruitTypes();

        pack();
        setBackground(Color.lightGray);
        setResizable(false);
        addWindowListener(this);

        // Place dialog relative to parentFrame's origin, and fully on-screen.
        Point parentOrigin = parentFrame.getLocation();
        Point origin = new Point(point.x + parentOrigin.x - scale, point.y +
            parentOrigin.y - scale);
        if (origin.x < 0)
        {
            origin.x = 0;
        }
        if (origin.y < 0)
        {
            origin.y = 0;
        }
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int adj = origin.x + getSize().width - d.width;
        if (adj > 0)
        {
            origin.x -= adj;
        }
        adj = origin.y + getSize().height - d.height;
        if (adj > 0)
        {
            origin.y -= adj;
        }
        setLocation(origin);

        setLayout(new GridLayout(0, 3));
        
        chits = new Chit[numChits];
        for (int i = 0; i < numChits; i++)
        {
            Creature creature = hex.getRecruit(i);

            chits[i] = new Chit(scale, creature.getImageName(), this);
            add(chits[i]);
            chits[i].addMouseListener(this);

            int numToRecruit = hex.getNumToRecruit(i);
            Label numToRecruitLabel = new Label("", Label.CENTER);
            if (numToRecruit > 0)
            {
                numToRecruitLabel.setText(Integer.toString(numToRecruit));
            }
            add(numToRecruitLabel);
            numToRecruitLabel.addMouseListener(this);

            int count = creature.getCount();
            Label countLabel = new Label(Integer.toString(count), 
                Label.CENTER);
            add(countLabel);
            countLabel.addMouseListener(this);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < numChits; i++)
        {
            tracker.addImage(chits[i].getImage(), 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(parentFrame, e.toString() +
                "waitForAll was interrupted");
        }
        imagesLoaded = true;

        pack();
        
        addMouseListener(this);

        setVisible(true);
        repaint();
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mousePressed(MouseEvent e)
    {
        dispose();
    }


    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
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
}
