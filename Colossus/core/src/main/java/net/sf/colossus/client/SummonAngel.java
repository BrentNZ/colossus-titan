package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;

import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Creature;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.variant.CreatureType;


/**
 * Allows a player to summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

final class SummonAngel extends KDialog implements MouseListener,
    ActionListener, WindowListener
{
    private static final Logger LOGGER = Logger.getLogger(SummonAngel.class
        .getName());

    private final Legion legion;
    private final List<Chit> sumChitList = new ArrayList<Chit>();
    private final JButton cancelButton;
    private static boolean active;
    private final Client client;
    private static final String baseSummonString = 
        ": Summon Angel into Legion ";
    private final SaveWindow saveWindow;
    private static String typeColonDonor = null;
    private Map<Chit, Legion> chitToDonor = new HashMap<Chit, Legion>();

    private SummonAngel(Client client, Legion legion)
    {
        super(client.getBoard().getFrame(), client.getOwningPlayer().getName()
            + baseSummonString + legion, true);

        this.client = client;
        this.legion = legion;

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        // TODO this should really not happen in a constructor
        SortedSet<Legion> possibleDonors = 
            client.findLegionsWithSummonableAngels(legion);
        if (possibleDonors.size() < 1)
        {
            cleanup(null, null);
            // trying to keep things final despite awkward exit point
            saveWindow = null;
            cancelButton = null;
            return;
        }

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        pack();

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        List<CreatureType> summonableList = client.getGame().getVariant()
            .getSummonableCreatureTypes();
        Iterator<CreatureType> it = summonableList.iterator();
        sumChitList.clear();

        for (Legion donor : possibleDonors)
        {
            Box box = new Box(BoxLayout.X_AXIS);
            contentPane.add(box);
            Marker marker = new Marker(scale, donor.getMarkerId());
            box.add(marker);
            box.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
            box.add(Box.createHorizontalGlue());
            Set<CreatureType> seen = new HashSet<CreatureType>();
            for (Creature creature : donor.getCreatures())
            {
                if (creature.getType().isSummonable())
                {
                    if (!seen.contains(creature.getType()))
                    {
                        seen.add(creature.getType());
                        Chit chit = 
                            new Chit(scale, creature.getType().getName());
                        box.add(chit);
                        chit.addMouseListener(this);
                        sumChitList.add(chit);
                        chitToDonor.put(chit, donor);
                    }
                }
            }
        }

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        contentPane.add(cancelButton);
        cancelButton.addActionListener(this);

        pack();

        saveWindow = new SaveWindow(client.getOptions(), "SummonAngel");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }

        setVisible(true);
        repaint();
    }

    /** Return a string like Angel:Bk12 or Archangel:Rd02, or null. */
    static String summonAngel(Client client, Legion legion)
    {
        LOGGER.log(Level.FINER, "called summonAngel for " + legion);
        if (!active)
        {
            active = true;
            LOGGER.log(Level.FINEST, "returning new SummonAngel dialog for "
                + legion);
            new SummonAngel(client, legion);
            active = false;
        }
        return typeColonDonor;
    }

    Legion getLegion()
    {
        return legion;
    }

    private void cleanup(Legion donor, String angel)
    {
        if (donor != null || angel != null)
        {
            typeColonDonor = angel + ":" + donor.toString();
        }
        saveWindow.saveLocation(getLocation());
        dispose();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        for (Chit c : sumChitList)
        {
            if ((source == c) && !(c.isDead()))
            {
                Legion donor = chitToDonor.get(c);
                cleanup(donor, c.getId());
                return;
            }
        }
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        cleanup(null, null);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null, null);
        }
    }
}
