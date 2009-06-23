package net.sf.colossus.server;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.Client.ConnectionInitException;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.game.events.AddCreatureEvent;
import net.sf.colossus.game.events.SummonEvent;
import net.sf.colossus.server.BattleServerSide.AngelSummoningStates;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariantKnower;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.webclient.RunGameInSameJVM;
import net.sf.colossus.webclient.WebClient;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


/**
 * Class Game gets and holds high-level data about a Titan game.
 *
 * This is the old design with the game information in the server. Some
 * of the functionality here is supposed to be moved into the {@link net.sf.colossus.game.Game}
 * class which then can be shared between server and clients (the class, not the instances).
 * Other parts should be moved into the {@link Server} class or elsewhere.
 *
 * @author David Ripton
 * @author Bruce Sherrod
 * @author Romain Dolbeau
 */
public final class GameServerSide extends Game
{
    private static final Logger LOGGER = Logger.getLogger(GameServerSide.class
        .getName());

    private int activePlayerNum;
    private int lastRecruitTurnNumber;
    private boolean engagementInProgress;
    private boolean battleInProgress;
    private boolean summoning;
    private boolean reinforcing;
    private boolean acquiring;
    private int pointsScored;
    private int turnCombatFinished;
    private Legion winner;
    private String engagementResult;
    private boolean pendingAdvancePhase;

    private boolean loadingGame;
    private boolean replayOngoing = false;
    private BattleServerSide battle;
    private Server server;
    // Negotiation
    private final Set<Proposal> attackerProposals = new HashSet<Proposal>();
    private final Set<Proposal> defenderProposals = new HashSet<Proposal>();

    private final LinkedList<Player> colorPickOrder = new LinkedList<Player>();
    private List<PlayerColor> colorsLeft;
    private final PhaseAdvancer phaseAdvancer = new GamePhaseAdvancer();
    private final Options options;

    private String hostingPlayerName = null;
    private String flagFilename = null;
    private INotifyWebServer notifyWebServer = null;
    private WebClient startingWebClient = null;

    private final WhatNextManager whatNextManager;
    private History history;

    /**
     * The object that handles the Game Saving procedure
     */
    private final GameSaving gameSaver;

    private static int gameCounter = 1;
    private final String gameId;

    private boolean hotSeatMode = false;
    // currently visible board-player (for hotSeatMode)
    private Player cvbPlayer = null;

    /** Shortcut for UnitTests,
     *  to create a Game with dummy input objects on the fly.
     */
    static GameServerSide makeNewGameServerSide()
    {
        Options startOptions = new Options(Constants.OPTIONS_START);
        WhatNextManager whatNextManager = new WhatNextManager(startOptions);

        Options serverOptions = new Options("UnitTest", true);
        return new GameServerSide(whatNextManager, serverOptions, null, new VariantKnower());
    }

    /** The normal constructor to be used everywhere
     *
     * @param startObj  The 'Start' object that started this game, and thus
     * manages the main control flow which thing to do 'next' when this game
     * is over.
     * @param serverOptions The server side options, initialized from the
     * GetPlayers dialog and/or command line options.
     */
    public GameServerSide(WhatNextManager whatNextMgr, Options serverOptions,
        Variant variant, IVariantKnower variantKnower)
    {
        super(variant, new String[0], variantKnower);
        // later perhaps from command line, GUI, or WebServer set it?
        gameId = "#" + (gameCounter++);
        this.whatNextManager = whatNextMgr;
        this.options = serverOptions;
        this.gameSaver = new GameSaving(this, options);

        InstanceTracker.register(this, "Game at port " + getPort());

        // The caretaker object was created by super(...)
        getCaretaker().addListener(new Caretaker.ChangeListener()
        {
            public void creatureTypeAvailabilityUpdated(CreatureType type,
                int availableCount)
            {
                updateCaretakerDisplaysFor(type);
            }

            public void creatureTypeDeadCountUpdated(CreatureType type,
                int deadCount)
            {
                updateCaretakerDisplaysFor(type);
            }

            public void fullUpdate()
            {
                updateCaretakerDisplays();
            }

        });
    }

    public void setFlagFilename(String flagFilename)
    {
        this.flagFilename = flagFilename;
    }

    public String getHostingPlayer()
    {
        return hostingPlayerName;
    }

    private int getPort()
    {
        int port = options.getIntOption(Options.serveAtPort);
        if (port < 0)
        {
            port = Constants.defaultPort;
        }
        return port;
    }

