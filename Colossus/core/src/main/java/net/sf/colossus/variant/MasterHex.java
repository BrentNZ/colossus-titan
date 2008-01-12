package net.sf.colossus.variant;


import java.awt.Color;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.Hex;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.server.Creature;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Class MasterHex describes one Masterboard hex, without GUI info.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class MasterHex extends Hex
{
    private final MasterHex[] neighbors = new MasterHex[6];

    private int labelSide;
    private final int[] exitType = new int[6];
    private final int[] entranceType = new int[6];

    /**
     * TODO these base exit types and labels are somehow used only during setup of the MasterBoard,
     * no real need to keep them around.
     * 
     * It seems to be related to the fact that the master board hexes are really triangles and
     * stored as such in the XML files. Maybe they should not be hexes in the code either.
     */
    private final int[] baseExitType = new int[3];
    private final String[] baseExitLabel = new String[3];

    private String terrain; // TODO should be typesafe enum

    // The hex vertexes are numbered like this:
    //
    //               normal                     inverted
    //
    //              0------1                  0------------1
    //             /        \                /              \
    //            /          \              /                \
    //           /            \            /                  \
    //          /              \          5                    2
    //         /                \          \                  /
    //        /                  \          \                /
    //       5                    2          \              /
    //        \                  /            \            /
    //         \                /              \          /
    //          \              /                \        /
    //           4------------3                  4------3

    public MasterHex()
    {
        super();
    }

    public String getTerrain()
    {
        return this.terrain;
    }

    public void setTerrain(String terrain)
    {
        this.terrain = terrain;
    }

    @Override
    public String getTerrainName()
    {
        return this.terrain;
    }

    public String getTerrainDisplayName()
    {
        return (TerrainRecruitLoader.getTerrainDisplayName(terrain));
    }

    public Color getTerrainColor()
    {
        return (TerrainRecruitLoader.getTerrainColor(terrain));
    }

    public static boolean isNativeCombatBonus(Creature creature, String terrain)
    {
        int bonusHazardCount = 0;
        int bonusHazardSideCount = 0;

        for (HazardTerrain hTerrain : HazardTerrain.getAllHazardTerrains())
        {
            int count = HexMap.getHazardCountInTerrain(hTerrain, terrain);
            if (hTerrain.isNativeBonusTerrain()
                && creature.isNativeIn(hTerrain))
            {
                bonusHazardCount += count;
            }
            else
            {
                if (hTerrain.isNonNativePenaltyTerrain()
                    && !creature.isNativeIn(hTerrain))
                {
                    bonusHazardCount -= count;
                }
            }
        }
        final char[] hazardSide = BattleHex.getHexsides();

        for (int i = 0; i < hazardSide.length; i++)
        {
            int count = net.sf.colossus.client.HexMap
                .getHazardSideCountInTerrain(hazardSide[i], terrain);
            if (BattleHex.isNativeBonusHexside(hazardSide[i])
                && creature.isNativeHexside(hazardSide[i]))
            {
                bonusHazardSideCount += count;
            }
            else
            {
                if (BattleHex.isNonNativePenaltyHexside(hazardSide[i])
                    && !creature.isNativeHexside(hazardSide[i]))
                {
                    bonusHazardSideCount -= count;
                }
            }
        }
        if (((bonusHazardCount + bonusHazardSideCount) > 0)
            && ((bonusHazardCount >= 3) || (bonusHazardSideCount >= 5)))
        {
            return true;
        }
        return false;
    }

    public MasterHex getNeighbor(int i)
    {
        assert (i >= 0) && (i <= 5) : "Neighbor index out of range";
        return neighbors[i];
    }

    void setNeighbor(int i, MasterHex hex)
    {
        assert (i >= 0) && (i <= 5) : "Neighbor index out of range";
        neighbors[i] = hex;
    }

    public void setLabel(int label)
    {
        setLabel(Integer.toString(label));
    }

    public int getLabelSide()
    {
        return labelSide;
    }

    public void setLabelSide(int labelSide)
    {
        this.labelSide = labelSide;
    }

    public int getExitType(int i)
    {
        return exitType[i];
    }

    public void setExitType(int i, int exitType)
    {
        this.exitType[i] = exitType;
    }

    public int getBaseExitType(int i)
    {
        return baseExitType[i];
    }

    public void setBaseExitType(int i, int exitType)
    {
        this.baseExitType[i] = exitType;
    }

    public String getBaseExitLabel(int i)
    {
        return baseExitLabel[i];
    }

    public void setBaseExitLabel(int i, String label)
    {
        this.baseExitLabel[i] = label;
    }

    public int getEntranceType(int i)
    {
        return entranceType[i];
    }

    public void setEntranceType(int i, int entranceType)
    {
        this.entranceType[i] = entranceType;
    }
}