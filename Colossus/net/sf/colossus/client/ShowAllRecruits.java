package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.RecruitGraph;


/**
 * Displays recruit trees for all MasterHex types.
 * @version $Id$
 * @author David Ripton
 */

final class ShowAllRecruits extends KDialog implements MouseListener,
            WindowListener
{
    private boolean allTerrains = false;
    // Avoid showing multiple allTerrains displays.
    private static boolean allTerrainsDisplayActive = false;

    ShowAllRecruits(JFrame parentFrame, String[] terrains, Point point,
            String singleTerrainHexLabel, JScrollPane pane)
    {
        super(parentFrame, "Recruits", false);

        if (point == null && singleTerrainHexLabel == null)
        {
            allTerrains = true;
            if (allTerrainsDisplayActive)
            {
                super.dispose();
                return;
            }
            allTerrainsDisplayActive = true;
        }

        setBackground(Color.lightGray);
        addWindowListener(this);
        getContentPane().setLayout(new BoxLayout(getContentPane(),
                BoxLayout.X_AXIS));

        for (int i = 0; i < terrains.length; i++)
        {
            doOneTerrain(terrains[i], singleTerrainHexLabel);
        }

        pack();
        addMouseListener(this);

        if (point != null)
        {
            placeRelative(parentFrame, point, pane);
        }

        setVisible(true);
        repaint();
    }

    void doOneTerrain(String terrain, String hexLabel)
    {
        Box panel = new Box(BoxLayout.Y_AXIS);
        panel.setAlignmentY(0);
        panel.setBorder(BorderFactory.createLineBorder(Color.black));
        panel.setBackground(TerrainRecruitLoader.getTerrainColor(terrain));

        JLabel terrainLabel = new JLabel(terrain);
        terrainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(terrainLabel);

        List creatures =
                TerrainRecruitLoader.getPossibleRecruits(terrain, hexLabel);
        Iterator it = creatures.iterator();
        boolean firstTime = true;
        int scale = 4 * Scale.get();
        Creature prevCreature = Creature.getCreatureByName(Constants.titan);
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();

            int numToRecruit;
            if (firstTime)
            {
                numToRecruit = 0;
                firstTime = false;
            }
            else
            {
                numToRecruit = TerrainRecruitLoader.numberOfRecruiterNeeded(
                        prevCreature, creature, terrain, hexLabel);
            }

            JLabel numToRecruitLabel = new JLabel("");
            if (numToRecruit > 0 && numToRecruit < RecruitGraph.BIGNUM)
            {
                numToRecruitLabel.setText(Integer.toString(numToRecruit));
                numToRecruitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            }

            panel.add(numToRecruitLabel);
            numToRecruitLabel.addMouseListener(this);

            Chit chit = new Chit(scale, creature.getName(), this);
            panel.add(chit);
            chit.addMouseListener(this);

            chit.repaint();

            prevCreature = creature;
        }
        getContentPane().add(panel);
    }

    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }

    public void mousePressed(MouseEvent e)
    {
        dispose();
    }

    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

    public void dispose()
    {
        if (allTerrains)
        {
            allTerrainsDisplayActive = false;
        }
        super.dispose();
    }
}
