import java.awt.*;

/**
 * Class Hex describes one general hex.
 * @version $Id$
 * @author David Ripton
 */

public class Hex
{
    public static final double SQRT3 = Math.sqrt(3.0);
    public static final double RAD_TO_DEG = 180 / Math.PI;

    protected int[] xVertex = new int[6];
    protected int[] yVertex = new int[6];
    protected Polygon hexagon;
    protected Rectangle rectBound;
    private boolean selected;
    private char terrain;
    protected int scale;
    protected double len;
    protected String label;


    public boolean select(Point point)
    {
        if (hexagon.contains(point))
        {
            selected = !selected;
            return true;
        }
        return false;
    }


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


    public boolean isSelected(Point point)
    {
        return (contains(point) && isSelected());
    }


    public Rectangle getBounds()
    {
        return rectBound;
    }


    public boolean contains(Point point)
    {
        return (hexagon.contains(point));
    }


    public Point getCenter()
    {
        return new Point((xVertex[0] + xVertex[3]) / 2,
            (yVertex[0] + yVertex[3]) / 2);
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


    // This needs to be overridden in subclasses.
    public String getTerrainName()
    {
        return "?????";
    }


    public String getDescription()
    {
        return getTerrainName() + " hex " + getLabel();
    }
}
