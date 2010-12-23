package net.sf.colossus.server;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.BuildInfo;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


public class GameSaving
{
    private static final Logger LOGGER = Logger.getLogger(GameSaving.class
        .getName());

    private final GameServerSide game;

    private final Options options;

    /**
     *  snapshot of game data (caretaker, players, legions, ...) at the last
     *  "commit point", initially those are taken only at start of a phase.
     *  (Later this might be also after each completed engagement/battle).
     *  Savegame contains then this snapshot plus the redo-Data which was
     *  additionally done after that.
     */
    private Element phaseStartSnapshot;

    /**
     * List of filenames that has been created by AutoSave.
     * If option "keep max N autosave files" is set, when N+1th file was
     * created, first from this list will be deleted and so on.
     */
    private final List<String> autoGeneratedFiles = new ArrayList<String>();

    public GameSaving(GameServerSide game, Options options)
    {
        this.options = options;
        this.game = game;
    }

    /**
     *  Take a new snapshot of the data (basic game data,
     *  players with legions, and history) at the begin of a phase.
     *  At every point of time there is always one such latest snapshot
     *  in this.phaseStartSnapshot.
     */
    private void takeSnapshotAtBeginOfPhase()
    {
        Element root = new Element("CommitPointSnapshot");

        addBasicData(root);
        addPlayerData(root);

        this.phaseStartSnapshot = root;
    }

    /**
     * When a commit point is reached (typically, one phase is "Done"
     * and a new phase begins),
     * 1) take new snapshot of overall game state, player, legion, caretaker data
     * 2) flush the so far redoLog data to the history,
     * 3) clear the redoLog data.
     *
     */
    public void commitPointReached()
    {
        // XXX
        /*
        System.out.println("Commit point reached. Turn="
            + game.getTurnNumber() + ", player= "
            + game.getActivePlayer().getName() + ", phase="
            + game.getPhaseName());
        */
        takeSnapshotAtBeginOfPhase();
        game.getHistory().flushRecentToRoot();
    }

    // unchecked conversions from JDOM
    @SuppressWarnings("unchecked")
    private void addSnapshotData(Element saveGameRoot, Element commitDataRoot)
    {
        Element copyOfSnapshot = (Element)commitDataRoot.clone();

        List<Element> kids = new LinkedList<Element>(copyOfSnapshot
            .getChildren());
        for (Element el : kids)
        {
            el.detach();
            Element newEl = (Element)el.clone();

            // System.out.println("    adding commit data, element name: "
            //     + el.getName());
            saveGameRoot.addContent(newEl);
        }
    }

    /**
     * Create the whole content that will be written to the save game file.
     * Takes the last phaseStartSnapshot plus redo-Data plus battle data plus
     * data files.
     *
     * @return The "ColossusSnapshot" root element containing all information
     */
    private Element createSavegameContent()
    {
        Element root = new Element("ColossusSnapshot");
        root.setAttribute("version", Constants.XML_SNAPSHOT_VERSION);
        root.setAttribute("createdByRelease", BuildInfo.getReleaseVersion()
            + " (" + BuildInfo.getRevisionInfoString() + ")");

        // System.out.println("- Adding snapshot data from last commit point");
        addSnapshotData(root, this.phaseStartSnapshot);

        // Everything up to last commit point:
        // System.out.println("- Adding history");
        root.addContent(game.getHistory().getCopy());

        // Add the events since last commit point, some of them are more
        // detailed level. Redo log might also be empty, add it anyway,
        // otherwise there is trouble during loading.
        // System.out.println("- Adding redoLog");
        Element redoLogElement = game.getHistory().getNewRedoLogElement();

        // temporary solution - the save game file will basically be same state
        // as it was before engagement started
        if (game.isEngagementInProgress())
        {
            redoLogElement = new Element("Redo");
        }
        root.addContent(redoLogElement);

        // Battle stuff

        if (game.isEngagementInProgress() && game.getBattleSS() != null)
        {
            /* Disabled until it works properly */
            boolean featureProperlyEnabled = false;
            if (featureProperlyEnabled)
            {
                addBattleData(root);
            }
        }

        return root;
    }

