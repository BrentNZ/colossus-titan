package net.sf.colossus.server;


import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Probs;
import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.HexMap;


/**
 * Class Critter represents an individual Titan Character.
 * @version $Id$
 * @author David Ripton
 */

final class Critter extends Creature implements Comparable
{
    private boolean visible;
    private Creature creature;
    private String markerId;
    private Battle battle;
    private boolean struck;
    private String currentHexLabel;
    private String startingHexLabel;
    /** Damage taken */
    private int hits;
    private Game game;
    /** Unique identifier for each critter. */
    private int tag;
    /** Counter used to assign unique tags. */
    private static int tagCounter = -1;


    Critter(Creature creature, boolean visible, String markerId, Game game)
    {
        super(creature);

        this.creature = creature;
        this.visible = visible;
        this.markerId = markerId;
        this.game = game;
        tag = ++tagCounter;
    }


    /** Deep copy for AI. */
    Critter AICopy(Game game)
    {
        Critter newCritter = new Critter(creature, visible, markerId, game);

        newCritter.battle = battle;
        newCritter.struck = struck;
        newCritter.currentHexLabel = currentHexLabel;
        newCritter.startingHexLabel = startingHexLabel;
        newCritter.hits = hits;
        newCritter.tag = tag;

        return newCritter;
    }


    void addBattleInfo(String currentHexLabel, String startingHexLabel,
        Battle battle)
    {
        this.currentHexLabel = currentHexLabel;
        this.startingHexLabel = startingHexLabel;
        this.battle = battle;
    }

    void setGame(Game game)
    {
        this.game = game;
    }

    boolean isVisible()
    {
        return visible;
    }

    void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    Creature getCreature()
    {
        return creature;
    }

    String getMarkerId()
    {
        return markerId;
    }

    void setMarkerId(String markerId)
    {
        this.markerId = markerId;
    }

    Legion getLegion()
    {
        return game.getLegionByMarkerId(markerId);
    }

    Player getPlayer()
    {
        return game.getPlayerByMarkerId(markerId);
    }

    String getPlayerName()
    {
        return game.getPlayerByMarkerId(markerId).getName();
    }

    int getTag()
    {
        return tag;
    }

    /** Return only the base part of the image name for this critter. */
    public String getImageName(boolean inverted)
    {
        String basename = super.getImageName(inverted);

        if (isTitan() && getPower() >= 6 && getPower() <= 20)
        {
            // Use Titan14.gif for a 14-power titan, etc.  Use the generic
            // Titan.gif (with X-4) for ridiculously big titans, to avoid
            // the need for an infinite number of images.
            basename = basename + getPower();
        }

        return basename;
    }


    String getDescription()
    {
        return getName() + " in " + getCurrentHex().getDescription();
    }


    public int getPower()
    {
        if (isTitan())
        {
            Player player = getPlayer();
            if (player != null)
            {
                return player.getTitanPower();
            }
            else
            {
                // Just in case player is dead.
                return 6;
            }
        }
        return super.getPower();
    }


    int getHits()
    {
        return hits;
    }


    void setHits(int hits)
    {
        this.hits = hits;
        battle.getGame().getServer().allSetBattleChitHits(tag, hits);
    }


    void heal()
    {
        hits = 0;
    }


    boolean wouldDieFrom(int hits)
    {
        return (hits + getHits() > getPower());
    }


    /** Apply damage to this critter.  Return the amount of excess damage
     *  done, which may sometimes carry to another target. */
    int wound(int damage)
    {
        int excess = 0;

        if (damage > 0)
        {
            setHits(hits + damage);
            if (hits > getPower())
            {
                excess = hits - getPower();
                hits = getPower();
            }

            // Check for death.
            if (hits >= getPower())
            {
                setDead(true);
            }

            battle.getGame().getServer().allRepaintBattleHex(currentHexLabel);
        }

        return excess;
    }


