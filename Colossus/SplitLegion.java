import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * @author David Ripton
 */

public class SplitLegion extends JDialog implements MouseListener,
    ActionListener, WindowListener
{
    private Legion oldLegion;
    private Legion newLegion;
    private ArrayList oldChits = new ArrayList(8);
    private ArrayList newChits = new ArrayList(8);
    private Marker oldMarker;
    private Marker newMarker;
    private Player player;
    private static final int scale = 60;
    private JFrame parentFrame;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();


    public SplitLegion(JFrame parentFrame, Legion oldLegion, Player player)
    {
        super(parentFrame, player.getName() + ": Split Legion " +
            oldLegion.getMarkerId(), true);

        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);

        this.oldLegion = oldLegion;
        this.player = player;
        this.parentFrame = parentFrame;

        String selectedMarker = PickMarker.pickMarker(parentFrame,
            player.getName(), player.getMarkersAvailable());

        if (selectedMarker == null)
        {
            dispose();
            return;
        }
        else
        {
            player.selectMarker(selectedMarker);
        }

        pack();

        newLegion = new Legion(player.getSelectedMarker(), oldLegion,
            oldLegion.getCurrentHex(), null, null, null, null, null,
            null, null, null, player);
        String imageName = player.getSelectedMarker();
        newMarker = new Marker(scale, imageName, this, null);
        newLegion.setMarker(newMarker);

        setBackground(Color.lightGray);
        setResizable(false);

        addMouseListener(this);
        addWindowListener(this);

        oldMarker = new Marker(scale, oldLegion.getImageName(), this,
            oldLegion);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(oldMarker, constraints);
        contentPane.add(oldMarker);

        Collection critters = oldLegion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            oldChits.add(chit);
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(newMarker, constraints);
        contentPane.add(newMarker);

        JButton button1 = new JButton("Done");
        button1.setMnemonic(KeyEvent.VK_D);
        JButton button2 = new JButton("Cancel");
        button2.setMnemonic(KeyEvent.VK_C);

         // Attempt to center the buttons.
        int chitWidth = Math.max(oldLegion.getHeight(),
            newLegion.getHeight()) + 1;
        if (chitWidth < 4)
        {
            constraints.gridwidth = 1;
        }
        else
        {
            constraints.gridwidth = 2;
        }
        int leadSpace = (chitWidth - 2 * constraints.gridwidth) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        constraints.gridx = leadSpace;
        constraints.gridy = 2;
        gridbag.setConstraints(button1, constraints);
        contentPane.add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth;
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
        button2.addActionListener(this);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
    }


    private void cancel()
    {
        newLegion.recombine(oldLegion, true);
        dispose();
    }


    // Move a Creature over to the other Legion and move its chit to the
    // end of the other line.
    private void moveCreatureToOtherLegion(Legion fromLegion, Legion toLegion,
        ArrayList fromChits, ArrayList toChits, int oldPosition, int gridy)
    {
        Creature creature = fromLegion.removeCreature(oldPosition,
            false, false);
        toLegion.addCreature(creature, false);

        Chit chit = (Chit)fromChits.remove(oldPosition);
        remove(chit);

        toChits.add(chit);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = gridy;
        constraints.gridwidth = 1;
        gridbag.setConstraints(chit, constraints);

        Container contentPane = getContentPane();
        contentPane.add(chit);

        // Update the stack heights on both markers.
        oldMarker.repaint();
        newMarker.repaint();

        pack();

        repaint();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = oldChits.indexOf(source);
        if (i != -1)
        {
            moveCreatureToOtherLegion(oldLegion, newLegion, oldChits,
                newChits, i, 1);
            return;
        }
        else
        {
            i = newChits.indexOf(source);
            if (i != -1)
            {
                moveCreatureToOtherLegion(newLegion, oldLegion, newChits,
                    oldChits, i, 0);
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


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        cancel();
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


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Done"))
        {
            // Check to make sure that each Legion is legal.
            // Each legion must have 2 <= height <= 7.
            // Also, if this was an initial split, each Legion
            // must have height 4 and one lord.
            if (oldLegion.getHeight() < 2 || newLegion.getHeight() < 2)
            {
                JOptionPane.showMessageDialog(parentFrame,
                    "Legion too short.");
                return;
            }
            if (oldLegion.getHeight() + newLegion.getHeight() == 8)
            {
                if (oldLegion.getHeight() != newLegion.getHeight())
                {
                    JOptionPane.showMessageDialog(parentFrame,
                        "Initial split must be 4-4.");
                    return;
                }
                else
                {
                    if (oldLegion.numLords() != 1)
                    {
                        JOptionPane.showMessageDialog(parentFrame,
                            "Each stack must have one lord.");
                        return;
                    }
                }
            }

            // The split is legal.

            // Resize the new legion to MasterBoard scale.
            newMarker.rescale(oldLegion.getMarker().getBounds().width);

            // Add the new legion to the player.
            if (player != null)
            {
                player.addLegion(newLegion);
            }

            // Set the new chit next to the old chit on the masterboard.
            MasterHex hex = newLegion.getCurrentHex();
            if (hex != null)
            {
                newLegion.getCurrentHex().addLegion(newLegion);
            }

            // Hide the contents of both legions.
            oldLegion.hideAllCreatures();
            newLegion.hideAllCreatures();

            // Mark the last legion split off.
            if (player != null)
            {
                player.markLastLegionSplitOff(newLegion);
            }

            // Exit.
            dispose();

            Game.logEvent(newLegion.getHeight() +
                " creatures were split off from legion " +
                oldLegion.getMarkerId() +
                " into new legion " + newLegion.getMarkerId());

            // This is needed for splits where the hex was not
            // highlighted because the legion was not 7+ high.
            if (hex != null)
            {
                hex.repaint();
            }
        }

        else if (e.getActionCommand().equals("Cancel"))
        {
            cancel();
        }
    }


    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing SplitLegion");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        Player player = new Player("Test", null);
        player.setTower(1);
        player.setColor("Red");
        player.initMarkersAvailable();
        player.selectMarker("Rd01");
        Legion legion = new Legion(player.getSelectedMarker(), null, null,
            Creature.titan, Creature.angel, Creature.ogre, Creature.ogre,
            Creature.centaur, Creature.centaur, Creature.gargoyle,
            Creature.gargoyle, player);
        Marker marker = new Marker(scale, player.getSelectedMarker(),
            frame, null);
        legion.setMarker(marker);

        new SplitLegion(frame, legion, player);
    }
}
