package net.sf.colossus.server;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.client.IClient;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


public class ClientHandlerStub implements IClient
{
    private static final Logger LOGGER = Logger
        .getLogger(ClientHandlerStub.class.getName());

    protected static final String sep = Constants.protocolTermSeparator;

    protected Server server;

    protected static int counter = 0;

    protected boolean isGone = false;
    protected String isGoneReason = "";

    protected String playerName;
    protected String signonName;

    // sync-when-disconnected stuff
    protected int messageCounter = 0;
    protected boolean isCommitPoint = false;

    protected long pingRequestCounter = 1;

    protected final ArrayList<MessageForClient> redoQueue = new ArrayList<MessageForClient>(
        100);

    // for optimization, do not re-send if exactly identical to the one sent
    // last time.
    private String previousInfoStringsString = "";

    public ClientHandlerStub(Server server)
    {
        LOGGER.fine("ClientHandlerStub instantiated");
        this.server = server;

        String tempId = "<no name yet #" + (counter++) + ">";
        InstanceTracker.register(this, tempId);
    }

    protected boolean isStub()
    {
        return true;
    }

    protected boolean canHandlePingRequest()
    {
        return true;
    }

    public boolean canHandleAdvancedSync()
    {
        return true;
    }

    protected boolean supportsReconnect()
    {
        // dummy
        return true;
    }

    public void setIsGone(String reason)
    {
        LOGGER.info("Setting isGone to true in CH for '" + getClientName()
            + "' (reason: " + reason + ")");
        this.isGone = true;
        this.isGoneReason = reason;
    }

    protected void sendToClient(String message)
    {
        enqueueToRedoQueue(messageCounter, message);
    }

    /**
     * Selector reported that client became writable again (after a prior
     * write attempt had not written all bytes). Now start/try writing the
     * message(s) which are still in the queue.
     */
    protected void flushQueuedContent()
    {
        // not needed here
    }

    protected void enqueueToRedoQueue(int messageNr, String message)
    {
        redoQueue.add(new MessageForClient(messageNr, 0, message));
        messageCounter++;
    }

    private int alreadyHandled = 0;

    protected void commitPoint()
    {
        PrintWriter writer = server.getGame().getIscMessageFile();
        if (writer == null)
        {
            return;
        }
        MessageForClient mfc;

        Iterator<MessageForClient> it = redoQueue.listIterator(alreadyHandled);
        while (it.hasNext())
        {
            mfc = it.next();
            String msg = mfc.getMessage();
            writer.println(msg);
            alreadyHandled++;
        }
        writer.flush();
    }

    // ======================================================================
    // IClient methods to sent requests to client over socket.

    /**
     * Server side disposes a client (and informs it about it first)
     * To be used only for "disposeAllClients()", otherwise setIsGone
     * reason is misleading.
     */
    public void disposeClient()
    {
        // Don't do it again
        if (isGone)
        {
            return;
        }

        setIsGone("Server disposes client (all clients)");
        server.queueClientHandlerForChannelChanges(this);
    }

    public void tellEngagement(MasterHex hex, Legion attacker, Legion defender)
    {
        sendToClient(Constants.tellEngagement + sep + hex.getLabel() + sep
            + attacker.getMarkerId() + sep + defender.getMarkerId());
    }