    boolean hasMoved()
    {
        return (!currentHexLabel.equals(startingHexLabel));
    }


    void commitMove()
    {
        startingHexLabel = currentHexLabel;
    }


    boolean hasStruck()
    {
        return struck;
    }


    void setStruck(boolean struck)
    {
        this.struck = struck;
    }


    BattleHex getCurrentHex()
    {
        return HexMap.getHexByLabel(battle.getTerrain(), currentHexLabel);
    }

    BattleHex getStartingHex()
    {
        return HexMap.getHexByLabel(battle.getTerrain(), startingHexLabel);
    }

    String getCurrentHexLabel()
    {
        return currentHexLabel;
    }

    void setCurrentHexLabel(String label)
    {
        this.currentHexLabel = label;
    }

    String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    void setStartingHexLabel(String label)
    {
        this.startingHexLabel = label;
    }


    /** Return the number of enemy creatures in contact with this critter.
     *  Dead critters count as being in contact only if countDead is true. */
    int numInContact(boolean countDead)
    {
        BattleHex hex = getCurrentHex();

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (hex.getHexside(i) != 'c' && hex.getOppositeHexside(i) != 'c')
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    Critter other = battle.getCritter(neighbor);
                    if (other != null && other.getPlayer() != getPlayer() &&
                        (countDead || !other.isDead()))
                    {
                        count++;
                    }
                }
            }
        }

        return count;
    }


    /** Return the number of friendly creatures adjacent to this critter.
     *  Dead critters do not count. */
    int numAdjacentAllies()
    {
        BattleHex hex = getCurrentHex();
        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            BattleHex neighbor = hex.getNeighbor(i);
            if (neighbor != null)
            {
                Critter other = battle.getCritter(neighbor);
                if (other != null && other.getPlayer() == getPlayer() &&
                    !other.isDead())
                {
                    count++;
                }
            }
        }
        return count;
    }


    /** Return true if there are any enemies adjacent to this critter.
     *  Dead critters count as being in contact only if countDead is true. */
    boolean isInContact(boolean countDead)
    {
        BattleHex hex = getCurrentHex();

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return false;
        }

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (hex.getHexside(i) != 'c' && hex.getOppositeHexside(i) != 'c')
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    Critter other = battle.getCritter(neighbor);
                    if (other != null && !other.getPlayer().getName().equals(
                        getPlayer().getName()) &&
                        (countDead || !other.isDead()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /** Most code should use Battle.doMove() instead, since it checks
     *  for legality and logs the move. */
    void moveToHex(String hexLabel)
    {
        currentHexLabel = hexLabel;
        game.getServer().pushUndoStack(getPlayerName(), currentHexLabel);
        Set set = new HashSet();
        set.add(startingHexLabel);
        set.add(currentHexLabel);
        battle.getGame().getServer().allAlignBattleChits(set);
    }


    void undoMove()
    {
        String formerHexLabel = currentHexLabel;
        currentHexLabel = startingHexLabel;
        Log.event(getName() + " undoes move and returns to " +
            startingHexLabel);
        Set set = new HashSet();
        set.add(formerHexLabel);
        set.add(currentHexLabel);
        battle.getGame().getServer().allAlignBattleChits(set);
    }


    boolean canStrike(Critter target)
    {
        String hexLabel = target.getCurrentHexLabel();
        return battle.findStrikes(this, true).contains(hexLabel);
    }


    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain. */
    int getDice(Critter target)
    {
        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int dice = getPower();

        boolean rangestrike = !isInContact(true);
        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice /= 2;

            // Dragon rangestriking from volcano: +2
            if (getName().equals("Dragon") && hex.getTerrain() == 'v')
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // Dragon striking from volcano: +2
            if (getName().equals("Dragon") && hex.getTerrain() == 'v')
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = Battle.getDirection(hex, targetHex, false);
            char hexside = hex.getHexside(direction);

            // Native striking down a dune hexside: +2
            if (hexside == 'd' && isNativeSandDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hexside == 's' && isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (!isNativeSandDune() &&
                hex.getOppositeHexside(direction) == 'd')
            {
                dice--;
            }
        }

        return dice;
    }


    private int getAttackerSkill(Critter target)
    {
        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = getSkill();

        boolean rangestrike = !isInContact(true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // Non-native striking out of bramble: -1
            if (hex.getTerrain() == 'r' && !isNativeBramble())
            {
                attackerSkill--;
            }

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = Battle.getDirection(hex, targetHex, false);
                char hexside = hex.getHexside(direction);
                // Striking down across wall: +1
                if (hexside == 'w')
                {
                    attackerSkill++;
                }
            }
            else if (hex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = Battle.getDirection(targetHex, hex, false);
                char hexside = targetHex.getHexside(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hexside == 's' && !isNativeSlope()) || hexside == 'w')
                {
                    attackerSkill--;
                }
            }
        }
        else if (!getName().equals("Warlock"))
        {
            // Range penalty
            if (battle.getRange(hex, targetHex, false) == 4)
            {
                attackerSkill--;
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!isNativeBramble())
            {
                attackerSkill -= battle.countBrambleHexes(hex, targetHex);
            }

            // Rangestrike up across wall: -1 per wall
            if (targetHex.hasWall())
            {
                int heightDeficit = targetHex.getElevation() -
                    hex.getElevation();
                if (heightDeficit > 0)
                {
                    // Because of the design of the tower map, a strike to
                    // a higher tower hex always crosses one wall per
                    // elevation difference.
                    attackerSkill -= heightDeficit;
                }
            }

            // Rangestrike into volcano: -1
            if (targetHex.getTerrain() == 'v')
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }


    int getStrikeNumber(Critter target)
    {
        boolean rangestrike = !isInContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-warlock: +1
        if (target.getCurrentHex().getTerrain() == 'r' &&
            target.isNativeBramble() &&
            !isNativeBramble() &&
            !(rangestrike && getName().equals("Warlock")))
        {
            strikeNumber++;
        }

        // Sixes always hit.
        if (strikeNumber > 6)
        {
            strikeNumber = 6;
        }

        return strikeNumber;
    }


    /** Allow the player to choose whether to take a penalty
     *  (fewer dice or higher strike number) in order to be
     *  allowed to carry.  Return true if the penalty is taken,
     *  or false if it is not. */
    private boolean chooseStrikePenalty(Critter target, Collection
        carryTargets)
    {
        StringBuffer prompt = new StringBuffer(
            "Take strike penalty to allow carrying to ");

        Critter carryTarget = null;
        Iterator it = carryTargets.iterator();
        while (it.hasNext())
        {
            carryTarget = (Critter)it.next();
            BattleHex targetHex = carryTarget.getCurrentHex();
            prompt.append(carryTarget.getName() + " in " +
                targetHex.getDescription());
            if (it.hasNext())
            {
                prompt.append(", ");
            }
        }
        prompt.append("?");

        if (game.getServer().getClientOption(getPlayer().getName(),
            Options.autoStrike))
        {
            return getPlayer().aiChooseStrikePenalty(this, target,
                carryTarget, battle);
        }
        else
        {
            return battle.getGame().getServer().chooseStrikePenalty(
                getPlayer().getName(), prompt.toString());
        }
    }


    /** Return true if there's any chance that this critter could take
     *  a strike penalty to carry when striking target. */
    boolean possibleStrikePenalty(Critter target)
    {
        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();
        if (numInContact(false) < 2)
        {
            return false;
        }

        int dice = getDice(target);

        // Carries are only possible if the striker is rolling more dice than
        // the target has hits remaining.
        if (dice <= target.getPower() - target.getHits())
        {
            return false;
        }

        int strikeNumber = getStrikeNumber(target);

        // Figure whether number of dice or strike number needs to be
        // penalized in order to carry.
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (hex.getHexside(i) != 'c' && hex.getOppositeHexside(i) != 'c')
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null && neighbor != targetHex)
                {
                    Critter critter = battle.getCritter(neighbor);
                    if (critter != null && critter.getPlayer() !=
                        getPlayer() && !critter.isDead())
                    {
                        int tmpDice = getDice(critter);
                        int tmpStrikeNumber = getStrikeNumber(critter);

                        // Strikes not up across dune hexsides cannot
                        // carry up across dune hexsides.
                        if (hex.getOppositeHexside(i) == 'd' &&
                            targetHex.getHexside(Battle.getDirection(
                            targetHex, hex, false)) != 'd')
                        {
                        }
                        else if (tmpStrikeNumber > strikeNumber ||
                            tmpDice < dice)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    /** Calculate number of dice and strike number needed to hit target,
     *  and whether any carries are possible.  Roll the dice and apply
     *  damage.  Highlight legal carry targets. */
    void strike(Critter target, boolean rollFakeDice)
    {
        // Sanity check
        if (target.getPlayer() == getPlayer())
        {
            Log.error(getName() + " tried to strike allied " +
                target.getName());
            return;
        }

        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();
        // Bug 488753 says AI creatures occasionally struck from
        // offboard.  So let's add an extra check.
        if (hex.isEntrance() || targetHex.isEntrance())
        {
            Log.error("Attempted offboard strike disallowed!");
            return;
        }

        battle.leaveCarryMode();
        boolean carryPossible = true;
        if (numInContact(false) < 2)
        {
            carryPossible = false;
        }

        int dice = getDice(target);

        // Carries are only possible if the striker is rolling more dice than
        // the target has hits remaining.
        if (carryPossible && (dice <= target.getPower() - target.getHits()))
        {
            carryPossible = false;
        }

        int strikeNumber = getStrikeNumber(target);

        // Figure whether number of dice or strike number needs to be
        // penalized in order to carry.
        if (carryPossible)
        {
            boolean haveCarryTarget = false;
            ArrayList penaltyOptions = new ArrayList();

            for (int i = 0; i < 6; i++)
            {
                // Adjacent creatures separated by a cliff are not in contact.
                if (hex.getHexside(i) != 'c' &&
                    hex.getOppositeHexside(i) != 'c')
                {
                    BattleHex neighbor = hex.getNeighbor(i);
                    if (neighbor != null && neighbor != targetHex)
                    {
                        Critter critter = battle.getCritter(neighbor);
                        if (critter != null && critter.getPlayer() !=
                            getPlayer() && !critter.isDead())
                        {
                            int tmpDice = getDice(critter);
                            int tmpStrikeNumber = getStrikeNumber(critter);

                            // Strikes not up across dune hexsides cannot
                            // carry up across dune hexsides.
                            if (hex.getOppositeHexside(i) == 'd' &&
                                targetHex.getHexside(Battle.getDirection(
                                targetHex, hex, false)) != 'd')
                            {
                                battle.removeCarryTarget(targetHex.getLabel());
                            }

                            else if (tmpStrikeNumber > strikeNumber ||
                                tmpDice < dice)
                            {
                                // Add this scenario to the list.
                                penaltyOptions.add(new PenaltyOption(critter,
                                    tmpDice, tmpStrikeNumber));
                            }

                            else
                            {
                                battle.addCarryTarget(neighbor.getLabel());
                                haveCarryTarget = true;
                            }
                        }
                    }
                }
            }

            // Sort penalty options by number of dice (ascending), then by
            //    strike number (descending).
            Collections.sort(penaltyOptions);

            // Find the group of PenaltyOptions with identical dice and
            //    strike numbers.
            PenaltyOption option;
            ArrayList critters = new ArrayList();
            ListIterator lit = penaltyOptions.listIterator();
            while (lit.hasNext())
            {
                option = (PenaltyOption)lit.next();
                int tmpDice = option.getDice();
                int tmpStrikeNumber = option.getStrikeNumber();
                critters.clear();
                critters.add(option.getCritter());

                while (lit.hasNext())
                {
                    option = (PenaltyOption)lit.next();
                    if (option.getDice() == tmpDice &&
                        option.getStrikeNumber() == tmpStrikeNumber)
                    {
                        critters.add(option.getCritter());
                    }
                    else
                    {
                        lit.previous();
                        break;
                    }
                }

                // Make sure the penalty is still relevant.
                if (tmpStrikeNumber > strikeNumber || tmpDice < dice)
                {
                    if (chooseStrikePenalty(target, critters))
                    {
                        if (tmpStrikeNumber > strikeNumber)
                        {
                            strikeNumber = tmpStrikeNumber;
                        }
                        if (tmpDice < dice)
                        {
                            dice = tmpDice;
                        }
                        Iterator it2 = critters.iterator();
                        while (it2.hasNext())
                        {
                            Critter critter = (Critter)it2.next();
                            battle.addCarryTarget(
                                critter.getCurrentHexLabel());
                            haveCarryTarget = true;
                        }
                    }
                }
            }

            if (!haveCarryTarget)
            {
                carryPossible = false;
            }
        }

        // Roll the dice.
        int damage = 0;

        int [] rolls = new int[dice];
        StringBuffer rollString = new StringBuffer(36);

        if (rollFakeDice)
        {
            for (int i = 0; i < dice; i++)
            {
                rolls[i] = Probs.rollFakeDie();
                rollString.append(rolls[i]);

                if (rolls[i] >= strikeNumber)
                {
                    damage++;
                }
            }
        }
        else
        {
            for (int i = 0; i < dice; i++)
            {
                rolls[i] = Game.rollDie();
                rollString.append(rolls[i]);

                if (rolls[i] >= strikeNumber)
                {
                    damage++;
                }
            }
        }

        Log.event(getName() + " in " + currentHexLabel +
            " strikes " + target.getName() + " in " +
            targetHex.getLabel() + " with strike number " +
            strikeNumber + " : " + rollString + ": " + damage +
            (damage == 1 ? " hit" : " hits"));

        int carryDamage = target.wound(damage);
        if (!carryPossible)
        {
            carryDamage = 0;
        }

        // Let the attacker choose whether to carry, if applicable.
        if (carryDamage > 0)
        {
            Log.event(carryDamage + (carryDamage == 1 ?
                " carry available" : " carries available"));
            battle.setCarryDamage(carryDamage);
            battle.getGame().getServer().highlightCarries(getPlayerName());
        }

        // Record that this attacker has struck.
        struck = true;

        // Display the rolls in the BattleDice dialog.
        if (game != null)
        {
            game.getServer().allSetBattleDiceValues(getName(),
                target.getName(), currentHexLabel, target.getCurrentHexLabel(),
                battle.getTerrain(), strikeNumber, damage, carryDamage, rolls);
        }
    }


    boolean isDead()
    {
        return (getHits() >= getPower());
    }

    void setDead(boolean dead)
    {
        if (dead)
        {
            hits = getPower();
            battle.getGame().getServer().allSetBattleChitDead(tag);
        }
    }

    /** Inconsistent with equals() */
    public int compareTo(Object object)
    {
        Critter critter = (Critter)object;

        if (isTitan())
        {
            return -1;
        }
        if (critter.isTitan())
        {
            return 1;
        }
        int diff = critter.getPointValue() - getPointValue();
        if (diff != 0)
        {
            return diff;
        }
        if (isRangestriker() && !critter.isRangestriker())
        {
            return -1;
        }
        if (!isRangestriker() && critter.isRangestriker())
        {
            return 1;
        }
        if (isFlier() && !critter.isFlier())
        {
            return -1;
        }
        if (!isFlier() && critter.isFlier())
        {
            return 1;
        }
        return getName().compareTo(critter.getName());
    }
}
