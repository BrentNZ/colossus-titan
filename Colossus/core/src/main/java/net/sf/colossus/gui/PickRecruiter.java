package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;


/**
 * Class PickRecruiter allows a player to choose which creature(s) recruit.
 * @version $Id$
 * @author David Ripton
 */

final class PickRecruiter extends KDialog implements MouseListener,
    WindowListener
{
    private final List<String> recruiters;
    private final List<Chit> recruiterChits = new ArrayList<Chit>();
    private final Marker legionMarker;
    private String recruiterName;
    private final SaveWindow saveWindow;

    /** recruiters is a list of creature name strings */
    private PickRecruiter(JFrame parentFrame, List<String> recruiters,
        String hexDescription, Legion legion, Client client)
    {
        super(parentFrame, client.getOwningPlayer().getName()
            + ": Pick Recruiter in " + hexDescription, true);

        recruiterName = null;
        this.recruiters = recruiters;

        addMouseListener(this);
        addWindowListener(this);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setBackground(Color.lightGray);
        int scale = 4 * Scale.get();

        JLabel label = new JLabel(
            "  There is more than one way you can recruit this.");
        label.setAlignmentX(FlowLayout.LEADING );
        
        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));
        contentPane.add(label);
        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));
        
        JPanel legionPane = new JPanel();
        String legionId = legion.getMarkerId();
        String text = "Current content of legion " + legionId + ":";
        legionPane.setBorder(BorderFactory.createTitledBorder(text));

        legionMarker = new Marker(scale, legion.getMarkerId());
        legionPane.add(legionMarker);

        List<String> imageNames = client.getLegionImageNames(legion);
        Iterator<String> it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = new Chit(scale, imageName);
            legionPane.add(chit);
        }

        contentPane.add(legionPane);
        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));

        JLabel label2 = new JLabel(
            "  Pick the creature type you want to reveal:  ");
        label2.setAlignmentX(FlowLayout.LEADING );
        contentPane.add(label2);
        
        JPanel recruiterPane = new JPanel();
        contentPane.add(recruiterPane);

        int i = 0;
        it = recruiters.iterator();
        while (it.hasNext())
        {
            String recruiterName = it.next();
            if (recruiterName.equals(Constants.titan))
            {
                recruiterName = ((LegionClientSide)legion).getTitanBasename();
            }
            Chit chit = new Chit(scale, recruiterName);
            recruiterChits.add(chit);
            recruiterPane.add(chit);
            chit.addMouseListener(this);
            i++;
        }

        pack();
        saveWindow = new SaveWindow(client.getOptions(), "PickRecruiter");
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

    private String getRecruiterName()
    {
        return recruiterName;
    }

    static synchronized String pickRecruiter(JFrame parentFrame, List<String> recruiters,
        String hexDescription, Legion legion, Client client)
    {
        PickRecruiter pr = new PickRecruiter(parentFrame, recruiters,
            hexDescription, legion, client);
        return pr.getRecruiterName();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = recruiterChits.indexOf(source);
        if (i != -1)
        {
            recruiterName = recruiters.get(i);
            if (recruiterName.startsWith(Constants.titan))
            {
                recruiterName = Constants.titan;
            }

            // Then exit.
            dispose();
        }
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

    @Override
    public void dispose()
    {
        saveWindow.saveLocation(getLocation());
        super.dispose();
    }
}