    /**
     * Adds the basic data: variant info, turn number, current player,
     * current phase, and caretaker.
     *
     * @param root The document root to which to add all the data
     */
    private void addBasicData(Element root)
    {
        Element el = new Element("Variant");
        el.setAttribute("dir", VariantSupport.getVarDirectory());
        el.setAttribute("file", VariantSupport.getVarFilename());
        el.setAttribute("name", VariantSupport.getVariantName());
        root.addContent(el);

        el = new Element("TurnNumber");
        el.addContent("" + game.getTurnNumber());
        root.addContent(el);

        el = new Element("CurrentPlayer");
        el.addContent("" + game.getActivePlayerNum());
        root.addContent(el);

        el = new Element("CurrentPhase");
        el.addContent("" + game.getPhase().toInt());
        root.addContent(el);

        // Caretaker stacks
        Element careTakerEl = new Element("Caretaker");
        for (CreatureType creature : game.getVariant().getCreatureTypes())
        {
            el = new Element("Creature");
            el.setAttribute("name", creature.getName());
            el.setAttribute("remaining", ""
                + game.getCaretaker().getAvailableCount(creature));
            el.setAttribute("dead", ""
                + game.getCaretaker().getDeadCount(creature));
            careTakerEl.addContent(el);
        }
        // XXX temporarily out of use to keep save games small during development / debugging
        root.addContent(careTakerEl);
    }

    /**
     * Helper method, returns "null" if given string is null;
     * used by dumpLegion.
     * @param in the string to "null"ify if needed
     * @return "null" or the string itself
     */
    private String notnull(String in)
    {
        if (in == null)
        {
            return "null";
        }
        return in;
    }

    /**
     * Dump the given legion to an XML element
     * @param legion For which legion to dump the data
     * @param inBattle Whether this legion is currently involved into an
     * ongoing battle (i.e. battle data needs to be dumped too)
     * @return An XML Element with all Legion data
     */
    private Element dumpLegion(LegionServerSide legion, boolean inBattle)
    {
        Element leg = new Element("Legion");

        leg.setAttribute("name", legion.getMarkerId());
        leg.setAttribute("currentHex", legion.getCurrentHex().getLabel());
        leg.setAttribute("startingHex", legion.getStartingHex().getLabel());
        leg.setAttribute("moved", "" + legion.hasMoved());
        EntrySide entrySide = legion.getEntrySide();
        if (entrySide == null)
        {
            // This should never happen. Let's follow it for a while
            // TODO This try-catch checking code can probably be removed
            // at some point.
            LOGGER.warning("EntrySide of Legion " + legion.getMarkerId()
                + " is null!?!");
            entrySide = EntrySide.NOT_SET;
        }
        leg.setAttribute("entrySide", "" + entrySide.ordinal());
        leg.setAttribute("parent", legion.getParent() != null ? notnull(legion
            .getParent().getMarkerId()) : "null");
        leg.setAttribute("recruitName", String.valueOf(legion.getRecruit()));
        leg.setAttribute("battleTally", "" + legion.getBattleTally());

        for (Creature critter : legion.getCreatures())
        {
            Element cre = new Element("Creature");

            cre.setAttribute("name", critter.getName());
            if (inBattle)
            {
                cre.setAttribute("hits", "" + critter.getHits());
                cre.setAttribute("currentHex", critter.getCurrentHex()
                    .getLabel());
                cre.setAttribute("startingHex", critter.getStartingHex()
                    .getLabel());
                cre.setAttribute("struck", "" + critter.hasStruck());
            }
            leg.addContent(cre);
        }
        return leg;
    }

