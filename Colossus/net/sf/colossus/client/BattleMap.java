package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Options;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;


/**
 * Class BattleMap implements the GUI for a Titan battlemap.
 * @version $Id$
 * @author David Ripton
 */

public final class BattleMap extends HexMap implements MouseListener,
    WindowListener
{
    private static Point location;
    private JFrame battleFrame;
    private JMenuBar menuBar;
    private JMenu phaseMenu;
    private Client client;
    private JLabel playerLabel;
    private Cursor defaultCursor;

    /** tag of the selected critter, or -1 if no critter is selected. */
    private int selectedCritterTag = -1;

    private static final String undoLast = "Undo Last";
    private static final String undoAll = "Undo All";
    private static final String doneWithPhase = "Done";
    private static final String concedeBattle = "Concede Battle";
    private static final String forceAdvancePhase = "Force Advance Phase";

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
    private AbstractAction concedeBattleAction;
    private AbstractAction forceAdvancePhaseAction;

    BattleMap(Client client, String masterHexLabel)
    {
        super(masterHexLabel);

        this.client = client;

        battleFrame = new JFrame();
        battleFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Container contentPane = battleFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        setupIcon();

        battleFrame.addWindowListener(this);
        addMouseListener(this);

        setupActions();
        setupTopMenu();

        setupEntrances();

        if (location == null)
        {
            location = new Point(0, 4 * Scale.get());
        }
        battleFrame.setLocation(location);

        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);

        setupPlayerLabel();
        contentPane.add(playerLabel, BorderLayout.NORTH);

        defaultCursor = battleFrame.getCursor();

        // Do not call pack() or setVisible(true) until after
        // BattleDice is added to frame.
    }


    // Simple constructor for testing and AICopy()
    BattleMap(String masterHexLabel)
    {
        super(masterHexLabel);
        setupEntrances();
    }


    BattleMap AICopy(String masterHexLabel)
    {
        BattleMap newMap = new BattleMap(masterHexLabel);
        return newMap;
    }


    private void setupActions()
    {
        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getPlayerName().equals(
                    client.getBattleActivePlayerName()))
                {
                    return;
                }
                if (client.getBattlePhase() == Constants.MOVE)
                {
                    selectedCritterTag = -1;
                    client.undoLastBattleMove();
                    highlightMobileCritters();
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getPlayerName().equals(
                    client.getBattleActivePlayerName()))
                {
                    return;
                }
                if (client.getBattlePhase() == Constants.MOVE)
                {
                    selectedCritterTag = -1;
                    client.undoAllBattleMoves();
                    highlightMobileCritters();
                }
            }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.getPlayerName().equals(
                    client.getBattleActivePlayerName()))
                {
                    return;
                }

                int phase = client.getBattlePhase();
                switch (phase)
                {
                    case Constants.MOVE:
                        // TODO Check this locally rather than asking for help.
                        if (!client.getOption(Options.autoPlay) &&
                            client.anyOffboardCreatures() && 
                            !confirmLeavingCreaturesOffboard())
                        {
                            return;
                        }
                        client.doneWithBattleMoves();
                        break;

                    case Constants.FIGHT:
                    case Constants.STRIKEBACK:
                        client.doneWithStrikes();
                        break;
                }
            }
        };


        forceAdvancePhaseAction = new AbstractAction(forceAdvancePhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.forceAdvanceBattlePhase();
            }
        };


        concedeBattleAction = new AbstractAction(concedeBattle)
        {
            public void actionPerformed(ActionEvent e)
            {
                String [] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(battleFrame,
                    "Are you sure you wish to concede the battle?",
                    "Confirm Concession?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    String playerName = client.getPlayerName();
                    Log.event(playerName + " concedes the battle");
                    client.concede();
                };
            }
        };
    }


    private void setupTopMenu()
    {
        menuBar = new JMenuBar();
        battleFrame.setJMenuBar(menuBar);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(phaseMenu);
    }


    void setupSummonMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() + " : Summon");
        phaseMenu.removeAll();

        JMenuItem mi;
        mi = phaseMenu.add(forceAdvancePhaseAction);
        mi.setMnemonic(KeyEvent.VK_F);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));

        requestFocus();
    }


    void setupRecruitMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() + " : Recruit");
        if (phaseMenu != null)
        {
            phaseMenu.removeAll();
        }

        JMenuItem mi;
        mi = phaseMenu.add(forceAdvancePhaseAction);
        mi.setMnemonic(KeyEvent.VK_F);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));

        requestFocus();
    }


    void setupMoveMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() + " : Move");

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(undoLastAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

        mi = phaseMenu.add(undoAllAction);
        mi.setMnemonic(KeyEvent.VK_A);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(forceAdvancePhaseAction);
        mi.setMnemonic(KeyEvent.VK_F);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));

        if (client.getPlayerName().equals(client.getBattleActivePlayerName()))
        {
            highlightMobileCritters();
            requestFocus();
        }
    }


    void setupFightMenu()
    {
        if (battleFrame == null)
        {
            return;
        }

        battleFrame.setTitle(client.getBattleActivePlayerName() +
            " Turn " + client.getBattleTurnNumber() +
            ((client.getBattlePhase() == Constants.FIGHT) ?
            " : Strike" : " : Strikeback"));
        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(forceAdvancePhaseAction);
        mi.setMnemonic(KeyEvent.VK_F);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));

        if (client.getPlayerName().equals(client.getBattleActivePlayerName()))
        {
            highlightCrittersWithTargets();
            requestFocus();
        }
    }


    JFrame getFrame()
    {
        return battleFrame;
    }


    private void setupIcon()
    {
        java.util.List directories = new java.util.ArrayList();
        directories.add(Constants.defaultDirName +
                        ResourceLoader.getPathSeparator() +
                        Constants.imagesDirName);
        
        String[] iconNames = { Constants.battlemapIconImage,
                               Constants.battlemapIconText +
                               "-Name-" +
                               Constants.battlemapIconTextColor,
                               Constants.battlemapIconSubscript +
                               "-Subscript-" +
                               Constants.battlemapIconTextColor };
        
        Image image =
            ResourceLoader.getCompositeImage(iconNames,
                                             directories);
        
        if (image == null)
        {
            Log.error("ERROR: Couldn't find Colossus icon");
            dispose();
        }
        else
        {
            battleFrame.setIconImage(image);
        }
    }

    /** Show which player owns this board. */
    void setupPlayerLabel()
    {
        String playerName = client.getPlayerName();
        playerLabel = new JLabel(playerName);

        String colorName = client.getColor();
        // If we call this before player colors are chosen, just use
        // the defaults.
        if (colorName != null)
        {
            Color color = PickColor.getBackgroundColor(colorName);
            playerLabel.setForeground(color);
        }
        playerLabel.repaint();
    }


    void placeNewChit(String imageName, boolean inverted, int tag, 
        String hexLabel)
    {
        client.addBattleChit(imageName, inverted, tag);
        alignChits(hexLabel);
        // Need to move map back in front of board after summon or muster.
        battleFrame.toFront();
    }


    public static BattleHex getEntrance(char terrain, String masterHexLabel,
        int entrySide)
    {
        return HexMap.getHexByLabel(terrain, "X" + entrySide);
    }


    /** Return the BattleChit containing the given point,
     *  or null if none does. */
    private BattleChit getBattleChitAtPoint(Point point)
    {
        Iterator it = client.getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            if (chit.contains(point))
            {
                return chit;
            }
        }
        return null;
    }


    void alignChits(String hexLabel)
    {
        GUIBattleHex hex = getGUIHexByLabel(hexLabel);
        int [] tags = client.getCritterTags(hexLabel);
        int numCritters = tags.length;
        if (numCritters < 1)
        {
            hex.repaint();
            return;
        }
        BattleChit chit = client.getBattleChit(tags[0]);
        if (chit == null)
        {
            hex.repaint();
            return;
        }
        int chitscale = chit.getBounds().width;
        int chitscale4 = chitscale / 4;

        Point point = new Point(hex.findCenter());

        // Cascade chits diagonally.
        int offset = ((chitscale * (1 + numCritters))) / 4;
        point.x -= offset;
        point.y -= offset;

        chit.setLocation(point);

        for (int i = 1; i < numCritters; i++)
        {
            point.x += chitscale4;
            point.y += chitscale4;
            chit = client.getBattleChit(tags[i]);
            if (chit != null)
            {
                chit.setLocation(point);
            }
        }
        hex.repaint();
    }

    void alignChits(Set hexLabels)
    {
        Iterator it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            alignChits(hexLabel);
        }
    }


    /** Select all hexes containing critters eligible to move. */
    private void highlightMobileCritters()
    {
        Set set = client.findMobileCritters();
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    private void highlightMoves(int tag)
    {
        Set set = client.showBattleMoves(tag);
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Select hexes containing critters that have valid strike targets. */
    void highlightCrittersWithTargets()
    {
        Set set = client.findCrittersWithTargets();
        unselectAllHexes();
        selectHexesByLabels(set);
        // XXX Needed?
        repaint();
    }

    /** Highlight all hexes with targets that the critter can strike. */
    private void highlightStrikes(int tag)
    {
        Set set = client.findStrikes(tag);
        unselectAllHexes();
        selectHexesByLabels(set);
        // XXX Needed?
        repaint();
    }


    void setDefaultCursor()
    {
        battleFrame.setCursor(defaultCursor);
    }

    void setWaitCursor()
    {
        battleFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }


    private boolean confirmLeavingCreaturesOffboard()
    {
        String [] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(battleFrame,
            "Are you sure you want to leave creatures offboard?",
            "Confirm Leaving Creatures Offboard?",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[1]);
        return (answer == JOptionPane.YES_OPTION);
    }


    private void actOnCritter(int tag, String hexLabel)
    {
        selectedCritterTag = tag;

        // XXX Put selected chit at the top of the z-order.
        // Then getGUIHexByLabel(hexLabel).repaint();

        switch (client.getBattlePhase())
        {
            case Constants.MOVE:
                // Highlight all legal destinations for this critter.
                highlightMoves(tag);
                break;

            case Constants.FIGHT:
            case Constants.STRIKEBACK:
                client.leaveCarryMode();
                highlightStrikes(tag);
                break;

            default:
                break;
        }
    }

    private void actOnHex(String hexLabel)
    {
        switch (client.getBattlePhase())
        {
            case Constants.MOVE:
                if (selectedCritterTag != -1)
                {
                    client.doBattleMove(selectedCritterTag, hexLabel);
                    selectedCritterTag = -1;
                    highlightMobileCritters();
                }
                break;

            case Constants.FIGHT:
            case Constants.STRIKEBACK:
                if (selectedCritterTag != -1)
                {
                    client.strike(selectedCritterTag, hexLabel);
                    selectedCritterTag = -1;
                }
                break;

            default:
                break;
        }
    }

    private void actOnMisclick()
    {
        switch (client.getBattlePhase())
        {
            case Constants.MOVE:
                selectedCritterTag = -1;
                highlightMobileCritters();
                break;

            case Constants.FIGHT:
            case Constants.STRIKEBACK:
                selectedCritterTag = -1;
                client.leaveCarryMode();
                highlightCrittersWithTargets();
                break;

            default:
                break;
        }
    }


    public void mousePressed(MouseEvent e)
    {
        // Only the active player can click on stuff.
        if (!client.getPlayerName().equals(client.getBattleActivePlayerName()))
        {
            return;
        }

        Point point = e.getPoint();

        BattleChit chit = getBattleChitAtPoint(point);
        BattleHex hex = getHexContainingPoint(point);
        String hexLabel = ""; 
        if (hex != null)
        {
            hexLabel = hex.getLabel();
        }

        if (chit != null && client.getPlayerNameByTag(chit.getTag()).equals(
            client.getBattleActivePlayerName()))
        {
            actOnCritter(chit.getTag(), hexLabel);
        }

        // No hits on friendly chits, so check map.
        else if (hex != null && hex.isSelected())
        {
            actOnHex(hexLabel);
        }

        // No hits on selected hexes, so clean up.
        else
        {
            actOnMisclick();
        }
    }


    public void windowClosing(WindowEvent e)
    {
        String [] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(battleFrame,
           "Are you sure you wish to quit?",
           "Quit Game?",
           JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
           null, options, options[1]);

        if (answer == JOptionPane.YES_OPTION)
        {
            System.exit(0);
        }
    }


    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
        }

        java.util.List battleChits = client.getBattleChits();
        ListIterator lit = battleChits.listIterator(battleChits.size());
        while (lit.hasPrevious())
        {
            BattleChit chit = (BattleChit)lit.previous();
            if (rectClip.intersects(chit.getBounds()))
            {
                chit.paintComponent(g);
            }
        }
    }


    void dispose()
    {
        // Save location for next object.
        location = getLocation();

        if (battleFrame != null)
        {
            battleFrame.dispose();
        }
    }


    void rescale()
    {
        setupHexes();
        setupEntrances();
        int chitScale = 4 * Scale.get();
        Iterator it = client.getBattleChits().iterator();
        while (it.hasNext())
        {
            BattleChit chit = (BattleChit)it.next();
            chit.rescale(chitScale);
        }
        alignChits(getAllHexLabels());
    }

    public static String entrySideName(int side)
    {
        switch (side)
        {
            case 1: return Constants.right;
            case 3: return Constants.bottom;
            case 5: return Constants.left;
            default: return "";
        }
    }

    public static int entrySideNum(String side)
    {
        if (side.equals(Constants.right))
        {
            return 1;
        }
        else if (side.equals(Constants.bottom))
        {
            return 3;
        }
        else if (side.equals(Constants.left))
        {
            return 5;
        }
        else
        {
            return -1;
        }
    }
}
