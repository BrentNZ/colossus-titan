package net.sf.colossus.server;


import java.util.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.client.GetPlayers;
import net.sf.colossus.parser.CreatureLoader;


/**
 * Class Creature represents the CONSTANT information about a Titan
 * Creature.
 *
 * Game related info is in Critter.  Counts of
 * recruited/available/dead are in Caretaker.
 *
 * @version $Id$
 * @author David Ripton, Bruce Sherrod 
*/

public class Creature implements Comparable
{
    private final String name;
    private final String pluralName;
    private final int power;
    private final int skill;
    private final boolean rangestrikes;
    private final boolean flies;
    private final boolean nativeBramble;
    private final boolean nativeDrift;
    private final boolean nativeBog;
    private final boolean nativeSandDune;
    private final boolean nativeSlope;
    private final boolean nativeVolcano;
    private final boolean waterDwelling;
    private final boolean magicMissile;
    private final boolean lord;
    private final boolean demilord;
    private final int maxCount;

    /** For marking unknown enemy creatures when tracking PBEM games. */
    public static final Creature unknown = new Creature("Unknown", 1, 1,
        false, false, false, false, false, false, false,
        false, false, false, false, false,
        1, "Unknown");

    /** Sometimes we need to iterate through all creature types. */
    private static java.util.List creatures = new ArrayList();


    public Creature(String name, int power, int skill, boolean rangestrikes,
        boolean flies, boolean nativeBramble, boolean nativeDrift,
        boolean nativeBog, boolean nativeSandDune, boolean nativeSlope,
        boolean nativeVolcano, boolean waterDwelling, boolean magicMissile,
        boolean lord, boolean demilord, int maxCount, String pluralName)
    {
        this.name = name;
        this.power = power;
        this.skill = skill;
        this.rangestrikes = rangestrikes;
        this.flies = flies;
        this.nativeBramble = nativeBramble;
        this.nativeDrift = nativeDrift;
        this.nativeBog = nativeBog;
        this.nativeSandDune = nativeSandDune;
        this.nativeSlope = nativeSlope;
        this.nativeVolcano = nativeVolcano;
        this.waterDwelling = waterDwelling;
        this.magicMissile = magicMissile;
        this.lord = lord;
        this.demilord = demilord;
        this.maxCount = maxCount;
        this.pluralName = pluralName;
    }


    public Creature(Creature creature)
    {
        this.name = creature.name;
        this.power = creature.power;
        this.skill = creature.skill;
        this.rangestrikes = creature.rangestrikes;
        this.flies = creature.flies;
        this.nativeBramble = creature.nativeBramble;
        this.nativeDrift = creature.nativeDrift;
        this.nativeBog = creature.nativeBog;
        this.nativeSandDune = creature.nativeSandDune;
        this.nativeSlope = creature.nativeSlope;
        this.nativeVolcano = creature.nativeVolcano;
        this.waterDwelling = creature.waterDwelling;
        this.magicMissile = creature.magicMissile;
        this.lord = creature.lord;
        this.demilord = creature.demilord;
        this.maxCount = creature.maxCount;
        this.pluralName = creature.pluralName;
    }


    static void loadCreatures()
    {
        try 
        {
            creatures.clear();
            java.util.List directories = new java.util.ArrayList();
            directories.add(GetPlayers.getVarDirectory());
            directories.add(Constants.defaultDirName);
            InputStream creIS = ResourceLoader.getInputStream(
                                               GetPlayers.getCreaturesName(),
                                               directories);
            if (creIS == null) 
            {
                throw new FileNotFoundException(GetPlayers.getCreaturesName());
            }
            CreatureLoader creatureLoader = new CreatureLoader(creIS);
            while (creatureLoader.oneCreature(creatures) >= 0) {}
        }
        catch (Exception e) 
        {
            System.out.println("Creatures def. loading failed : " + e);
            System.exit(1);
        }
    }

    public static java.util.List getCreatures()
    {
        return creatures;
    }

    public int getMaxCount()
    {
        return maxCount;
    }

    public boolean isLord()
    {
        return lord;
    }

    public boolean isImmortal()
    {
        return (lord || demilord);
    }

    public boolean isTitan()
    {
        return name.equals("Titan");
    }

    public boolean isAngel()
    {
        return name.equals("Angel") || name.equals("Archangel");
    }

    public String getName()
    {
        return name;
    }

    public String getPluralName()
    {
        return pluralName;
    }

    public String getImageName(boolean inverted)
    {
        StringBuffer basename = new StringBuffer();
        if (inverted)
        {
            basename.append(Constants.invertedPrefix);
        }
        basename.append(name);
        return basename.toString();
    }

    public String getImageName()
    {
        return getImageName(false);
    }


    public int getPower()
    {
        return power;
    }

    public int getSkill()
    {
        return skill;
    }

    public int getPointValue()
    {
        return getPower() * getSkill();
    }

    public boolean isRangestriker()
    {
        return rangestrikes;
    }

    public boolean isFlier()
    {
        return flies;
    }


    public boolean isNativeBramble()
    {
        return nativeBramble;
    }

    public boolean isNativeDrift()
    {
        return nativeDrift;
    }

    public boolean isNativeBog()
    {
        return nativeBog;
    }

    public boolean isNativeSandDune()
    {
        return nativeSandDune;
    }

    public boolean isNativeSlope()
    {
        return nativeSlope;
    }

    public boolean isNativeVolcano()
    {
        return nativeVolcano;
    }

    public boolean isWaterDwelling()
    {
        return waterDwelling;
    }

    public boolean useMagicMissile()
    {
        return magicMissile;
    }

    public static Creature getCreatureByName(String name)
    {
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            if (name.equals(creature.getName()))
            {
                return creature;
            }
        }

        Log.error("There is no creature called " + name);
        return null;
    }


    public String toString()
    {
        return name;
    }


    /** Compare by name. */
    public int compareTo(Object object)
    {
        if (object instanceof Creature)
        {
            Creature other = (Creature)object;
            return (name.compareTo(other.name));
        }
        else
        {
            throw new ClassCastException();
        }
    }


    /** Compare by name. */
    public boolean equals(Object object)
    {
        if (!(object instanceof Creature))
        {
            return false;
        }
        Creature other = (Creature)object;
        return name.equals(other.getName());
    }

    public int hashCode()
    {
        return name.hashCode();
    }
}
