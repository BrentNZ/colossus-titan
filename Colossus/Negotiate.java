import java.awt.*;
import java.awt.event.*;

/**
 * Class Negotiate allows two players to settle an engagement.
 * @version $Id$
 * author David Ripton
 */

class Negotiate extends Dialog implements MouseListener, ActionListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Legion attacker;
    private Legion defender;
    private Chit [] attackerChits;
    private Chit [] defenderChits;
    private Chit attackerMarker;
    private Chit defenderMarker;
    private static final int scale = 60;
    private Frame parentFrame;
    private Button button1;
    private Button button2;
    private boolean laidOut = false;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    Negotiate(Frame parentFrame, Legion attacker, Legion defender)
    {
        super(parentFrame, attacker.getMarkerId() + " Negotiates with " +
            defender.getMarkerId(), true);

        setLayout(null);

        this.attacker = attacker;
        this.defender = defender;
        this.parentFrame = parentFrame;

        pack();
        setBackground(Color.lightGray);
        setSize(getPreferredSize());
        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        Point location = Concede.returnLocation();
        if (location == null)
        {
            location = new Point(d.width / 2 - getSize().width / 2, d.height / 2
                - getSize().height / 2);
        }
        setLocation(location);

        addMouseListener(this);

        attackerChits = new Chit[attacker.getHeight()];
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            attackerChits[i] = new Chit((i + 1) * scale + (scale / 5),
                scale / 2, scale, attacker.getCritter(i).getImageName(),
                this);
        }

        defenderChits = new Chit[defender.getHeight()];
        for (int i = 0; i < defender.getHeight(); i++)
        {
            defenderChits[i] = new Chit((i + 1) * scale + (scale / 5),
                2 * scale, scale, defender.getCritter(i).getImageName(),
                this);
        }

        attackerMarker = new Marker(scale / 5, scale / 2, scale,
            attacker.getImageName(), this, attacker);

        defenderMarker = new Marker(scale / 5, 2 * scale, scale,
            defender.getImageName(), this, defender);

        tracker = new MediaTracker(this);

        for (int i = 0; i < attacker.getHeight(); i++)
        {
            tracker.addImage(attackerChits[i].getImage(), 0);
        }
        for (int i = 0; i < defender.getHeight(); i++)
        {
            tracker.addImage(defenderChits[i].getImage(), 0);
        }
        tracker.addImage(attackerMarker.getImage(), 0);
        tracker.addImage(defenderMarker.getImage(), 0);

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(parentFrame, "waitForAll was interrupted");
        }
        imagesLoaded = true;

        button1 = new Button("Agree");
        button2 = new Button("Fight");
        add(button1);
        add(button2);
        button1.addActionListener(this);
        button2.addActionListener(this);

        setVisible(true);
        repaint();
    }
    
    
    public void cleanup()
    {
        Concede.saveLocation(getLocation());
        dispose();
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();
        Rectangle rectClip = g.getClipBounds();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        attackerMarker.paint(offGraphics);
        defenderMarker.paint(offGraphics);

        for (int i = 0; i < attacker.getHeight(); i++)
        {
            attackerChits[i].paint(offGraphics);
        }
        for (int i = 0; i < defender.getHeight(); i++)
        {
            defenderChits[i].paint(offGraphics);
        }

        if (!laidOut)
        {
            Insets insets = getInsets();
            button1.setBounds(insets.left + d.width / 9, 13 * d.height / 16 -
                insets.bottom, d.width / 3, d.height / 8);
            button2.setBounds(5 * d.width / 9 - insets.right,
                13 * d.height / 16 - insets.bottom, d.width / 3, d.height / 8);
            laidOut = true;
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            Chit chit = attackerChits[i];
            if (chit.select(point))
            {
                chit.toggleDead();
                Rectangle clip = chit.getBounds();
                repaint(clip.x, clip.y, clip.width, clip.height);
                return;
            }
        }
        for (int i = 0; i < defender.getHeight(); i++)
        {
            Chit chit = defenderChits[i];
            if (chit.select(point))
            {
                chit.toggleDead();
                Rectangle clip = chit.getBounds();
                repaint(clip.x, clip.y, clip.width, clip.height);
                return;
            }
        }
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mouseClicked(MouseEvent e)
    {
    }


    public void mouseReleased(MouseEvent e)
    {
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Agree"))
        {
            // Count remaining chits.
            int attackersLeft = 0;
            for (int i = 0; i < attacker.getHeight(); i++)
            {
                if (!attackerChits[i].isDead())
                {
                    attackersLeft++;
                }
            }
            int defendersLeft = 0;
            for (int i = 0; i < defender.getHeight(); i++)
            {
                if (!defenderChits[i].isDead())
                {
                    defendersLeft++;
                }
            }

            // Ensure that at least one legion is completely eliminated.
            if (attackersLeft > 0 && defendersLeft > 0)
            {
                new MessageBox(parentFrame, 
                    "At least one legion must be eliminated.");
                return;
            }

            MasterHex hex = attacker.getCurrentHex();

            // If this is a mutual elimination, remove both legions and
            // give no points.
            if (attackersLeft == 0 && defendersLeft == 0)
            {
                attacker.removeLegion();
                defender.removeLegion();

                // If either was the titan stack, its owner dies and gives
                // half points to the victor.
                if (attacker.numCreature(Creature.titan) == 1)
                {
                    attacker.getPlayer().die(defender.getPlayer(), true);
                }

                if (defender.numCreature(Creature.titan) == 1)
                {
                    defender.getPlayer().die(attacker.getPlayer(), true);
                }
            }

            // If this is not a mutual elimination, figure out how many
            // points the victor receives.
            else
            {
                Legion winner;
                Legion loser;
                Chit [] winnerChits;

                if (defendersLeft == 0)
                {
                    winner = attacker;
                    loser = defender;
                    winnerChits = attackerChits;
                }
                else
                {
                    winner = defender;
                    loser = attacker;
                    winnerChits = defenderChits;
                }

                // Ensure that the winning legion doesn't contain a dead
                // Titan.
                for (int i = winner.getHeight() - 1; i >= 0; i--)
                {
                    if (winnerChits[i].isDead() && winner.getCreature(i) ==
                        Creature.titan)
                    {
                        new MessageBox(parentFrame,
                            "Titan cannot die unless his whole stack dies.");
                        return;
                    }
                }

                // Remove all dead creatures from the winning legion.
                for (int i = winner.getHeight() - 1; i >= 0; i--)
                {
                    if (winnerChits[i].isDead())
                    {
                        winner.removeCreature(i);
                    }
                }

                int points = loser.getPointValue();

                // Remove the losing legion.
                loser.removeLegion();

                // Add points, and angels if necessary.
                winner.addPoints(points);

                // If this was the titan stack, its owner dies and gives half
                // points to the victor.
                if (loser.numCreature(Creature.titan) == 1)
                {
                    loser.getPlayer().die(winner.getPlayer(), true);
                }
            }

            // Exit this dialog.
            cleanup();

            // Unselect and repaint the hex.
            hex.unselect();
            hex.repaint();
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            // Exit this dialog.
            cleanup();
        }
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(scale * (Math.max(attacker.getHeight(),
            defender.getHeight()) + 2), 4 * scale);
    }
}