    /**
     * Adds the data for all players and their legions to an XML document
     *
     * @param root The document root to which to add the data
     */
    private void addPlayerData(Element root)
    {
        // Players
        Element el;
        for (Player p : game.getPlayers())
        {
            PlayerServerSide player = (PlayerServerSide)p;
            el = new Element("Player");
            el.setAttribute("name", player.getName());
            el.setAttribute("type", player.getType());
            el.setAttribute("color", player.getColor().getName());
            el.setAttribute("startingTower", player.getStartingTower()
                .getLabel());
            el.setAttribute("score", "" + player.getScore());
            el.setAttribute("dead", "" + player.isDead());
            el.setAttribute("mulligansLeft", "" + player.getMulligansLeft());
            el.setAttribute("colorsElim", player.getPlayersElim());
            el.setAttribute("summoned", "" + player.hasSummoned());

            Collection<LegionServerSide> legions = player.getLegions();
            Iterator<LegionServerSide> it2 = legions.iterator();

            while (it2.hasNext())
            {
                LegionServerSide legion = it2.next();

                el
                    .addContent(dumpLegion(legion,
                        game.isBattleInProgress()
                            && (legion == game.getBattleSS()
                                .getAttackingLegion() || legion == game
                                .getBattleSS().getDefendingLegion())));
            }
            root.addContent(el);
        }
    }

    private void addBattleData(Element root)
    {
        Element bat = new Element("Battle");

        bat.setAttribute("masterHexLabel", game.getBattleSS().getLocation()
            .getLabel());
        bat.setAttribute("turnNumber", ""
            + game.getBattleSS().getBattleTurnNumber());
        bat.setAttribute("activePlayer", ""
            + game.getBattleSS().getBattleActivePlayer().getName());
        bat.setAttribute("phase", ""
            + game.getBattleSS().getBattlePhase().ordinal());
        bat.setAttribute("summonState", ""
            + game.getBattleSS().getSummonState());
        bat.setAttribute("carryDamage", ""
            + game.getBattleSS().getCarryDamage());
        bat.setAttribute("preStrikeEffectsApplied", ""
            + game.getBattleSS().arePreStrikeEffectsApplied());

        for (BattleHex hex : game.getBattleSS().getCarryTargets())
        {
            Element ct = new Element("CarryTarget");
            ct.addContent(hex.getLabel());
            bat.addContent(ct);
        }
        root.addContent(bat);
    }

    /**
     * Generate the filename for autosaving (or just "Save" where one does
     * specify file name either) according to the pattern:
     *   DIRECTORY/snap TIMESTAMP TURN-PLAYER-PHASE
     *
     * @return The file name/path, including directory
     */
    private String makeAutosaveFileName()
    {
        Date date = new Date();
        boolean withInfo = options.getOption(Options.autosaveVerboseNames,
            true);

        String infoPart = "";
        if (withInfo)
        {
            infoPart = "_" + game.getTurnNumber() + "-"
                + game.getActivePlayer() + "-" + game.getPhase();
        }

        String name = Constants.SAVE_DIR_NAME + Constants.XML_SNAPSHOT_START
            + date.getTime() + infoPart + Constants.XML_EXTENSION;
        return name;
    }

    /**
     * Ensure that saves/ directory in Colossus-home exists, or create it.
     * Throws IOException if creation fails.
     *
     * @throws IOException if the saves directory does not exist and creation
     *                     fails
     */
    private void ensureSavesDirectory() throws IOException
    {
        File savesDir = new File(Constants.SAVE_DIR_NAME);

        if (!savesDir.exists() || !savesDir.isDirectory())
        {
            LOGGER.info("Trying to make directory " + Constants.SAVE_DIR_NAME);
            if (!savesDir.mkdirs())
            {
                LOGGER.log(Level.SEVERE, "Could not create saves directory "
                    + Constants.SAVE_DIR_NAME);
                throw new IOException("Could not create saves directory "
                    + Constants.SAVE_DIR_NAME);
            }
        }
    }

