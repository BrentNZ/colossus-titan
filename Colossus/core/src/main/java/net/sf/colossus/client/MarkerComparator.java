package net.sf.colossus.client;


import java.util.Comparator;


/**
 *  Compare markers.
 *  @version $Id$
 *  @author David Ripton
 */
public class MarkerComparator implements Comparator<String>
{
    private String shortColor;

    public MarkerComparator(String shortColor)
    {
        if (shortColor == null)
        {
            this.shortColor = "None";
        }
        else
        {
            this.shortColor = shortColor;
        }
    }

    public int compare(String s1, String s2)
    {
        if (s1.startsWith(shortColor) && !s2.startsWith(shortColor))
        {
            return -1;
        }
        if (!s1.startsWith(shortColor) && s2.startsWith(shortColor))
        {
            return 1;
        }
        return s1.compareTo(s2);
    }
}
