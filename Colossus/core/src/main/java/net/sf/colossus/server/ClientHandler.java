package net.sf.colossus.server;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.IClient;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.actions.Recruitment;
import net.sf.colossus.game.actions.Summoning;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 *  Holds all data specific to one client connection.
 *  (Earlier this was the class ServerSocketThread, but since changing
 *   to NIO it's not an own thread any more.)
 *
 *  The code in here is (should be) executed exclusively by the server
 *  thread as reaction to something happening on the selector
 *  - first the client connection being accepted, and then later always
 *  when data from client was received (usually from THIS client, but
 *  there might be other cases).
 *
 *  @author David Ripton
 */
final class ClientHandler extends ClientHandlerStub implements IClient
{
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class
        .getName());

    // server is stored in ClientHandlerStub
    private final SocketChannel socketChannel;
    private final SelectionKey selectorKey;
    private int clientVersion = 0;
    private boolean spectator;

    private boolean didExplicitDisconnect = false;
    private boolean withdrawnAlready = false;
    private int cantSendMessageRepeated = 0;
    private boolean temporarilyDisconnected = false;
    private boolean obsolete = false;

    private String incompleteInput = "";
    private String incompleteText = "";

    // Charset and encoder: by default according to the property,
    // fallback US-ASCII
    private static final String DEFAULT_CHAR_SET = System
        .getProperty("file.encoding");
    private final String CHARSET_NAME = DEFAULT_CHAR_SET != null ? DEFAULT_CHAR_SET
        : "US-ASCII";
    private final Charset charset = Charset.forName(CHARSET_NAME);
    private final CharsetEncoder encoder = charset.newEncoder();
    private final CharsetDecoder decoder = charset.newDecoder();

    // sync-when-disconnected stuff
    private int commitPointCounter = 0;

    private final static int MAX_KEEP_LINES = 5;
    private final ArrayList<String> recentlyProcessedLines = new ArrayList<String>(
        MAX_KEEP_LINES);

    private long lastPingReplyReceived = -1;

    // Note that the client (SocketClientThread) sends ack every
    // CLIENT_CTR_ACK_EVERY messages (currently 20)
    // The two values above and the client value must fit together
    // that it does not cause a deadlock.

    ClientHandler(Server server, SocketChannel channel, SelectionKey selKey)
    {
        super(server);

        this.socketChannel = channel;
        this.selectorKey = selKey;

        String tempId = "<no name yet #" + (counter++) + ">";
        InstanceTracker.register(this, tempId);
    }

    public SelectionKey getSelectorKey()
    {
        return selectorKey;
    }

    public SocketChannel getSocketChannel()
    {
        return socketChannel;
    }

    // if "isGone" is true, connection to this client is gone
    // Server uses this to decide whether any nonAI player is
    // (even if perhaps dead) still connected (= watching).
    public boolean isGone()
    {
        return this.isGone;
    }

    @Override
    protected boolean isStub()
    {
        return false;
    }

    public boolean isSpectator()
    {
        return spectator;
    }

    public boolean didExplicitDisconnect()
    {
        return didExplicitDisconnect;
    }

    public void setTemporarilyDisconnected()
    {
        temporarilyDisconnected = true;
    }

    /* Not really needed at the moment; every reconnect creates a new ClientHandler
    private void clearTemporarilyDisconnected()
    {
        temporarilyDisconnected = false;
    }
    */

    public boolean isTemporarilyDisconnected()
    {
        return temporarilyDisconnected;
    }

    @Override
    protected boolean canHandlePingRequest()
    {
        return clientVersion >= IServer.CLIENT_VERSION_UNDERSTANDS_PING;
    }

    public long getMillisSincePingReply()
    {
        return lastPingReplyReceived - new Date().getTime();
    }

    protected boolean canHandleNewVariantXML()
    {
        return clientVersion >= IServer.CLIENT_VERSION_VARIANT_XML_OK;
    }

    /**
     * Server side disposes a client (and informs it about it first)
     * To be used only for "disposeAllClients()", otherwise setIsGone
     * reason is misleading.
     */
    @Override
    public void disposeClient()
    {
        // Don't do it again
        if (isGone)
        {
            return;
        }

        sendViaChannel(Constants.dispose);
        setIsGone("Server disposes client (all clients)");
        server.queueClientHandlerForChannelChanges(this);
        server.clientWontConfirmCatchup(this,
            "Client disposed from server side.");
    }

    // Called by Server's select reader
    public void processInput(ByteBuffer byteBuffer)
    {
        try
        {
            CharBuffer charBuff = decoder.decode(byteBuffer);

            String msg = incompleteInput + charBuff.toString();
            incompleteInput = "";
            incompleteText = "";

            LOGGER.finest("Decoded string is >>>>>" + msg + "<<<<<");

            int processed = 0;

            String lines[] = msg.split("\r\n|\n|\r", -1);
            int len = lines.length;
            for (int i = 0; i < len; i++)
            {
                String line = lines[i];
                if (i < len - 1)
                {
                    LOGGER.finest("before processing cmd '" + line + "'");
                    List<String> li = Split.split(sep, line);
                    String method = li.remove(0);
                    if (signonName == null && !method.equals(Constants.signOn))
                    {
                        LOGGER
                            .log(Level.SEVERE,
                                "First packet must be signOn, but it is "
                                    + method);
                    }
                    else
                    {
                        doCallMethodInTryBlock(line, method, li);
                    }
                    LOGGER.finest("after  processing line '" + line + "'");
                    processed++;
                }
                else if (i == len - 1 && line.equals(""))
                {
                    // Received characters ended with newline, producing one
                    // empty string at the end. Perfect.
                }
                else
                {
                    LOGGER.log(Level.FINEST,
                        "last item incomplete, storing it: '" + line + "'");
                    incompleteInput = line;
                    incompleteText = " (not handled: incomplete input '"
                        + incompleteInput + "')";
                }
            }

            LOGGER.log(Level.FINEST, "Processed " + processed + " commands"
                + incompleteText + ".");
        }
        catch (CharacterCodingException cce)
        {
            LOGGER.log(Level.SEVERE,
                "CharacterCodingException while reading from channel"
                    + socketChannel, cce);
        }
    }

    private void sendViaChannel(String msg)
    {
        sendViaChannelRaw(msg);
    }

    @Override
    protected void flushQueuedContent()
    {
        sendViaChannelRaw(null);
    }

    @Override
    protected void enqueueToRedoQueue(int messageNr, String message)
    {
        if (supportsReconnect())
        {
            redoQueue.add(new MessageForClient(messageNr,
                (isCommitPoint ? commitPointCounter : 0), message));
            messageCounter++;
        }
    }

    @Override
    protected void commitPoint()
    {
        if (supportsReconnect())
        {
            commitPointCounter++;
            isCommitPoint = true;
            sendToClient(Constants.commitPoint + sep + commitPointCounter
                + sep + messageCounter);
            isCommitPoint = false;
        }
    }

    /**
     * Remove the messages in redoQueue prior to given commit point
     * @param confirmedNr Commit point from which we now know that client has
     * successfully received it
     */
    private void confirmCommitPoint(int confirmedNr)
    {
        int found = -1;
        int size = redoQueue.size();
        for (int i = 0; i < size && found == -1; i++)
        {
            MessageForClient mfc = redoQueue.get(i);
            if (mfc.getCommitNumber() == confirmedNr)
            {
                found = i;
            }
        }

        if (found != -1)
        {
            /*
             * TODO: If we would subclass ArrayList, could use the more
             * efficient method removeRange (it is "protected")
             */
            // int deleted = 0;
            for (int i = 0; i <= found; i++)
            {
                // deleted++;
                redoQueue.get(0);
                redoQueue.remove(0);
            }
        }
    }

    @Override
    public boolean supportsReconnect()
    {
        return clientVersion >= IServer.CLIENT_VERSION_CAN_RECONNECT;
    }

    public boolean canHandleBattleMoveNak()
    {
        return clientVersion >= IServer.CLIENT_VERSION_CAN_HANDLE_NAK;
    }

    @Override
    public boolean canHandleAdvancedSync()
    {
        return clientVersion >= IServer.CLIENT_VERSION_CAN_HANDLE_NAK;
    }

    public void cloneRedoQueue(ClientHandler oldCH)
    {
        // Remove the reconnect-related messages
        redoQueue.clear();
        redoQueue.addAll(oldCH.redoQueue);
        commitPointCounter = oldCH.commitPointCounter;
    }

    public void initRedoQueueFromStub(ClientHandlerStub stub)
    {
        boolean kickPhaseSeen = false;

        redoQueue.clear();

        for (MessageForClient mfc : stub.redoQueue)
        {
            String method = mfc.getMethod();
            if (method.equals(Constants.kickPhase))
            {
                kickPhaseSeen = true;
            }

            if (method.equals(Constants.initBoard))
            {
                int maxTurn = server.getGame().getTurnNumber();
                String replayOn = Constants.replayOngoing + sep + true + sep
                    + maxTurn;
                reEnqueue(new MessageForClient(0, 0, replayOn));
                reEnqueue(mfc);
            }

            // skip all the later ones; the initial ones are needed.
            else if (kickPhaseSeen
                && method.equals(Constants.updatePlayerInfo))
            {
                // skip
            }

            else if (method.equals(Constants.updateCreatureCount)
                || method.equals(Constants.tellWhatsHappening))
            {
                // skip
            }
            else
            {
                reEnqueue(mfc);
            }
        }

        // Set Replay OFF
        String replayOn = Constants.replayOngoing + sep + false + sep + 0;
        reEnqueue(new MessageForClient(0, 0, replayOn));

        LOGGER.fine("Initialized redoQueue from stub, contains now "
            + redoQueue.size() + " items!");
    }

    private int newCounter = 0;

    private void reEnqueue(MessageForClient mfc)
    {
        MessageForClient newOne = new MessageForClient(mfc, newCounter);
        newCounter++;
        redoQueue.add(newOne);
    }

    /**
     * Re-send all data after the message from which we know client got it
     *
     * @param lastReceivedMessageNr Last message which client did still receive
     * @param syncRequestNumber Every request has own unique id, so we don't mix them
     */
    public void syncAfterReconnect(int lastReceivedMessageNr,
        int syncRequestNumber)
    {
        // to get client out of initial read-lines loop, to get it into
        // normal "read from socket and parse line" loop
        setPlayerName(signonName);

        int size = redoQueue.size();
        for (int i = 0; i < size; i++)
        {
            MessageForClient mfc = redoQueue.get(i);
            int queueMsgNr = mfc.getMessageNr();
            if (queueMsgNr > lastReceivedMessageNr)
            {
                String message = mfc.getMessage();
                sendViaChannelRaw(message);
                messageCounter = queueMsgNr;
            }
        }
        commitPoint();
        if (canHandleAdvancedSync())
        {
            tellSyncCompleted(syncRequestNumber);
        }
        if (!isSpectator())
        {
            server.othersTellReconnectCompleted(this);
        }
    }

    ByteBuffer bb;
    String encodedMsg; // used only for logging
    int should;
    int writtenTotal;

    int previousRetries = 0;

    private long temporarilyInTrouble = -1;

    public boolean isTemporarilyInTrouble()
    {
        return (temporarilyInTrouble != -1);
    }

    public long howLongAlreadyInTrouble()
    {
        if (temporarilyInTrouble == -1)
        {
            return 0;
        }
        long now = new Date().getTime();
        return now - temporarilyInTrouble;
    }

    private void setTemporarilyInTrouble()
    {
        long now = new Date().getTime();
        temporarilyInTrouble = now;
        if (!isSpectator())
        {
            server.othersTellOneHasNetworkTrouble(this);
        }
        selectorKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void clearTemporarilyInTrouble()
    {
        temporarilyInTrouble = -1;
        if (!isSpectator())
        {
            server.othersTellOnesTroubleIsOver(this);
        }
    }

    String lastEncodedMsg = "";

    private void handleEncoding(String msg)
    {
        try
        {
            lastEncodedMsg = msg;
            encodedMsg = msg;
            String dataToSend = msg + "\n";
            CharBuffer cb = CharBuffer.allocate(dataToSend.length());
            cb.put(dataToSend);
            cb.flip();

            bb = encoder.encode(cb);
            should = bb.limit();
            writtenTotal = 0;
        }
        catch (CharacterCodingException e)
        {
            LOGGER.log(Level.WARNING, "EncondingException '" + e.getMessage()
                + "'" + " was thrown while encoding String '" + msg + "'"
                + " for writing it to" + " channel for player " + playerName
                + "; details follow", e);
        }

    }

    /** The queue in which messages are stored, until they were really written.
     *  Usually empty; stuff piles up only when writing to socket fails,
     *  e.g. network or client too slow.
     */
    LinkedList<String> queue = new LinkedList<String>();

    private void sendViaChannelRaw(String msg)
    {
        // Something left undone last time. Postpone this here for a moment.
        // Except if it is null, then caller called us dedicatedly to give
        // us a chance to finish earlier stuff.
        if (msg != null)
        {
            queue.add(msg);
        }

        if (isTemporarilyInTrouble())
        {
            return;
        }

        if (previousRetries > 0)
        {
            // Try writing old stuff away.
            attemptWritingToChannel();
        }

        // If there was no problem and/or now it went well, proceed with new
        // stuff... if there was.
        while (previousRetries == 0 && queue.size() > 0)
        {
            String queueMsg = queue.poll();
            handleEncoding(queueMsg);
            attemptWritingToChannel();
        }

        if (previousRetries > 0)
        {
            setTemporarilyInTrouble();
        }
        else
        {
            if (isTemporarilyInTrouble())
            {
                LOGGER.warning("temporaryInTrouble still true for player "
                    + getPlayerName() + "? This should never happen.");
            }
            temporarilyInTrouble = -1;
        }
    }

    private void attemptWritingToChannel()
    {
        if (isGone())
        {
            LOGGER.warning("isGone already true when attempting "
                + "to do WriteToChannel " + lastEncodedMsg + " for player"
                + getPlayerName());
            LOGGER.warning("Reason: " + this.isGoneReason);
            Thread.dumpStack();
            return;
        }
        // Attempt to write away what is in buffer
        try
        {
            int written = socketChannel.write(bb);
            if (written > 0)
            {
                writtenTotal += written;
            }

            if (writtenTotal < should)
            {
                // Not all written
                previousRetries += 1;
                if (spectator)
                {
                    LOGGER.info("trouble writing, temporarily giving up "
                        + "writing to client " + getPlayerName());
                }
                else
                {
                    LOGGER.warning("trouble writing, temporarily giving up "
                        + "writing to client " + getPlayerName());
                }
            }
            else
            {
                // OK, now all was written
                // TODO nowadays we do only one try, can this be a boolean instead?
                if (previousRetries > 0)
                {
                    LOGGER.info("Now succeeded, attempt = " + previousRetries);
                }
                previousRetries = 0;
            }
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.WARNING,
                "IOException '" + ioe.getMessage() + "'"
                    + " was thrown while writing String '" + encodedMsg + "'"
                    + " to channel for player " + playerName
                    + "; details follow:", ioe);

            if (this.supportsReconnect())
            {
                setTemporarilyDisconnected();
            }
            else
            {
                setIsGone("IOException and reconnect not supported");
                withdrawnAlready = true;
                server.withdrawFromGame(spectator ? null : playerName);
                server.queueClientHandlerForChannelChanges(this);
                server.clientWontConfirmCatchup(this,
                    "IO Exception while writing to client " + playerName);
            }
        }
        Thread.yield();
    }

    /**
     * Make sure player is withdrawn from game.
     * Explicit if via Withdraw message from client, implicit because
     * of disconnect message or connection problems.
     * This is just a wrapper for the both situations where for
     * !withdrawnAlready should be checked
     *
     * @param explicit Whether client has requested withdraw explicitly
     */
    private void withdrawIfNeeded(boolean explicit)
    {
        if (!withdrawnAlready)
        {
            if (!explicit)
            {
                LOGGER.log(Level.FINE,
                    "Client disconnected without explicit withdraw - "
                        + "doing automatic withdraw (if needed) for player "
                        + playerName);
            }
            withdrawnAlready = true;
            server.withdrawFromGame();
        }
    }

    // same player re-connected with new ClientHandler
    // => this one here should "be gone" / inactive
    public void declareObsolete()
    {
        obsolete = true;
        setIsGone("Declared obsolete because new client "
            + "with same name connected");
    }

    public String dumpLastProcessedLines()
    {
        StringBuffer sb = new StringBuffer("## Last " + MAX_KEEP_LINES
            + " processed lines were:");
        int i = 0;
        for (String rLine : recentlyProcessedLines)
        {
            i++;
            sb.append("\n      #" + i + ": " + rLine);
        }
        return sb.toString();
    }

    private void doCallMethodInTryBlock(String line, String method,
        List<String> li)
    {
        try
        {
            while (recentlyProcessedLines.size() >= MAX_KEEP_LINES)
            {
                recentlyProcessedLines.remove(0);
            }
            recentlyProcessedLines.add(line);
            callMethod(method, li);
        }
        catch (Exception e)
        {
            String message = "Woooah! An exception was caught while "
                + "processing from client " + getPlayerName()
                + " the input line:\n    === " + line + " ===\n"
                + "\nStack trace:\n" + ErrorUtils.makeStackTraceString(e)
                + "\n\nGame might be unstable or hang from now on...";
            LOGGER.severe(message);
            LOGGER.info(dumpLastProcessedLines());
            ErrorUtils.showExceptionDialog(null, message, "Exception caught!",
                true);
        }
    }

    /**
     * This is the longish if-elseif-else block which deserialized all
     * client-to-server calls back from String to actual methodCalls.
     * @param method The method to execute
     * @param args   A list of argument Strings
     */
    private void callMethod(String method, List<String> args)
    {
        if (method.equals(Constants.signOn))
        {
            String signonTryName = args.remove(0);
            boolean remote = Boolean.valueOf(args.remove(0)).booleanValue();
            this.spectator = false;
            String buildInfo;
            if (args.size() < 2)
            {
                LOGGER.info("Connecting client with signonName "
                    + signonTryName + " did not send version/build info - "
                    + "treating that as version -1, build info NONE.");
                clientVersion = -1;
                buildInfo = "NONE";
            }
            else
            {
                clientVersion = Integer.parseInt(args.remove(0));
                buildInfo = args.remove(0);
                if (args.size() > 0)
                {
                    this.spectator = Boolean.valueOf(args.remove(0))
                        .booleanValue();
                }
            }
            String reasonFail = server.addClient(this, signonTryName, remote,
                clientVersion, buildInfo, spectator);

            if (reasonFail == null)
            {
                sendToClient("Ack: signOn");
                this.signonName = signonTryName;
            }
            else
            {
                LOGGER.info("Rejecting client " + signonTryName);
                nak("SignOn", reasonFail);
            }
            InstanceTracker.setId(this, signonTryName);
        }
        else if (method.equals(Constants.joinGame))
        {
            String playerName = args.remove(0);
            if (!playerName.equals(signonName))
            {
                LOGGER.severe("Joining game with different name '"
                    + playerName + "' than signonName + '" + signonName
                    + "' is currently not supported!");
                return;
            }
            LOGGER.info("Received joinGame from client " + signonName);
            setPlayerName(signonName);
            server.joinGame(signonName);
        }

        else if (method.equals(Constants.watchGame))
        {
            LOGGER.info("Got watchGame request from client " + signonName);
            server.watchGame();
        }

        else if (method.equals(Constants.requestGameInfo))
        {
            server.replyToRequestGameInfo();
        }

        else if (method.equals(Constants.fixName))
        {
            String newName = args.remove(0);
            // Prevent an infinite loop oscillating between two names.
            // @TODO: is this still needed?
            if (!newName.equals(playerName)
                && !newName.startsWith(Constants.byColor))
            {
                setPlayerName(newName);
            }
        }
        else if (method.equals(Constants.leaveCarryMode))
        {
            server.leaveCarryMode();
        }
        else if (method.equals(Constants.doneWithBattleMoves))
        {
            server.doneWithBattleMoves();
        }
        else if (method.equals(Constants.doneWithStrikes))
        {
            server.doneWithStrikes();
        }
        else if (method.equals(Constants.acquireAngel))
        {
            String markerId = args.remove(0);
            String angelType = args.remove(0);
            server.acquireAngel(resolveLegion(markerId),
                resolveCreatureTypeNullOk(angelType));
        }
        else if (method.equals(Constants.doSummon))
        {
            Legion legion = resolveLegion(args.remove(0));
            if (legion == null)
            {
                server.doSummon(null);
                return;
            }
            Legion donor = resolveLegion(args.remove(0));
            CreatureType creatureType = resolveCreatureType(args.remove(0));
            server.doSummon(new Summoning(legion, donor, creatureType));
        }
        else if (method.equals(Constants.doRecruit))
        {
            Legion legion = resolveLegion(args.remove(0));
            // Refusing a reinforcement sends as "recruited" null.
            CreatureType recruited = resolveCreatureTypeNullOk(args.remove(0));
            CreatureType recruiter = resolveCreatureTypeNullOk(args.remove(0));
            server.doRecruit(new Recruitment(legion, recruited, recruiter));
        }
        else if (method.equals(Constants.engage))
        {
            String hexLabel = args.remove(0);
            server.engage(resolveMasterHex(hexLabel));
        }
        else if (method.equals(Constants.concede))
        {
            String markerId = args.remove(0);
            server.concede(resolveLegion(markerId));
        }
        else if (method.equals(Constants.doNotConcede))
        {
            String markerId = args.remove(0);
            server.doNotConcede(resolveLegion(markerId));
        }
        else if (method.equals(Constants.flee))
        {
            String markerId = args.remove(0);
            server.flee(resolveLegion(markerId));
        }
        else if (method.equals(Constants.doNotFlee))
        {
            String markerId = args.remove(0);
            server.doNotFlee(resolveLegion(markerId));
        }
        else if (method.equals(Constants.makeProposal))
        {
            String proposalString = args.remove(0);
            server.makeProposal(proposalString);
        }
        else if (method.equals(Constants.fight))
        {
            String hexLabel = args.remove(0);
            server.fight(resolveMasterHex(hexLabel));
        }
        else if (method.equals(Constants.doBattleMove))
        {
            int tag = Integer.parseInt(args.remove(0));
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            server.doBattleMove(tag, hex);
        }
        else if (method.equals(Constants.strike))
        {
            int tag = Integer.parseInt(args.remove(0));
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            server.strike(tag, hex);
        }
        else if (method.equals(Constants.applyCarries))
        {
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            server.applyCarries(hex);
        }
        else if (method.equals(Constants.undoBattleMove))
        {
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            server.undoBattleMove(hex);
        }
        else if (method.equals(Constants.assignStrikePenalty))
        {
            String prompt = args.remove(0);
            server.assignStrikePenalty(prompt);
        }
        else if (method.equals(Constants.mulligan))
        {
            server.mulligan();
        }
        else if (method.equals(Constants.undoSplit))
        {
            String splitoffId = args.remove(0);
            server.undoSplit(resolveLegion(splitoffId));
        }
        else if (method.equals(Constants.undoMove))
        {
            String markerId = args.remove(0);
            server.undoMove(resolveLegion(markerId));
        }
        else if (method.equals(Constants.undoRecruit))
        {
            String markerId = args.remove(0);
            server.undoRecruit(resolveLegion(markerId));
        }
        else if (method.equals(Constants.doneWithSplits))
        {
            server.doneWithSplits();
        }
        else if (method.equals(Constants.doneWithMoves))
        {
            server.doneWithMoves();
        }
        else if (method.equals(Constants.doneWithEngagements))
        {
            server.doneWithEngagements();
        }
        else if (method.equals(Constants.doneWithRecruits))
        {
            server.doneWithRecruits();
        }
        else if (method.equals(Constants.withdrawFromGame))
        {
            LOGGER.info("Received explicit 'withdrawFromGame' request from "
                + "Client " + getClientName()
                + " - calling 'withdrawIfNeeded'.");
            withdrawIfNeeded(true);
        }
        else if (method.equals(Constants.disconnect))
        {
            didExplicitDisconnect = true;
            setIsGone("received explit 'disconnect' request from client");
            LOGGER.info("Received explicit 'disconnect' request from Client "
                + getClientName() + " - calling 'withdrawIfNeeded'.");
            withdrawIfNeeded(false);
            server.sendDisconnect();
        }

        else if (method.equals(Constants.stopGame))
        {
            setIsGone("received explicit 'stopGame' request from " + "client"
                + getPlayerName());
            server.sendDisconnect();
            server.stopGame();
        }
        else if (method.equals(Constants.doSplit))
        {
            String parentId = args.remove(0);
            String childId = args.remove(0);
            String results = args.remove(0);
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            for (String name : results.split(","))
            {
                creatures.add(resolveCreatureType(name));
            }
            server.doSplit(resolveLegion(parentId), childId, creatures);
        }
        else if (method.equals(Constants.doMove))
        {
            String markerId = args.remove(0);
            String hexLabel = args.remove(0);
            EntrySide entrySide = EntrySide.fromLabel(args.remove(0));
            boolean teleport = Boolean.valueOf(args.remove(0)).booleanValue();
            CreatureType teleportingLord = resolveCreatureTypeNullOk(args
                .remove(0));
            server.doMove(resolveLegion(markerId), resolveMasterHex(hexLabel),
                entrySide, teleport, teleportingLord);
        }
        else if (method.equals(Constants.assignColor))
        {
            String color = args.remove(0);
            server.assignColor(PlayerColor.getByName(color));
        }
        else if (method.equals(Constants.assignFirstMarker))
        {
            String markerId = args.remove(0);
            server.assignFirstMarker(markerId);
        }
        else if (method.equals(Constants.newGame))
        {
            server.newGame();
        }
        else if (method.equals(Constants.loadGame))
        {
            String filename = args.remove(0);
            server.loadGame(filename);
        }
        else if (method.equals(Constants.saveGame))
        {
            String filename = args.remove(0);
            server.saveGame(filename);
        }

        else if (method.equals(Constants.checkConnection))
        {
            server.checkServerConnection();
        }

        else if (method.equals(Constants.peerRequestReceived))
        {
            String respondingClientName = args.remove(0);
            int queueLen = Integer.parseInt(args.remove(0));
            server.peerRequestReceived(respondingClientName, queueLen);
        }

        else if (method.equals(Constants.peerRequestProcessed))
        {
            String respondingClientName = args.remove(0);
            server.peerRequestProcessed(respondingClientName);
        }

        else if (method.equals(Constants.checkAllConnections))
        {
            String requestingClientName = args.remove(0);
            server.checkAllConnections(requestingClientName);
        }

        else if (method.equals(Constants.requestSyncDelta))
        {
            int lastReceivedMsgNr = Integer.parseInt(args.remove(0));
            int syncRequestNr = -1;
            // clients version 3 don't send this, only from 4 on
            if (args.size() > 0)
            {
                syncRequestNr = Integer.parseInt(args.remove(0));
            }
            server.requestSyncDelta(lastReceivedMsgNr, syncRequestNr);
        }

        else if (method.equals(Constants.catchupConfirmation))
        {
            server.clientConfirmedCatchup();
        }

        else if (method.equals(Constants.replyToPing))
        {
            lastPingReplyReceived = new Date().getTime();
            long replyReceived = lastPingReplyReceived;
            if (args.size() >= 3)
            {
                int requestNr = Integer.parseInt(args.remove(0));
                long requestSent = Long.parseLong(args.remove(0));
                long replySent = Long.parseLong(args.remove(0));
                server.replyToPing(playerName, requestNr, requestSent,
                    replySent, replyReceived);
            }
            else
            {
                server.replyToPing(playerName, 0, 0L, 0L, replyReceived);
            }
        }

        else if (method.equals(Constants.confirmCommitPoint))
        {
            int cpNr = Integer.parseInt(args.remove(0));
            confirmCommitPoint(cpNr);
        }

        else if (method.equals(Constants.logMsgToServer))
        {
            String severity = args.remove(0);
            String message = args.remove(0);
            server.logMsgToServer(severity, message);
        }

        else
        {
            LOGGER.log(Level.SEVERE, "Bogus packet (Server, method: " + method
                + ", args: " + args + ")");
        }
    }

    private BattleHex resolveBattleHex(String hexLabel)
    {
        return server.getGame().getBattleSS().getLocation().getTerrain()
            .getHexByLabel(hexLabel);
    }

    // TODO resolveX methods are on both sides of the network, they should
    // be extracted into some resolver object (or a base class)
    private CreatureType resolveCreatureType(String name)
    {
        return server.getGame().getVariant().getCreatureByName(name);
    }

    /**
     * There are cases where "null" comes over network and is not meant to
     * be resolved to a CreatureType, namely:
     * teleportingLord if no teleport; null recruiter; decline Acquire.
     * TODO What to do with the "Anything"?
     * @param name Name of the creatureType to find, might be "null"
     * @return CreatureType for that name, or null if name is "null"
     */
    private CreatureType resolveCreatureTypeNullOk(String name)
    {
        return name.equals("null") ? null : resolveCreatureType(name);
    }

    private MasterHex resolveMasterHex(String hexLabel)
    {
        return server.getGame().getVariant().getMasterBoard()
            .getHexByLabel(hexLabel);
    }

    private Legion resolveLegion(String markerId)
    {
        // TODO: currently doSummon still allows a null legion (and thus legion marker
        //       on the network) to indicate that a summon was skipped. To disallow
        //       having the null values in here we would need to introduce a new
        //       network message such as "doneSummoning".
        // Comment (Clemens): one day I would like to get Summon as part of
        // the move phase, not as "do it or decline it request" from server.
        // When we get there, there is no need for null nor for explicit
        // doneSummoning.
        if (markerId.equals("null"))
        {
            return null;
        }
        return server.getGame().getLegionByMarkerId(markerId);
    }

    // Wrapper for all the send-over-socket methods:
    @Override
    protected void sendToClient(String message)
    {
        enqueueToRedoQueue(messageCounter, message);

        /*
        // For development purposes... remove when done:
        List<String> li = Split.split(sep, message);
        String method = li.get(0);

        if (isSpectator())
        {
            System.out.println("-->" + method);
        }
        else if (getClientName().equals("remote"))
        {
            System.out.println("==>" + method);
        }
        */

        /*
        if (isGone())
        {
            String tmpString = message + "                  ";
            String msgStart = tmpString.substring(0, 20);
            LOGGER.info("No point to send '" + msgStart
                + "' connection already gone: " + playerName);
            return;
        }
        */

        if (obsolete || socketChannel == null || isGone)
        {
            // do not send any more
            if (cantSendMessageRepeated < 3)
            {
                int flags = (isGone ? 1 : 0) | (obsolete ? 2 : 0)
                    | (socketChannel == null ? 4 : 0);
                LOGGER.info("Attempt to send to player " + playerName
                    + " when client connection already gone (reason: " + flags
                    + ")- message: " + message);
                cantSendMessageRepeated++;
            }
        }
        else
        {
            if (Constants.USE_RECORDER)
            {
                server.getRecorder().recordMessageToClient(this, message);
            }
            if (server.getGame().isGameOver())
            {
                LOGGER.info("GameOver: Sending to " + playerName + ": "
                    + message);
            }
            sendViaChannel(message);

            // TODO: are the null checks needed? Can that ever happen?
            // They were here as explicit if-cases, producing SEVERE log
            // messages if null.
            if (server != null && server.getGame() != null
                && server.getGame().isLoadingGame())
            {
                // Give clients some opportunity to process it
                // (especially during replay during loading game)
                Thread.yield();
            }
        }
    }

    // =======================================================================
    // The IClient methods (which serialize the calls into sendToClient
    // executions) are all in ClientHandlerStub.
    // =======================================================================

    /**
     * Debug stuff,  only for testing/development purposes
     */

    public boolean fakeDisconnect = false;

    public void fakeDisconnectClient()
    {
        this.fakeDisconnect = true;
    }

    public void clearDisconnectClient()
    {
        this.fakeDisconnect = false;
    }

    public boolean wasFakeDisconnectFlagSet()
    {
        return fakeDisconnect;
    }
}