    /**
     * Produce one "automatically generated file name" for saving games,
     * including directory handling:
     *
     * 1) Creates the save game directory if it does not exist yet,
     *    including error handling.
     * 2) Generates an "automatic" file name (both for autoSave and File-Save)
     * 3) if it is autosave and the option to keep only a limited number of
     *    autosave files, add it to the list of autosave file names
     *
     * @param filename User specified filename, null for autosave or File-Save
     * @param autoSave Whether or not this was triggered by autosave
     * @param keep     How many autosave files to keep, 0 for "keep all"
     * @return         The automatically generated file name
     */
    private String automaticFilenameHandling(final String filename,
        boolean autoSave, int keep)
    {
        String autosaveFilename = makeAutosaveFileName();
        if (autoSave)
        {
            // Real autosave
            LOGGER.finest("Autosaving game to " + autosaveFilename);
            if (keep > 0)
            {
                autoGeneratedFiles.add(autosaveFilename);
            }
        }
        else
        {
            // File-Save (without providing a name) from File menu
            LOGGER.finest("File-Save saving the game to " + autosaveFilename);
        }

        return autosaveFilename;
    }

    /**
     * High-level method to save a file. Used for all three cases: Auto-save,
     * User specified file and File-Save (without specified file name).
     *
     * @param filename user specified filename, null for auto-save or File-Save
     * @param autoSave Whether or not this is autoSave
     * @throws IOException if the saves directory (for autosave or File-Save)
     *                     does not exist and creation fails
     */
    private synchronized void saveGame(final String filename, boolean autoSave)
        throws IOException
    {
        int keep = options.getIntOption(Options.autosaveMaxKeep);

        String fn = null;
        if (filename == null || filename.equals("null"))
        {
            // Might throw IOException if directory can't be created
            ensureSavesDirectory();
            fn = automaticFilenameHandling(filename, autoSave, keep);
            // automaticFilenameHandling did the logging already
        }
        else
        {
            fn = filename;
            LOGGER.info("Saving game to user-provided file name " + filename);
        }

        FileWriter fileWriter;
        PrintWriter out;
        try
        {
            fileWriter = new FileWriter(fn);
            out = new PrintWriter(fileWriter);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Couldn't open " + fn, e);
            return;
        }

        // Not here any more. Should now be taken at begin of each phase.
        // takeSnapshotAtBeginOfPhase();
        Element root = createSavegameContent();
        Document doc = new Document(root);

        // Now write it all out to the file
        try
        {
            XMLOutputter putter = new XMLOutputter(Format.getPrettyFormat());
            putter.output(doc, out);
            fileWriter.close();
            if ((filename == null || filename.equals("null")) && keep > 0)
            {
                while (autoGeneratedFiles.size() > keep)
                {
                    String delfilename = autoGeneratedFiles.remove(0);
                    File fileToDelete = new File(delfilename);
                    boolean success = fileToDelete.delete();
                    if (!success)
                    {
                        LOGGER.warning("Failed to delete autosave file "
                            + delfilename + "!");
                    }
                }
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Error writing XML savegame.", ex);
        }
    }

    // =====================================================================
    // And here it comes, the actual method that is called by GameServerSide
    // =====================================================================

    /**
     *  Call saveGame in a try-catch block. If any exception is caught,
     *  log it, show an error dialog, and additionally if this was triggered
     *  by autosave, disable the autosave from now on.
     *
     *  @param filename The name of the file to create
     *  @param autoSave True if this was triggered by autoSave
     */
    void saveGameWithErrorHandling(final String filename, boolean autoSave)
    {
        try
        {
            saveGame(filename, autoSave);
        }
        catch (Exception e)
        {
            String autosaveNowOffMmessage = "";
            if (autoSave)
            {
                options.setOption(Options.autosave, false);
                autosaveNowOffMmessage = " (autosave now disabled)";
            }

            String doWhat = autoSave ? "auto-save" : "save";
            String toWhere = filename == null ? "<automatically generated filename>"
                : (" file " + filename);
            String message = "Woooah! An exception was caught while "
                + "trying to " + doWhat + " game to " + toWhere
                + "\nStack trace:\n" + ErrorUtils.makeStackTraceString(e)
                + "\nSaving the game did probably not succeed"
                + autosaveNowOffMmessage + ".\n";
            LOGGER.warning(message);
            ErrorUtils.showExceptionDialog(null, message,
                "Exception caught during saving!", false);
        }
    }

}
