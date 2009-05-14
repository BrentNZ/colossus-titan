package net.sf.colossus.ai;

import java.util.logging.Logger;
import net.sf.colossus.client.Client;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;

/**
 *
 * @author Romain Dolbeau
 */
class PreserveCreatureTacticalObjective implements TacticalObjective
{
    private static final Logger LOGGER = Logger.getLogger(
            PreserveCreatureTacticalObjective.class.getName());
    private final Creature critter;
    private final Legion liveLegion;
    private final Client client;
    private final int count;
    private final int priority;

    private int countCreatureType(Legion legion)
    {
        int lcount = 0;
        for (Creature lcreature : legion.getCreatures())
        {
            if (lcreature.getType().equals(critter.getType()))
            {
                lcount++;
            }
        }
        return lcount;
    }

    PreserveCreatureTacticalObjective(int priority, Client client, Legion liveLegion,
            Creature critter)
    {
        this.priority = priority;
        this.critter = critter;
        this.liveLegion = liveLegion;
        this.client = client;
        count = countCreatureType(liveLegion);
        if (count <= 0)
        {
            LOGGER.warning("Trying to preserve all " + critter.getName() +
                    " but there isn't any in " + liveLegion.getMarkerId());
        }
    }

    public boolean objectiveAttained()
    {
        if (countCreatureType(liveLegion) >= count)
        {
            return true;
        }
        return false;
    }

    public int situationContributeToTheObjective()
    {
        int mcount = 0;
        for (BattleCritter dCritter : client.getActiveBattleUnits())
        {
            if (dCritter.getCreatureType().equals(critter.getType()))
            {
                int lcount = 0;
                for (BattleCritter aCritter : client.getInactiveBattleUnits())
                {
                    int range = Battle.getRange(dCritter.getCurrentHex(),
                            aCritter.getCurrentHex(), false);
                    if (range == 2)
                    {
                        lcount += aCritter.getPointValue();
                    }
                    else if (aCritter.isRangestriker()
                          && (range <= aCritter.getSkill())
                          && (aCritter.useMagicMissile()
                           || (!dCritter.isLord())))
                    {
                        lcount += aCritter.getPointValue() / 2;
                    }
                }
                if (lcount > mcount)
                {
                    mcount = lcount;
                }
            }
        }
        return -(mcount * getPriority());
    }

    public int getPriority()
    {
        return priority;
    }

    public String getDescription()
    {
        return "Preserving all " + critter.getName();
    }
}