package net.sf.colossus.xmlparser;


import java.util.*;
import java.io.*;

import org.jdom.*;
import org.jdom.input.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.client.BattleHex;


/**
 * BattlelandLoader loads the battle hex data
 * @author Romain Dolbeau
 * @version $Id$
 */
public class BattlelandLoader
{

    /** hold the list of label for the startlist */
    private java.util.List startlist = null;

    /** is the terrain a Tower ? */
    private boolean isTower = false;

    /** optional subtitle for the Battlelands */
    private String subtitle = null;

    private String terrain = null;

    public BattlelandLoader(InputStream batIS, BattleHex[][] h)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(batIS);
            Element root = doc.getRootElement();
            terrain = root.getAttributeValue("terrain");
            isTower = root.getAttribute("tower").getBooleanValue();
            subtitle = root.getAttributeValue("subtitle");

            List hexlist = root.getChildren("battlehex");
            for (Iterator it = hexlist.iterator(); it.hasNext();)
            {
                Element el = (Element)it.next();
                handleHex(el, h);
            }
        }
        catch (JDOMException ex)
        {
            Log.error("JDOM" + ex.toString());
        }
        catch (IOException ex)
        {
            Log.error("IO" + ex.toString());
        }
    }

    private void handleHex(Element el, BattleHex[][] h)
        throws JDOMException
    {
        int xpos = el.getAttribute("x").getIntValue();
        int ypos = el.getAttribute("y").getIntValue();
        BattleHex hex = h[xpos][ypos];

        String terrain = el.getAttributeValue("terrain");
        hex.setTerrain(terrain);

        int elevation = el.getAttribute("elevation").getIntValue();
        hex.setElevation(elevation);

        List borders = el.getChildren("border");
        for (Iterator it = borders.iterator(); it.hasNext();)
        {
            Element border = (Element)it.next();
            int number = border.getAttribute("number").getIntValue();
            char type = border.getAttributeValue("type").charAt(0);
            hex.setHexside(number, type);
        }
    }

    public java.util.List getStartList()
    {
        return startlist;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public boolean isTower()
    {
        return isTower;
    }
}
