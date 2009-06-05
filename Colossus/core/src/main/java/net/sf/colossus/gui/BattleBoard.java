package net.sf.colossus.gui;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.GameClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.MasterHex;


/**
 * A GUI representation of a battle in the game.
 *
 * TODO this is split of the former BattleMap which did everything by itself. The
 * split is not really completed, there is still code which potentially belongs into
 * the other class.
 */
@SuppressWarnings("serial")
public final class BattleBoard extends KFrame
{
    private static final Logger LOGGER = Logger.getLogger(BattleMap.class
        .getName());

    private static int count = 1;

    private JMenuBar menuBar;
    private JMenu phaseMenu;
    private JMenu helpMenu;
    private final InfoPanel infoPanel;
    private final DicePanel dicePanel;
    private final ClientGUI gui;
    private final String infoText;

    /** tag of the selected critter, or -1 if no critter is selected. */
    private int selectedCritterTag = -1;

    private static final String undoLast = "Undo Last";
    private static final String undoAll = "Undo All";
    private static final String doneWithPhase = "Done";
    private static final String concedeBattle = "Concede Battle";
    private static final String showTerrainHazard = "Show Terrain";

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
    private AbstractAction concedeBattleAction;
    private AbstractAction showTerrainHazardAction;

    private final SaveWindow saveWindow;

    private final BattleMap battleMap;

    private static class DicePanel extends JPanel
    {
        private final BattleDice battleDice;
        private final JScrollBar scrollBar;

        DicePanel()
        {
            setLayout(new BorderLayout());
            battleDice = new BattleDice();
            add(battleDice, BorderLayout.CENTER);

            scrollBar = new JScrollBar(JScrollBar.VERTICAL);
            scrollBar.setVisibleAmount(1);
            scrollBar.addAdjustmentListener(new AdjustmentListener()
            {
                public void adjustmentValueChanged(AdjustmentEvent pArg0)
                {
                    if (pArg0.getAdjustmentType() == AdjustmentEvent.TRACK)
                    {
                        battleDice.setCurrentRoll(pArg0.getValue() + 1);
                    }
                }
            });
            add(scrollBar, BorderLayout.EAST);
        }

        void rescale()
        {
            battleDice.rescale();
        }

        void addValues(String pBattlePhaseDesc, String pAttckerDesc,
            String pStrikerDesc, String pTargetDesc, int pTargetNumber,
            List<String> pRolls)
        {
            int max = battleDice.getHistoryLength();
            battleDice.addValues(pBattlePhaseDesc, pAttckerDesc, pStrikerDesc,
                pTargetDesc, pTargetNumber, pRolls);
            scrollBar.setMaximum(max);
            scrollBar.setValue(max);
        }

        void showLastRoll()
        {
            battleDice.showLastRoll();
        }
    }

    public BattleBoard(final ClientGUI gui, MasterHex masterHex,
        Legion attacker, Legion defender)
    {
        super(); // title will be set later

        this.gui = gui;

        String attackerMarkerId = attacker.getMarkerId();
        String defenderMarkerId = defender.getMarkerId();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout());

