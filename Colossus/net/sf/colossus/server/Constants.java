package net.sf.colossus.server;


import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;


/**
 * Class Constants just holds constants.
 * @version $Id$
 * @author David Ripton
 */

public final class Constants
{
    // Constants for phases of a turn.
    public static final int SPLIT = 1;
    public static final int MOVE = 2;
    public static final int FIGHT = 3;
    public static final int MUSTER = 4;


    // Constants for savegames
    public static final String saveDirname = "saves";
    public static final String saveExtension = ".sav";
    public static final String saveGameVersion =
        "Colossus savegame version 9";

    public static final String configVersion =
        "Colossus config file version 2";

    // Phases of a battle turn
    public static final int SUMMON = 0;
    public static final int RECRUIT = 1;
    //public static final int MOVE = 2;
    //public static final int FIGHT = 3;
    public static final int STRIKEBACK = 4;

    // Angel-summoning states
    public static final int NO_KILLS = 0;
    public static final int FIRST_BLOOD = 1;
    public static final int TOO_LATE = 2;

    // Legion tags
    public static final int DEFENDER = 0;
    public static final int ATTACKER = 1;

    // Constants for hexside gates.
    public static final int NONE = 0;
    public static final int BLOCK = 1;
    public static final int ARCH = 2;
    public static final int ARROW = 3;
    public static final int ARROWS = 4;

    public static final int ARCHES_AND_ARROWS = -1;
    public static final int ARROWS_ONLY = -2;
    public static final int NOWHERE = -1;

    public static final String invertedPrefix = "i_";

    // MasterBoard size
    public final static int MIN_HORIZ_SIZE=15;
    public final static int MIN_VERT_SIZE=8;
    public final static int MAX_HORIZ_SIZE=20;
    public final static int MAX_VERT_SIZE=12;

    public static final String [] colorNames =
        {"Black", "Blue", "Brown", "Gold", "Green", "Red"};

    public static final int MIN_DELAY = 0;      //in ms
    public static final int MAX_DELAY = 5000;

    // Entry sides
    public static final String bottom = "Bottom";
    public static final String right = "Right";
    public static final String left = "Left";

    /** Default directory for datafiles */
    public static String defaultDirName = "Default";
    /** Images subdirectory name */
    public static String imageDirName = "images";
}
