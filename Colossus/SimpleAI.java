import java.io.*;
import java.util.*;
import org.apache.log4j.*;

/**
 * Simple implementation of a Titan AI
 * @version $Id$
 * @author Bruce Sherrod
 * @author David Ripton
 */


class SimpleAI implements AI
{
    private Minimax minimax = new Minimax();

    // log4j
    public static Category cat = Category.getInstance(AI.class.getName());

    static
    {
        // log4j
        PropertyConfigurator.configure(Game.logConfigFilename);
    }

    public static void debugln(String s)
    {
        cat.debug(s);
    }


    public void muster(Game game)
    {
        // Do not recruit if this legion is a scooby snack.
        double scoobySnackFactor = 0.15;
        int minimumSizeToRecruit = (int)(scoobySnackFactor
                * game.getAverageLegionPointValue());
        Player player = game.getActivePlayer();
        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();

            if (legion.hasMoved() && legion.canRecruit()
                && (legion.hasTitan() ||
                legion.getPointValue() >= minimumSizeToRecruit))
            {
                Creature recruit = chooseRecruit(game, legion,
                        legion.getCurrentHex());

                if (recruit != null)
                {
                    game.doRecruit(recruit, legion, game.getMasterFrame());
                }
            }
        }

        game.getBoard().unselectAllHexes();
        game.updateStatusScreen();
    }


    public Creature reinforce(Legion legion, Game game)
    {
        return chooseRecruit(game, legion, legion.getCurrentHex());
    }


    private static Creature chooseRecruit(Game game, Legion legion,
        MasterHex hex)
    {
        List recruits = game.findEligibleRecruits(legion, hex);

        if (recruits.size() == 0)
        {
            return null;
            // pick the last creature in the list (which is the
            // best/highest recruit)
        }

        Creature recruit = (Creature)recruits.get(recruits.size() - 1);

        // take third cyclops in brush
        if (recruit == Creature.gorgon && recruits.contains(Creature.cyclops)
            && legion.numCreature(Creature.behemoth) == 0
            && legion.numCreature(Creature.cyclops) == 2)
        {
            recruit = Creature.cyclops;
        }
        // take a fourth cyclops in brush
        // (so that we can split out a cyclops and still keep 3)
        else if (recruit == Creature.gorgon
                 && recruits.contains(Creature.cyclops)
                 && legion.getHeight() == 6
                 && legion.numCreature(Creature.behemoth) == 0
                 && legion.numCreature(Creature.gargoyle) == 0)
        {
            recruit = Creature.cyclops;
        }
        // prefer warlock over guardian (should be built in now)
        else if (recruits.contains(Creature.guardian)
                 && recruits.contains(Creature.warlock))
        {
            recruit = Creature.warlock;
        }
        // take a third lion/troll if we've got at least 1 way to desert/swamp
        // from here and we're not about to be attacked
        else if (recruits.contains(Creature.lion)
                 && recruits.contains(Creature.ranger)
                 && legion.numCreature(Creature.lion) == 2
                 && getNumberOfWaysToTerrain(legion, hex, 'D') > 0)
        {
            recruit = Creature.lion;
        }
        else if (recruits.contains(Creature.troll)
                 && recruits.contains(Creature.ranger)
                 && legion.numCreature(Creature.troll) == 2
                 && getNumberOfWaysToTerrain(legion, hex, 'S') > 0)
        {
            recruit = Creature.troll;
        }
        // tower creature selection:
        else if (recruits.contains(Creature.ogre)
                 && recruits.contains(Creature.centaur)
                 && recruits.contains(Creature.gargoyle)
                 && recruits.size() == 3)
        {
            // if we have 2 centaurs or ogres, take a third
            if (legion.numCreature(Creature.ogre) == 2)
            {
                recruit = Creature.ogre;
            }
            else if (legion.numCreature(Creature.centaur) == 2)
            {
                recruit = Creature.centaur;
                // else if we have 1 of a tower creature, take a matching one
            }
            else if (legion.numCreature(Creature.gargoyle) == 1)
            {
                recruit = Creature.gargoyle;
            }
            else if (legion.numCreature(Creature.ogre) == 1)
            {
                recruit = Creature.ogre;
            }
            else if (legion.numCreature(Creature.centaur) == 1)
            {
                recruit = Creature.centaur;
                // else if there's cyclops left and we don't have 2
                // gargoyles, take a gargoyle
            }
            else if (game.getCaretaker().getCount(Creature.cyclops) > 6
                     && legion.numCreature(Creature.gargoyle) < 2)
            {
                recruit = Creature.gargoyle;
                // else if there's trolls left and we don't have 2 ogres,
                // take an ogre
            }
            else if (game.getCaretaker().getCount(Creature.troll) > 6
                     && legion.numCreature(Creature.ogre) < 2)
            {
                recruit = Creature.ogre;
                // else if there's lions left and we don't have 2 lions,
                // take a centaur
            }
            else if (game.getCaretaker().getCount(Creature.lion) > 6
                     && legion.numCreature(Creature.centaur) < 2)
            {
                recruit = Creature.centaur;
                // else we dont really care; take anything
            }
        }

        return recruit;
    }


    public void split(Game game)
    {
        Player player = game.getActivePlayer();

        for (int i = player.getNumLegions() - 1; i >= 0; i--)
        {
            Legion legion = player.getLegion(i);

            if (legion.getHeight() < 7)
            {
                continue;
                // don't split if we're likely to be forced to attack and lose
                // don't split if we're likely to want to fight and we need to
                // be 7 high.
                // only consider this if we're not doing initial game split
            }

            if (legion.getHeight() == 7)
            {
                int forcedToAttack = 0;

                for (int roll = 1; roll <= 6; roll++)
                {
                    Set moves = game.listMoves(legion, true,
                            legion.getCurrentHex(), roll);
                    int safeMoves = 0;
                    Iterator moveIt = moves.iterator();
                    while (moveIt.hasNext())
                    {
                        String hexLabel = (String)moveIt.next();
                        MasterHex hex = game.getBoard().getHexFromLabel(
                            hexLabel);

                        if (hex.getNumEnemyLegions(player) == 0)
                        {
                            safeMoves++;
                        }
                        else
                        {
                            Legion enemy = hex.getEnemyLegion(player);
                            int result = estimateBattleResults(legion, true,
                                    enemy, hex);

                            if (result == WIN_WITH_MINIMAL_LOSSES)
                            {
                                debugln("We can safely split AND attack with "
                                        + legion);

                                safeMoves++;
                            }

                            int result2 = estimateBattleResults(legion, false,
                                    enemy, hex);

                            if (result2 == WIN_WITH_MINIMAL_LOSSES
                                && result != WIN_WITH_MINIMAL_LOSSES
                                && roll <= 4)
                            {
                                // don't split so that we can attack!
                                debugln("Not splitting " + legion +
                                    " because we want the muscle to attack");

                                forcedToAttack = 999;
                            }
                        }
                    }

                    if (safeMoves == 0)
                    {
                        forcedToAttack++;
                    }
                }

                // If we'll be forced to attack on 2 or more rolls; don't split
                if (forcedToAttack >= 2)
                {
                    continue;
                }
            }

            // TODO: don't split if we're about to be attacked and we
            // need the muscle
            // TODO: don't split if there's no upwards recruiting
            // potential from our current location
            // create the new legion
            Legion newLegion = legion.split(chooseCreaturesToSplitOut(legion,
                    game.getNumPlayers()));
        }
    }


    /**
     * Decide how to split this legion, and return a list of
     * Creatures to remove.
     */
    private static List chooseCreaturesToSplitOut(Legion legion,
            int numPlayers)
    {
        //
        // split a 7 or 8 high legion somehow
        //
        // idea: pick the 2 weakest creatures and kick them
        // out. if there are more than 2 weakest creatures,
        // prefer a pair of matching ones.  if none match,
        // kick out the left-most ones (the oldest ones)
        //
        // For an 8-high starting legion, call helper
        // method doInitialGameSplit()
        //
        // TODO: keep 3 cyclops if we don't have a behemoth
        // (split out a gorgon instead)
        //
        // TODO: prefer to split out creatures that have no
        // recruiting value (e.g. if we have 1 angel, 2
        // centaurs, 2 gargoyles, and 2 cyclops, split out the
        // gargoyles)
        //
        if (legion.getHeight() == 8)
        {
            return doInitialGameSplit(legion.getCurrentHexLabel(), numPlayers);
        }

        Critter weakest1 = null;
        Critter weakest2 = null;

        for (Iterator critterIt = legion.getCritters().iterator();
             critterIt.hasNext(); )
        {
            Critter critter = (Critter)critterIt.next();

            if (weakest1 == null)
            {
                weakest1 = critter;
            }
            else if (weakest2 == null)
            {
                weakest2 = critter;
            }
            else if (critter.getPointValue() < weakest1.getPointValue())
            {
                weakest1 = critter;
            }
            else if (critter.getPointValue() < weakest2.getPointValue())
            {
                weakest2 = critter;
            }
            else if (critter.getPointValue() == weakest1.getPointValue()
                     && critter.getPointValue() == weakest2.getPointValue())
            {
                if (critter.getName().equals(weakest1.getName()))
                {
                    weakest2 = critter;
                }
                else if (critter.getName().equals(weakest2.getName()))
                {
                    weakest1 = critter;
                }
            }
        }

        ArrayList creaturesToRemove = new ArrayList();

        creaturesToRemove.add(weakest1);
        creaturesToRemove.add(weakest2);

        return creaturesToRemove;
    }


    private static List chooseCreaturesToSplitOut(Legion legion)
    {
        return chooseCreaturesToSplitOut(legion, 2);
    }


    // From Hugh Moore:
    //
    // It really depends on how many players there are and how good I
    // think they are.  In a 5 or 6 player game, I will pretty much
    // always put my gargoyles together in my Titan group. I need the
    // extra strength, and I need it right away.  In 3-4 player
    // games, I certainly lean toward putting my gargoyles together.
    // If my opponents are weak, I sometimes split them for a
    // challenge.  If my opponents are strong, but my situation looks
    // good for one reason or another, I may split them.  I never
    // like to split them when I am in tower 3 or 6, for obvious
    // reasons. In two player games, I normally split the gargoyles,
    // but two player games are fucked up.
    //

    /** Return a list of exactly four creatures (including one lord) to
     *  split out. */
    private static List doInitialGameSplit(String label, int numPlayers)
    {
        // in CMU style splitting, we split centaurs in even towers,
        // ogres in odd towers.
        final boolean oddTower = "100".equals(label) || "300".equals(label)
                || "500".equals(label);
        final Creature splitCreature = oddTower ? Creature.ogre
                : Creature.centaur;
        final Creature nonsplitCreature = oddTower ? Creature.centaur
                : Creature.ogre;

        // if lots of players, keep gargoyles with titan (we need the muscle)
        if (numPlayers > 4)
        {
            return CMUsplit(true, splitCreature, nonsplitCreature);
        }
        // don't split gargoyles in tower 3 or 6 (because of the extra jungles)
        else if ("300".equals(label) || "600".equals(label))
        {
            return CMUsplit(false, splitCreature, nonsplitCreature);
        }
        //
        // new idea due to David Ripton: split gargoyles in tower 2 or
        // 5, because on a 5 we can get to brush and jungle and get 2
        // gargoyles.  I'm not sure if this is really better than recruiting
        // a cyclops and leaving the other group in the tower, but it's
        // interesting so we'll try it.
        //
        else if ("200".equals(label) || "500".equals(label))
        {
            return MITsplit(true, splitCreature, nonsplitCreature);
        }
        //
        // otherwise, mix it up for fun
        else
        {
            if (Game.rollDie() <= 2)
            {
                return MITsplit(true, splitCreature, nonsplitCreature);
            }
            else
            {
                return CMUsplit(true, splitCreature, nonsplitCreature);
            }
        }
    }


    // Keep the gargoyles together.
    private static List CMUsplit(boolean favorTitan, Creature splitCreature,
            Creature nonsplitCreature)
    {
        LinkedList splitoffs = new LinkedList();

        if (favorTitan)
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(splitCreature);
            }
            else
            {
                splitoffs.add(Creature.angel);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(splitCreature);
            }
        }
        else
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
            }
            else
            {
                splitoffs.add(Creature.angel);
            }

            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(Creature.gargoyle);
                splitoffs.add(splitCreature);
            }
            else
            {
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(splitCreature);
            }
        }

        return splitoffs;
    }


    // Split the gargoyles.
    private static List MITsplit(boolean favorTitan, Creature splitCreature,
            Creature nonsplitCreature)
    {
        LinkedList splitoffs = new LinkedList();

        if (favorTitan)
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(Creature.gargoyle);
            }
            else
            {
                splitoffs.add(Creature.angel);
                splitoffs.add(splitCreature);
                splitoffs.add(splitCreature);
                splitoffs.add(Creature.gargoyle);
            }
        }
        else
        {
            if (Game.rollDie() <= 3)
            {
                splitoffs.add(Creature.titan);
            }
            else
            {
                splitoffs.add(Creature.angel);
            }

            if (Game.rollDie() <= 3)
            {
                splitoffs.add(nonsplitCreature);
                splitoffs.add(nonsplitCreature);
                splitoffs.add(Creature.gargoyle);
            }
            else
            {
                splitoffs.add(splitCreature);
                splitoffs.add(splitCreature);
                splitoffs.add(Creature.gargoyle);
            }
        }

        return splitoffs;
    }


    public void masterMove(Game game)
    {
        if (true)
        {
            simpleMove(game);
        }
        else
        {
            PlayerMove playermove = (PlayerMove)minimax.search(
                new MasterBoardPosition(game, game.getActivePlayerNum()), 1);

            // apply the PlayerMove
            for (Iterator it = playermove.moves.entrySet().iterator();
                 it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry)it.next();
                Legion legion = (Legion)entry.getKey();
                MasterHex hex = (MasterHex)entry.getValue();
                game.doMove(legion, hex);
            }
        }
    }


    /** little helper to store info about possible moves */
    private class MoveInfo
    {
        final Legion legion;
        /** hex to move to.  if hex == null, then this means sit still. */
        final MasterHex hex;
        final int value;
        final int difference;       // difference from sitting still

        MoveInfo(Legion legion, MasterHex hex, int value, int difference)
        {
            this.legion = legion;
            this.hex = hex;
            this.value = value;
            this.difference = difference;
        }
    }

    public void simpleMove(Game game)
    {
        Player player = game.getActivePlayer();

        // consider mulligans
        handleMulligans(game, player);

        /** cache all places enemies can move to, for use in risk analysis. */
        HashMap[] enemyAttackMap = buildEnemyAttackMap(game, player);

        // A mapping from Legion to ArrayList of MoveInfo objects,
        // listing all moves that we've evaluated.  We use this if
        // we're forced to move.
        HashMap moveMap = new HashMap();

        // Sort legions into order of decreasing importance, so that
        // the important ones get the first chance to move to good hexes.
        player.sortLegions();

        boolean movedALegion = handleVoluntaryMoves(game, player, moveMap,
            enemyAttackMap);

        // make sure we move splits (when forced)
        movedALegion = handleForcedSplitMoves(game, player,
            moveMap) || movedALegion;

        // make sure we move at least one legion
        if (!movedALegion)
        {
            handleForcedSingleMove(game, player, moveMap);

            // Perhaps that forced move opened up a good move for
            // another legion.  So iterate again.
            handleVoluntaryMoves(game, player, moveMap, enemyAttackMap);
        }
    }

    private static void handleMulligans(Game game, Player player)
    {
        // TODO: This is really stupid.  Do something smart here.
        if (player.getMulligansLeft() > 0 && (player.getMovementRoll() == 2 ||
            player.getMovementRoll() == 5))
        {
            player.takeMulligan();
            player.rollMovement();
            // Necessary to update the movement roll in the title bar.
            game.getBoard().setupMoveMenu();
        }
    }

    /** Return true if something was moved. */
    private boolean handleVoluntaryMoves(Game game, Player player,
        HashMap moveMap, HashMap [] enemyAttackMap)
    {
        boolean movedALegion = false;

        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            // compute the value of sitting still
            ArrayList moveList = new ArrayList();

            moveMap.put(legion, moveList);

            MoveInfo sitStillMove = new MoveInfo(legion, null,
                evaluateMove(game, legion, legion.getCurrentHex(), false,
                enemyAttackMap), 0);

            moveList.add(sitStillMove);

            // find the best move (1-ply search)
            MasterHex bestHex = null;
            int bestValue = Integer.MIN_VALUE;
            Set set = game.listMoves(legion, true);

            for (Iterator moveIterator = set.iterator();
                moveIterator.hasNext();)
            {
                final String hexLabel = (String)moveIterator.next();
                final MasterHex hex = game.getBoard().getHexFromLabel(
                    hexLabel);
                final int value = evaluateMove(game, legion, hex, true,
                    enemyAttackMap);

                if (value > bestValue || bestHex == null)
                {
                    bestValue = value;
                    bestHex = hex;
                }

                MoveInfo move = new MoveInfo(legion, hex, value,
                        value - sitStillMove.value);

                moveList.add(move);
            }

            // if we found a move that's better than sitting still, move
            if (bestValue > sitStillMove.value)
            {
                game.doMove(legion, bestHex);
                movedALegion = true;
            }
        }

        return movedALegion;
    }

    /** Return true if something was moved. */
    private static boolean handleForcedSplitMoves(Game game, Player player,
        HashMap moveMap)
    {
        boolean movedALegion = false;
        List legions = player.getLegions();
        Iterator it = legions.iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            MasterHex hex = legion.getCurrentHex();
            List friendlyLegions = hex.getFriendlyLegions(player);

            outer: while (friendlyLegions.size() > 1
                   && game.countConventionalMoves(legion) > 0)
            {
                // Pick the legion in this hex whose best move has the
                // least difference with its sitStillValue, scaled by
                // the point value of the legion, and force it to move.
                debugln("Ack! forced to move a split group");

                // first, concatenate all the moves for all the
                // legions that are here, and sort them by their
                // difference from sitting still multiplied by
                // the value of the legion.
                ArrayList allmoves = new ArrayList();
                Iterator friendlyLegionIt = friendlyLegions.iterator();
                while (friendlyLegionIt.hasNext())
                {
                    Legion friendlyLegion = (Legion)friendlyLegionIt.next();
                    ArrayList moves = (ArrayList)moveMap.get(friendlyLegion);
                    allmoves.addAll(moves);
                }

                Collections.sort(allmoves, new Comparator()
                {
                    public int compare(Object o1, Object o2)
                    {
                        MoveInfo m1 = (MoveInfo)o1;
                        MoveInfo m2 = (MoveInfo)o2;
                        return m2.difference * m2.legion.getPointValue() -
                            m1.difference * m1.legion.getPointValue();
                    }
                });

                // now, one at a time, try applying moves until we
                // have handled our split problem.
                Iterator moveIt = allmoves.iterator();
                while (moveIt.hasNext())
                {
                    MoveInfo move = (MoveInfo)moveIt.next();
                    if (move.hex == null)
                    {
                        continue;       // skip the sitStill moves
                    }
                    debugln("forced to move split legion " + move.legion
                            + " to " + move.hex + " taking penalty "
                            + move.difference
                            + " in order to handle illegal legion " + legion);
                    game.doMove(move.legion, move.hex);

                    movedALegion = true;
                    // check again if this legion is ok; if so, break
                    friendlyLegions = hex.getFriendlyLegions(player);
                    if (friendlyLegions.size() > 1
                        && game.countConventionalMoves(legion) > 0)
                    {
                        continue;
                    }
                    else
                    {
                        break outer;    // legion is all set
                    }
                }
            }
        }
        return movedALegion;
    }

    /** Return true if something was moved. */
    private void handleForcedSingleMove(Game game, Player player,
        HashMap moveMap)
    {
        debugln("Ack! forced to move someone");

        // Pick the legion whose best move has the least
        // difference with its sitStillValue, scaled by the
        // point value of the legion, and force it to move.

        // first, concatenate all the moves for all the
        // legions that are here, and sort them by their
        // difference from sitting still

        ArrayList allmoves = new ArrayList();
        List legions = player.getLegions();
        Iterator friendlyLegionIt = legions.iterator();
        while (friendlyLegionIt.hasNext())
        {
            Legion friendlyLegion = (Legion)friendlyLegionIt.next();
            ArrayList moves = (ArrayList)moveMap.get(friendlyLegion);

            allmoves.addAll(moves);
        }

        Collections.sort(allmoves, new Comparator()
        {
            public int compare(Object o1, Object o2)
            {
                MoveInfo m1 = (MoveInfo)o1;
                MoveInfo m2 = (MoveInfo)o2;
                return m2.difference * m2.legion.getPointValue() -
                    m1.difference * m1.legion.getPointValue();
            }
        });

        // now, one at a time, try applying moves until we
        // have moved a legion
        Iterator moveIt = allmoves.iterator();
        while (moveIt.hasNext() && player.legionsMoved() == 0
               && player.countMobileLegions() > 0)
        {
            MoveInfo move = (MoveInfo)moveIt.next();

            if (move.hex == null)
            {
                continue;       // skip the sitStill moves
            }

            debugln("forced to move " + move.legion + " to " + move.hex
                    + " taking penalty " + move.difference
                    + " in order to handle illegal legion " + move.legion);
            game.doMove(move.legion, move.hex);
        }
    }


    private static HashMap[] buildEnemyAttackMap(Game game, Player player)
    {
        HashMap[] enemyMap = new HashMap[7];

        for (int i = 1; i <= 6; i++)
        {
            enemyMap[i] = new HashMap();
        }

        // for each enemy player
        Collection players = game.getPlayers();
        Iterator playerIt = players.iterator();
        while (playerIt.hasNext())
        {
            Player enemyPlayer = (Player)playerIt.next();

            if (enemyPlayer == player)
            {
                continue;
                // for each legion that player controls
            }

            List legions = enemyPlayer.getLegions();
            Iterator legionIt = legions.iterator();
            while (legionIt.hasNext())
            {
                Legion legion = (Legion)legionIt.next();

                // debugln("checking where " + legion +
                //    " can attack next turn..");
                // for each movement roll he might make
                for (int roll = 1; roll <= 6; roll++)
                {
                    // count the moves he can get to
                    Set set = game.listMoves(legion, false,
                            legion.getCurrentHex(), roll);
                    Iterator moveIt = set.iterator();
                    while (moveIt.hasNext())
                    {
                        String hexlabel = (String)moveIt.next();

                        for (int effectiveRoll = roll; effectiveRoll <= 6;
                             effectiveRoll++)
                        {
                            // legion can attack to hexlabel on a effectiveRoll
                            ArrayList list = (ArrayList)enemyMap[effectiveRoll]
                                .get(hexlabel);

                            if (list == null)
                            {
                                list = new ArrayList();
                            }

                            if (list.contains(legion))
                            {
                                continue;
                            }

                            list.add(legion);
                            // debugln("" + legion + " can attack "
                            // + hexlabel + " on a " + effectiveRoll);
                            enemyMap[effectiveRoll].put(hexlabel, list);
                        }
                    }
                }
            }
        }

        // debugln("built map");
        return enemyMap;
    }


    //
    // cheap, inaccurate evaluation function.  Returns a value for
    // moving this legion to this hex.  The value defines a distance
    // metric over the set of all possible moves.
    //
    // TODO: should be parameterized with weights
    //
    private static int evaluateMove(Game game, Legion legion, MasterHex hex,
            boolean canRecruitHere, HashMap[] enemyAttackMap)
    {
        // Avoid using MIN_VALUE and MAX_VALUE because of possible overflow.
        final int WIN_GAME = Integer.MAX_VALUE / 2;
        final int LOSE_GAME = Integer.MIN_VALUE / 2;
        final int LOSE_LEGION = -10000;

        int value = 0;
        // consider making an attack
        final Legion enemyLegion = hex.getEnemyLegion(legion.getPlayer());

        if (enemyLegion != null)
        {
            final int enemyPointValue = enemyLegion.getPointValue();
            final int result = estimateBattleResults(legion, enemyLegion, hex);

            switch (result)
            {

                case WIN_WITH_MINIMAL_LOSSES:
                    debugln("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and WIN_WITH_MINIMAL_LOSSES");

                    // we score a fraction of an angel
                    value += (24 * enemyPointValue) / 100;
                    // plus a fraction of a titan strength
                    value += (6 * enemyPointValue) / 100;
                    // plus some more for killing a group (this is arbitrary)
                    value += (10 * enemyPointValue) / 100;

                    // TODO: if enemy titan, we also score half points
                    // (this may make the AI unfairly gun for your titan)
                    break;

                case WIN_WITH_HEAVY_LOSSES:
                    debugln("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and WIN_WITH_HEAVY_LOSSES");
                // don't do this with our titan unless we can win the game
                    Player player = legion.getPlayer();
                    List legions = player.getLegions();
                    boolean haveOtherAngels = false;
                    Iterator it = legions.iterator();
                    while (it.hasNext())
                    {
                        Legion l = (Legion)it.next();

                        if (l == legion)
                        {
                            continue;
                        }

                        if (l.numCreature(Creature.angel) == 0)
                        {
                            continue;
                        }

                        haveOtherAngels = true;

                        break;
                    }

                    if (legion.hasTitan())
                    {
                        // unless we can win the game with this attack
                        if (enemyLegion.hasTitan() &&
                            game.getNumLivingPlayers() == 2)
                        {
                            // do it and win the game
                            value += enemyPointValue;
                        }
                        else
                        {
                            // ack! we'll fuck up our titan group
                            value += LOSE_LEGION + 10;
                        }
                    }
                    // don't do this if we'll lose our only angel group
                    // and won't score enough points to make up for it
                    else if (legion.numCreature(Creature.angel) > 0
                             &&!haveOtherAngels && enemyPointValue < 88)
                    {
                        value += LOSE_LEGION + 5;
                    }
                    else
                    {
                        // we score a fraction of an angel & titan strength
                        value += (30 * enemyPointValue) / 100;
                        // but we lose this group
                        value -= (20 * legion.getPointValue()) / 100;
                        // TODO: if we have no other angels, more penalty here
                        // TODO: if enemy titan, we also score half points
                        // (this may make the AI unfairly gun for your titan)
                    }
                    break;

                case DRAW:
                    debugln("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and DRAW");

                    // If this is an unimportant group for us, but
                    // is enemy titan, do it.  This might be an
                    // unfair use of information for the AI
                    if (legion.numLords() == 0 && enemyLegion.hasTitan())
                    {
                        // Arbitrary value for killing a player but
                        // scoring no points: it's worth a little
                        // If there are only 2 players, we should do this.
                        if (game.getNumPlayers() == 2)
                        {
                            value = WIN_GAME;
                        }
                        else
                        {
                            value += enemyPointValue / 6;
                        }
                    }
                    else
                    {
                        // otherwise no thanks
                        value += LOSE_LEGION + 2;
                    }
                    break;

                case LOSE_BUT_INFLICT_HEAVY_LOSSES:
                    debugln("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex
                            + " and LOSE_BUT_INFLICT_HEAVY_LOSSES");

                    // TODO: how important is it that we damage
                    // his group?
                    value += LOSE_LEGION + 1;
                    break;

                case LOSE:
                    debugln("legion " + legion + " can attack " + enemyLegion
                            + " in " + hex + " and LOSE");

                    value += LOSE_LEGION;
                    break;
            }
        }

        // consider what we can recruit
        Creature recruit = null;

        if (canRecruitHere)
        {
            recruit = chooseRecruit(game, legion, hex);

            if (recruit != null)
            {
                int oldval = value;

                if (legion.getHeight() < 6)
                {
                    value += recruit.getPointValue();
                }
                else
                {
                    // if we're 6-high, then the value of a recruit is
                    // equal to the improvement in the value of the
                    // pieces that we'll have after splitting.
                    // TODO this should call our splitting code to see
                    // what split decision we would make

                    // This special case was overkill.  A 6-high stack
                    // with 3 lions, or a 6-high stack with 3 clopses,
                    // sometimes refused to go to a safe desert/jungle
                    // because the value of the recruit was toned down
                    // too much. So the effect has been reduced by half.
                    debugln("--- 6-HIGH SPECIAL CASE");

                    Critter weakest1 = null;
                    Critter weakest2 = null;

                    for (Iterator critterIt = legion.getCritters().iterator();
                         critterIt.hasNext(); )
                    {
                        Critter critter = (Critter)critterIt.next();

                        if (weakest1 == null)
                        {
                            weakest1 = critter;
                        }
                        else if (weakest2 == null)
                        {
                            weakest2 = critter;
                        }
                        else if (critter.getPointValue()
                                 < weakest1.getPointValue())
                        {
                            weakest1 = critter;
                        }
                        else if (critter.getPointValue()
                                 < weakest2.getPointValue())
                        {
                            weakest2 = critter;
                        }
                        else if (critter.getPointValue()
                                 == weakest1.getPointValue()
                                 && critter.getPointValue()
                                    == weakest2.getPointValue())
                        {
                            if (critter.getName().equals(weakest1.getName()))
                            {
                                weakest2 = critter;
                            }
                            else if (critter.getName().equals(
                                weakest2.getName()))
                            {
                                weakest1 = critter;
                            }
                        }
                    }

                    int minCreaturePV = Math.min(weakest1.getPointValue(),
                            weakest2.getPointValue());
                    int maxCreaturePV = Math.max(weakest1.getPointValue(),
                            weakest2.getPointValue());
                    // point value of my best 5 pieces right now
                    int oldPV = legion.getPointValue() - minCreaturePV;
                    // point value of my best 5 pieces after adding this
                    // recruit and then splitting off my 2 weakest
                    int newPV = legion.getPointValue()
                        - weakest1.getPointValue()
                        - weakest2.getPointValue()
                        + Math.max(maxCreaturePV, recruit.getPointValue());

                    value += (((newPV - oldPV) + recruit.getPointValue()) / 2);
                }

                debugln("--- if " + legion + " moves to " + hex
                        + " then recruit " + recruit.toString() + " (adding "
                        + (value - oldval) + ")");
            }
        }

        // consider what we might be able to recruit next turn, from here
        int nextTurnValue = 0;

        for (int roll = 1; roll <= 6; roll++)
        {
            Set moves = game.listMoves(legion, true, hex, roll);
            int bestRecruitVal = 0;
            Creature bestRecruit = null;

            for (Iterator nextMoveIt = moves.iterator();
                 nextMoveIt.hasNext(); )
            {
                String nextLabel = (String)nextMoveIt.next();
                MasterHex nextHex = game.getBoard().getHexFromLabel(nextLabel);
                // if we have to fight in that hex and we can't
                // WIN_WITH_MINIMAL_LOSSES, then assume we can't
                // recruit there.  IDEA: instead of doing any of this
                // work, perhaps we could recurse here to get the
                // value of being in _that_ hex next turn... and then
                // maximize over choices and average over die rolls.
                // this would be essentially minimax but ignoring the
                // others players ability to move.
                Legion enemy = nextHex.getEnemyLegion(legion.getPlayer());

                if (enemy != null
                    && estimateBattleResults(legion, enemy, nextHex)
                       != WIN_WITH_MINIMAL_LOSSES)
                {
                    continue;
                }

                List nextRecruits = game.findEligibleRecruits(legion, nextHex);

                if (nextRecruits.size() == 0)
                {
                    continue;
                }

                Creature nextRecruit =
                    (Creature)nextRecruits.get(nextRecruits.size() - 1);
                int val = nextRecruit.getSkill() * nextRecruit.getPower();

                if (val > bestRecruitVal)
                {
                    bestRecruitVal = val;
                    bestRecruit = nextRecruit;
                }
            }

            nextTurnValue += bestRecruitVal;
        }

        nextTurnValue /= 6;     // 1/6 chance of each happening
        value += nextTurnValue;

        // consider risk of being attacked
        if (enemyAttackMap != null)
        {
            if (canRecruitHere)
            {
                debugln("considering risk of moving " + legion + " to " + hex);
            }
            else
            {
                debugln("considering risk of leaving " + legion + " in " +
                    hex);
            }

            HashMap[] enemiesThatCanAttackOnA = enemyAttackMap;
            int roll;

            for (roll = 1; roll <= 6; roll++)
            {
                List enemies =
                    (List)enemiesThatCanAttackOnA[roll].get(hex.getLabel());

                if (enemies == null)
                {
                    continue;
                }

                debugln("got enemies that can attack on a " + roll + " :"
                        + enemies);

                Iterator it = enemies.iterator();
                while (it.hasNext())
                {
                    Legion enemy = (Legion)it.next();
                    final int result = estimateBattleResults(enemy, false,
                            legion, hex, recruit);

                    if (result == WIN_WITH_MINIMAL_LOSSES ||
                        result == DRAW && legion.hasTitan())
                    {
                        break;
                        // break on the lowest roll from which we can
                        // be attacked and killed
                    }
                }
            }

            if (roll < 7)
            {
                final double chanceToAttack = (7.0 - roll) / 6.0;
                final double risk;

                if (legion.hasTitan())
                {
                    risk = LOSE_LEGION * chanceToAttack;
                }
                else
                {
                    risk = -legion.getPointValue() / 2 * chanceToAttack;
                }

                value += risk;
            }
        }

        // TODO: consider mobility.  e.g., penalty for suckdown
        // squares, bonus if next to tower or under the top
        // TODO: consider what we can attack next turn from here
        // TODO: consider nearness to our other legions
        // TODO: consider being a scooby snack (if so, everything
        // changes: we want to be in a location with bad mobility, we
        // want to be at risk of getting killed, etc)
        // TODO: consider risk of being scooby snacked (this might be inherent)
        // TODO: consider splitting up our good recruitment rolls
        // (i.e. if another legion has warbears under the top that
        // recruit on 1,3,5, and we have a behemoth with choice of 3/5
        // to jungle or 4/6 to jungle, prefer the 4/6 location).
        debugln("EVAL " + legion
                + (canRecruitHere ? " move to " : " stay in ") + hex + " = "
                + value);

        return value;
    }


    private static final int WIN_WITH_MINIMAL_LOSSES = 0;
    private static final int WIN_WITH_HEAVY_LOSSES = 1;
    private static final int DRAW = 2;
    private static final int LOSE_BUT_INFLICT_HEAVY_LOSSES = 3;
    private static final int LOSE = 4;

    private static int estimateBattleResults(Legion attacker, Legion defender,
            MasterHex hex)
    {
        return estimateBattleResults(attacker, false, defender, hex, null);
    }


    private static int estimateBattleResults(Legion attacker,
            boolean attackerSplitsBeforeBattle, Legion defender, MasterHex hex)
    {
        return estimateBattleResults(attacker, attackerSplitsBeforeBattle,
                defender, hex, null);
    }


    private static int estimateBattleResults(Legion attacker,
            boolean attackerSplitsBeforeBattle, Legion defender,
            MasterHex hex, Creature recruit)
    {
        char terrain = hex.getTerrain();
        double attackerPointValue = getCombatValue(attacker, terrain);

        if (attackerSplitsBeforeBattle)
        {
            // remove PV of the split
            List creaturesToRemove = chooseCreaturesToSplitOut(attacker);
            Iterator creatureIt = creaturesToRemove.iterator();
            while (creatureIt.hasNext())
            {
                Creature creature = (Creature)creatureIt.next();

                attackerPointValue -= getCombatValue(creature, terrain);
            }
        }

        if (recruit != null)
        {
            // debugln("adding in recruited " + recruit +
            // " when evaluating battle");
            attackerPointValue += getCombatValue(recruit, terrain);
        }

        // TODO: add angel call
        double defenderPointValue = getCombatValue(defender, terrain);

        // TODO: add in enemy's most likely turn 4 recruit
        if (hex.getTerrain() == 'T')
        {
            // defender in the tower!  ouch!
            defenderPointValue *= 1.2;
        }

        // TODO: adjust for entry side
        // really dumb estimator
        double ratio = (double)attackerPointValue / (double)defenderPointValue;

        if (ratio >= 1.30)
        {
            return WIN_WITH_MINIMAL_LOSSES;
        }
        else if (ratio >= 1.15)
        {
            return WIN_WITH_HEAVY_LOSSES;
        }
        else if (ratio >= 0.85)
        {
            return DRAW;
        }
        else if (ratio >= 0.70)
        {
            return LOSE_BUT_INFLICT_HEAVY_LOSSES;
        }
        else    // ratio less than 0.70
        {
            return LOSE;
        }
    }


    private static int getNumberOfWaysToTerrain(Legion legion, MasterHex hex,
            char terrainType)
    {
        // if moves[i] is true, then a roll of i can get us to terrainType.
        boolean[] moves = new boolean[7];
        // consider normal moves
        int block = Game.ARCHES_AND_ARROWS;

        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == MasterHex.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }

        findNormalMovesToTerrain(legion, legion.getPlayer(), hex, 6, block,
                Game.NOWHERE, terrainType, moves);

        // consider tower teleport
        if (hex.getTerrain() == 'T' && legion.numLords() > 0
            && moves[6] == false)
        {
            // hack: assume that we can always tower teleport to the terrain we
            // want to go to.
            // TODO: check to make sure there's an open terrain to teleport to.
            // (probably want a lookup table for this)
            moves[6] = true;
        }

        int count = 0;

        for (int i = 0; i < moves.length; i++)
        {
            if (moves[i])
            {
                count++;
            }
        }

        return count;
    }


    private static void findNormalMovesToTerrain(Legion legion, Player player,
            MasterHex hex, int roll, int block, int cameFrom,
            char terrainType, boolean[] moves)
    {
        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (hex.getNumEnemyLegions(player) > 0)
        {
            if (hex.getNumFriendlyLegions(player) == 0)
            {
                // we can move to here
                if (hex.getTerrain() == terrainType)
                {
                    moves[roll] = true;
                }
            }

            return;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                // Account for spin cycles.
                if (player.getLegion(i).getCurrentHex() == hex
                    && player.getLegion(i) != legion)
                {
                    return;
                }
            }

            if (hex.getTerrain() == terrainType)
            {
                moves[roll] = true;
            }

            return;
        }

        if (block >= 0)
        {
            findNormalMovesToTerrain(legion, player, hex.getNeighbor(block),
                    roll - 1, Game.ARROWS_ONLY, (block + 3) % 6, terrainType,
                    moves);
        }
        else if (block == Game.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARCH && i != cameFrom)
                {
                    findNormalMovesToTerrain(legion, player,
                            hex.getNeighbor(i), roll - 1, Game.ARROWS_ONLY,
                            (i + 3) % 6, terrainType, moves);
                }
            }
        }
        else if (block == Game.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= MasterHex.ARROW && i != cameFrom)
                {
                    findNormalMovesToTerrain(legion, player,
                            hex.getNeighbor(i), roll - 1, Game.ARROWS_ONLY,
                            (i + 3) % 6, terrainType, moves);
                }
            }
        }

        return;
    }


    public boolean flee(Legion legion, Legion enemy, Game game)
    {
        // TODO Make this smarter.
        char terrain = legion.getCurrentHex().getTerrain();

        if (getCombatValue(legion, terrain)
            < 0.7 * getCombatValue(enemy, terrain))
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    public boolean concede(Legion legion, Legion enemy, Game game)
    {
        // TODO Make this smarter.
        return false;
    }


    public void strike(Legion legion, Battle battle, Game game,
                       boolean fakeDice)
    {
        // Repeat until no attackers with valid targets remain.
        while (!battle.isOver() && battle.highlightCrittersWithTargets() > 0)
        {
            doOneStrike(legion, battle, fakeDice);
        }
    }


    private void doOneStrike(Legion legion, Battle battle, boolean fakeDice)
    {
        // Simple one-ply group strike algorithm.
        // First make forced strikes, including rangestrikes for
        // rangestrikers with only one target.
        battle.makeForcedStrikes(true);

        // Then create a map containing each target and the likely number
        // of hits it would take if all possible creatures attacked it.
        HashMap map = new HashMap();
        Collection critters = battle.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();

            if (critter.getLegion() == legion)
            {
                Set set = battle.findStrikes(critter, true);
                Iterator it2 = set.iterator();
                while (it2.hasNext())
                {
                    String hexLabel = (String)it2.next();
                    Critter target = battle.getCritterFromHexLabel(hexLabel);
                    int dice = critter.getDice(target);
                    int strikeNumber = critter.getStrikeNumber(target);
                    double h = Probs.meanHits(dice, strikeNumber);

                    if (map.containsKey(target))
                    {
                        double d = ((Double)map.get(target)).doubleValue();

                        h += d;
                    }

                    map.put(target, new Double(h));
                }
            }
        }

        // Pick the most important target that can likely be killed this
        // turn.  If none can, pick the most important target.
        boolean canKillSomething = false;
        Critter bestTarget = null;
        char terrain = battle.getTerrain();

        it = map.keySet().iterator();
        while (it.hasNext())
        {
            Critter target = (Critter)it.next();
            double h = ((Double)map.get(target)).doubleValue();

            if (h + target.getHits() >= target.getPower())
            {
                // We can probably kill this target.
                if (bestTarget == null ||!canKillSomething
                    || getKillValue(target, terrain)
                       > getKillValue(bestTarget, terrain))
                {
                    bestTarget = target;
                    canKillSomething = true;
                }
            }
            else
            {
                // We probably can't kill this target.
                if (bestTarget == null
                    || (!canKillSomething
                        && getKillValue(target, terrain)
                           > getKillValue(bestTarget, terrain)))
                {
                    bestTarget = target;
                }
            }
        }

        if (bestTarget == null)
        {
            debugln("no targets");

            return;
        }

        debugln("Best target is " + bestTarget.getDescription());

        // Having found the target, pick an attacker.  The
        // first priority is finding one that does not need
        // to worry about carry penalties to hit this target.
        // The second priority is using the weakest attacker,
        // so that more information is available when the
        // stronger attackers strike.
        Critter bestAttacker = null;

        it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();

            if (critter.canStrike(bestTarget))
            {
                if (critter.possibleStrikePenalty(bestTarget))
                {
                    if (bestAttacker == null ||
                        (bestAttacker.possibleStrikePenalty(bestTarget)
                            && getCombatValue(critter, terrain) <
                                getCombatValue(bestAttacker, terrain)))
                    {
                        bestAttacker = critter;
                    }
                }
                else
                {
                    if (bestAttacker == null ||
                        bestAttacker.possibleStrikePenalty(bestTarget) ||
                        getCombatValue(critter, terrain) <
                            getCombatValue(bestAttacker, terrain))
                    {
                        bestAttacker = critter;
                    }
                }
            }
        }

        debugln("Best attacker is " + bestAttacker.getDescription());
        // Having found the target and attacker, strike.
        // Take a carry penalty if there is still a 95%
        // chance of killing this target.
        bestAttacker.strike(bestTarget, fakeDice);

        // If there are any carries, apply them first to
        // the biggest creature that could be killed with
        // them, then to the biggest creature.
        while (battle.getCarryDamage() > 0)
        {
            bestTarget = null;

            Set set = battle.findCarryTargets();

            it = set.iterator();
            while (it.hasNext())
            {
                String hexLabel = (String)it.next();
                Critter target = battle.getCritterFromHexLabel(hexLabel);

                if (target.wouldDieFrom(battle.getCarryDamage()))
                {
                    if (bestTarget == null ||
                        !bestTarget.wouldDieFrom(battle.getCarryDamage()) ||
                        getKillValue(target, terrain)
                           > getKillValue(bestTarget, terrain))
                    {
                        bestTarget = target;
                    }
                }
                else
                {
                    if (bestTarget == null ||
                        (!bestTarget.wouldDieFrom(battle.getCarryDamage()) &&
                        getKillValue(target, terrain) >
                        getKillValue(bestTarget, terrain)))
                    {
                        bestTarget = target;
                    }
                }

                debugln("Best carry target is " + bestTarget.getDescription());
                battle.applyCarries(bestTarget);
            }
        }
    }


    public boolean chooseStrikePenalty(Critter critter, Critter target,
            Critter carryTarget, Battle battle, Game game)
    {
        // If we still have a 95% chance to kill target even after
        // taking the penalty to carry to carryTarget, return true.
        int dice = Math.min(critter.getDice(target),
                critter.getDice(carryTarget));
        int strikeNumber = Math.max(critter.getStrikeNumber(target),
                critter.getStrikeNumber(carryTarget));
        int hitsNeeded = target.getPower() - target.getHits();

        if (Probs.probHitsOrMore(dice, strikeNumber, hitsNeeded) >= 0.95)
        {
            return true;
        }
        else
        {
            return false;
        }
    }


    public static int getCombatValue(Creature creature, char terrain)
    {
        int val = creature.getPointValue();

        if (creature.isFlier())
        {
            val++;
        }

        if (creature.isRangestriker())
        {
            val++;
        }

        if (MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val++;
        }

        // Weak titans can't risk fighting effectively.
        if (creature.isTitan())
        {
            int power = creature.getPower();

            if (power < 9)
            {
                val -= (6 + 2 * (9 - power));
            }
        }

        return val;
    }


    public static int getCombatValue(Legion legion, char terrain)
    {
        int val = 0;
        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();

            val += getCombatValue(critter, terrain);
        }

        return val;
    }


    public static int getKillValue(Creature creature, char terrain)
    {
        int val = creature.getPointValue();
        if (creature.isFlier())
        {
            val++;
        }
        if (creature.isRangestriker())
        {
            val++;
        }
        if (MasterHex.isNativeCombatBonus(creature, terrain))
        {
            val++;
        }
        if (creature.isTitan())
        {
            val += 100;
        }
        return val;
    }


    ////////////////////////////////////////////////////////////////
    // minimax stuff
    ////////////////////////////////////////////////////////////////
    class MasterBoardPosition implements Minimax.GamePosition
    {
        // AICopy() of the game
        Game game;
        // the player for whom we're doing the evaluation.
        // note, this is NOT the same as Game.getActivePlayerNum()
        int activePlayerNum;
        HashMap[] enemyAttackMap;

        public MasterBoardPosition(Game game, int activePlayerNum)
        {
            this.game = game.AICopy();
            this.activePlayerNum = activePlayerNum;
            enemyAttackMap = buildEnemyAttackMap(game,
                game.getPlayer(activePlayerNum));
        }


        public MasterBoardPosition(MasterBoardPosition position)
        {
            this.game = position.game.AICopy();
            this.activePlayerNum = position.activePlayerNum;
            enemyAttackMap = buildEnemyAttackMap(game,
                game.getPlayer(activePlayerNum));
        }


        public int maximize()
        {
            if (activePlayerNum < 0)
            {
                return Minimax.AVERAGE;
            }
            else if (game.getActivePlayerNum() == activePlayerNum)
            {
                return Minimax.MAXIMIZE;
            }
            else
            {
                return Minimax.MINIMIZE;
            }
        }


        public int evaluation()
        {
            debugln("evaluating game position");

            // TODO: need to correct for the fact that more material
            // is not always better.
            // idea: score for legion markers available?
            final Player activePlayer =
                game.getPlayer(Math.abs(activePlayerNum));

            // check for loss
            if (activePlayer.isDead())
            {
                debugln("evaluation: loss! " + Integer.MIN_VALUE);

                return Integer.MIN_VALUE;
            }

            // check for victory
            {
                int playersRemaining = game.getNumPlayersRemaining();
                switch (playersRemaining)
                {
                    case 0:
                        debugln("evaluation: draw! " + 0);
                        return 0;

                    case 1:
                        debugln("evaluation: win! " + Integer.MAX_VALUE);
                        return Integer.MAX_VALUE;
                }
            }

            int value = 0;

            for (Iterator playerIt = game.getPlayers().iterator();
                 playerIt.hasNext(); )
            {
                Player player = (Player)playerIt.next();
                if (player == activePlayer)
                {
                    for (Iterator it = player.getLegions().iterator();
                         it.hasNext(); )
                    {
                        Legion legion = (Legion)it.next();
                        value += evaluateMove(game, legion,
                                legion.getCurrentHex(), legion.hasMoved(),
                                enemyAttackMap);
                    }
                    // TODO: add additional value for player having
                    // stacks near each other
                }
                else
                {
                    for (Iterator it = player.getLegions().iterator();
                         it.hasNext(); )
                    {
                        Legion legion = (Legion)it.next();
                        value += evaluateMove(game, legion,
                                legion.getCurrentHex(), legion.hasMoved(),
                                null);
                    }
                    // TODO: add additional value for player having
                    // his stacks near each other
                }
            }
            debugln("evaluation: " + value);
            return value;
        }


        public Iterator generateMoves()
        {
            debugln("generating moves..");

            // check for loss
            final Player activePlayer =
                game.getPlayer(Math.abs(activePlayerNum));

            if (activePlayer.isDead())                  // oops! we lost
            {
                return new ArrayList().iterator();      // no moves
            }

            // check for victory
            {
                int playersRemaining = game.getNumPlayersRemaining();
                if (playersRemaining < 2)               // draw or win
                {
                    return new ArrayList().iterator();  // no moves
                }
            }

            // dice moves
            if (activePlayerNum < 0)
            {
                // dice moves
                int playernum = game.getActivePlayerNum() * -1;
                ArrayList moves = new ArrayList(6);

                for (int i = 1; i <= 6; i++)
                {
                    moves.add(new DiceMove(i, this));
                }
                return moves.iterator();
            }

            // enumerate moves for player i
            debugln("hack! not considering all moves..");

            // HACK: consider all moves for first legion only.
            // (this is just for testing)
            ArrayList allmoves = new ArrayList();
            Legion legion = (Legion)game.getActivePlayer().getLegions().get(0);

            Iterator it = game.listMoves(legion, true).iterator();
            while (it.hasNext())
            {
                String hexLabel = (String)it.next();
                MasterHex hex = game.getBoard().getHexFromLabel(hexLabel);
                HashMap moves = new HashMap();
                moves.put(legion, hex);
                PlayerMove move = new PlayerMove(moves, this);
                allmoves.add(move);
            }
            debugln("considering " + allmoves.size() + " possible moves ");
            return allmoves.iterator();
        }


        public Minimax.Move generateNullMove()
        {
            return new PlayerMove(null, this);
        }


        public Minimax.GamePosition applyMove(Minimax.Move move)
        {
            debugln("applying moves..");

            if (move instanceof DiceMove)
            {
                debugln("applying dice move");

                // apply dice rolling
                DiceMove dicemove = (DiceMove)move;
                MasterBoardPosition position =
                    new MasterBoardPosition(dicemove.position);

                position.activePlayerNum = Math.abs(activePlayerNum);
                int roll = dicemove.roll;
                position.game.getActivePlayer().setMovementRoll(roll);
                return position;
            }
            else if (move instanceof PlayerMove)
            {
                debugln("applying player move");

                PlayerMove playermove = (PlayerMove)move;
                MasterBoardPosition position =
                    new MasterBoardPosition(playermove.position);

                // apply the PlayerMove moves
                for (Iterator it = playermove.moves.entrySet().iterator();
                     it.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry)it.next();
                    Legion legion = (Legion)entry.getKey();
                    MasterHex hex = (MasterHex)entry.getValue();

                    debugln("applymove: try " + legion + " to " + hex);
                    game.doMove(legion, hex);
                }

                // advance phases until we reach the next move phase
                game.advancePhase();

                while (game.getPhase() != Game.MOVE)
                {
                    switch (game.getPhase())
                    {
                        case Game.FIGHT:
                            // fake resolution for all fights
                            // TODO: need more accurate fight estimator
                            Player player = game.getActivePlayer();

                            for (int i = 0; i < player.getNumLegions(); i++)
                            {
                                Legion legion = player.getLegion(i);
                                MasterHex hex = legion.getCurrentHex();
                                Legion enemy = hex.getEnemyLegion(player);

                                if (enemy == null)
                                {
                                    continue;
                                }

                                Player enemyPlayer = enemy.getPlayer();
                                int myPV = legion.getPointValue();
                                int enemyPV = enemy.getPointValue();
                                boolean myTitan = legion.hasTitan();
                                boolean enemyTitan = enemy.hasTitan();

                                if (myPV * 0.8 > enemyPV)
                                {
                                    // i win
                                    enemy.remove();
                                    player.addPoints(enemyPV);

                                    if (enemyTitan)
                                    {
                                        enemyPlayer.die(player, false);
                                    }
                                }
                                else if (enemyPV * 0.8 > myPV)
                                {
                                    // enemy wins
                                    legion.remove();
                                    enemyPlayer.addPoints(myPV);

                                    if (myTitan)
                                    {
                                        player.die(enemyPlayer, false);
                                    }
                                }
                                else
                                {
                                    // both groups destroyed
                                    legion.remove();
                                    enemy.remove();
                                }
                            }
                            break;

                        case Game.SPLIT:
                            split(game);
                            break;

                        case Game.MUSTER:
                            muster(game);
                            break;
                    }

                    // now advance again until we get to MOVE phase
                    game.advancePhase();
                }

                // set activePlayer negative so that we average over dice rolls
                position.activePlayerNum = -1 * Math.abs(activePlayerNum);

                return position;
            }
            else
            {
                throw new RuntimeException("ack! bad move type");
            }
        }
    }

    class MasterBoardPositionMove implements Minimax.Move
    {
        protected MasterBoardPosition position;
        private int value;

        public void setValue(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    }

    class DiceMove extends MasterBoardPositionMove
    {
        int roll;

        public DiceMove(int roll, MasterBoardPosition position)
        {
            this.position = position;
            this.roll = roll;
        }
    }

    class PlayerMove extends MasterBoardPositionMove
    {
        HashMap moves;

        public PlayerMove(HashMap moves, MasterBoardPosition position)
        {
            this.position = position;
            this.moves = moves;
        }
    }




    ////////////////////////////////////////////////////////////////
    // battle minimax stuff
    ////////////////////////////////////////////////////////////////

    /*
     Battles are 2-player games within the multiplayer titan game.
     They must be evaluated within that context.  So not all
     winning positions are equally good, and not all losing
     positions are equally bad, since the surviving contents of
     the winning stack matter. All results that kill the last
     enemy titan are equally good, though, and all results that
     get our titan killed are equally bad.

     We can greatly simplify analysis by assuming that every strike
     will score the average number of hits.  This may or may not
     be good enough.  In particular, exposing a titan in a situation
     where a slightly above average number of hits will kill it is
     probably unwise.

     When finding all possible moves, we need to take into account
     that friendly creatures can block one another's moves.  Not
     every creature gets to move first.
     There are 27 hexes on each battle map.  A fast creature starting
     near the middle of the map can move to all of them, terrain and
     other creatures permitting.  So the possible number of different
     positions after one move is huge.

     */


    public void battleMove(Legion legion, Battle battle, Game game)
    {
        debugln("Called battleMove() for legion " + legion.getMarkerId());

        int legionNum;
        if (legion.getPlayer() == game.getActivePlayer())
        {
            legionNum = Battle.ATTACKER;
        }
        else
        {
            legionNum = Battle.DEFENDER;
        }
        BattlePosition bp = new BattlePosition(game, legionNum);
        LegionMove lm = (LegionMove)minimax.search(bp, 1);

        // Apply the LegionMove
        ArrayList moves = lm.getMoves();
        if (moves != null)
        {
            Iterator it = moves.iterator();
            while (it.hasNext())
            {
                CritterMove cm = (CritterMove)it.next();
                Critter critter = cm.getCritter(battle);
                BattleHex hex = cm.getEndingHex(battle.getBattleMap());
                battle.doMove(critter, hex);
            }
        }
        else
        {
            debugln("No moves were generated.");
        }

        debugln("Done with battleMove for legion " + legion.getMarkerId());
    }


    private static int evaluateCritterMove(Battle battle, Critter critter,
        boolean considerBuddies)
    {
        char terrain = battle.getTerrain();
        int value = getCombatValue(critter, terrain);

        // Subtract for wounds.
        value -= critter.getPower() * critter.getHits() / 2;

        BattleHex hex = critter.getCurrentHex();

        // Add for sitting in favorable terrain.
        // Subtract for sitting in unfavorable terrain.
        if (hex.isEntrance())
        {
            // Staying offboard to die is really bad.
            value -= getCombatValue(critter, terrain);
        }
        else if (MasterHex.isNativeCombatBonus(critter, terrain))
        {
            if (hex.isNativeBonusTerrain())
            {
                value += 3;
            }
        }
        else
        {
            if (hex.isNonNativePenaltyTerrain())
            {
                value -= 3;
            }
        }

        // Reward the ability to rangestrike.
        // TODO Range 3 is better than range 4 for non-warlocks.
        if (critter.isRangestriker() &&!critter.isInContact(true)
            && battle.findStrikes(critter, true).size() >= 1)
        {
            value += 4;
        }

        // Reward being adjacent to an enemy if attacking.
        // Penalize being adjacent to an enemy if defending.
        if (critter.isInContact(false))
        {
            if (critter.getLegion() == battle.getAttacker())
            {
                value += 1;
            }
            else
            {
                value -= 1;
            }
        }

        // Reward adjacent friendly creatures.
        int buddies = 0;
        if (considerBuddies)
        {
            buddies = critter.numAdjacentAllies();
            value += buddies;
        }

        // Reward titans sticking to the edges of the back row
        // surrounded by allies.
        if (critter.isTitan())
        {
            value += 5 * buddies;

            if (terrain == 'T')
            {
                // Stick to the center of the tower.
                value += 50 * hex.getElevation();
            }
            else
            {
                BattleHex entrance =
                    battle.getBattleMap().getEntrance(critter.getLegion());
                for (int i = 0; i < 6; i++)
                {
                    if (entrance.getNeighbor(i) == hex)
                    {
                        value += 20;
                    }
                    BattleHex neighbor = hex.getNeighbor(i);
                    if (neighbor == null || neighbor.getTerrain() == 't')
                    {
                        value += 10;
                    }
                }
            }
        }

        // Hack.  We want to encourage defending critters to hang back,
        // and attacking critters to move forward.

        return value;
    }


    class BattlePosition implements Minimax.GamePosition
    {
        private Game game;
        private int activeLegionNum;
        private Battle battle;

        public BattlePosition(Game game, int activeLegionNum)
        {
            this.game = game.AICopy();
            this.activeLegionNum = activeLegionNum;
            battle = game.getBattle();

            this.game.setOption(Options.showDice, false);
        }

        public BattlePosition(BattlePosition position)
        {
            this(position.game, position.activeLegionNum);
        }

        public int maximize()
        {
            if (battle.getActiveLegionNum() == activeLegionNum)
            {
                return Minimax.MAXIMIZE;
            }
            else
            {
                return Minimax.MINIMIZE;
            }
        }

        public int evaluation()
        {
            debugln("evaluating battle position");

            int value = winLoseOrDraw();
            if (value == BATTLE_NOT_OVER)
            {
                Legion legion = battle.getLegion(activeLegionNum);

                // Iterate over both players' critters in battle.
                Iterator it = battle.getCritters().iterator();
                while (it.hasNext())
                {
                    Critter critter = (Critter)it.next();
                    if (critter.getLegion() == legion)
                    {
                        value += evaluateCritterMove(battle, critter, true);
                    }
                    else
                    {
                        value -= evaluateCritterMove(battle, critter, true);
                    }
                }

                debugln("evaluation: " + value);
            }
            return value;
        }


        public static final int GAME_WIN = Integer.MAX_VALUE;
        public static final int GAME_LOSS = Integer.MIN_VALUE;
        public static final int GAME_DRAW = -2;
        public static final int BATTLE_WIN = Integer.MAX_VALUE / 2;
        public static final int BATTLE_LOSS = Integer.MIN_VALUE / 2;
        public static final int BATTLE_DRAW = -1;
        public static final int BATTLE_NOT_OVER = 0;

        int winLoseOrDraw()
        {
            int value = BATTLE_NOT_OVER;
            int numPlayersRemaining = game.getNumPlayersRemaining();

            if (numPlayersRemaining == 0)
            {
                value = GAME_DRAW;

                debugln("evaluation: game draw! " + value);
            }
            else if (numPlayersRemaining == 1)
            {
                Player player = battle.getActiveLegion().getPlayer();

                if (player.isDead())
                {
                    value = GAME_LOSS;
                    debugln("evaluation: game loss! " + value);
                }
                else
                {
                    value = GAME_WIN;
                    debugln("evaluation: game win! " + value);
                }
            }
            else
            {
                // Check for battle win, loss, or draw
                if (battle.isAttackerElim() && battle.isDefenderElim())
                {
                    value = BATTLE_DRAW;
                    debugln("evaluation: battle draw! " + value);
                }
                else if (battle.isAttackerElim())
                {
                    if (activeLegionNum == battle.ATTACKER)
                    {
                        value = BATTLE_LOSS;
                        debugln("evaluation: battle loss! " + value);
                    }
                    else
                    {
                        value = BATTLE_WIN;
                        debugln("evaluation: battle win! " + value);
                    }
                }
                else if (battle.isDefenderElim())
                {
                    if (activeLegionNum == battle.DEFENDER)
                    {
                        value = BATTLE_LOSS;
                        debugln("evaluation: battle loss! " + value);
                    }
                    else
                    {
                        value = BATTLE_WIN;
                        debugln("evaluation: battle win! " + value);
                    }
                }
            }
            return value;
        }


        /** Limit the number of moves considered for each legion, to
         *  prevent combinatorial explosion. */
        final int MAX_LEGION_MOVES = 100000;


        /** Generates LegionMoves. */
        public Iterator generateMoves()
        {
            debugln("generating battle moves..");

            if (winLoseOrDraw() != BATTLE_NOT_OVER)
            {
                debugln("battle is over -- no moves generated");
                return new ArrayList().iterator();      // no moves
            }

            final BattleMap map = battle.getBattleMap();
            final char terrain = battle.getTerrain();

            // A "creature class" is a creature name and a starting hex
            // label.  We use creature classes instead of critters so
            // that multiple identical creatures in the same entrance
            // hex can be considered together, and to avoid AICopy()
            // issues.

            // allCritterMoves is a HashMap of creature classes to moveLists.
            HashMap allCritterMoves = new HashMap();

            // There are just too many possible combinations of creature
            // moves to always iterate through all of them.  (There are
            // 27 hexes on the battle map.  A fast creature near the
            // center of an open board can reach them all.  Given
            // a legion of 7 such creatures facing only one enemy,
            // each creature has 20 possible moves.  That means
            // 20 ** 7 = 1.28 billion legion moves.)

            // So find all moves for each critter, and try to score them
            // in isolation without considering the moves of other
            // critters in the legion.  Find all possible legion moves
            // using just the N best moves for each legion.

            ArrayList allCritters = (ArrayList)battle.getCritters();
            // Deep copy only the critters from the active legion.
            ArrayList critters = new ArrayList();
            Iterator it = allCritters.iterator();
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                if (critter.getLegion() == battle.getActiveLegion())
                {
                    critters.add(critter);
                }
            }

            // Sort critters in decreasing order of importance.  Keep
            // identical creatures together with a secondary sort by
            // creature name.
            Collections.sort(critters, new Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    Critter critter1 = (Critter)o1;
                    Critter critter2 = (Critter)o2;
                    int diff = getKillValue(critter2, terrain) -
                        getKillValue(critter1, terrain);
                    if (diff != 0)
                    {
                        return diff;
                    }
                    else
                    {
                        return critter1.getName().compareTo(
                            critter2.getName());
                    }
                }
            });

            // There are about numCritters ** mean moves per critter
            // possible legion moves.  So retrict the number of moves
            // per critter. 
            int maxCritterMoves = (int)Math.floor(Math.pow(MAX_LEGION_MOVES,
                1.0 / critters.size()));

            it = critters.iterator();
            String lastCritterName = "";
            while (it.hasNext())
            {
                Critter critter = (Critter)it.next();
                String critterName = critter.getName();
                String currentHexLabel = critter.getCurrentHexLabel();

                // Skip this critter if we just figured out the moves
                // for another entering creature of the same type.
                if (currentHexLabel.startsWith("entrance") && 
                    critterName.equals(lastCritterName))
                {
                    continue;
                }
                lastCritterName = critterName;

                // moves is a list of hex labels where one critter
                // can move.
                Set moves = battle.showMoves(critter);

                // Not moving is also an option, unless the
                // critter is offboard.
                // XXX Test the situation where some critters are 
                // forced to stay offboard, e.g. 7 trolls defending in 
                // the desert.
                if (!currentHexLabel.startsWith("entrance"))
                {
                    moves.add(currentHexLabel);
                }

                StringBuffer buf = new StringBuffer("Found " + moves.size() +
                    " moves for " + critter.getDescription() + ":");

                // moveList is a list of CritterMoves for one creature class.
                ArrayList moveList = new ArrayList();

                Iterator it2 = moves.iterator();
                while (it2.hasNext())
                {
                    String hexLabel = (String)it2.next();
                    buf.append(" " + hexLabel);

                    BattleHex hex = map.getHexFromLabel(hexLabel);
                    CritterMove cm = new CritterMove(critterName,
                       currentHexLabel, hexLabel);

                    // XXX Need to actually move critter to hex to evaluate.
                    // We should write some static battle methods to fix.
                    critter.moveToHex(hex);

                    // Compute and save the value for each CritterMove.
                    cm.setValue(evaluateCritterMove(battle, critter, false));
                    moveList.add(cm);

                    // XXX Need to put the critter back where it started.
                    critter.moveToHex(critter.getStartingHex());
                }
                debugln(buf.toString());

                // Sort critter moves in descending order of score.
                Collections.sort(moveList, new Comparator()
                {
                    public int compare(Object o1, Object o2)
                    {
                        CritterMove cm1 = (CritterMove)o1;
                        CritterMove cm2 = (CritterMove)o2;
                        return cm2.getValue() - cm1.getValue();
                    }
                });

                int numMoves = Math.min(maxCritterMoves, moveList.size());
                debugln("Considering best " + numMoves + " moves for " + 
                    critter.getDescription());
                List bestMoves = new ArrayList();
                // XXX Isn't there a public util method for this?
                for (int i = 0; i < numMoves; i++)
                {
                    CritterMove cm = (CritterMove)moveList.get(i);
                    bestMoves.add(cm);
                }
                allCritterMoves.put(critterName + currentHexLabel, bestMoves);
            }

            // Now we need to create legion moves from critter moves.
            // Each legion move contains one critter move (or non-move)
            // for each critter.

            HashSet allLegionMoves = new HashSet();
            ArrayList critterMoves = new ArrayList();

            return generateLegionMoves(critters, allCritterMoves);
        }


        /** Generate all possible LegionMoves for the given set of
         *  CritterMoves. allCritterMoves maps "creature classes" to
         *  ArrayLists of CritterMoves. */
        private Iterator generateLegionMoves(ArrayList critters, HashMap
            allCritterMoves)
        {
            debugln("Called generateLegionMoves");

            int [] numMoves = new int[7];
            Arrays.fill(numMoves, 0);

            int numCritters = critters.size();
            ArrayList [] moveList = new ArrayList[7];

            int j;
            for (j = 0; j < numCritters; j++)
            {
                Critter critter = (Critter)critters.get(j);
                moveList[j] = (ArrayList)allCritterMoves.get(
                    critter.getName() + critter.getStartingHexLabel());
                numMoves[j] = moveList[j].size();
            }

            int [] i = new int[7];
            ArrayList allLegionMoves = new ArrayList();
            ArrayList critterMoves = new ArrayList();
            HashSet hexesTaken = new HashSet();
            String hexLabel;

            i[6] = 0;
            do
            {
                i[5] = 0;
                do
                {
                    i[4] = 0;
                    do
                    {
                        i[3] = 0;
                        do
                        {
                            i[2] = 0;
                            do
                            {
                                i[1] = 0;
                                do
                                {
                                    i[0] = 0;
                                    do
                                    {
                                        boolean broken = false;
                                        critterMoves.clear();
                                        hexesTaken.clear();

                                        for (j = 0; j < numCritters; j++)
                                        {
                                            CritterMove cm = (CritterMove)
                                                moveList[j].get(i[j]);
                                            hexLabel = cm.getEndingHexLabel();
                                            if (!hexesTaken.add(hexLabel))
                                            {
                                                // This hex is already taken.
                                                // This LegionMove is bogus.
                                                broken = true;
                                                break;
                                            }

                                            critterMoves.add(cm);
                                        }

                                        if (!broken)
                                        {
                                            LegionMove lm = new LegionMove(
                                                critterMoves, this);
                                            allLegionMoves.add(lm);
                                        }
                                    }
                                    while (++i[0] < numMoves[0]);
                                }
                                while (++i[1] < numMoves[1]);
                            }
                            while (++i[2] < numMoves[2]);
                        }
                        while (++i[3] < numMoves[3]);
                    }
                    while (++i[4] < numMoves[4]);
                }
                while (++i[5] < numMoves[5]);
            }
            while (++i[6] < numMoves[6]);

            debugln("generated " + allLegionMoves.size() + " legion moves");

            // TODO Prune legion moves that are impossible because
            // critters get in each other's way.
            // TODO For legion moves that are possible but difficult to
            // make because critters can block each other's moves if
            // moved in the wrong order, find the optimal order in
            // which to move critters.  (Those moving far before those
            // staying near, then natives before non-natives, with fliers
            // moving last.)

            return allLegionMoves.iterator();
        }


        public Minimax.Move generateNullMove()
        {
            return new LegionMove(null, this);
        }

        // XXX Only two creatures ever seem to get moved.
        public Minimax.GamePosition applyMove(Minimax.Move move)
        {
            debugln("applying legion battle move");

            LegionMove LegionMove = (LegionMove)move;
            BattlePosition position =
                new BattlePosition(LegionMove.position);
            BattleMap map = position.battle.getBattleMap();

            // apply the CritterMoves
            Iterator it = LegionMove.moves.iterator();
            while (it.hasNext())
            {
                // XXX Does CritterMove need to hold strings instead of
                // critters and hexes, so that they refer to the new
                // position rather than the real one?
                CritterMove cm = (CritterMove)it.next();
                Critter critter = cm.getCritter(position.battle);
                BattleHex hex = cm.getEndingHex(map);

                debugln("applymove: try " + critter + " to " + hex.getLabel());
                position.battle.doMove(critter, hex);
            }

// XXX Do we really want to advance phases?  We're basically limited to
// one ply by combinatorics.
/*
            // advance phases until we reach the next move phase
            do
            {
                battle.advancePhase();

                switch (game.getPhase())
                {
                    case Battle.SUMMON:
                        // TODO Summon an angel if possible.
                        break;

                    case Battle.RECRUIT:
                        // TODO Recruit a reinforcement if possible.
                        break;

                    case Battle.FIGHT:
                    case Battle.STRIKEBACK:
                        Legion legion = position.battle.getActiveLegion();
                        Player player = legion.getPlayer();
                        player.aiStrike(legion, position.battle, true, true);
                        break;
                }
            }
            while (!position.battle.isOver() || position.battle.getPhase()
                != Battle.MOVE);
*/

            position.activeLegionNum = activeLegionNum;
            return position;
        }
    }


    class LegionMove implements Minimax.Move
    {
        private int value;
        private BattlePosition position;
        private ArrayList moves;  // List of CritterMoves

        public LegionMove(ArrayList moves, BattlePosition position)
        {
            this.position = position;
            this.moves = moves;
        }

        public void setValue(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        public ArrayList getMoves()
        {
            if (moves != null)
            {
                return moves;
            }
            else
            {
                return null;
            }
        }
    }

    class CritterMove implements Minimax.Move
    {
        private int value;
        private String creatureName;
        private String startingHexLabel;
        private String endingHexLabel;

        public CritterMove(String creatureName, String startingHexLabel,
            String endingHexLabel)
        {
            super();
            this.creatureName = creatureName;
            this.startingHexLabel = startingHexLabel;
            this.endingHexLabel = endingHexLabel;
        }

        public void setValue(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        public String getCreatureName()
        {
            return creatureName;
        }

        // Assumes the move has not yet been made.
        public Critter getCritter(Battle battle)
        {
            return battle.getCritterFromHexLabel(startingHexLabel);
        }

        public String getStartingHexLabel()
        {
            return startingHexLabel;
        }

        public String getEndingHexLabel()
        {
            return endingHexLabel;
        }

        public BattleHex getStartingHex(BattleMap map)
        {
            return map.getHexFromLabel(startingHexLabel);
        }

        public BattleHex getEndingHex(BattleMap map)
        {
            return map.getHexFromLabel(endingHexLabel);
        }
    }
}
