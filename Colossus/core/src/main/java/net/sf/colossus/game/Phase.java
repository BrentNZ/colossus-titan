package net.sf.colossus.game;


/**
 * TODO this should probably start numbering with zero as does the BattlePhase and
 * as does any other Java enum -- currently SPLIT is serialized as "1"
 *
 * TODO instead: now that I added an artificial phase "INIT" (needed during start
 * of the game, otherwise updateStatusScreen tries to do toString on null),
 * - are then the fromInt/toInit still needed?
 */
public enum Phase
{
    INIT("Init"), SPLIT("Split"), MOVE("Move"), FIGHT("Fight"), MUSTER(
        "Muster");

    /**
     * Deserialize enum from integer value.
     *
     * @param i The number for the phase.
     * @return The matching Phase instance.
     *
     * @throws ArrayOutOfBoundsException iff the number is not valid.
     */
    public static Phase fromInt(int i)
    {
        return values()[i];
    }

    /**
     * Serialize the object to an integer code.
     *
     * Used for savegames.
     *
     * @return An integer code representing the phase.
     */
    public int toInt()
    {
        return ordinal();
    }

    /**
     * Returns a non-localized UI string for the phase.
     */
    @Override
    public String toString()
    {
        return name;
    }

    private final String name;

    private Phase(String name)
    {
        this.name = name;
    }
}