    public void tellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        sendToClient(Constants.tellEngagementResults + sep
            + (winner != null ? winner.getMarkerId() : null) + sep + method
            + sep + points + sep + turns);
    }

    public void tellWhatsHappening(String message)
    {
        sendToClient(Constants.tellWhatsHappening + sep + message);
    }

    public void tellMovementRoll(int roll)
    {
        sendToClient(Constants.tellMovementRoll + sep + roll);
    }

    public void syncOption(String optname, String value)
    {
        sendToClient(Constants.syncOption + sep + optname + sep + value);
    }

    public void updatePlayerInfo(List<String> infoStrings)
    {
        String infoStringsString = Glob.glob(infoStrings);
        if (previousInfoStringsString.equals(infoStringsString))
        {
            LOGGER.finest("Skipping the re-send of identical player infos.");
        }
        else
        {
            sendToClient(Constants.updatePlayerInfo + sep + infoStringsString);
            previousInfoStringsString = infoStringsString;
        }
    }

    /**
     * A new way to pass changed player info to clients.
     * Shortened info (e.g. color, dead state, ...) not sent every time;
     * Includes a reason why sent (mostly used internally on server side
     * for debugging/development purposes),
     * and a flag whether this info should be redundant; idea behind it:
     * on the long run, clients should be able to update game/player info
     * "autonomously" (same logic implemented on client as on server)
     * instead of server doing it and synching to all clients.
     * So this redundant is meant to be used as safety net to detect
     * where that new approach might miss something.
     */
    public void updateOnePlayersInfo(boolean redundant, String reason,
        String ShouldBeSeveralSeparateVariablesHerePerhaps)
    {
        // TODO: this is not implemented yet at all...
    }

    public void setColor(PlayerColor color)
    {
        sendToClient(Constants.setColor + sep + color.getName());
    }

    public void updateCreatureCount(CreatureType type, int count, int deadCount)
    {
        sendToClient(Constants.updateCreatureCount + sep + type.getName()
            + sep + count + sep + deadCount);
    }

    public void removeLegion(Legion legion)
    {
        sendToClient(Constants.removeLegion + sep + legion.getMarkerId());
    }

    public void setLegionStatus(Legion legion, boolean moved,
        boolean teleported, EntrySide entrySide, CreatureType lastRecruit)
    {
        sendToClient(Constants.setLegionStatus + sep + legion.getMarkerId()
            + sep + moved + sep + teleported + sep + entrySide.ordinal() + sep
            + lastRecruit);
    }

    public void addCreature(Legion legion, CreatureType creature, String reason)
    {
        sendToClient(Constants.addCreature + sep + legion.getMarkerId() + sep
            + creature + sep + reason);
    }

    public void removeCreature(Legion legion, CreatureType creature,
        String reason)
    {
        sendToClient(Constants.removeCreature + sep + legion + sep + creature
            + sep + reason);
    }

    public void revealCreatures(Legion legion,
        final List<CreatureType> creatures, String reason)
    {
        sendToClient(Constants.revealCreatures + sep + legion.getMarkerId()
            + sep + Glob.glob(creatures) + sep + reason);
    }

    /** print the 'revealEngagagedCreature'-message,
     *   args: markerId, isAttacker, list of creature names
     * @param markerId legion marker name that is currently in battle
     * @param creatures List of creatures in this legion
     * @param isAttacker true for attacker, false for defender
     * @param reason why this was revealed
     * @author Towi, copied from revealCreatures
     */
    public void revealEngagedCreatures(final Legion legion,
        final List<CreatureType> creatures, final boolean isAttacker,
        String reason)
    {
        sendToClient(Constants.revealEngagedCreatures + sep
            + legion.getMarkerId() + sep + isAttacker + sep
            + Glob.glob(creatures) + sep + reason);
    }

    public void removeDeadBattleChits()
    {
        sendToClient(Constants.removeDeadBattleChits);
    }

    public void placeNewChit(String imageName, boolean inverted, int tag,
        BattleHex hex)
    {
        sendToClient(Constants.placeNewChit + sep + imageName + sep + inverted
            + sep + tag + sep + hex.getLabel());
    }

    public void tellReplay(boolean val, int maxTurn)
    {
        sendToClient(Constants.replayOngoing + sep + val + sep + maxTurn);
    }

    public void tellRedo(boolean val)
    {
        sendToClient(Constants.redoOngoing + sep + val);
    }

    public void initBoard()
    {
        sendToClient(Constants.initBoard);
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
        sendToClient(Constants.setPlayerName + sep + playerName);
    }

    public String getSignonName()
    {
        return this.signonName;
    }

    // silently choose whatever useful, mostly for logging
    public String getClientName()
    {
        return playerName != null ? playerName
            : (signonName != null ? signonName : "ClientNameNotSet");
    }

    public String getPlayerName()
    {
        if (this.playerName == null)
        {
            LOGGER.warning("CH.playerName still null, returning signOnName '"
                + signonName + "'");
            Thread.dumpStack();
            return this.signonName;
        }
        return this.playerName;
    }

    public void createSummonAngel(Legion legion)
    {
        sendToClient(Constants.createSummonAngel + sep + legion.getMarkerId());
    }

    public void askAcquireAngel(Legion legion, List<CreatureType> recruits)
    {
        sendToClient(Constants.askAcquireAngel + sep + legion.getMarkerId()
            + sep + Glob.glob(recruits));
    }

    public void askChooseStrikePenalty(List<String> choices)
    {
        sendToClient(Constants.askChooseStrikePenalty + sep
            + Glob.glob(choices));
    }

    public void tellGameOver(String message, boolean disposeFollows)
    {
        sendToClient(Constants.tellGameOver + sep + message + sep
            + disposeFollows);
    }

    public void tellPlayerElim(Player player, Player slayer)
    {
        // slayer can be null
        sendToClient(Constants.tellPlayerElim + sep + player.getName() + sep
            + (slayer != null ? slayer.getName() : null));
    }

    public void askConcede(Legion ally, Legion enemy)
    {
        sendToClient(Constants.askConcede + sep + ally.getMarkerId() + sep
            + enemy.getMarkerId());
    }

    public void askFlee(Legion ally, Legion enemy)
    {
        sendToClient(Constants.askFlee + sep + ally.getMarkerId() + sep
            + enemy.getMarkerId());
    }

    public void askNegotiate(Legion attacker, Legion defender)
    {
        sendToClient(Constants.askNegotiate + sep + attacker.getMarkerId()
            + sep + defender.getMarkerId());
    }

    public void tellProposal(String proposalString)
    {
        sendToClient(Constants.tellProposal + sep + proposalString);
    }

    public void tellSlowResults(int targetTag, int slowValue)
    {
        sendToClient(Constants.tellSlowResults + sep + targetTag + sep
            + slowValue);
    }

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions)
    {
        sendToClient(Constants.tellStrikeResults + sep + strikerTag + sep
            + targetTag + sep + strikeNumber + sep + Glob.glob(rolls) + sep
            + damage + sep + killed + sep + wasCarry + sep + carryDamageLeft
            + sep + Glob.glob(carryTargetDescriptions));
    }

    public void initBattle(MasterHex hex, int battleTurnNumber,
        Player battleActivePlayer, BattlePhase battlePhase, Legion attacker,
        Legion defender)
    {
        sendToClient(Constants.initBattle + sep + hex.getLabel() + sep
            + battleTurnNumber + sep + battleActivePlayer.getName() + sep
            + battlePhase.ordinal() + sep + attacker.getMarkerId() + sep
            + defender.getMarkerId());
    }

    public void cleanupBattle()
    {
        sendToClient(Constants.cleanupBattle);
    }

    public void nextEngagement()
    {
        sendToClient(Constants.nextEngagement);
    }

    public void doReinforce(Legion legion)
    {
        sendToClient(Constants.doReinforce + sep + legion.getMarkerId());
    }

    public void didRecruit(Legion legion, CreatureType recruit,
        CreatureType recruiter, int numRecruiters)
    {
        sendToClient(Constants.didRecruit + sep + legion.getMarkerId() + sep
            + recruit + sep + recruiter + sep + numRecruiters);
    }

    public void undidRecruit(Legion legion, CreatureType recruit)
    {
        sendToClient(Constants.undidRecruit + sep + legion + sep + recruit);
    }

    public void setupTurnState(Player activePlayer, int turnNumber)
    {
        commitPoint();
        sendToClient(Constants.setupTurnState + sep + activePlayer.getName()
            + sep + turnNumber);
    }

    public void setupSplit(Player activePlayer, int turnNumber)
    {
        commitPoint();
        sendToClient(Constants.setupSplit + sep + activePlayer.getName() + sep
            + turnNumber);
    }

    public void setupMove()
    {
        commitPoint();
        sendToClient(Constants.setupMove);
    }

    public void setupFight()
    {
        commitPoint();
        sendToClient(Constants.setupFight);
    }

    public void setupMuster()
    {
        commitPoint();
        sendToClient(Constants.setupMuster);
    }

    public void kickPhase()
    {
        sendToClient(Constants.kickPhase);
    }

    public void setupBattleSummon(Player battleActivePlayer,
        int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleSummon + sep
            + battleActivePlayer.getName() + sep + battleTurnNumber);
    }

    public void setupBattleRecruit(Player battleActivePlayer,
        int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleRecruit + sep
            + battleActivePlayer.getName() + sep + battleTurnNumber);
    }

    public void setupBattleMove(Player battleActivePlayer, int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleMove + sep
            + battleActivePlayer.getName() + sep + battleTurnNumber);
    }

    public void setupBattleFight(BattlePhase battlePhase,
        Player battleActivePlayer)
    {
        sendToClient(Constants.setupBattleFight + sep + battlePhase.ordinal()
            + sep + battleActivePlayer.getName());
    }

    public void tellLegionLocation(Legion legion, MasterHex hex)
    {
        sendToClient(Constants.tellLegionLocation + sep + legion.getMarkerId()
            + sep + hex.getLabel());
    }

    public void tellBattleMove(int tag, BattleHex startingHex,
        BattleHex endingHex, boolean undo)
    {
        sendToClient(Constants.tellBattleMove + sep + tag + sep
            + startingHex.getLabel() + sep + endingHex.getLabel() + sep + undo);
    }

    public void didMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, EntrySide entrySide, boolean teleport,
        CreatureType teleportingLord, boolean splitLegionHasForcedMove)
    {
        sendToClient(Constants.didMove + sep + legion.getMarkerId() + sep
            + startingHex.getLabel() + sep + currentHex.getLabel() + sep
            + entrySide.getLabel() + sep + teleport + sep
            + (teleportingLord == null ? "null" : teleportingLord) + sep
            + splitLegionHasForcedMove);
    }

    public void undidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove)
    {
        sendToClient(Constants.undidMove + sep + legion.getMarkerId() + sep
            + formerHex.getLabel() + sep + currentHex.getLabel() + sep
            + splitLegionHasForcedMove);
    }

    public void didSummon(Legion summoner, Legion donor, CreatureType summon)
    {
        sendToClient(Constants.didSummon + sep + summoner + sep + donor + sep
            + summon);
    }

    public void undidSplit(Legion splitoff, Legion survivor, int turn)
    {
        sendToClient(Constants.undidSplit + sep + splitoff.getMarkerId() + sep
            + survivor.getMarkerId() + sep + turn);
    }

    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<CreatureType> splitoffs, int turn)
    {
        // hex can be null when loading a game
        // TODO make sure we always have a hex
        assert parent != null : " Split needs parent";
        assert child != null : " Split needs child";
        assert hex != null : "Split needs location";
        sendToClient(Constants.didSplit + sep + hex.getLabel() + sep
            + parent.getMarkerId() + sep + child.getMarkerId() + sep
            + childHeight + sep + Glob.glob(splitoffs) + sep + turn);
    }

    public void askPickColor(List<PlayerColor> colorsLeft)
    {
        sendToClient(Constants.askPickColor + sep + Glob.glob(colorsLeft));
    }

    public void askPickFirstMarker()
    {
        sendToClient(Constants.askPickFirstMarker);
    }

    public void log(String message)
    {
        sendToClient(Constants.log + sep + message);
    }

    public void nak(String reason, String errmsg)
    {
        sendToClient(Constants.nak + sep + reason + sep + errmsg);
    }

    public void setBoardActive(boolean val)
    {
        sendToClient(Constants.boardActive + sep + val);
    }

    public void tellInitialGameInfo(String variantName,
        Collection<String> playerNames)
    {
        String allPlayerNames = Glob.glob(playerNames);
        sendToClient(Constants.gameInitInfo + sep + variantName + sep
            + allPlayerNames);
    }

    public void confirmWhenCaughtUp()
    {
        LOGGER.info("Sending request to confirm catchup to client "
            + playerName);
        sendToClient(Constants.askConfirmCatchUp);
    }

    public void serverConfirmsConnection()
    {
        LOGGER.info("Sending server connection confirmation to client "
            + playerName);
        sendToClient(Constants.serverConnectionOK);
    }

    /* One client has asked for connnection confirmation; here server
     * relays this to each client.
     */
    public void relayedPeerRequest(String requestingClientName)
    {
        LOGGER.info("Relaying peerRequest from client " + requestingClientName
            + " to client " + getClientName());
        sendToClient(Constants.relayedPeerRequest + sep + requestingClientName);
    }

    /* Relay the "received" response back to requester */
    public void peerRequestReceivedBy(String respondingClientName, int queueLen)
    {
        LOGGER.info("Relaying back the received message of client "
            + getClientName() + " to " + respondingClientName);
        sendToClient(Constants.relayBackReceivedMsg + sep
            + respondingClientName + sep + queueLen);
    }

    /* Relay the "processed" response back to requester */
    public void peerRequestProcessedBy(String respondingClientName)
    {
        LOGGER.info("Relaying back the processed message of client "
            + getClientName() + " to " + respondingClientName);
        sendToClient(Constants.relayBackProcessedMsg + sep
            + respondingClientName);
    }

    public void pingRequest(long requestSent)
    {
        if (canHandlePingRequest())
        {
            sendToClient(Constants.pingRequest + sep + pingRequestCounter
                + sep + requestSent);
            pingRequestCounter++;
        }
    }

    public void messageFromServer(String message)
    {
        // appears as pop-up window with "OK" button
        sendToClient(Constants.messageFromServer + sep + message);
    }

    public void appendToConnectionLog(String message)
    {
        if (canHandleAdvancedSync())
        {
            sendToClient(Constants.appendToConnectionLog + sep + message);
        }
    }

    public void tellSyncCompleted(int syncRequestNumber)
    {
        sendToClient(Constants.syncCompleted + sep + syncRequestNumber);
    }

}