        setupIcon();

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                gui.askNewCloseQuitCancel(BattleBoard.this, true);
            }
        });

        setupActions();
        setupTopMenu();
        setupHelpMenu();

        saveWindow = new SaveWindow(gui.getOptions(), "BattleMap");

        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            location = new Point(0, 4 * Scale.get());
        }
        setLocation(location);

        battleMap = new BattleMap(getClient(), masterHex, attackerMarkerId,
            defenderMarkerId, gui);
        contentPane.add(new JScrollPane(battleMap), BorderLayout.CENTER);
        battleMap.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // Only the active player can click on stuff.
                if (!isMyBattleTurn())
                {
                    return;
                }

                Point point = e.getPoint();

                GUIBattleChit battleChit = getBattleChitAtPoint(point);
                GUIBattleHex hex = battleMap.getHexContainingPoint(point);

                handleMousePressed(battleChit, hex);
            }
        });

        infoPanel = new InfoPanel();
        contentPane.add(infoPanel, BorderLayout.NORTH);

        PlayerColor color = getClient().getColor();
        if (color != null)
        {
            Color bgColor = PickColor.getBackgroundColor(color);
            contentPane.setBorder(BorderFactory.createLineBorder(bgColor));
        }

        dicePanel = new DicePanel();
        getContentPane().add(dicePanel, BorderLayout.SOUTH);

        infoText = gui.getOwningPlayerName() + ": "
            + LegionServerSide.getMarkerName(attackerMarkerId) + " ("
            + attackerMarkerId + ") attacks "
            + LegionServerSide.getMarkerName(defenderMarkerId) + " ("
            + defenderMarkerId + ") in " + masterHex.getLabel();

        setTitle(getInfoText());

        String instanceId = gui.getOwningPlayerName() + ": "
            + attackerMarkerId + "/" + defenderMarkerId + " (" + count + ")";
        count++;
        net.sf.colossus.util.InstanceTracker.setId(this, instanceId);

        pack();
        setVisible(true);

        // @TODO: perhaps those could be done earlier, but in previous code
        // (still in Client) they were done after BattleBoard instantiation,
        // so I keep them like that, for now.
        setPhase(getGame().getBattlePhase());
        setTurn(getGame().getBattleTurnNumber());
        setBattleMarkerLocation(false, "X" + attacker.getEntrySide().ordinal());
        setBattleMarkerLocation(true, "X" + defender.getEntrySide().ordinal());
        reqFocus();
    }

    private void handleMousePressed(GUIBattleChit battleChit, GUIBattleHex hex)
    {
        gui.resetStrikeNumbers();

        // This checks only if its a chit of the active player
        // But that's ok, this here is called only if it's own phase
        boolean ownChit = (battleChit != null && getGame().getPlayerByTag(
            battleChit.getTag()).equals(
                getGame().getBattleActivePlayer()));

        boolean ownChit2 = (battleChit != null && battleChit.getBattleUnit()
            .getLegion().getPlayer().equals(getGame().getBattleActivePlayer()));

        assert ownChit == ownChit2 : "checks for 'is own chit' return different result!";
        boolean isPickCarryOngoing = gui.isPickCarryOngoing();
        if (isPickCarryOngoing)
        {
            if (battleChit != null && !ownChit)
            {
                gui.handlePickCarry(hex);
            }
            else
            {
                // not a chit, or at least not own chit
            }
        }
        else if (battleChit != null && ownChit)
        {
            actOnCritter(battleChit);
        }

        // No hits on friendly chits, so check map.
        else if (hex != null && hex.isSelected())
        {
            actOnHex(hex.getHexModel());
        }

        // No hits on selected hexes, so clean up.
        else
        {
            actOnMisclick();
        }
    }

    private void setBattleMarkerLocation(boolean isDefender, String hexLabel)
    {
        BattleHex hex = battleMap.getHexByLabel(hexLabel);
        battleMap.setBattleMarkerLocation(isDefender, hex);
    }

    private Client getClient()
    {
        return gui.getClient();
    }

    private GameClientSide getGame()
    {
        return (GameClientSide)gui.getGame();
    }

    // Handy shortcut because it's used frequently
    private boolean isFightPhase()
    {
        return getGame().getBattlePhase().isFightPhase();
    }

    // Handy shortcut because it's used frequently
    private boolean isMovePhase()
    {
        return getGame().isBattlePhase(BattlePhase.MOVE);
    }

    private boolean isMyBattleTurn()
    {
        return gui.getOwningPlayer().equals(getGame().getBattleActivePlayer());
    }

    private String getInfoText()
    {
        return infoText;
    }

    private void setupActions()
    {
        showTerrainHazardAction = new AbstractAction(showTerrainHazard)
        {
            public void actionPerformed(ActionEvent e)
            {
                new BattleTerrainHazardWindow(BattleBoard.this, getClient(),
                    battleMap.getMasterHex());
            }
        };
        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!isMyBattleTurn())
                {
                    return;
                }
                if (isMovePhase())
                {
                    selectedCritterTag = -1;
                    gui.undoLastBattleMove();
                    highlightMobileCritters();
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!isMyBattleTurn())
                {
                    return;
                }
                if (isMovePhase())
                {
                    selectedCritterTag = -1;
                    gui.undoAllBattleMoves();
                    highlightMobileCritters();
                }
            }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!isMyBattleTurn())
                {
                    return;
                }

                if (isMovePhase())
                {
                    if (!getClient().getOptions().getOption(Options.autoPlay)
                        && getGame().getBattle().anyOffboardCreatures()
                        && !confirmLeavingCreaturesOffboard())
                    {
                        return;
                    }
                    unselectAllHexes();
                    battleMap.unselectEntranceHexes();
                    getClient().doneWithBattleMoves();
                }
                else if (isFightPhase())
                {
                    unselectAllHexes();
                    battleMap.unselectEntranceHexes();
                    gui.resetStrikeNumbers();
                    getClient().doneWithStrikes();
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Bogus phase");
                }
            }
        };

        concedeBattleAction = new AbstractAction(concedeBattle)
        {
            public void actionPerformed(ActionEvent e)
            {
                String[] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(BattleBoard.this,
                    "Are you sure you wish to concede the battle?",
                    "Confirm Concession?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    LOGGER.log(Level.INFO, gui.getOwningPlayerName()
                        + " concedes the battle");
                    getClient().concede();
                }
            }
        };
    }

    private void setupTopMenu()
    {
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(phaseMenu);
        menuBar.add(helpMenu);
    }

    private void setupHelpMenu()
    {
        JMenuItem mi;

        mi = helpMenu.add(showTerrainHazardAction);
        mi.setMnemonic(KeyEvent.VK_T);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));

        reqFocus();
    }

    public void setupSummonMenu()
    {
        phaseMenu.removeAll();

        reqFocus();
    }

    public void setupRecruitMenu()
    {
        if (phaseMenu != null)
        {
            phaseMenu.removeAll();
        }

        reqFocus();
    }

    public void setupMoveMenu()
    {
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

        if (isMyBattleTurn())
        {
            highlightMobileCritters();
            reqFocus();
        }
    }

    void setupFightMenu()
    {
        phaseMenu.removeAll();

        if (getClient().getMyEngagedLegion() == null)
        {
            // We are not involved - we can't do concede or done
            return;
        }
        JMenuItem mi;

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(concedeBattleAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        if (isMyBattleTurn())
        {
            highlightCrittersWithTargets();
            reqFocus();
        }
    }

    // TODO make this fetch the phase, instead every caller have to query it
    // to pass it in
    public void setPhase(BattlePhase newBattlePhase)
    {
        if (isMyBattleTurn())
        {
            enableDoneButton();
            infoPanel.setOwnPhase(newBattlePhase.toString());
        }
        else
        {
            disableDoneButton();
            infoPanel.setForeignPhase(newBattlePhase.toString());
        }
    }

    public void setTurn(int newturn)
    {
        infoPanel.turnPanel.advTurn(newturn);
    }

    private void setupIcon()
    {
        List<String> directories = new ArrayList<String>();
        directories.add(Constants.defaultDirName
            + StaticResourceLoader.getPathSeparator() + Constants.imagesDirName);

        String[] iconNames = {
            Constants.battlemapIconImage,
            Constants.battlemapIconText + "-Name-"
                + Constants.battlemapIconTextColor,
            Constants.battlemapIconSubscript + "-Subscript-"
                + Constants.battlemapIconTextColor };

        Image image = StaticResourceLoader.getCompositeImage(iconNames, directories,
            60, 60);

        if (image == null)
        {
            LOGGER.log(Level.SEVERE, "ERROR: Couldn't find Colossus icon");
            dispose();
        }
        else
        {
            setIconImage(image);
        }
    }

    /** Return the BattleChit containing the given point,
     *  or null if none does. */
    private GUIBattleChit getBattleChitAtPoint(Point point)
    {
        // iterate through list backwards, so the topmost chit is found
        List<GUIBattleChit> chits = gui.getGUIBattleChits();
        ListIterator<GUIBattleChit> it = chits
            .listIterator(chits.size());
        while (it.hasPrevious())
        {
            GUIBattleChit battleChit = it.previous();
            if (battleChit.contains(point))
            {
                return battleChit;
            }
        }
        return null;
    }

    public void alignChits(BattleHex battleHex)
    {
        GUIBattleHex hex = battleMap.getGUIHexByModelHex(battleHex);
        List<GUIBattleChit> battleChits = gui
            .getGUIBattleChitsInHex(battleHex);
        int numChits = battleChits.size();
        if (numChits < 1)
        {
            hex.repaint();
            return;
        }

        GUIBattleChit battleChit = battleChits.get(0);
        int chitscale = battleChit.getBounds().width;
        int chitscale4 = chitscale / 4;

        Point point = new Point(hex.findCenter());

        // Cascade chits diagonally.
        int offset = ((chitscale * (1 + numChits))) / 4;
        point.x -= offset;
        point.y -= offset;

        battleMap.add(battleChit);
        battleChit.setLocation(point);

        for (int i = 1; i < numChits; i++)
        {
            point.x += chitscale4;
            point.y += chitscale4;
            battleChit = battleChits.get(i);
            battleChit.setLocation(point);
        }
        hex.repaint();
    }

    private void alignChits(Set<BattleHex> battleHexes)
    {
        for (BattleHex battleHex : battleHexes)
        {
            alignChits(battleHex);
        }
    }

    /** Select all hexes containing critters eligible to move. */
    public void highlightMobileCritters()
    {
        Set<BattleHex> set = getClient().findMobileCritterHexes();
        unselectAllHexes();
        battleMap.unselectEntranceHexes();
        battleMap.selectHexes(set);
        battleMap.selectEntranceHexes(set);
    }

    private void highlightMoves(BattleCritter critter)
    {
        Set<BattleHex> set = getClient().showBattleMoves(critter);
        battleMap.unselectAllHexes();
        battleMap.unselectEntranceHexes();
        battleMap.selectHexes(set);
    }

    /** Select hexes containing critters that have valid strike targets. */
    public void highlightCrittersWithTargets()
    {
        Set<BattleHex> set = getClient().findCrittersWithTargets();
        unselectAllHexes();
        battleMap.selectHexes(set);
        // XXX Needed?
        repaint();
    }

    /** Highlight all hexes with targets that the critter can strike. */
    private void highlightStrikes(BattleUnit battleUnit)
    {
        Set<BattleHex> set = getClient().findStrikes(battleUnit.getTag());
        unselectAllHexes();
        gui.resetStrikeNumbers();
        battleMap.selectHexes(set);
        gui.setStrikeNumbers(battleUnit, set);
        // XXX Needed?
        repaint();
    }

    /** Highlight all hexes to which carries could be applied */
    public void highlightPossibleCarries(Set<BattleHex> set)
    {
        unselectAllHexes();
        gui.resetStrikeNumbers();
        battleMap.selectHexes(set);
        // XXX Needed?
        repaint();
    }

    public void setWaitCursor()
    {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private boolean confirmLeavingCreaturesOffboard()
    {
        String[] options = new String[2];
        options[0] = "Yes";
        options[1] = "No";
        int answer = JOptionPane.showOptionDialog(this,
            "Are you sure you want to leave creatures offboard?",
            "Confirm Leaving Creatures Offboard?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return (answer == JOptionPane.YES_OPTION);
    }

    private void actOnCritter(GUIBattleChit battleChit)
    {
        selectedCritterTag = battleChit.getTag();

        // XXX Put selected chit at the top of the z-order.
        // Then getGUIHexByLabel(hexLabel).repaint();
        if (isMovePhase())
        {
            highlightMoves(battleChit.getBattleUnit());
        }
        else if (isFightPhase())
        {
            getClient().leaveCarryMode();
            highlightStrikes(battleChit.getBattleUnit());
        }
    }

    private void actOnHex(BattleHex hex)
    {
        if (isMovePhase())
        {
            if (selectedCritterTag != -1)
            {
                getClient().doBattleMove(selectedCritterTag, hex);
                selectedCritterTag = -1;
                highlightMobileCritters();
            }
        }
        else if (isFightPhase())
        {
            if (selectedCritterTag != -1)
            {
                getClient().strike(selectedCritterTag, hex);
                selectedCritterTag = -1;
            }
        }
    }

    private void actOnMisclick()
    {
        if (isMovePhase())
        {
            selectedCritterTag = -1;
            highlightMobileCritters();
        }
        else if (isFightPhase())
        {
            selectedCritterTag = -1;
            getClient().leaveCarryMode();
            highlightCrittersWithTargets();
        }
    }

    public void rescale()
    {
        battleMap.setupHexes();

        int chitScale = 4 * Scale.get();
        for (GUIBattleChit battleChit : gui.getGUIBattleChits())
        {
            battleChit.rescale(chitScale);
        }
        alignChits(battleMap.getAllHexes());

        dicePanel.rescale();

        setSize(getPreferredSize());
        pack();
        repaint();
    }

    public void reqFocus()
    {
        if (getClient().getOptions().getOption(Options.stealFocus))
        {
            requestFocus();
            toFront();
        }
    }

    private class TurnPanel extends JPanel
    {

        private final JLabel[] turn;
        private int turnNumber;

        private TurnPanel()
        {
            this(getGame().getVariant().getMaxBattleTurns());
        }

        private TurnPanel(int MAXBATTLETURNS)
        {
            super(new GridLayout((MAXBATTLETURNS + 1) % 8 + 1, 0));
            turn = new JLabel[MAXBATTLETURNS + 1];
            // Create Special labels for Recruitment turns
            int[] REINFORCEMENTTURNS = getGame().getVariant()
                .getReinforcementTurns();
            for (int j : REINFORCEMENTTURNS)
            {
                turn[j - 1] = new JLabel((j) + "+", SwingConstants.CENTER);
                resetTurn(j); // Set thin Border
            }
            // make the final "extra" turn label to show "time loss"
            turn[turn.length - 1] = new JLabel("Loss", SwingConstants.CENTER);
            resetTurn(turn.length);
            // Create remaining labels
            for (int i = 0; i < turn.length; i++)
            {
                if (turn[i] == null)
                {
                    turn[i] = new JLabel(Integer.toString(i + 1),
                        SwingConstants.CENTER);
                    resetTurn(i + 1); // Set thin Borders
                }
            }
            turnNumber = 0;

            for (JLabel label : turn)
            {
                this.add(label);
            }
        }

        private void advTurn(int newturn)
        {
            if (turnNumber > 0)
            {
                resetTurn(turnNumber);
            }
            setTurn(newturn);
        }

        private void resetTurn(int newTurn)
        {
            setBorder(turn[newTurn - 1], 1);
        }

        private void setTurn(int newTurn)
        {
            if (isMyBattleTurn())
            {
                setBorder(turn[newTurn - 1], 5);
            }
            else
            {
                setBorder(turn[newTurn - 1], 3);
            }
            turnNumber = newTurn;
        }

        private void setBorder(JLabel turn, int thick)
        {
            if (thick == 3)
            {
                turn.setBorder(BorderFactory.createLineBorder(Color.GRAY, 3));
            }
            else
            {
                if (thick == 5)
                {
                    turn.setBorder(BorderFactory
                        .createLineBorder(Color.RED, 5));
                }
                else
                {
                    turn
                        .setBorder(BorderFactory.createLineBorder(Color.BLACK));
                }
            }
        }
    } // class TurnPanel

    private class InfoPanel extends JPanel
    {
        private final TurnPanel turnPanel;
        private final JButton doneButton;
        private final JLabel phaseLabel;

        // since inner class most methods can be private.
        private InfoPanel()
        {
            super();
            setLayout(new java.awt.BorderLayout());

            doneButton = new JButton(doneWithPhaseAction);
            add(doneButton, BorderLayout.WEST);

            phaseLabel = new JLabel("- phase -");
            add(phaseLabel, BorderLayout.EAST);

            turnPanel = new TurnPanel();
            add(turnPanel, BorderLayout.CENTER);
        }

        private void setOwnPhase(String s)
        {
            phaseLabel.setText(s);
            doneButton.setEnabled(true);
        }

        private void setForeignPhase(String s)
        {
            String name = getGame().getBattleActivePlayer().getName();
            phaseLabel.setText("(" + name + ") " + s);
            doneButton.setEnabled(false);
        }

        private void disableDoneButton()
        {
            doneButton.setEnabled(false);
        }

        private void enableDoneButton()
        {
            doneButton.setEnabled(true);
        }
    }

    private void enableDoneButton()
    {
        infoPanel.enableDoneButton();
    }

    private void disableDoneButton()
    {
        infoPanel.disableDoneButton();
    }

    public void unselectAllHexes()
    {
        battleMap.unselectAllHexes();
    }

    public void unselectHex(BattleHex hex)
    {
        battleMap.unselectHex(hex);
    }

    public void addDiceResults(String strikerDesc, String targetDesc,
        int targetNumber, List<String> rolls)
    {
        if (rolls.size() == 0)
        {
            return;
        }
        dicePanel.addValues("Battle Phase "
            + getGame().getBattleTurnNumber(),
            getGame()
            .getBattleActivePlayer().getName(), strikerDesc, targetDesc,
            targetNumber, rolls);
        dicePanel.showLastRoll();
    }

    // TODO get rid of this, is is only introduced for refactoring purposes
    public BattleHex getBattleHexByLabel(String hexLabel) {
        return battleMap.getHexByLabel(hexLabel);
    }

    @Override
    public String toString()
    {
        return "BattleBoard for: " + infoText;
    }
}
