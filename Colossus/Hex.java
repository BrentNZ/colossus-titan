import java.awt.*;
import java.awt.geom.*;

/**
 * Class Hex describes one general hex.
 * @version $Id$
 * @author David Ripton
 */

public abstract class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    public static final double RAD_TO_DEG = 180 / Math.PI;

    // The hex vertexes are numbered like this:
    //
    //              0---------1
    //             /           \
    //            /             \
    //           /               \
    //          /                 \
    //         5                   2
    //          \                 /
    //           \               /
    //            \             /
    //             \           /
    //              4---------3

    protected double[] xVertex = new double[6];
    protected double[] yVertex = new double[6];
    protected GeneralPath hexagon;
    protected Rectangle rectBound;
    private boolean selected;
    private char terrain;
    protected int scale;
    protected double len;
    protected String label;
    private int xCoord = -1;
    private int yCoord = -1;
    protected Point center;

    /** Globally turns antialiasing on or off for all hexes. */
    protected static boolean antialias;


    public void select()
    {
        selected = true;
    }


    public void unselect()
    {
        selected = false;
    }


    public boolean isSelected()
    {
        return selected;
    }


    public Rectangle getBounds()
    {
        return rectBound;
    }


    public boolean contains(Point point)
    {
        return (hexagon.contains(point));
    }


    public char getTerrain()
    {
        return terrain;
    }


    public void setTerrain(char terrain)
    {
        this.terrain = terrain;
    }


    public String getLabel()
    {
        return label;
    }


    public String getName()
    {
        return label;
    }


    public void setLabel(String label)
    {
        this.label = label;
    }


    public abstract String getTerrainName();


    public String getDescription()
    {
        return getTerrainName() + " hex " + getLabel();
    }

    public String toString()
    {
        return getDescription();
    }

    public void setXCoord(int xCoord)
    {
        this.xCoord = xCoord;
    }

    public int getXCoord()
    {
        return xCoord;
    }

    public void setYCoord(int yCoord)
    {
        this.yCoord = yCoord;
    }

    public int getYCoord()
    {
        return yCoord;
    }

    public static boolean getAntialias()
    {
        return antialias;
    }

    public static void setAntialias(boolean enabled)
    {
        antialias = enabled;
    }


    /** Return a GeneralPath polygon, with the passed number of sides,
     *  and the passed x and y coordinates.  Close the polygon if the
     *  argument closed is true. */
    public static GeneralPath makePolygon(int sides, double [] x, double [] y,
        boolean closed)
    {
        GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD,
            sides);
        polygon.moveTo((float)x[0], (float)y[0]);
        for (int i = 1; i < sides; i++)
        {
            polygon.lineTo((float)x[i], (float)y[i]);
        };
        if (closed)
        {
            polygon.closePath();
        }

        return polygon;
    }


    /** Return the Point closest to the center of the passed polygon */
    protected Point findCenter()
    {
        return new Point((int)Math.round((xVertex[0] + xVertex[3]) / 2),
            (int)Math.round((yVertex[0] + yVertex[3]) / 2));
    }


    /** Return the next hexside number in a clockwise direction, or -1 if
     *  the given hexside is outside the valid range 0-5. */
    public static int nextHexsideNum(int hexside)
    {
        switch (hexside)
        {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 0;
            default:
                return -1;
        }
    }


    /** Return the opposite hexside number, or -1 if the given hexside
     *  is outside the valid range 0-5. */
    public static int oppositeHexsideNum(int hexside)
    {
        switch (hexside)
        {
            case 0:
                return 3;
            case 1:
                return 4;
            case 2:
                return 5;
            case 3:
                return 0;
            case 4:
                return 1;
            case 5:
                return 2;
            default:
                return -1;
        }
    }


    /** Coerce the given number into the valid hexside range 0-5,
        without using an expensive mod if possible. */
    public static int hexsideNum(int hexside)
    {
        switch (hexside)
        {
            case -6:
            case 0:
            case 6:
                return 0;
            case -5:
            case 1:
            case 7:
                return 1;
            case -4:
            case 2:
            case 8:
                return 2;
            case -3:
            case 3:
            case 9:
                return 3;
            case -2:
            case 4:
            case 10:
                return 4;
            case -1:
            case 5:
            case 11:
                return 5;
            default:
                return (6 + hexside) % 6;
        }
    }


    public static void main(String [] args)
    {
        MasterHex.main(args);
    }
}
