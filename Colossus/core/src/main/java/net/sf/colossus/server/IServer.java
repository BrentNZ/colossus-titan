package net.sf.colossus.server;


import java.util.List;

import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.events.RecruitEvent;
import net.sf.colossus.game.events.SummonEvent;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 *  IServer is an interface for the client-accessible parts of Server.
 *
 *  @author David Ripton
 */
public interface IServer
{
    public static final int CLIENT_VERSION = 1;

    // Clients that do not send version yet at all, are treated as version -1.
    // For those, even show on server side an error dialog and refuse them to
    // connect.
    public static final int MINIMUM_CLIENT_VERSION = 0;

    public void leaveCarryMode();

    public void doneWithBattleMoves();

    public void doneWithStrikes();

    public void acquireAngel(Legion legion, CreatureType angelType);

    /**
     * Handles a summon event
     *
     * @param event The summon event or null if summoning is not wanted.
     */
    public void doSummon(SummonEvent event);

    // TODO extend or subclass event to include recruiter
    public void doRecruit(RecruitEvent event);

    public void engage(MasterHex hex);

    public void concede(Legion legion);

    public void doNotConcede(Legion legion);

    public void flee(Legion legion);

    public void doNotFlee(Legion legion);

    public void makeProposal(String proposalString);

    public void fight(MasterHex hex);

    public void doBattleMove(int tag, BattleHex hex);

    public void strike(int tag, BattleHex hex);

    public void applyCarries(BattleHex hex);

    public void undoBattleMove(BattleHex hex);

    public void assignStrikePenalty(String prompt);

    public void mulligan();

    public void undoSplit(Legion splitoff);

    public void undoMove(Legion legion);

    public void undoRecruit(Legion legion);

    public void doneWithSplits();

    public void doneWithMoves();

    public void doneWithEngagements();

    public void doneWithRecruits();

    public void withdrawFromGame();

    public void disconnect();

    public void stopGame();

    /**
     * Executes a split of certain creatures from a legion.
     *
     * @param parent The legion to split the creatures out of.
     * @param childMarker A marker for the new legion.
     * @param creaturesToSplit The creatures to split out.
     */
    public void doSplit(Legion parent, String childMarker,
        List<CreatureType> creaturesToSplit);

    public void doMove(Legion legion, MasterHex hex, EntrySide entrySide,
        boolean teleport, CreatureType teleportingLord);

    public void assignColor(PlayerColor color);

    public void assignFirstMarker(String markerId);

    // XXX Disallow the following methods in network games
    public void newGame();

    public void loadGame(String filename);

    public void saveGame(String filename);

    public void checkServerConnection();

    public void clientConfirmedCatchup();
}
