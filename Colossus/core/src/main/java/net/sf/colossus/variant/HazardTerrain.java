package net.sf.colossus.variant;


import java.util.Collection;
import java.util.LinkedList;


/** 
 * This is a typesafe enumeration of all hazard terrains, i.e. the
 * terrains used in the battle maps.
 */
public class HazardTerrain extends Hazards
{
    public HazardTerrain(String name, char code,
        EffectOnMovement effectOnGroundMovement,
        EffectOnMovement effectOnFlyerMovement,
        EffectOnStrike effectforDefendingInTerrain,
        ScopeOfEffectOnStrike scopeForDefenceEffect,
        int defenceEffectAdjustment,
        EffectOnStrike effectforAttackingFromTerrain,
        ScopeOfEffectOnStrike scopeForAttackEffect,
        int attackEffectAdjustment,
        EffectOnStrike effectForBeingRangeSruckInTerrain,
        ScopeOfEffectOnStrike scopeForRangeStruckEffect,
        int RangeStruckEffectAdjustment,
        EffectOnStrike effectforRangeStrikeFromTerrain,
        ScopeOfEffectOnStrike scopeForRangeStrikeEffect,
        int RangeStrikeEffectAdjustment,
        RangeStrikeSpecialEffect RangeStrikeSpecial,
        SpecialEffect terrainSpecial)
    {
        super(name, code, effectOnGroundMovement, effectOnFlyerMovement,
            effectforDefendingInTerrain, scopeForDefenceEffect,
            defenceEffectAdjustment, effectforAttackingFromTerrain,
            scopeForAttackEffect, attackEffectAdjustment,
            effectForBeingRangeSruckInTerrain, scopeForRangeStruckEffect,
            RangeStruckEffectAdjustment, effectforRangeStrikeFromTerrain,
            scopeForRangeStrikeEffect, RangeStrikeEffectAdjustment,
            RangeStrikeSpecial, terrainSpecial);
    }

    public boolean isNativeBonusTerrain()
    {
        return (scopeForAttackEffect.equals(ScopeOfEffectOnStrike.NATIVES)
            || scopeForDefenceEffect.equals(ScopeOfEffectOnStrike.NATIVES)
            || scopeForAttackEffect.equals(ScopeOfEffectOnStrike.PATRIOTS) || scopeForDefenceEffect
            .equals(ScopeOfEffectOnStrike.PATRIOTS))
            && (effectforAttackingFromTerrain
                .equals(EffectOnStrike.SKILLBONUS)
                || effectforAttackingFromTerrain
                    .equals(EffectOnStrike.POWERBONUS)
                || effectforDefendingInTerrain
                    .equals(EffectOnStrike.SKILLBONUS) || effectforDefendingInTerrain
                .equals(EffectOnStrike.POWERBONUS));
    }

    public boolean isNonNativePenaltyTerrain()
    {
        return (scopeForAttackEffect.equals(ScopeOfEffectOnStrike.FOREIGNERS) || scopeForDefenceEffect
            .equals(ScopeOfEffectOnStrike.FOREIGNERS))
            && (effectforAttackingFromTerrain
                .equals(EffectOnStrike.SKILLPENALTY)
                || effectforAttackingFromTerrain
                    .equals(EffectOnStrike.POWERPENALTY)
                || effectforDefendingInTerrain
                    .equals(EffectOnStrike.SKILLPENALTY) || effectforDefendingInTerrain
                .equals(EffectOnStrike.POWERPENALTY));
    }

    public boolean isNativeOnly()
    {
        return effectOnGroundMovement.equals(EffectOnMovement.BLOCKFOREIGNER);
    }

    public static HazardTerrain getTerrainByName(String name)
    {
        return (HazardTerrain)getByName(name);
    }

    /**
     * Returns all available hazard terrains.
     * 
     * This is not variant-specific, any terrain known to the program is listed even
     * if it is not available in the current variant.
     * 
     * TODO this should really be a question to ask a variant instance
     */
    public static final Collection<HazardTerrain> getAllHazardTerrains()
    {
        Collection<Hazards> hazards = getAllHazards();
        Collection<HazardTerrain> terrain = new LinkedList<HazardTerrain>();
        for (Hazards hazard : hazards)
        {
            if (hazard instanceof HazardTerrain)
            {
                terrain.add((HazardTerrain)hazard);
            }
        }
        return terrain;
    }

    public static final HazardTerrain PLAINS = new HazardTerrain("Plains",
        ' ', EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain TREE = new HazardTerrain("Tree", 't',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.BLOCKED, ScopeOfEffectOnStrike.IMPERIALS, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEBLOCKED, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain BRAMBLES = new HazardTerrain("Brambles",
        'b', EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.SLOWFOREIGNER,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.PATRIOTS, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.IMPERIALS, 1,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.PATRIOTS, 1,
        EffectOnStrike.SKILLPENALTY, ScopeOfEffectOnStrike.IMPERIALS, 1,
        RangeStrikeSpecialEffect.RANGESTRIKESKILLPENALTY,
        SpecialEffect.NOSPECIAL);

    public static final HazardTerrain DRIFT = new HazardTerrain("Drift", 'd',
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.SLOWFOREIGNER,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.HEALTHDRAIN);

    public static final HazardTerrain VOLCANO = new HazardTerrain("Volcano",
        'v', EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.BLOCKFOREIGNER,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 2,
        EffectOnStrike.SKILLBONUS, ScopeOfEffectOnStrike.NATIVES, 1,
        EffectOnStrike.POWERBONUS, ScopeOfEffectOnStrike.NATIVES, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain BOG = new HazardTerrain("Bog", 'o',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain SAND = new HazardTerrain("Sand", 's',
        EffectOnMovement.SLOWFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain TOWER = new HazardTerrain("Tower", 'w',
        EffectOnMovement.FREEMOVE, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain LAKE = new HazardTerrain("Lake", 'l',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.FREEMOVE,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    public static final HazardTerrain STONE = new HazardTerrain("Stone", 'n',
        EffectOnMovement.BLOCKFOREIGNER, EffectOnMovement.BLOCKALL,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        EffectOnStrike.NOEFFECT, ScopeOfEffectOnStrike.ALL, 0,
        RangeStrikeSpecialEffect.RANGESTRIKEFREE, SpecialEffect.NOSPECIAL);

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("+++++++++++++++++++++++++++\n");
        builder.append(getName());
        builder.append(":\n");
        return builder.toString();
    }

    public boolean blocksFlying()
    {
        return effectOnFlyerMovement.equals(EffectOnMovement.BLOCKALL);
    }

    public boolean isFlyersOnly()
    {
        return effectOnGroundMovement.equals(EffectOnMovement.BLOCKALL);
    }

    public boolean isNativeFlyersOnly()
    {
        return effectOnFlyerMovement.equals(EffectOnMovement.BLOCKFOREIGNER);
    }

    public boolean slowsNonNative()
    {
        return effectOnFlyerMovement.equals(EffectOnMovement.SLOWFOREIGNER)
            || effectOnGroundMovement.equals(EffectOnMovement.SLOWFOREIGNER);
    }
}
