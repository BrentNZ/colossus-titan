package Default;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.variant.CreatureType;


public class DefaultHint implements net.sf.colossus.server.HintInterface
{
    private final DevRandom rnd = new DevRandom();

    // Convert list of recruits from Creature to String for easier compares.
    public static List<String> creaturesToStrings(List<CreatureType> creatures)
    {
        List<String> recruits = new ArrayList<String>();
        for (Iterator<CreatureType> it = creatures.iterator(); it.hasNext();)
        {
            Object ob = it.next();
            String str = ob.toString();
            recruits.add(str);
        }
        return recruits;
    }

    public String getRecruitHint(String terrain, LegionInfo legion,
        List<CreatureType> recruits, HintOracleInterface oracle,
        String[] section)
    {
        List<String> recruitNames = creaturesToStrings(recruits);
        List<String> sect = Arrays.asList(section);

        if (terrain.equals("Brush") || terrain.equals("Jungle"))
        {
            int numCyclops = legion.numCreature("Cyclops");
            if (numCyclops > 0 && numCyclops < 3
                && !legion.contains("Behemoth") && !legion.contains("Serpent")
                && oracle.creatureAvailable("Behemoth") >= 2
                && oracle.creatureAvailable("Cyclops") >= 1)
            {
                return "Cyclops";
            }
        }
        else if (terrain.equals("Plains"))
        {
            if (recruitNames.contains("Lion") && !legion.contains("Griffon")
                && legion.numCreature("Lion") == 2
                && oracle.canReach("Desert")
                && oracle.creatureAvailable("Griffon") >= 2)
            {
                return "Lion";
            }
            if (sect.contains(Constants.sectionDefensiveAI))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear") && legion.getHeight() < 6
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Woods")
                    && !oracle.hexLabel().equals("1")
                    && !oracle.hexLabel().equals("15")
                    && !oracle.hexLabel().equals("29"))
                {
                    return "Centaur";
                }
            }
            else if (sect.contains(Constants.sectionOffensiveAI))
            {
                if (recruitNames.contains("Centaur")
                    && legion.numCreature("Centaur") == 2
                    && !legion.contains("Warbear") && legion.getHeight() <= 2
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Woods"))
                {
                    return "Centaur";
                }
            }
        }
        else if (terrain.equals("Marsh"))
        {
            if (recruitNames.contains("Troll") && !legion.contains("Wyvern")
                && legion.numCreature("Troll") == 2
                && oracle.canReach("Swamp")
                && oracle.creatureAvailable("Wyvern") >= 2)
            {
                return "Troll";
            }
            if (sect.contains(Constants.sectionDefensiveAI))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur") && legion.getHeight() < 6
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Hills")
                    && !oracle.hexLabel().equals("8")
                    && !oracle.hexLabel().equals("22")
                    && !oracle.hexLabel().equals("36"))
                {
                    return "Ogre";
                }
            }
            else if (sect.contains(Constants.sectionOffensiveAI))
            {
                if (recruitNames.contains("Ogre")
                    && legion.numCreature("Ogre") == 2
                    && !legion.contains("Minotaur") && legion.getHeight() <= 2
                    && oracle.biggestAttackerHeight() == 0
                    && oracle.canReach("Hills"))
                {
                    return "Ogre";
                }
            }
        }
        else if (terrain.equals("Tower"))
        {
            if (recruitNames.contains("Warlock"))
            {
                return "Warlock";
            }
            if (recruitNames.contains("Guardian"))
            {
                return "Guardian";
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 2)
            {
                return "Ogre";
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 2)
            {
                return "Centaur";
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 1
                && oracle.creatureAvailable("Cyclops") >= 3)
            {
                return "Gargoyle";
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 1
                && oracle.creatureAvailable("Troll") >= 2)
            {
                return "Ogre";
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 1
                && oracle.creatureAvailable("Lion") >= 2)
            {
                return "Centaur";
            }
            if (recruitNames.contains("Gargoyle")
                && legion.numCreature("Gargoyle") == 0
                && oracle.creatureAvailable("Cyclops") >= 6)
            {
                return "Gargoyle";
            }
            if (recruitNames.contains("Ogre")
                && legion.numCreature("Ogre") == 0
                && oracle.creatureAvailable("Troll") >= 6)
            {
                return "Ogre";
            }
            if (recruitNames.contains("Centaur")
                && legion.numCreature("Centaur") == 0
                && oracle.creatureAvailable("Lion") >= 6)
            {
                return "Centaur";
            }
        }

        return recruitNames.get(recruitNames.size() - 1);
    }

    public List<String> getInitialSplitHint(String label, String[] section)
    {
        List<String> li = new ArrayList<String>();
        if (label.equals("100"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Centaur");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Ogre");
            }
        }
        else if (label.equals("200"))
        {
            li.add("Titan");
            li.add("Gargoyle");
            li.add("Gargoyle");
            li.add("Ogre");
        }
        else if (label.equals("300"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Ogre");
            }
            else
            {
                li.add("Titan");
                li.add("Centaur");
                li.add("Centaur");
                li.add("Ogre");
            }
        }
        else if (label.equals("400"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Ogre");
                li.add("Ogre");
            }
            else
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Centaur");
            }
        }
        else if (label.equals("500"))
        {
            li.add("Titan");
            li.add("Gargoyle");
            li.add("Gargoyle");
            li.add("Centaur");
        }
        else if (label.equals("600"))
        {
            if (rnd.nextFloat() < 0.5)
            {
                li.add("Titan");
                li.add("Gargoyle");
                li.add("Gargoyle");
                li.add("Centaur");
            }
            else
            {
                li.add("Titan");
                li.add("Ogre");
                li.add("Ogre");
                li.add("Centaur");
            }
        }
        else
        {
            throw new RuntimeException("Bad hex label " + label);
        }
        return li;
    }

    public int getHintedRecruitmentValueOffset(String name, String[] section)
    {
        return 0;
    }
}
