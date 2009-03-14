package net.sf.colossus.tools;

import java.io.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import net.sf.colossus.parser.BattlelandRandomizerLoader;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.gui.GUIBattleHex;
import net.sf.colossus.variant.HazardTerrain;

/**
 * Class BuilderHexMap displays a basic battle map.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */
public class BuilderHexMap extends HexMap
{

    protected boolean isTower = false;
    protected int scale = 2 * 15;
    protected int cx = 6 * scale;
    protected int cy = 2 * scale;




    BuilderHexMap()
    {
        super(null, false);

        setOpaque(true);
        setBackground(Color.white);
        setupHexes();
    }

    protected void setupHexes()
    {
        setupHexesGUI();
        setupNeighbors(h);
        setDisplayName("Unnamed Battleland");
        setBasicName("Unnamed Battleland");
        setSubtitle(null);
    }

    BattleHex[][] getBattleHexArray()
    {
        BattleHex[][] h2 = new BattleHex[6][6];
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (h[i][j] != null)
                {
                    h2[i][j] = h[i][j].getHexModel();
                }
            }

        }
        return h2;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(60 * 15, 55 * 15);
    }

    public String dumpAsString()
    {
        StringBuilder buf = new StringBuilder();
        HazardTerrain terrain;
        char s;
        int e;
        List<GUIBattleHex> localStartList = new ArrayList<GUIBattleHex>();

        buf.append("<?xml version=\"1.0\"?>\n");
        buf.append("<battlemap terrain=\"" + getBasicName() + "\" tower=\"" + (isTower
                ? "True" : "False") + "\"");
        if (getSubtitle() != null)
        {
            buf.append(" subtitle=\"" + getSubtitle() + "\"");
        }
        buf.append(">\n");
        buf.append(
                "<!--  Battlelands generated by BattlelandsBuilder, the XML edition -->\n");

        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                if (show[i][j])
                {
                    boolean doDumpSides = false;
                    boolean hasSlope = false;
                    boolean hasWall = false;
                    terrain = h[i][j].getHexModel().getTerrain();
                    e = h[i][j].getHexModel().getElevation();

                    if (h[i][j].isSelected())
                    {
                        localStartList.add(h[i][j]);
                    }

                    for (int k = 0; k < 6; k++)
                    {
                        s = h[i][j].getHexModel().getHexside(k);
                        if (s != ' ')
                        {
                            doDumpSides = true;
                        }
                        if (s == 's')
                        {
                            hasSlope = true;
                        }
                        if (s == 'w')
                        {
                            hasWall = true;
                        }
                    }
                    if (doDumpSides ||
                            (!terrain.equals(HazardTerrain.getTerrainByName(
                            "Plains"))) ||
                            (e != 0))
                    {
                        if ((e < 1) && hasSlope)
                        {
                            buf.append(
                                    "<!--  WARNING: slope on less-than-1 elevation Hex -->\n");
                        }
                        if ((!terrain.equals(HazardTerrain.getTerrainByName(
                                "Tower"))) && hasWall)
                        {
                            buf.append(
                                    "<!--  WARNING: wall on non-Tower Hex -->\n");
                        }
                        if ((e < 1) && hasWall)
                        {
                            buf.append(
                                    "<!--  WARNING: wall on less-than-1 elevation Hex -->\n");
                        }
                        if ((terrain.equals(HazardTerrain.getTerrainByName(
                                "Lake"))) && doDumpSides)
                        {
                            buf.append(
                                    "<!--  WARNING: non-default sides on Lake -->\n");
                        }
                        if ((terrain.equals(HazardTerrain.getTerrainByName(
                                "Tree"))) && doDumpSides)
                        {
                            buf.append(
                                    "<!--  WARNING: non-default sides on Tree -->\n");
                        }
                        buf.append("<battlehex x=\"" + i + "\" y=\"" + j +
                                "\" terrain=\"" + h[i][j].getHexModel().
                                getTerrain().getName() + "\" elevation=\"" + h[i][j].getHexModel().
                                getElevation() + "\">\n");
                        if (doDumpSides)
                        {
                            for (int k = 0; k < 6; k++)
                            {
                                if (h[i][j].getHexModel().getHexside(k) != ' ')
                                {
                                    buf.append("<border number=\"" + k +
                                            "\" type=\"" + h[i][j].getHexModel().
                                            getHexside(k) + "\" />\n");
                                }
                            }
                        }
                        buf.append("</battlehex>\n");
                    }
                }
            }
        }

        if (!localStartList.isEmpty())
        {
            buf.append("<!--  This terrain has a startlist -->\n");
            buf.append("<startlist>\n");
            for (GUIBattleHex lh : localStartList)
            {
                buf.append("<battlehexref label=\"" +
                        lh.getHexModel().getLabel() + "\" />\n");
            }
            buf.append("</startlist>\n");
        }
        buf.append("</battlemap>\n");
        return (buf.toString());
    }

    void eraseMap()
    {
        for (GUIBattleHex hex : hexes)
        {
            hex.getHexModel().setTerrain(
                    HazardTerrain.getTerrainByName("Plains"));
            hex.getHexModel().setElevation(0);
            for (int i = 0; i < 6; i++)
            {
                hex.getHexModel().setHexside(i, ' ');
            }
        }
    }

    public void doRandomization(BattleHex[][] h, InputStream inputFile)
    {
        BattlelandRandomizerLoader parser =
                new BattlelandRandomizerLoader(inputFile);
        try
        {
            while (parser.oneArea(h) >= 0)
            {
            }
            parser.resolveAllHexsides(h);
            isTower = parser.isTower();
            List<String> startList = parser.getStartList();
            if (startList != null)
            {
                selectHexesByLabels(new HashSet<String>(startList));
            }
            setBasicName(parser.getTitle());
            setDisplayName(parser.getTitle());
            setSubtitle(parser.getSubtitle());
        } catch (Exception e)
        {
            System.err.println(e);
        }
    }
}