    private void initServer()
    {
        // create it even if not needed (=no web server).
        // This way we can have all the "if <there is a webserver>"
        // wrappers inside the notify Class, instead spread over the code...

        if (startingWebClient != null)
        {
            notifyWebServer = startingWebClient.getWhomToNotify();
        }
        else
        {
            notifyWebServer = new NotifyWebServerViaFile(flagFilename);
        }

        if (server != null)
        {
            server.setObsolete();
            server.disposeAllClients();
        }
        server = new Server(this, whatNextManager, getPort());
        if (startingWebClient != null)
        {
            // TODO get rid of this:
            // WebClient needs this to start the players local Client.
            startingWebClient.setLocalServer(server);
        }
        try
        {
            server.initFileServer();
            server.initSocketServer();
            server.start();
            notifyWebServer.readyToAcceptClients();

            createLocalClients();

            boolean gotAll = true;
            if (gotAll)
            {
                ViableEntityManager.register(this, "Server/Game " + gameId);

                // Tell WebClient to inform the WebServer that the game
                // was started (on this player's PC) and all clients have
                // connected (WebClients updates the status in bottom row
                // then).
                notifyWebServer.allClientsConnected();

                if (startingWebClient != null)
                {
                    // Tell WebClient to inform the WebServer that the game
                    // was started (on this player's PC) and all clients have
                    // connected (WebClients updates the status in bottom row
                    // then).
                    startingWebClient.informGameStartedLocally();
                }
            }
            else
            {
                LOGGER.warning("waitForClients returned false, "
                    + "indicating that not all clients connected.");
            }
        }

        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Server initialization got Exception "
                + e.getMessage(), e);
        }
    }

    public void createLocalClients()
    {
        boolean atLeastOneBoardNeeded = Constants.FORCE_VIEW_BOARD;
        for (Player player : getPlayers())
        {
            String type = player.getType();
            // getDeadBeforeSave to Game instead?
            if (!((PlayerServerSide)player).getDeadBeforeSave()
                && !type.endsWith(Constants.network))
            {

                if (player.getName().equals(getHostingPlayer()))
                {
                    LOGGER.info("Skipping creation of local client for "
                        + "hosting player " + player.getName());
                }
                else
                {
                    boolean createGUI = !player.isAI();
                    if (atLeastOneBoardNeeded)
                    {
                        // Yes, only depending on the atLeast..., no matter
                        // whether for this player true or not!
                        // This relies on the fact that for cmdline setup
                        // Human players are created before AI players.
                        createGUI = true;
                        atLeastOneBoardNeeded = false;
                    }
                    LOGGER.info("Creating local client for player "
                        + player.getName() + ", type " + type);
                    createLocalClient((PlayerServerSide)player, createGUI,
                        type);
                }
            }
        }
    }

    private void createLocalClient(PlayerServerSide player, boolean createGUI,
        String type)
    {
        String playerName = player.getName();
        boolean dontUseOptionsFile = player.isAI();
        LOGGER.finest("Called Server.createLocalClient() for " + playerName);

        try
        {
            Client.createClient("127.0.0.1", getPort(), playerName, type,
                whatNextManager, server, false, dontUseOptionsFile, createGUI);
        }
        catch (ConnectionInitException e)
        {
            LOGGER.warning("Creating local client for player " + playerName
                + " failed, reason " + e.getMessage());
        }
    }


    /**
     * Update the dead and available counts for a creature type on all clients.
     */
    private void updateCaretakerDisplaysFor(CreatureType type)
    {
        if (replayOngoing)
        {
            return;
        }
        if (server != null)
        {
            server.allUpdateCreatureCount(type, getCaretaker()
                .getAvailableCount(type), getCaretaker().getDeadCount(type));
        }
    }

    /**
     * Update the dead and available counts for all creature types on all clients.
     */
    private void updateCaretakerDisplays()
    {
        if (replayOngoing)
        {
            return;
        }
        for (CreatureType type : getVariant().getCreatureTypes())
        {
            updateCaretakerDisplaysFor(type);
        }
    }

    private void cleanupWhenGameOver()
    {
        server.waitUntilGameFinishes();
        server.cleanup();
        server = null;

        ViableEntityManager.unregister(this);
    }

    private void clearFlags()
    {
        engagementInProgress = false;
        battleInProgress = false;
        summoning = false;
        reinforcing = false;
        acquiring = false;
        pendingAdvancePhase = false;
        loadingGame = false;
        engagementResult = null;
    }

    private void addPlayersFromOptions()
    {
        for (int i = 0; i < VariantSupport.getMaxPlayers(); i++)
        {
            String name = options.getStringOption(Options.playerName + i);
            String type = options.getStringOption(Options.playerType + i);

            if (name != null && type != null && !type.equals(Constants.none))
            {
                addPlayer(name, type);
                LOGGER.info("Added " + type + " player " + name);
            }
        }

        // No longer need the player name and type options
        // - except if needed for next stresstest round.
        if (!Options.isStresstest())
        {
            options.clearPlayerInfo();
        }

        getVariant().getCreatureByName("Titan").setMaxCount(getNumPlayers());
    }

    public boolean startNewGameAndWaitUntilOver(String hostingPlayer)
    {
        boolean ok = newGame(hostingPlayer);

        if (ok)
        {
            // Main thread has now nothing to do any more, can wait
            // until game finishes.
            cleanupWhenGameOver();
        }
        return ok;
    }

    /** Start a new game. */
    boolean newGame(String hostingPlayer)
    {
        hostingPlayerName = hostingPlayer;

        // In case game was started by WebClient locally on user's
        // computer, WebClient cannot be passed into the GameServerSide
        // (to do so, the IStartHandler interface would need to import the
        // WebClient class, and then we would have a cyclic dependency).
        // So, the initiating WebClient stores itself into a static
        // variable which we query here.
        startingWebClient = RunGameInSameJVM.getInitiatingWebClient();

        clearFlags();

        turnNumber = 1;
        lastRecruitTurnNumber = -1;
        setPhase(Phase.SPLIT);
        players.clear();

        VariantSupport.loadVariantByName(options
            .getStringOption(Options.variant), true);

        LOGGER.info("Starting new game");

        CustomRecruitBase.resetAllInstances();
        CustomRecruitBase.setGame(this);

        addPlayersFromOptions();
        // reset the caretaker after we have the players to get the right Titan counts
        getCaretaker().resetAllCounts();

        hotSeatMode = options.getOption(Options.hotSeatMode);

        history = new History();

        // initServer returns after all clients have connected
        // (or if server startup failed for some reason)
        initServer();

        // Some more stuff is done from newGame2() when the last
        // expected client has connected.

        if (!server.isServerRunning())
        {
            // Startup Failed: clean up
            server.cleanup();
            server = null;
            return false;
        }

        // Inform caller that startup went ok
        return true;
    }

    /* Called from the last ClientHandler connecting
     *  ( = when expected nr. of clients has connected).
     */
    void newGame2()
    {
        syncOptions();
        server.allInitBoard();
        assignTowers();

        // Renumber players in descending tower order.
        sortPlayersDescendingTower();
        activePlayerNum = 0;
        assignColors();
    }

    /**
     * Temporary solution ... I do not know a better way how to do the
     * sorting on players (type List<Player>) itself.
     * I don't want to pull up the Comparator predicate, because
     * ClientSide might have totally different idea of the right "order"...
     */
    private void sortPlayersDescendingTower()
    {
        List<PlayerServerSide> playersSS = new ArrayList<PlayerServerSide>();
        for (Player p : getPlayers())
        {
            playersSS.add((PlayerServerSide)p);
        }
        Collections.sort(playersSS);
        players.clear();
        for (PlayerServerSide p : playersSS)
        {
            players.add(p);
        }
    }

    private boolean nameIsTaken(String name, Player checkedPlayer)
    {
        for (int i = 0; i < getNumPlayers(); i++)
        {
            Player player = players.get(i);
            if (player.getName().equals(name) && !player.equals(checkedPlayer))
            {
                return true;
            }
        }
        return false;
    }

    /** If the name is taken, add random digits to the end. */
    String getUniqueName(final String name, Player player)
    {
        if (!nameIsTaken(name, player))
        {
            return name;
        }
        return getUniqueName(name + Dice.rollDie(), player);
    }

    /** Find a Player for a new remote client.
     *  If loading a game, this is the network player with a matching
     *  player name.  If a new game, it's the first network player whose
     *  name is still set to <By client> */
    Player findNetworkPlayer(final String playerName)
    {
        for (int i = 0; i < getNumPlayers(); i++)
        {
            Player curPlayer = players.get(i);

            if (curPlayer.getType().endsWith(Constants.network))
            {
                if (isLoadingGame())
                {
                    if (curPlayer.getName().equals(playerName))
                    {
                        return curPlayer;
                    }
                }
                else
                {
                    if (curPlayer.getName().startsWith(Constants.byClient))
                    {
                        curPlayer.setName(playerName);
                        return curPlayer;
                    }
                }
            }
        }
        return null;
    }

    /** Send all current game option values to all clients. */
    private void syncOptions()
    {
        Enumeration<String> en = options.propertyNames();

        while (en.hasMoreElements())
        {
            String name = en.nextElement();
            String value = options.getStringOption(name);

            server.allSyncOption(name, value);
        }
    }

    private void assignColors()
    {
        List<PlayerColor> cli = new ArrayList<PlayerColor>(Arrays
            .asList(PlayerColor.values()));

        colorsLeft = new ArrayList<PlayerColor>();

        /* Add the first 6 colors in random order, ... */
        for (int i = 0; i < Constants.DEFAULT_MAX_PLAYERS; i++)
        {
            colorsLeft.add(cli.remove(Dice
                .rollDie(Constants.DEFAULT_MAX_PLAYERS - i) - 1));
        }

        /* ... and finish with the newer ones, also in random order */
        int newer = cli.size();

        for (int i = 0; i < newer; i++)
        {
            colorsLeft.add(cli.remove(Dice.rollDie(newer - i) - 1));
        }

        // Let human players pick colors first, followed by AI players.
        // Within each group, players pick colors in ascending tower order.
        colorPickOrder.clear();

        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            Player player = players.get(i);

            if (player.isHuman())
            {
                colorPickOrder.add(player);
            }
        }
        for (int i = getNumPlayers() - 1; i >= 0; i--)
        {
            Player player = players.get(i);

            if (player.isAI())
            {
                colorPickOrder.add(player);
            }
        }

        nextPickColor();
    }

    void nextPickColor()
    {
        if (colorPickOrder.size() >= 1)
        {
            Player playerName = colorPickOrder.getFirst();

            server.askPickColor(playerName, colorsLeft);
        }
        else
        {
            // All players are done picking colors; continue.
            newGame3();
        }
    }

    private String makeNameByType(String templateName, String type)
    {
        String number = templateName.substring(Constants.byType.length());
        // type is the full class name of client, e.g.
        //   "net.sf.colossus.ai.SimpleAI"
        String prefix = Constants.aiPackage;
        int len = prefix.length();

        String shortName = type.substring(len);
        String newName;

        if (shortName.equals("Human"))
        {
            newName = "Human" + number;
        }
        else if (shortName.equals("SimpleAI"))
        {
            newName = "Simple" + number;
        }
        else if (shortName.equals("CowardSimpleAI"))
        {
            newName = "Coward" + number;
        }
        else if (shortName.equals("RationalAI"))
        {
            newName = "Rational" + number;
        }
        else if (shortName.equals("HumanHaterRationalAI"))
        {
            newName = "Hater" + number;
        }
        else if (shortName.equals("MilvangAI"))
        {
            newName = "Milvang" + number;
        }
        else if (shortName.equals("ExperimentalAI"))
        {
            newName = "Experimental" + number;
        }
        else if (shortName.equals("ParallelEvaluatorAI"))
        {
            newName = "ParallelEvaluator" + number;
        }
        else
        {
            newName = null;
        }
        return newName;
    }

    void assignColor(Player player, PlayerColor color)
    {
        colorPickOrder.remove(player);
        colorsLeft.remove(color);
        ((PlayerServerSide)player).setColor(color);
        String type = ((PlayerServerSide)player).getType();
        String gotName = player.getName();
        if (gotName.startsWith(Constants.byType))
        {
            String newName = makeNameByType(gotName, type);
            if (newName != null)
            {
                LOGGER.info("Setting for \"" + gotName + "\" new name: "
                    + newName);
                server.setPlayerName(player, newName);
                player.setName(newName);
            }
            else
            {
                LOGGER.log(Level.WARNING, "Type " + type + " not recognized"
                    + ". Giving name by color instead (" + color + ")");
                gotName = Constants.byColor;
            }
        }

        if (gotName.startsWith(Constants.byColor))
        {
            server.setPlayerName(player, color.getName());
            player.setName(color.getName());
        }
        LOGGER.info(player + " chooses color " + color);
        ((PlayerServerSide)player).initMarkersAvailable();
        server.allUpdatePlayerInfo();
        server.askPickFirstMarker(player);
    }

    Player getNextColorPicker()
    {
        return colorPickOrder.getFirst();
    }

    /** Done picking player colors; proceed to start game. */
    private void newGame3()
    {
        server.allUpdatePlayerInfo();

        for (Player p : getPlayers())
        {
            PlayerServerSide player = (PlayerServerSide)p;
            placeInitialLegion(player, player.getFirstMarker());
            server.allRevealLegion(player.getLegions().get(0),
                Constants.reasonInitial);
            server.allUpdatePlayerInfo();
        }

        server.allTellAllLegionLocations();
        server.allSetupTurnState();
        updateCaretakerDisplays();
        setupSplit();

        gameSaver.commitPointReached();
        autoSave();

        server.allRequestConfirmCatchup("KickstartGame");
    }

    /** Randomize towers by rolling dice and rerolling ties. */
    private void assignTowers()
    {
        int numPlayers = getNumPlayers();
        MasterHex[] playerTower = new MasterHex[numPlayers];
        Set<MasterHex> towerSet = getVariant().getMasterBoard().getTowerSet();

        // first create a list with all tower hexes
        List<MasterHex> towerList = new ArrayList<MasterHex>(towerSet);

        if (getOption(Options.balancedTowers))
        {
            towerList = getBalancedTowers(numPlayers, towerList);
        }

        int playersLeft = numPlayers - 1;

        while ((playersLeft >= 0) && (!towerList.isEmpty()))
        {
            int which = Dice.rollDie(towerList.size());
            playerTower[playersLeft] = towerList.remove(which - 1);
            playersLeft--;
        }

        for (int i = 0; i < numPlayers; i++)
        {
            Player player = players.get(i);
            LOGGER.info(player + " gets tower " + playerTower[i]);
            player.setStartingTower(playerTower[i]);
        }
    }

    /** Return a list with a balanced order of numPlayer towers chosen
     from towerList, which must hold numeric strings. */
    static List<MasterHex> getBalancedTowers(int numPlayers,
        final List<MasterHex> towerList)
    {
        int numTowers = towerList.size();

        if (numPlayers > numTowers)
        {
            LOGGER.log(Level.SEVERE, "More players than towers!");
            return towerList;
        }

        // Make a sorted copy, converting String to Integer.
        ArrayList<Integer> numericList = new ArrayList<Integer>();

        for (MasterHex tower : towerList)
        {
            Integer i = new Integer(tower.getLabel());

            numericList.add(i);
        }
        Collections.sort(numericList);

        double towersPerPlayer = (double)numTowers / numPlayers;

        // First just find a balanced sequence starting at zero.
        double counter = 0.0;
        int numDone = 0;
        List<Integer> sequence = new ArrayList<Integer>();
        // Prevent floating-point roundoff error.
        double epsilon = 0.0000001;

        while (numDone < numPlayers)
        {
            sequence.add(Integer.valueOf((int)Math.floor(counter + epsilon)));
            numDone++;
            counter += towersPerPlayer;
        }

        // Pick a random starting point.  (Zero-based)
        int startingTower = Dice.rollDie(numTowers) - 1;

        // Offset the sequence by the starting point, and get only
        // the number of starting towers we need.
        List<MasterHex> returnList = new ArrayList<MasterHex>();

        Iterator<Integer> it = sequence.iterator();
        numDone = 0;
        while (it.hasNext() && numDone < numPlayers)
        {
            Integer raw = it.next();
            int cooked = (raw.intValue() + startingTower) % numTowers;
            Integer numericLabel = numericList.get(cooked);

            returnList.add(VariantSupport.getCurrentVariant().getMasterBoard()
                .getHexByLabel(numericLabel.toString()));
            numDone++;
        }
        return returnList;
    }

    Server getServer()
    {
        return server;
    }

    PlayerServerSide addPlayer(String name, String shortTypeName)
    {
        PlayerServerSide player = new PlayerServerSide(name, this,
            shortTypeName);

        players.add(player);
        return player;
    }

    // Only meant for GameSaving!
    int getActivePlayerNum()
    {
        return activePlayerNum;
    }

    Player getActivePlayer()
    {
        // Sanity check in case called before all players are loaded.
        if (activePlayerNum < players.size())
        {
            return players.get(activePlayerNum);
        }
        else
        {
            return null;
        }
    }

    /**
     * Resolve playerName into Player object. Name might be null,
     * then returns null.
     * @param playerName
     * @return The player object for given player name, null if name was null
     */
    Player getPlayerByNameIgnoreNull(String playerName)
    {
        if (playerName == null)
        {
            return null;
        }
        else
        {
            return getPlayerByName(playerName);
        }
    }

    /**
     * Resolve playerName into Player object.
     * Name must not be null. If no player for given name found,
     * it would throw IllegalArgumentException
     * @param playerName
     * @return Player object for given name.
     */
    Player getPlayerByName(String playerName)
    {
        assert playerName != null : "Name for player to find must not be null!";

        for (Player player : players)
        {
            if (player.getName().equals(playerName))
            {
                return player;
            }
        }
        throw new IllegalArgumentException("No player object found for name '"
            + playerName + "'");
    }

    /**
     *  NOTE: to be used only during loading a Game!
     *  Client side has a more sophisticated version that takes
     *  slain players and their inherited markers into account.
     */
    Player getPlayerByShortColor(String shortColor)
    {
        if (shortColor != null)
        {
            for (Player player : getPlayers())
            {
                if (shortColor.equals(player.getShortColor()))
                {
                    return player;
                }
            }
        }
        return null;
    }

    /**
     * A player requested he wants to withdraw (or connection was lost, and
     * server socket handling does withdraw then).
     *
     * @param player The player that wishes to withdraw from the game
     *
     * TODO Notify all players.
     */
    void handlePlayerWithdrawal(Player player)
    {
        String name = player.getName();

        if (player.isDead())
        {
            LOGGER.log(Level.FINE, "Nothing to do for withdrawal of player "
                + name + " - is already dead.");
            return;
        }

        LOGGER.log(Level.FINE, "Player " + name + " withdraws from the game.");

        // If player quits while engaged, set slayer.
        Player slayer = null;
        Legion legion = player.getTitanLegion();
        if (legion != null && isEngagement(legion.getCurrentHex()))
        {
            slayer = getFirstEnemyLegion(legion.getCurrentHex(), player)
                .getPlayer();
        }
        ((PlayerServerSide)player).die(slayer);
        checkForVictory();

        // checks if game over state is reached, and if yes, announces so;
        // and returns false.
        // Otherwise it returns true and that means game shall go on.
        if (gameShouldContinue())
        {
            if (player == getActivePlayer())
            {
                advancePhase(getPhase(), player);
            }
        }
    }

    private Player getWinner()
    {
        int remaining = 0;
        Player result = null;
        for (Player player : getPlayers())
        {
            if (!player.isDead())
            {
                remaining++;
                if (remaining > 1)
                {
                    return null;
                }
                else
                {
                    result = player;
                }
            }
        }
        return result;
    }

    // TODO Up to game.Game or not?
    //      In practice it is only needed in server side.
    void checkForVictory()
    {
        if (isGameOver())
        {
            LOGGER
                .severe("checkForVictory called although game is already over!!");
            return;
        }

        int remaining = getNumLivingPlayers();

        switch (remaining)
        {
            case 0:
                LOGGER.info("Reached game over state -- Draw at "
                    + new Date().getTime());
                setGameOver(true, "Draw");
                break;

            case 1:
                String winnerName = getWinner().getName();
                LOGGER.info("Reached game over state -- " + winnerName
                    + " wins at " + new Date().getTime());
                setGameOver(true, winnerName + " wins");
                break;

            default:
                break;
        }
    }

    private void announceGameOver(boolean disposeFollows)
    {
        server.allFullyUpdateAllLegionContents(Constants.reasonGameOver);
        LOGGER.info("Announcing: Game over -- " + getGameOverMessage());
        server.allTellGameOver(getGameOverMessage(), disposeFollows);
    }

    boolean isLoadingGame()
    {
        return loadingGame;
    }

    boolean isReplayOngoing()
    {
        return replayOngoing;
    }

    public void stopAllDueToFunctionalTestCompleted()
    {
        server.doSetWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
        LOGGER.info("kickstartGame - before stop server running");
        server.stopServerRunning();
        server.cleanupStartlog();
    }

    public void kickstartGame()
    {
        LOGGER.info("All clients have caught up with loading/replay or "
            + "pickColor, now kicking off the Game!");

        if (Options.isFunctionalTest())
        {
            stopAllDueToFunctionalTestCompleted();
        }
        else
        {
            server.kickPhase();
        }
    }

    /**
     * Advance to the next phase, only if the passed oldPhase and playerName
     * are current.
     */
    void advancePhase(final Phase oldPhase, final Player player)
    {
        if (oldPhase != phase)
        {
            LOGGER.severe("Player " + player
                + " called advancePhase illegally (reason: " + "oldPhase ("
                + oldPhase + ") != phase (" + phase + "))");
            return;
        }
        if (pendingAdvancePhase)
        {
            LOGGER
                .severe("Player "
                    + player
                    + " called advancePhase illegally (reason: pendingAdvancePhase is true)");
            return;
        }
        if (!player.equals(getActivePlayer()))
        {
            LOGGER.severe("Player " + player
                + " called advancePhase illegally (reason: "
                + "wrong player [" + player + " vs. " + getActivePlayer()
                + "])");
            return;
        }
        if (getOption(Options.autoStop) && onlyAIsRemain()
            && !isGameOver())
        {
            LOGGER.info("Not advancing because no humans remain");
            setGameOver(true, "All humans eliminated");
        }

        if (gameShouldContinue())
        {
            phaseAdvancer.advancePhase();
        }
    }

    /** Wrap the complexity of phase advancing. */
    class GamePhaseAdvancer implements PhaseAdvancer
    {

        /** Advance to the next phase, only if the passed oldPhase and
         *  playerName are current. */
        public void advancePhase()
        {
            pendingAdvancePhase = true;
            advancePhaseInternal();
        }

        /** Advance to the next phase, with no error checking. */
        public void advancePhaseInternal()
        {
            Phase oldPhase = phase;
            if (oldPhase == Phase.SPLIT)
            {
                setPhase(Phase.MOVE);
            }
            else if (oldPhase == Phase.MOVE)
            {
                // skip phase totally if there aren't any engagements.
                if (findEngagements().size() > 0)
                {
                    setPhase(Phase.FIGHT);
                }
                else
                {
                    setPhase(Phase.MUSTER);
                }
            }
            else if (oldPhase == Phase.FIGHT)
            {
                setPhase(Phase.MUSTER);
            }

            if (oldPhase == Phase.MUSTER
                || (getActivePlayer().isDead() && getNumLivingPlayers() > 0))
            {
                advanceTurn();
            }
            else
            {
                LOGGER.info("Phase advances to " + phase);
            }

            pendingAdvancePhase = false;

            if (isPhase(Phase.SPLIT))
            {
                server.allSetupTurnState();
            }

            // A new phase starts.
            // First, set it up and create commit point (take snapshot)
            setupPhase();
            gameSaver.commitPointReached();

            if (isAutoSavePoint())
            {
                autoSave();
            }

            // Next, initial actions in that phase. So far, only for move.
            if (isPhase(Phase.MOVE))
            {
                Player player = getActivePlayer();
                ((PlayerServerSide)player).rollMovement();
            }

            // Inform Client now it's his time to act.
            server.kickPhase();
        }

        /**
         * Make the next player being the activePlayer, and set phase to Split.
         * If that next player is dead, advance again (recursively).
         */
        public void advanceTurn()
        {
            clearFlags();
            activePlayerNum++;
            if (activePlayerNum == getNumPlayers())
            {
                activePlayerNum = 0;
                turnNumber++;
                if (turnNumber - lastRecruitTurnNumber > 100
                    && Options.isStresstest())
                {
                    LOGGER.info("\nLast recruiting is 100 turns ago - "
                        + "exiting to prevent AIs from endlessly "
                        + "running around...\n");
                    System.exit(0);
                }
            }

            /* notify all CustomRecruitBase object that we change the
             active player, for bookkeeping purpose */
            CustomRecruitBase.everyoneAdvanceTurn(activePlayerNum);

            setPhase(Phase.SPLIT);
            if (getActivePlayer().isDead() && getNumLivingPlayers() > 0)
            {
                advanceTurn();
            }
            else
            {
                LOGGER.info(getActivePlayer() + "'s turn, number "
                    + turnNumber);
            }
        }
    }

    private void setupPhase()
    {
        if (isPhase(Phase.SPLIT))
        {
            setupSplit();
        }
        else if (isPhase(Phase.MOVE))
        {
            setupMove();
        }
        else if (isPhase(Phase.FIGHT))
        {
            setupFight();
        }
        else if (isPhase(Phase.MUSTER))
        {
            setupMuster();
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Bogus phase");
        }
    }

    private void setupSplit()
    {
        Player player = getActivePlayer();

        if (player == null)
        {
            LOGGER.log(Level.SEVERE, "No players");
            dispose();
            return;
        }
        ((PlayerServerSide)player).resetTurnState();
        server.allSetupSplit();
        if (hotSeatMode)
        {
            hotSeatModeChangeBoards();
        }
    }

    private void hotSeatModeChangeBoards()
    {
        Player activePlayer = getActivePlayer();

        // game just started - find the local player which shall
        // get the board first, and hide all other local ones.
        if (cvbPlayer == null)
        {
            int i;
            for (i = 0; i < getNumPlayers(); i++)
            {
                Player iPlayer = players.get(i);
                if (iPlayer.isLocalHuman() && !server.isClientGone(iPlayer))
                {
                    // This is a local alive player.
                    if (cvbPlayer == null)
                    {
                        cvbPlayer = iPlayer;
                        server.setBoardVisibility(iPlayer, true);
                    }
                    else
                    {
                        server.setBoardVisibility(iPlayer, false);
                    }
                }
            }
            return;
        }

        // otherwise, switch board to next, then and only then
        // if activePlayer is now the next local human which is
        // still connected ( = has not closed his board).
        if (activePlayer.isLocalHuman() && !server.isClientGone(activePlayer))
        {
            server.setBoardVisibility(cvbPlayer, false);
            server.setBoardVisibility(activePlayer, true);
            cvbPlayer = activePlayer;
        }
    }

    private void setupMove()
    {
        server.allSetupMove();
    }

    private void setupFight()
    {
        server.allSetupFight();
        server.nextEngagement();
    }

    private void setupMuster()
    {
        Player player = getActivePlayer();
        ((PlayerServerSide)player).removeEmptyLegions();
        // If a player has been eliminated we can't count on his client
        // still being around to advance the turn.
        if (player.isDead())
        {
            advancePhase(Phase.MUSTER, player);
        }
        else
        {
            server.allSetupMuster();
        }
    }

    /**
     * So far, we do autosave only at begin of each players turn, i.e. when
     * a split phase was just set up.
     * @return Whether now is a time when autosave is due.
     */
    private boolean isAutoSavePoint()
    {
        if (isPhase(Phase.SPLIT))
        {
            return true;
        }
        return false;
    }

    void autoSave()
    {
        if (getOption(Options.autosave) && !isGameOver())
        {
            gameSaver.saveGameWithErrorHandling(null, true);
        }
    }

    void saveGameWithErrorHandling(String filename, boolean autoSave)
    {
        gameSaver.saveGameWithErrorHandling(filename, autoSave);
    }

    /**
     * Try to load a game from saveDirName/filename.
     *
     * If the filename is "--latest" then load the latest savegame found in saveDirName.
     */
    // JDOM lacks generics, so we need casts
    @SuppressWarnings("unchecked")
    public void loadGame(String filename)
    {
        File file = null;

        if (filename.equals("--latest"))
        {
            File dir = new File(Constants.SAVE_DIR_NAME);

            if (!dir.exists() || !dir.isDirectory())
            {
                LOGGER.log(Level.SEVERE, "No saves directory");
                dispose();
                return;
            }
            String[] filenames = dir.list(new XMLSnapshotFilter());

            if (filenames.length < 1)
            {
                LOGGER.log(Level.SEVERE,
                    "No XML savegames found in saves directory");
                dispose();
                return;
            }
            file = new File(Constants.SAVE_DIR_NAME
                + latestSaveFilename(filenames));
        }
        else if (filename.indexOf("/") >= 0 || filename.indexOf("\\") >= 0)
        {
            // Already a full path
            file = new File(filename);
        }
        else
        {
            file = new File(Constants.SAVE_DIR_NAME + filename);
        }

        if (!file.exists())
        {
            String tryXMLFile = file.getPath() + ".xml";
            File xmlFile = new File(tryXMLFile);
            if (xmlFile.exists())
            {
                LOGGER.warning("Given filename does not exist - loading "
                    + "instead the one with .xml appended to the name!");
                file = xmlFile;
            }
            else
            {
                LOGGER.severe("Cannot load saved game: file " + file.getPath()
                    + " does not exist!");
                return;
            }
        }

        try
        {
            LOGGER.info("Loading game from " + file);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(file);

            Element root = doc.getRootElement();
            Attribute ver = root.getAttribute("version");

            if (!ver.getValue().equals(Constants.XML_SNAPSHOT_VERSION))
            {
                LOGGER.severe("Can't load this savegame version.");
                // TODO not only would this fail to load quietly, it also fails
                // to fail quietly rather noisily by causing an NPE in dispose().
                dispose();
                return;
            }

            // Reset flags that are not in the savegame file.
            clearFlags();
            loadingGame = true;

            Element el = root.getChild("Variant");
            Attribute dir = el.getAttribute("dir");
            Attribute fil = el.getAttribute("file");

            Attribute namAttr = el.getAttribute("name");
            String varName = null;
            if (namAttr != null)
            {
                varName = namAttr.getValue();
            }

            VariantSupport.freshenVariant(fil.getValue(), dir.getValue());

            // then load data files
            List<Element> datafilesElements = root.getChildren("DataFile");
            Iterator<Element> it = datafilesElements.iterator();
            while (it.hasNext())
            {
                Element dea = it.next();
                String mapKey = dea.getAttributeValue("DataFileKey");
                List<?> contentList = dea.getContent();
                if (contentList.size() > 0)
                {
                    String content = ((CDATA)contentList.get(0)).getText();
                    LOGGER.finest("DataFileKey: " + mapKey
                        + " DataFileContent :\n" + content);
                    StaticResourceLoader
                        .putIntoFileCache(mapKey, content.getBytes());
                }
                else
                {
                    StaticResourceLoader.putIntoFileCache(mapKey, new byte[0]);
                }
            }

            // we're server, but the file generation process has been done
            // by loading the savefile.
            Variant variant = VariantSupport.loadVariant(varName, fil
                .getValue(), dir.getValue(), false);
            setVariant(variant);
            // old save games (before r3360, 09/2008) do not save the
            // variant name - retrieve it back from VariantSupport.
            // TODO remove this one day?
            if (varName == null)
            {
                varName = VariantSupport.getVariantName();
            }
            options.setOption(Options.variant, varName);

            el = root.getChild("TurnNumber");
            turnNumber = Integer.parseInt(el.getTextTrim());
            // not quite the same as it was when saved, but the idea of lastRTN
            // is only to prevent stresstest games from hanging forever...
            lastRecruitTurnNumber = turnNumber;

            el = root.getChild("CurrentPlayer");
            activePlayerNum = Integer.parseInt(el.getTextTrim());

            el = root.getChild("CurrentPhase");
            setPhase(Phase.fromInt(Integer.parseInt(el.getTextTrim())));

            Element ct = root.getChild("Caretaker");
            List<Element> kids = ct.getChildren();
            it = kids.iterator();

            while (it.hasNext())
            {
                el = it.next();
                String creatureName = el.getAttribute("name").getValue();
                int remaining = el.getAttribute("remaining").getIntValue();
                int dead = el.getAttribute("dead").getIntValue();
                CreatureType creature = getVariant().getCreatureByName(
                    creatureName);

                getCaretaker().setAvailableCount(creature, remaining);
                getCaretaker().setDeadCount(creature, dead);
            }

            players.clear();
            if (battle != null)
            {
                server.allCleanupBattle();
            }

            // Players
            List<Element> playerElements = root.getChildren("Player");

            it = playerElements.iterator();
            while (it.hasNext())
            {
                Element pla = it.next();

                String name = pla.getAttribute("name").getValue();
                String type = pla.getAttribute("type").getValue();

                PlayerServerSide player = addPlayer(name, type);

                String colorName = pla.getAttribute("color").getValue();
                player.setColor(PlayerColor.getByName(colorName));

                String towerLabel = pla.getAttribute("startingTower")
                    .getValue();
                player.setStartingTower(getVariant().getMasterBoard()
                    .getHexByLabel(towerLabel));

                int score = pla.getAttribute("score").getIntValue();
                player.setScore(score);

                // Don't use the normal "dead" attribute - will be set
                // during replay...
                boolean dead = pla.getAttribute("dead").getBooleanValue();
                player.setDeadBeforeSave(dead);

                int mulligansLeft = pla.getAttribute("mulligansLeft")
                    .getIntValue();
                player.setMulligansLeft(mulligansLeft);

                /*
                player.setMovementRoll(pla.getAttribute("movementRoll")
                    .getIntValue());
                */

                /*
                player.setTeleported(pla.getAttribute("teleported")
                    .getBooleanValue());
                */
                // TODO what about the donor value? Just summoned is
                // good enough, so that at least one cannot summon
                // twice in same engagements-phase,
                // but not good enough to save a game in mid-battle
                // in particular in battle turn 4.
                player.setSummoned(pla.getAttribute("summoned")
                    .getBooleanValue());

                String playersElim = pla.getAttribute("colorsElim").getValue();
                if ("null".contains(playersElim))
                {
                    playersElim = "";
                }
                player.setPlayersElim(playersElim);

                List<Element> legionElements = pla.getChildren("Legion");
                Iterator<Element> it2 = legionElements.iterator();
                while (it2.hasNext())
                {
                    Element leg = it2.next();
                    readLegion(leg, player);
                }
            }

            // Battle stuff
            // TODO if the loading of a battle would be moved into the BattleServerSide class, then
            // the AngelSummoningStates and LegionTags enums could be private (possibly with some holes
            // for testing)
            Element bat = root.getChild("Battle");
            if (bat != null)
            {
                String engagementHexLabel = bat.getAttribute("masterHexLabel")
                    .getValue();
                MasterHex engagementHex = getVariant().getMasterBoard()
                    .getHexByLabel(engagementHexLabel);
                int battleTurnNum = bat.getAttribute("turnNumber")
                    .getIntValue();
                String battleActivePlayerName = bat.getAttribute(
                    "activePlayer").getValue();
                BattlePhase battlePhase = BattlePhase.values()[bat
                    .getAttribute("phase").getIntValue()];
                AngelSummoningStates summonState = AngelSummoningStates
                    .valueOf(bat.getAttribute("summonState").getValue());
                int carryDamage = bat.getAttribute("carryDamage")
                    .getIntValue();
                boolean driftDamageApplied = bat.getAttribute(
                    "driftDamageApplied").getBooleanValue();

                List<Element> cts = bat.getChildren("CarryTarget");
                Set<BattleHex> carryTargets = new HashSet<BattleHex>();
                Iterator<Element> it2 = cts.iterator();
                while (it2.hasNext())
                {
                    Element cart = it2.next();
                    carryTargets.add(engagementHex.getTerrain().getHexByLabel(
                        cart.getTextTrim()));
                }

                Player attackingPlayer = getActivePlayer();
                Legion attacker = getFirstFriendlyLegion(engagementHex,
                    attackingPlayer);
                Legion defender = getFirstEnemyLegion(engagementHex,
                    attackingPlayer);

                BattleServerSide.LegionTags activeLegionTag;
                if (battleActivePlayerName.equals(attackingPlayer.getName()))
                {
                    activeLegionTag = BattleServerSide.LegionTags.ATTACKER;
                }
                else
                {
                    activeLegionTag = BattleServerSide.LegionTags.DEFENDER;
                }

                battle = new BattleServerSide(this, attacker, defender,
                    activeLegionTag, engagementHex, battleTurnNum, battlePhase);
                battle.setSummonState(summonState);
                battle.setCarryDamage(carryDamage);
                battle.setDriftDamageApplied(driftDamageApplied);
                battle.setCarryTargets(carryTargets);
            }

            // Backup Legion data and wipe it out, so that history
            // starts from a clean table. After history reply, we compare
            // whether the replay result matches this "loaded data".
            for (Player buPlayer : getPlayers())
            {
                ((PlayerServerSide)buPlayer).backupLoadedData();
            }

            // Load history (RedoLog stuff is handled later)
            history = new History(root);

            initServer();
            // Remaining stuff has been moved to loadGame2()

            // Some more stuff is done from loadGame2() when the last
            // expected client has connected.
            // Main thread has now nothing to do any more, can wait
            // until game finishes.
            cleanupWhenGameOver();
        }
        catch (RuntimeException rte)
        {
            LOGGER.log(Level.SEVERE, "RuntimeException!! "
                + "While trying to load (corrupt?) savegame", rte);
            dispose();
            return;
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE, "Tried to load corrupt savegame", ex);
            dispose();
            return;
        }
    }

    // JDOM lacks generics, so we need casts
    @SuppressWarnings("unchecked")
    private void readLegion(Element leg, PlayerServerSide player)
        throws DataConversionException
    {
        String markerId = leg.getAttribute("name").getValue();
        String currentHexLabel = leg.getAttribute("currentHex").getValue();
        MasterHex currentHex = getVariant().getMasterBoard().getHexByLabel(
            currentHexLabel);
        String startingHexLabel = leg.getAttribute("startingHex").getValue();
        MasterHex startingHex = getVariant().getMasterBoard().getHexByLabel(
            startingHexLabel);
        boolean moved = leg.getAttribute("moved").getBooleanValue();
        EntrySide entrySide = EntrySide.values()[leg.getAttribute(
        "entrySide").getIntValue()];
        String parentId = leg.getAttribute("parent").getValue();
        if (parentId.equals("null"))
        {
            parentId = null;
        }

        CreatureType recruit = null;
        String recruitName = leg.getAttribute("recruitName").getValue();
        if (recruitName != null && !recruitName.equals("null"))
        {
            recruit = getVariant().getCreatureByName(recruitName);
        }

        int battleTally = leg.getAttribute("battleTally").getIntValue();

        // Critters
        // find legion for them, if it doesn't exist create one
        LegionServerSide legion = (LegionServerSide)player
            .getLegionByMarkerId(markerId);
        if (legion == null)
        {
            // TODO can there ever be a legion before? If not: collect all data
            // first (including critters) and then create the legion in one go
            Legion parentLegion = player.getLegionByMarkerId(parentId);
            legion = new LegionServerSide(markerId, parentLegion, currentHex,
                startingHex, player, this);
            player.addLegion(legion);
        }
        else
        {
            LOGGER.warning("Legion for marker does already exist?");
        }

        List<Element> creatureElements = leg.getChildren("Creature");
        for (Element cre : creatureElements)
        {
            String name = cre.getAttribute("name").getValue();
            CreatureServerSide critter = new CreatureServerSide(getVariant()
                .getCreatureByName(name), null, this);

            // Battle stuff
            if (cre.getAttribute("hits") != null)
            {
                int hits = cre.getAttribute("hits").getIntValue();

                critter.setHits(hits);

                MasterBoardTerrain terrain = getVariant().getMasterBoard()
                    .getHexByLabel(currentHexLabel).getTerrain();
                String currentBattleHexLabel = cre.getAttribute("currentHex")
                    .getValue();
                BattleHex currentBattleHex = terrain
                    .getHexByLabel(currentBattleHexLabel);
                critter.setCurrentHex(currentBattleHex);

                String startingBattleHexLabel = cre
                    .getAttribute("startingHex").getValue();
                BattleHex startingBattleHex = terrain
                    .getHexByLabel(startingBattleHexLabel);
                critter.setStartingHex(startingBattleHex);

                boolean struck = cre.getAttribute("struck").getBooleanValue();

                critter.setStruck(struck);
            }

            legion.addCritter(critter);
        }

        legion.setMoved(moved);
        legion.setRecruit(recruit);
        legion.setEntrySide(entrySide);
        legion.addToBattleTally(battleTally);
    }

    /* Called from the last ClientHandler connecting
     *  ( = when expected nr. of clients has connected).
     */
    boolean loadGame2()
    {
        server.allSetColor();

        syncOptions();

        server.allUpdatePlayerInfo(true);
        replayOngoing = true;
        server.allTellReplay(true, turnNumber);
        server.allInitBoard();

        // XXX
        // System.out
        //     .println("\n################\nFiring history events from XML");
        history.fireEventsFromXML(server);
        // System.out
        //    .println("################\nCompleted Firing history events from XML\n");

        boolean ok = resyncBackupData();
        LOGGER.info("Loading and resync result: " + ok);

        if (!ok)
        {
            LOGGER.severe("Loading and resync failed - Aborting!!");
            return false;
        }

        for (Player player : getPlayers())
        {
            ((PlayerServerSide)player).computeMarkersAvailable();
        }

        server.allFullyUpdateLegionStatus();
        server.allUpdatePlayerInfo(false);
        server.allTellAllLegionLocations();
        updateCaretakerDisplays();

        server.allSetupTurnState();
        setupPhase();
        // Create an initial snapshot
        gameSaver.commitPointReached();

        server.allTellRedo(true);
        // XXX
        // System.out.println("\n################\nProcessing redo log ");
        history.processRedoLog(server);

        // XXX
        // System.out
        //    .println("################\nCompleted Processing redo log\n");

        server.allTellRedo(false);
        server.allTellReplay(false, 0);
        replayOngoing = false;

        if (battle != null)
        {
            battle.setServer(getServer());
            battle.init();
        }

        server.allRequestConfirmCatchup("KickstartGame");
        return ok;
    }

    private boolean resyncBackupData()
    {
        boolean allOk = true;
        for (Player player : getPlayers())
        {
            allOk = allOk && ((PlayerServerSide)player).resyncBackupData();
        }
        return allOk;
    }

    /** Extract and return the numeric part of a filename. */
    private long numberValue(String filename)
    {
        StringBuilder numberPart = new StringBuilder();

        for (int i = 0; i < filename.length(); i++)
        {
            char ch = filename.charAt(i);

            if (Character.isDigit(ch))
            {
                numberPart.append(ch);
            }
        }
        try
        {
            return Long.parseLong(numberPart.toString());
        }
        catch (NumberFormatException e)
        {
            return -1L;
        }
    }

    /** Find the save filename with the highest numerical value.
     (1000000000.sav comes after 999999999.sav) */
    private String latestSaveFilename(String[] filenames)
    {
        return Collections.max(Arrays.asList(filenames),
            new Comparator<String>()
            {
                public int compare(String s1, String s2)
                {
                    long diff = (numberValue(s1) - numberValue(s2));

                    if (diff > Integer.MAX_VALUE)
                    {
                        return Integer.MAX_VALUE;
                    }
                    if (diff < Integer.MIN_VALUE)
                    {
                        return Integer.MIN_VALUE;
                    }
                    return (int)diff;
                }
            });
    }

    /**
     * Return a list of eligible recruits, as Creatures.
     *
     * TODO second parameter is probably superfluous
     */
    List<CreatureType> findEligibleRecruits(Legion legion, MasterHex hex)
    {
        List<CreatureType> recruits;

        MasterBoardTerrain terrain = hex.getTerrain();

        recruits = new ArrayList<CreatureType>();
        List<CreatureType> tempRecruits = TerrainRecruitLoader
            .getPossibleRecruits(terrain, hex);
        List<CreatureType> recruiters = TerrainRecruitLoader
            .getPossibleRecruiters(terrain, hex);

        for (CreatureType creature : tempRecruits)
        {
            for (CreatureType lesser : recruiters)
            {
                if ((TerrainRecruitLoader.numberOfRecruiterNeeded(lesser,
                    creature, terrain, hex) <= ((LegionServerSide)legion)
                    .numCreature(lesser))
                    && (recruits.indexOf(creature) == -1))
                {
                    recruits.add(creature);
                }
            }
        }

        // Make sure that the potential recruits are available.
        Iterator<CreatureType> it = recruits.iterator();
        while (it.hasNext())
        {
            CreatureType recruit = it.next();
            if (getCaretaker().getAvailableCount(recruit) < 1)
            {
                it.remove();
            }
        }
        return recruits;
    }

    /** Return a list of eligible recruiter creatures. */
    private List<CreatureType> findEligibleRecruiters(Legion legion,
        String recruitName)
    {
        List<CreatureType> recruiters;
        CreatureType recruit = getVariant().getCreatureByName(recruitName);
        if (recruit == null)
        {
            return new ArrayList<CreatureType>();
        }

        MasterHex hex = legion.getCurrentHex();
        MasterBoardTerrain terrain = hex.getTerrain();
        recruiters = TerrainRecruitLoader.getPossibleRecruiters(terrain, hex);
        Iterator<CreatureType> it = recruiters.iterator();
        while (it.hasNext())
        {
            CreatureType possibleRecruiter = it.next();
            int needed = TerrainRecruitLoader.numberOfRecruiterNeeded(
                possibleRecruiter, recruit, terrain, hex);

            if (needed < 1
                || needed > ((LegionServerSide)legion)
                    .numCreature(possibleRecruiter))
            {
                // Zap this possible recruiter.
                it.remove();
            }
        }
        return recruiters;
    }

    /**
     * Return true if this legion can recruit this recruit
     * without disclosing a recruiter.
     */
    private boolean anonymousRecruitLegal(Legion legion, CreatureType recruit)
    {
        return TerrainRecruitLoader.anonymousRecruitLegal(recruit, legion
            .getCurrentHex().getTerrain(), legion.getCurrentHex());
    }

    /** Add recruit to legion. */
    void doRecruit(Legion legion, CreatureType recruit, CreatureType recruiter)
    {
        if (recruit == null)
        {
            LOGGER.log(Level.SEVERE, "null recruit in Game.doRecruit()");
            return;
        }
        // Check for recruiter legality.
        List<CreatureType> recruiters = findEligibleRecruiters(legion, recruit
            .getName());

        if (recruiter == null)
        {
            // If recruiter can be anonymous, then this is okay.
            if (!anonymousRecruitLegal(legion, recruit))
            {
                LOGGER.log(Level.SEVERE, "null recruiter in Game.doRecruit()");
                // XXX Let it go for now  Should return later
            }
            else
            {
                LOGGER.finest("null recruiter okay");
            }
        }
        else if (!recruiters.contains(recruiter))
        {
            LOGGER.log(Level.SEVERE, "Illegal recruiter "
                + recruiter.getName() + " for recruit " + recruit.getName());
            return;
        }

        lastRecruitTurnNumber = turnNumber;

        if (((LegionServerSide)legion).addCreature(recruit, true))
        {
            MasterHex hex = legion.getCurrentHex();
            int numRecruiters = 0;

            if (recruiter != null)
            {
                // Mark the recruiter(s) as visible.
                numRecruiters = TerrainRecruitLoader.numberOfRecruiterNeeded(
                    recruiter, recruit, hex.getTerrain(), hex);
            }

            LOGGER.info("Legion "
                + legion
                + " in "
                + hex.getDescription()
                + " recruits "
                + recruit.getName()
                + " with "
                + (recruiter == null ? "nothing" : numRecruiters
                    + " "
                    + (numRecruiters > 1 ? recruiter.getPluralName()
                        : recruiter.getName())));

            // Recruits are one to a customer.
            legion.setRecruit(recruit);
            reinforcing = false;
        }
        else
        {
            LOGGER.finest("Did not add creature " + recruit.getName()
                + " - none left in caretaker to take!");
        }
    }

    /**
     * If the legion can acquire (height < 7), find out which acquirable it
     * might get for the pointsToAdd, and fire off the askAcquirable messages.
     * @param legion  Legion which earned the points and thus is entitled to
     *                get the acqirable
     * @param scoreBeforeAdd Score from which to start
     * @param pointsToAdd How many points were earned
     */
    public void acquireMaybe(LegionServerSide legion, int scoreBeforeAdd,
        int pointsToAdd)
    {
        if (legion.getHeight() < 7)
        {
            // calculate and set them as pending
            legion.setupAcquirableDecisions(scoreBeforeAdd, pointsToAdd);
            // make the server send the ask... to the client
            legion.askAcquirablesDecisions();
        }

    }

    void dispose()
    {
        if (server != null)
        {
            server.stopServerRunning();
        }
        notifyWebServer.serverStoppedRunning();
        notifyWebServer = null;
    }

    private void placeInitialLegion(PlayerServerSide player, String markerId)
    {
        String name = player.getName();
        player.selectMarkerId(markerId);
        LOGGER.info(name + " selects initial marker");

        // Lookup coords for chit starting from player[i].getTower()
        MasterHex hex = player.getStartingTower();
        LegionServerSide legion = getStartingLegion(markerId, hex, player);
        player.addLegion(legion);
    }

    /** Set the entry side relative to the hex label. */
    // TODO I've seen this code somewhere else
    private EntrySide findEntrySide(MasterHex hex, int cameFrom)
    {
        int entrySide = -1;
        if (cameFrom != -1)
        {
            if (hex.getTerrain().hasStartList())
            {
                entrySide = 3;
            }
            else
            {
                entrySide = (6 + cameFrom - hex.getLabelSide()) % 6;
            }
        }
        return EntrySide.values()[entrySide];
    }

    /** Recursively find conventional moves from this hex.
     *  If block >= 0, go only that way.  If block == -1, use arches and
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.  Return a set of
     *  hexLabel:entrySide tuples.
     *
     *  TODO use proper data structure instead of String serializations
     */
    private Set<String> findNormalMoves(MasterHex hex, Legion legion,
        int roll, int block, int cameFrom, boolean ignoreFriends)
    {
        Set<String> set = new HashSet<String>();
        Player player = legion.getPlayer();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (getNumEnemyLegions(hex, player) > 0)
        {
            if (getNumFriendlyLegions(hex, player) == 0 || ignoreFriends)
            {
                // Set the entry side relative to the hex label.
                if (cameFrom != -1)
                {
                    set.add(hex.getLabel() + ":"
                        + findEntrySide(hex, cameFrom).getLabel());
                }
            }
            return set;
        }

        if (roll == 0)
        {
            // XXX fix
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            List<? extends Legion> legions = player.getLegions();
            for (Legion otherLegion : legions)
            {
                if (!ignoreFriends && otherLegion != legion
                    && hex.equals(otherLegion.getCurrentHex()))
                {
                    return set;
                }
            }

            if (cameFrom != -1)
            {
                set.add(hex.getLabel() + ":"
                    + findEntrySide(hex, cameFrom).getLabel());
                return set;
            }
        }

        if (block >= 0)
        {
            set.addAll(findNormalMoves(hex.getNeighbor(block), legion,
                roll - 1, Constants.ARROWS_ONLY, (block + 3) % 6,
                ignoreFriends));
        }
        else if (block == Constants.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i).ordinal() >= Constants.HexsideGates.ARCH
                    .ordinal()
                    && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6,
                        ignoreFriends));
                }
            }
        }
        else if (block == Constants.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i).ordinal() >= Constants.HexsideGates.ARROW
                    .ordinal()
                    && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6,
                        ignoreFriends));
                }
            }
        }
        return set;
    }

    /** Recursively find all unoccupied hexes within roll hexes, for
     *  tower teleport. */
    private Set<MasterHex> findNearbyUnoccupiedHexes(MasterHex hex,
        Legion legion, int roll, int cameFrom, boolean ignoreFriends)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.
        Set<MasterHex> set = new HashSet<MasterHex>();
        if (!isOccupied(hex))
        {
            set.add(hex);
        }
        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom
                    && (hex.getExitType(i) != Constants.HexsideGates.NONE || hex
                        .getEntranceType(i) != Constants.HexsideGates.NONE))
                {
                    set.addAll(findNearbyUnoccupiedHexes(hex.getNeighbor(i),
                        legion, roll - 1, (i + 3) % 6, ignoreFriends));
                }
            }
        }
        return set;
    }

    private int findBlock(MasterHex hex)
    {
        int block = Constants.ARCHES_AND_ARROWS;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == Constants.HexsideGates.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }
        return block;
    }

    /** Return set of hexLabels describing where this legion can move
     *  without teleporting.  Include moves currently blocked by friendly
     *  legions if ignoreFriends is true. */
    Set<MasterHex> listNormalMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        if (((LegionServerSide)legion).hasMoved())
        {
            return new HashSet<MasterHex>();
        }
        Set<String> tuples = findNormalMoves(hex, legion, movementRoll,
            findBlock(hex), Constants.NOWHERE, ignoreFriends);

        // Extract just the hexLabels from the hexLabel:entrySide tuples.
        Set<MasterHex> result = new HashSet<MasterHex>();
        Iterator<String> it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = it.next();
            List<String> parts = Split.split(':', tuple);
            String hexLabel = parts.get(0);

            result.add(getVariant().getMasterBoard().getHexByLabel(hexLabel));
        }
        return result;
    }

    private boolean towerTeleportAllowed()
    {
        if (getOption(Options.noTowerTeleport))
        {
            return false;
        }
        if (getTurnNumber() == 1 && getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    private boolean towerToTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (getTurnNumber() == 1 && getOption(Options.noFirstTurnT2TTeleport))
        {
            return false;
        }
        return true;
    }

    private boolean towerToNonTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (getOption(Options.towerToTowerTeleportOnly))
        {
            return false;
        }
        return true;
    }

    private boolean titanTeleportAllowed()
    {
        if (getOption(Options.noTitanTeleport))
        {
            return false;
        }
        if (getTurnNumber() == 1 && getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    /** Return set of hexLabels describing where this legion can teleport.
     *  Include moves currently blocked by friendly legions if
     *  ignoreFriends is true. */
    private Set<MasterHex> listTeleportMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        Player player = legion.getPlayer();
        Set<MasterHex> result = new HashSet<MasterHex>();
        if (movementRoll != 6 || legion.hasMoved() || player.hasTeleported())
        {
            return result;
        }

        // Tower teleport
        if (hex.getTerrain().isTower() && legion.numLords() > 0
            && towerTeleportAllowed())
        {
            // Mark every unoccupied hex within 6 hexes.
            if (towerToNonTowerTeleportAllowed())
            {
                result.addAll(findNearbyUnoccupiedHexes(hex, legion, 6,
                    Constants.NOWHERE, ignoreFriends));
            }

            if (towerToTowerTeleportAllowed())
            {
                // Mark every unoccupied tower.
                Set<MasterHex> towerSet = getVariant().getMasterBoard()
                    .getTowerSet();
                for (MasterHex tower : towerSet)
                {
                    if ((!isOccupied(tower) || (ignoreFriends && getNumEnemyLegions(
                        tower, player) == 0))
                        && (!(tower.equals(hex))))
                    {
                        result.add(tower);
                    }
                }
            }
            else
            {
                // Remove nearby towers from set.
                Set<MasterHex> towerSet = getVariant().getMasterBoard()
                    .getTowerSet();
                for (MasterHex tower : towerSet)
                {
                    result.remove(tower);
                }
            }
        }

        // Titan teleport
        if (player.canTitanTeleport() && legion.hasTitan()
            && titanTeleportAllowed())
        {
            // Mark every hex containing an enemy stack that does not
            // already contain a friendly stack.
            for (Legion other : getAllEnemyLegions(player))
            {
                MasterHex otherHex = other.getCurrentHex();
                if (!isEngagement(otherHex) || ignoreFriends)
                {
                    result.add(otherHex);
                }
            }
        }
        result.remove(null);
        return result;
    }

    /** Return a Set of Strings "Left" "Right" or "Bottom" describing
     *  possible entry sides.  If the hex is unoccupied, just return
     *  one entry side since it doesn't matter. */
    private Set<EntrySide> listPossibleEntrySides(Legion legion,
        MasterHex targetHex, boolean teleport)
    {
        Set<EntrySide> entrySides = new HashSet<EntrySide>();
        Player player = legion.getPlayer();
        int movementRoll = ((PlayerServerSide)player).getMovementRoll();
        MasterHex currentHex = legion.getCurrentHex();

        if (teleport)
        {
            if (listTeleportMoves(legion, currentHex, movementRoll, false)
                .contains(targetHex))
            {
                // Startlisted terrain only have bottom entry side.
                // Don't bother finding more than one entry side if unoccupied.
                if (!isOccupied(targetHex)
                    || targetHex.getTerrain().hasStartList())
                {
                    entrySides.add(EntrySide.BOTTOM);
                    return entrySides;
                }
                else
                {
                    entrySides.add(EntrySide.BOTTOM);
                    entrySides.add(EntrySide.LEFT);
                    entrySides.add(EntrySide.RIGHT);
                    return entrySides;
                }
            }
            else
            {
                return entrySides;
            }
        }

        // Normal moves.
        Set<String> tuples = findNormalMoves(currentHex, legion, movementRoll,
            findBlock(currentHex), Constants.NOWHERE, false);
        Iterator<String> it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = it.next();
            List<String> parts = Split.split(':', tuple);
            String hl = parts.get(0);

            if (hl.equals(targetHex.getLabel()))
            {
                String buf = parts.get(1);

                entrySides.add(EntrySide.fromLabel(buf));

                // Clemens 4.10.2007:
                // This optimization can lead to problems ("Illegal entry side")
                // in mountains/tundra on a movement roll 4, when client and
                // server store the items in their Move-hashmaps in different
                // order (different java version, platform, ... ?)
                // So, removed this optimization to see whether it fixes the bug:
                //  [colossus-Bugs-1789116 ] illegal move: 29 plain to 2000 tundra
                /*
                 // Don't bother finding more than one entry side if unoccupied.
                 if (!isOccupied(targetHexLabel))
                 {
                 return entrySides;
                 }
                 */
            }
        }
        return entrySides;
    }

    void createSummonAngel(Legion attacker)
    {
        if (!isGameOver())
        {
            summoning = true;
            server.createSummonAngel(attacker);
        }
    }

    /** Called locally and from Battle. */
    void reinforce(Legion legion)
    {
        reinforcing = true;
        server.reinforce(legion);
    }

    void doneReinforcing()
    {
        reinforcing = false;
        checkEngagementDone();
    }

    /**
     * Handles summoning of a creature.
     *
     * @param event The summoning event (or null if summoning is to be skipped)
     *
     * TODO get rid of downcasts
     */
    void doSummon(SummonEvent event)
    {
        PlayerServerSide player = (PlayerServerSide)getActivePlayer();

        if (event != null
            && ((LegionServerSide)event.getLegion()).canSummonAngel())
        {
            Legion legion = event.getLegion();
            // Only one angel can be summoned per turn.
            player.setSummoned(true);
            Legion donor = event.getDonor();
            player.setDonor(((LegionServerSide)donor));

            // Move the angel or archangel.
            CreatureType angel = event.getAddedCreatureType();
            ((LegionServerSide)donor).removeCreature(angel, false, false);
            ((LegionServerSide)legion).addCreature(angel, false);

            server.allTellRemoveCreature(donor, angel, true,
                Constants.reasonSummon);
            server.allTellAddCreature(event, true);

            server.allTellDidSummon(legion, donor, angel);

            LOGGER.info("One " + angel + " is summoned from legion " + donor
                + " into legion " + legion);
        }

        // Need to call this regardless to advance past the summon phase.
        if (battle != null)
        {
            battle.finishSummoningAngel(player.hasSummoned());
            summoning = false;
        }
        else
        {
            summoning = false;
            checkEngagementDone();
        }
    }

    BattleServerSide getBattle()
    {
        return battle;
    }

    boolean isBattleInProgress()
    {
        return battleInProgress;
    }

    boolean isEngagementInProgress()
    {
        return engagementInProgress;
    }

    History getHistory()
    {
        return history;
    }

    void finishBattle(MasterHex masterHex, boolean attackerEntered,
        int points, int turnDone)
    {
        battle.cleanRefs();
        battle = null;
        server.allCleanupBattle();
        LegionServerSide battleWinner = null;

        // Handle any after-battle angel summoning or recruiting.
        if (getNumLegions(masterHex) == 1)
        {
            battleWinner = (LegionServerSide)getFirstLegion(masterHex);

            // Make all creatures in the victorious legion visible.
            server.allRevealLegion(battleWinner, Constants.reasonWinner);
            // Remove battle info from winning legion and its creatures.
            battleWinner.clearBattleInfo();

            if (battleWinner.getPlayer() == getActivePlayer())
            {
                // Attacker won, so possibly summon angel.
                if (battleWinner.canSummonAngel())
                {
                    createSummonAngel(battleWinner);
                }
            }
            else
            {
                // Defender won, so possibly recruit reinforcement.
                if (attackerEntered && battleWinner.canRecruit())
                {
                    LOGGER
                        .finest("Calling Game.reinforce() from Game.finishBattle()");
                    reinforce(battleWinner);
                }
            }
        }
        battleInProgress = false;

        setEngagementResult(Constants.erMethodFight, battleWinner, points,
            turnDone);
        checkEngagementDone();
    }

    /** Return true and call Server.didSplit() if the split succeeded.
     *  Return false if it failed. */
    boolean doSplit(Legion parent, String childId,
        List<CreatureType> creaturesToSplit)
    {
        PlayerServerSide player = (PlayerServerSide)parent.getPlayer();

        // Need a legion marker to split.
        if (!player.isMarkerAvailable(childId))
        {
            LOGGER.log(Level.SEVERE, "Marker " + childId
                + " is not available.");
            return false;
        }

        // Pre-split legion must have 4+ creatures.
        if (((LegionServerSide)parent).getHeight() < 4)
        {
            LOGGER.log(Level.SEVERE, "Legion " + parent
                + " is too short to split.");
            return false;
        }

        if (creaturesToSplit == null)
        {
            LOGGER
                .finest("Empty split list (" + parent + ", " + childId + ")");
            return false;
        }

        // Each legion must have 2+ creatures after the split.
        if (creaturesToSplit.size() < 2
            || ((LegionServerSide)parent).getHeight()
                - creaturesToSplit.size() < 2)
        {
            LOGGER.finest("Too small/big split list (" + parent + ", "
                + childId + ")");
            return false;
        }

        List<CreatureType> tempCreatureTypes = new ArrayList<CreatureType>();

        for (Creature creature : parent.getCreatures())
        {
            tempCreatureTypes.add(creature.getType());
        }

        for (CreatureType creatureType : creaturesToSplit)
        {
            if (!tempCreatureTypes.remove(creatureType))
            {
                LOGGER.finest("Unavailable creature in split list (" + parent
                    + ", " + childId + ") : " + creatureType.getName());
                return false;
            }
        }

        if (getTurnNumber() == 1)
        {
            // Only allow a single split on turn 1.
            if (player.getLegions().size() > 1)
            {
                LOGGER.log(Level.SEVERE, "Cannot split twice on Turn 1.");
                return false;
            }
            // Each stack must contain exactly 4 creatures.
            if (creaturesToSplit.size() != 4)
            {
                return false;
            }
            // Each stack must contain exactly 1 lord.
            int numLords = 0;

            for (CreatureType creature : creaturesToSplit)
            {
                if (creature.isLord())
                {
                    numLords++;
                }
            }
            if (numLords != 1)
            {
                return false;
            }
        }

        Legion newLegion = ((LegionServerSide)parent)
            .split(creaturesToSplit,
            childId);
        if (newLegion == null)
        {
            return false;
        }

        server.allTellDidSplit(parent, newLegion, getTurnNumber(), true);

        // viewableAll depends on the splitPrediction to tell then true contents,
        // and viewableOwn it does not harm; it only helps the AIs :)

        String viewModeOpt = options.getStringOption(Options.viewMode);
        int viewModeOptNum = options.getNumberForViewMode(viewModeOpt);

        if (viewModeOptNum == Options.viewableAllNum
            || viewModeOptNum == Options.viewableOwnNum)
        {
            server.allRevealLegion(parent, Constants.reasonSplit);
            server.allRevealLegion(newLegion, Constants.reasonSplit);
        }
        else
        {
            server.oneRevealLegion(parent, player, Constants.reasonSplit);
            server.oneRevealLegion(newLegion, player, Constants.reasonSplit);
        }
        return true;
    }

    /** Move the legion to the hex if legal.  Return a string telling
     *  the reason why it is illegal, or null if ok and move was done.
     */
    String doMove(Legion legion, MasterHex hex, EntrySide entrySide,
        boolean teleport, CreatureType teleportingLord)
    {
        assert legion != null : "Legion must not be null";

        Player player = legion.getPlayer();
        // Verify that the move is legal.
        if (teleport)
        {
            if (!listTeleportMoves(legion, legion.getCurrentHex(),
                ((PlayerServerSide)player).getMovementRoll(), false).contains(
                hex))
            {
                String marker = legion.getMarkerId() + " "
                    + ((LegionServerSide)legion).getMarkerName();
                Set<MasterHex> set = listTeleportMoves(legion, legion
                    .getCurrentHex(), ((PlayerServerSide)player)
                    .getMovementRoll(), false);
                return "List for teleport moves " + set + " of " + marker
                    + " from " + legion.getCurrentHex()
                    + " does not contain '" + hex + "'";
            }
        }
        else
        {
            if (!listNormalMoves(legion, legion.getCurrentHex(),
                ((PlayerServerSide)player).getMovementRoll(), false).contains(
                hex))
            {
                String marker = legion.getMarkerId() + " "
                    + ((LegionServerSide)legion).getMarkerName();
                Set<MasterHex> set = listNormalMoves(legion, legion
                    .getCurrentHex(), ((PlayerServerSide)player)
                    .getMovementRoll(), false);
                return "List for normal moves " + set + " + of " + marker
                    + " from " + legion.getCurrentHex()
                    + " does not contain '" + hex + "'";
            }
        }

        // Verify that the entry side is legal.
        Set<EntrySide> legalSides = listPossibleEntrySides(legion, hex,
            teleport);
        if (!legalSides.contains(entrySide))
        {
            return "EntrySide '" + entrySide + "' is not valid, valid are: "
                + legalSides.toString();
        }

        // If this is a tower hex, the only entry side is the bottom.
        if (hex.getTerrain().hasStartList()
            && !entrySide.equals(EntrySide.BOTTOM))
        {
            LOGGER.log(Level.WARNING, "Tried to enter invalid side of tower");
            entrySide = EntrySide.BOTTOM;
        }

        // If the legion teleported, reveal a lord.
        if (teleport)
        {
            // Verify teleporting lord.
            if (teleportingLord == null
                || !((LegionServerSide)legion).listTeleportingLords(hex)
                    .contains(teleportingLord))
            {
                // boil out if assertions are enabled -- gives a stacktrace
                assert false : "Illegal teleport";
                if (teleportingLord == null)
                {
                    return "teleportingLord null";
                }
                return "list of telep. lords "
                    + ((LegionServerSide)legion).listTeleportingLords(hex)
                        .toString() + " does not contain '" + teleportingLord
                    + "'";
            }
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            creatures.add(teleportingLord);
            server.allRevealCreatures(legion, creatures,
                Constants.reasonTeleport);
        }
        ((LegionServerSide)legion).moveToHex(hex, entrySide, teleport,
            teleportingLord);
        legionMoveEvent(legion, hex, entrySide, teleport, teleportingLord);
        return null;
    }

    void undoMove(Legion legion)
    {
        MasterHex formerHex = legion.getCurrentHex();

        PlayerServerSide activePlayer = (PlayerServerSide)getActivePlayer();
        activePlayer.undoMove(legion);
        MasterHex currentHex = legion.getCurrentHex();

        // TODO calculate on client side instead where needed
        // needed in undidMove to decide whether to dis/enable button
        boolean splitLegionHasForcedMove = activePlayer
            .splitLegionHasForcedMove();

        legionUndoMoveEvent(legion);
        server.allTellUndidMove(legion, formerHex, currentHex,
            splitLegionHasForcedMove);
    }

    void engage(MasterHex hex)
    {
        // Do not allow clicking on engagements if one is
        // already being resolved.
        if (isEngagement(hex) && !engagementInProgress)
        {
            engagementInProgress = true;
            Player player = getActivePlayer();
            Legion attacker = getFirstFriendlyLegion(hex, player);
            Legion defender = getFirstEnemyLegion(hex, player);

            server.allTellEngagement(hex, attacker, defender);

            ((LegionServerSide)attacker).sortCritters();
            ((LegionServerSide)defender).sortCritters();

            server.oneRevealLegion(attacker, defender.getPlayer(),
                Constants.reasonEngaged);
            server.oneRevealLegion(defender, attacker.getPlayer(),
                Constants.reasonEngaged);

            if (defender.canFlee())
            {
                // Fleeing gives half points and denies the
                // attacker the chance to summon an angel.
                server.askFlee(defender, attacker);
            }
            else
            {
                engage2(hex);
            }
        }
        else
        {
            LOGGER.finest("illegal call to Game.engage() "
                + engagementInProgress);
        }
    }

    // Defender did not flee; attacker may concede early.
    private void engage2(MasterHex hex)
    {
        Player player = getActivePlayer();
        Legion attacker = getFirstFriendlyLegion(hex, player);
        Legion defender = getFirstEnemyLegion(hex, player);

        server.askConcede(attacker, defender);
    }

    // Attacker did not concede early; negotiate.
    private void engage3(MasterHex hex)
    {
        Player player = getActivePlayer();
        Legion attacker = getFirstFriendlyLegion(hex, player);
        Legion defender = getFirstEnemyLegion(hex, player);

        attackerProposals.clear();
        defenderProposals.clear();
        server.twoNegotiate(attacker, defender);
    }

    void flee(Legion legion)
    {
        Legion attacker = getFirstEnemyLegion(legion.getCurrentHex(), legion
            .getPlayer());

        handleConcession(legion, attacker, true);
    }

    void concede(Legion attacker)
    {
        if (battleInProgress)
        {
            battle.concede(attacker.getPlayer());
        }
        else
        {
            Legion defender = getFirstEnemyLegion(attacker.getCurrentHex(),
                attacker.getPlayer());

            handleConcession(attacker, defender, false);
        }
    }

    void doNotFlee(Legion legion)
    {
        engage2(legion.getCurrentHex());
    }

    /** Used only for pre-battle attacker concession. */
    void doNotConcede(Legion legion)
    {
        engage3(legion.getCurrentHex());
    }

    /** playerName offers proposal. */
    void makeProposal(String playerName, String proposalString)
    {
        // If it's too late to negotiate, just throw this away.
        if (battleInProgress)
        {
            return;
        }

        Proposal proposal = Proposal.makeFromString(proposalString, this);
        final Set<Proposal> ourProposals;
        final Set<Proposal> opponentProposals;

        if (playerName.equals(getActivePlayer().getName()))
        {
            ourProposals = attackerProposals;
            opponentProposals = defenderProposals;
        }
        else
        {
            ourProposals = defenderProposals;
            opponentProposals = attackerProposals;
        }

        // If this player wants to fight, cancel negotiations.
        if (proposal.isFight())
        {
            Legion attacker = proposal.getAttacker();
            fight(attacker.getCurrentHex());
        }

        // If this proposal matches an earlier one from the other player,
        // settle the engagement.
        else if (opponentProposals.contains(proposal))
        {
            handleNegotiation(proposal);
        }

        // Otherwise remember this proposal and continue.
        else
        {
            ourProposals.add(proposal);
            Player other = null;
            if (playerName.equals(getActivePlayer().getName()))
            {
                Legion defender = proposal.getDefender();

                other = defender.getPlayer();
            }
            else
            {
                other = getActivePlayer();
            }

            // Tell the other player about the proposal.
            server.tellProposal(other, proposal);
        }
    }

    void fight(MasterHex hex)
    {
        if (!battleInProgress)
        {
            Player player = getActivePlayer();
            Legion attacker = getFirstFriendlyLegion(hex, player);
            Legion defender = getFirstEnemyLegion(hex, player);

            // If the second player clicks Fight from the negotiate
            // dialog late, just exit.
            if (attacker == null || defender == null)
            {
                return;
            }

            battleInProgress = true;

            // Reveal both legions to all players.
            server.allRevealEngagedLegion(attacker, true,
                Constants.reasonBattleStarts);
            server.allRevealEngagedLegion(defender, false,
                Constants.reasonBattleStarts);

            battle = new BattleServerSide(this, attacker, defender,
                BattleServerSide.LegionTags.DEFENDER, hex, 1, BattlePhase.MOVE);
            battle.init();
        }
    }

    private void handleConcession(Legion loser, Legion winner, boolean fled)
    {
        // Figure how many points the victor receives.
        int points = ((LegionServerSide)loser).getPointValue();

        if (fled)
        {
            points /= 2;
            LOGGER.info("Legion " + loser + " flees from legion " + winner);
        }
        else
        {
            LOGGER.info("Legion " + loser + " concedes to legion " + winner);
        }

        // Add points, and angels if necessary.
        ((PlayerServerSide)winner.getPlayer()).awardPoints(points,
            (LegionServerSide)winner, fled);
        // @TODO: probably the truncating is not needed at all here?
        // Remove any fractional points.
        ((PlayerServerSide)winner.getPlayer()).truncScore();

        // Need to grab the player reference before the legion is
        // removed.
        Player losingPlayer = loser.getPlayer();

        String reason = fled ? Constants.reasonFled
            : Constants.reasonConcession;
        server.allRevealEngagedLegion(loser, losingPlayer
            .equals(getActivePlayer()), reason);

        // server.allRemoveLegion(loser.getMarkerId());

        // If this was the titan stack, its owner dies and gives half
        // points to the victor.
        if (((LegionServerSide)loser).hasTitan())
        {
            // first remove dead legion, then the rest. Cannot do the
            // loser.remove outside/before the if (or would need to store
            // the hasTitan information as extra boolean)
            ((LegionServerSide)loser).remove();
            ((PlayerServerSide)losingPlayer).die(winner.getPlayer());
            checkForVictory();
        }
        else
        {
            // simply remove the dead legion.
            ((LegionServerSide)loser).remove();
        }

        // No recruiting or angel summoning is allowed after the
        // defender flees or the attacker concedes before entering
        // the battle.
        String method = fled ? Constants.erMethodFlee
            : Constants.erMethodConcede;
        setEngagementResult(method, winner, points, 0);
        checkEngagementDone();
    }

    private void handleNegotiation(Proposal results)
    {
        LegionServerSide attacker = (LegionServerSide)results.getAttacker();
        LegionServerSide defender = (LegionServerSide)results.getDefender();
        LegionServerSide negotiatedWinner = null;
        int points = 0;

        if (results.isMutual())
        {
            boolean attackerHasTitan = attacker.hasTitan();
            boolean defenderHasTitan = defender.hasTitan();

            // Remove both legions and give no points.
            attacker.remove();
            defender.remove();

            LOGGER.info(attacker + " and " + defender
                + " agree to mutual elimination");

            // If both Titans died, eliminate both players.
            if (attackerHasTitan && defenderHasTitan)
            {
                // Make defender die first, to simplify turn advancing.
                defender.getPlayer().die(null);
                attacker.getPlayer().die(null);
                checkForVictory();
            }

            // If either was the titan stack, its owner dies and gives
            // half points to the victor.
            else if (attackerHasTitan)
            {
                attacker.getPlayer().die(defender.getPlayer());
                checkForVictory();
            }

            else if (defenderHasTitan)
            {
                defender.getPlayer().die(attacker.getPlayer());
                checkForVictory();
            }
        }
        else
        {
            // One legion was eliminated during negotiations.
            negotiatedWinner = (LegionServerSide)results.getWinner();
            LegionServerSide loser;
            if (negotiatedWinner == defender)
            {
                loser = attacker;
            }
            else
            {
                loser = defender;
            }

            StringBuilder log = new StringBuilder("Winning legion ");

            log.append(negotiatedWinner.getLongMarkerName());
            log.append(" loses creatures ");

            // Remove all dead creatures from the winning legion.
            List<String> winnerLosses = results.getWinnerLosses();
            Iterator<String> it = winnerLosses.iterator();
            while (it.hasNext())
            {
                String creatureName = it.next();
                log.append(creatureName);
                if (it.hasNext())
                {
                    log.append(", ");
                }
                CreatureType creature = getVariant().getCreatureByName(
                    creatureName);
                negotiatedWinner.removeCreature(creature, true, true);
                server.allTellRemoveCreature(negotiatedWinner, creature, true,
                    Constants.reasonNegotiated);
            }
            LOGGER.info(log.toString());

            server.oneRevealLegion(negotiatedWinner, attacker.getPlayer(),
                Constants.reasonNegotiated);
            server.oneRevealLegion(negotiatedWinner, defender.getPlayer(),
                Constants.reasonNegotiated);

            points = loser.getPointValue();

            PlayerServerSide losingPlayer = loser.getPlayer();

            // Need to check and remember this before removing the legion
            boolean loserHasTitan = loser.hasTitan();

            // Remove the losing legion.
            loser.remove();

            // Add points, and angels if necessary.
            negotiatedWinner.getPlayer().awardPoints(points, negotiatedWinner,
                false);

            LOGGER.info("Legion " + loser + " is eliminated by legion "
                + negotiatedWinner + " via negotiation");

            // If this was the titan stack, its owner dies and gives half
            // points to the victor.
            if (loserHasTitan)
            {
                losingPlayer.die(negotiatedWinner.getPlayer());
                checkForVictory();
            }

            if (isGameOver())
            {
                LOGGER.info("Negotiation (non-mutual) causes Game Over - "
                    + "skipping summon/reinforce procedures.");

            }
            else if (negotiatedWinner == defender)
            {
                if (defender.canRecruit())
                {
                    // If the defender won the battle by agreement,
                    // he may recruit.
                    reinforce(defender);
                }
            }
            else
            {
                if (attacker.canSummonAngel())
                {
                    // If the attacker won the battle by agreement,
                    // he may summon an angel.
                    createSummonAngel(attacker);
                }
            }
        }

        setEngagementResult(Constants.erMethodNegotiate, negotiatedWinner,
            points, 0);
        checkEngagementDone();
    }

    void askAcquireAngel(PlayerServerSide player, Legion legion,
        List<CreatureType> recruits)
    {
        acquiring = true;
        server.askAcquireAngel(player, legion, recruits);
    }

    void doneAcquiringAngels()
    {
        acquiring = false;
        checkEngagementDone();
    }

    private void setEngagementResult(String aResult, Legion winner,
        int aPoints, int aTurn)
    {
        engagementResult = aResult;
        this.winner = winner;
        pointsScored = aPoints;
        turnCombatFinished = aTurn;
    }

    private void checkEngagementDone()
    {
        if (summoning || reinforcing || acquiring || engagementResult == null)
        {
            return;
        }

        engagementInProgress = false;

        server.allUpdatePlayerInfo();

        server.allTellEngagementResults(winner, engagementResult,
            pointsScored, turnCombatFinished);

        engagementResult = null;

        // This output is produced for optimizing AI battle functionality:
        String result = "not set";
        if (winner == null)
        {
            result = "draw";
        }
        else
        {
            result = "winner is " + winner.getMarkerId() + " (player "
                + winner.getPlayer().getName() + ")";
        }
        LOGGER.info("Battle completed, result: " + result);

        // This comes from a system property:
        if (Constants.END_AFTER_FIRST_BATTLE)
        {
            LOGGER.info("endAfterFirstBattle is set, terminating game.");
            server.doSetWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
            server.triggerDispose();
            return;
        }

        if (gameShouldContinue())
        {
            server.nextEngagement();
        }
    }

    /*
     * returns true if game should go on.
     */
    public boolean gameShouldContinue()
    {
        if (isGameOver())
        {
            if (getOption(Options.autoQuit))
            {
                LOGGER.info("Reached Game Over - announce and quit");
                announceGameOver(true);
                server.doSetWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
                LOGGER
                    .info("Reached Game Over, AutoQuit - trigger Game Dispose");
                server.triggerDispose();
            }
            else
            {
                LOGGER.info("Reached Game Over - just announce");
                announceGameOver(false);
            }
            LOGGER.info("Game is now over - returning false");
            return false;
        }
        else
        {
            LOGGER.finest("Game is NOT over yet - returning true");
            return true;
        }
    }

    @Override
    public LegionServerSide getLegionByMarkerId(String markerId)
    {
        for (Player player : players)
        {
            LegionServerSide legion = (LegionServerSide)player
                .getLegionByMarkerId(markerId);
            if (legion != null)
            {
                return legion;
            }
        }
        LOGGER.warning("Can't find legion for markerId '" + markerId + "'");
        assert false : "Request for unknown legion '" + markerId + "'";
        return null;
    }

    // TODO copy and paste from Client class
    public Player getPlayerByMarkerId(String markerId)
    {
        assert markerId != null : "Parameter must not be null";

        String shortColor = markerId.substring(0, 2);
        return getPlayerUsingColor(shortColor);
    }

    // TODO copy and paste from Client class
    private Player getPlayerUsingColor(String shortColor)
    {
        assert players != null : "Game not yet initialized";
        assert shortColor != null : "Parameter must not be null";

        // Stage 1: See if the player who started with this color is alive.
        for (Player player : players)
        {
            if (shortColor.equals(player.getShortColor()) && !player.isDead())
            {
                return player;
            }
        }

        // Stage 2: He's dead.  Find who killed him and see if he's alive.
        for (Player player : players)
        {
            if (player.getPlayersElim().indexOf(shortColor) != -1)
            {
                // We have the killer.
                if (!player.isDead())
                {
                    return player;
                }
                else
                {
                    return getPlayerUsingColor(player.getShortColor());
                }
            }
        }
        return null;
    }

    private LegionServerSide getStartingLegion(String markerId, MasterHex hex,
        Player player)
    {
        CreatureType[] startCre = TerrainRecruitLoader
            .getStartingCreatures(hex);
        LegionServerSide legion = new LegionServerSide(markerId, null, hex,
            hex, player, this, VariantSupport.getCurrentVariant()
                .getCreatureByName(Constants.titan), VariantSupport
                .getCurrentVariant().getCreatureByName(
                    getVariant().getPrimaryAcquirable()), startCre[2],
            startCre[2], startCre[0], startCre[0], startCre[1], startCre[1]);

        for (Creature critter : legion.getCreatures())
        {
            getCaretaker().takeOne(critter.getType());
        }
        return legion;
    }

    int mulligan()
    {
        if (getPhase() != Phase.MOVE)
        {
            return -1;
        }
        PlayerServerSide player = (PlayerServerSide)getActivePlayer();
        player.takeMulligan();
        server.allUpdatePlayerInfo();
        setupMove();
        player.rollMovement();
        server.kickPhase();
        return player.getMovementRoll();
    }

    boolean getOption(String optname)
    {
        return options.getOption(optname);
    }

    int getIntOption(String optname)
    {
        return options.getIntOption(optname);
    }

    // History wrappers.  Time to start obeying the Law of Demeter.
    void addCreatureEvent(AddCreatureEvent event)
    {
        lastRecruitTurnNumber = turnNumber;
        history.addCreatureEvent(event, turnNumber);
    }

    void removeCreatureEvent(Legion legion, CreatureType creature)
    {
        history.removeCreatureEvent(legion, creature, turnNumber);
    }

    void splitEvent(Legion parent, Legion child, List<CreatureType> splitoffs)
    {
        history.splitEvent(parent, child, splitoffs, turnNumber);
    }

    void mergeEvent(String splitoffId, String survivorId)
    {
        history.mergeEvent(splitoffId, survivorId, turnNumber);
    }

    void revealEvent(boolean allPlayers, List<Player> players,
        Legion legion, List<CreatureType> creatureNames)
    {
        history.revealEvent(allPlayers, players, legion, creatureNames,
            turnNumber);
    }

    void playerElimEvent(Player player, Player slayer)
    {
        history.playerElimEvent(player, slayer, turnNumber);
    }

    void movementRollEvent(Player player, int roll)
    {
        history.movementRollEvent(player, roll);
    }

    void legionMoveEvent(Legion legion, MasterHex newHex,
        EntrySide entrySide,
        boolean teleport, CreatureType lord)
    {
        history.legionMoveEvent(legion, newHex, entrySide, teleport, lord);
    }

    void legionUndoMoveEvent(Legion legion)
    {
        history.legionUndoMoveEvent(legion);
    }

    INotifyWebServer getNotifyWebServer()
    {
        return this.notifyWebServer;
    }

}
