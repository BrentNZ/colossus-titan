package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.Combos;


/**
 *  Predicts splits for one enemy player, and adjusts predictions as 
 *  creatures are revealed.
 *  @version $Id$
 *  @author David Ripton
 *  @author Kim Milvang-Jensen
 *  @see SplitPrediction.txt
 * 
 */

public class PredictSplitNode implements Comparable<PredictSplitNode>
{
    private final String markerId; // Not unique!
    private final int turnCreated;
    private CreatureInfoList creatures = new CreatureInfoList();

    // only if atSplit
    private final CreatureInfoList removed = new CreatureInfoList();

    private final PredictSplitNode parent;
    // Size of child2 at the time this node was split.
    private int childSize2;
    private PredictSplitNode child1; // child that keeps the marker
    private PredictSplitNode child2; // child with the new marker
    private static CreatureInfoComparator cic = new CreatureInfoComparator();

    PredictSplitNode(String markerId, int turnCreated, CreatureInfoList cil,
        PredictSplitNode parent)
    {
        this.markerId = markerId;
        this.turnCreated = turnCreated;
        this.creatures = cil.clone();
        this.parent = parent;
        clearChildren();
    }

    private void clearChildren()
    {
        childSize2 = 0;
        child1 = null;
        child2 = null;
    }

    String getMarkerId()
    {
        return markerId;
    }

    String getFullName()
    {
        return markerId + '(' + turnCreated + ')';
    }

    PredictSplitNode getChild1()
    {
        return child1;
    }

    PredictSplitNode getChild2()
    {
        return child2;
    }

    PredictSplitNode getParent()
    {
        return parent;
    }

    int getTurnCreated()
    {
        return turnCreated;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer(getFullName() + ":");
        for (Iterator<CreatureInfo> it = getCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            sb.append(" " + ci.toString());
        }
        for (Iterator<CreatureInfo> it = getRemovedCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            sb.append(" " + ci.toString() + "-");
        }
        return sb.toString();
    }

    /** Return list of CreatureInfo */
    CreatureInfoList getCreatures()
    {
        Collections.sort(creatures, cic);
        return creatures;
    }

    void setCreatures(CreatureInfoList creatures)
    {
        this.creatures = creatures;
    }

    /** Return list of CreatureInfo */
    CreatureInfoList getRemovedCreatures()
    {
        CreatureInfoList cil = new CreatureInfoList();
        cil.addAll(removed);
        return cil;
    }

    /** Return list of CreatureInfo where certain == true. */
    CreatureInfoList getCertainCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator<CreatureInfo> it = getCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            if (ci.isCertain())
            {
                list.add(ci);
            }
        }
        return list;
    }

    int numCertainCreatures()
    {
        return getCertainCreatures().size();
    }

    int numUncertainCreatures()
    {
        return getHeight() - numCertainCreatures();
    }

    boolean allCertain()
    {
        for (Iterator<CreatureInfo> it = getCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            if (!ci.isCertain())
            {
                return false;
            }
        }
        return true;
    }

    boolean hasSplit()
    {
        if (child1 == null && child2 == null)
        {
            return false;
        }
        assert child1 != null || child2 != null : "One child legion";
        return true;
    }

    List<PredictSplitNode> getChildren()
    {
        List<PredictSplitNode> li = new ArrayList<PredictSplitNode>();
        if (hasSplit())
        {
            li.add(child1);
            li.add(child2);
        }
        return li;
    }

    /**
     * Return true if all of this node's children, grandchildren, etc. have no
     * uncertain creatures
     */
    boolean allDescendentsCertain()
    {
        if (child1 == null)
        {
            return true;
        }
        else
        {
            return child1.allCertain() && child2.allCertain()
                && child1.allDescendentsCertain()
                && child2.allDescendentsCertain();
        }
    }

    /**
     * Return list of CreatureInfo where atSplit == true, plus removed
     * creatures.
     */
    CreatureInfoList getAtSplitOrRemovedCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator<CreatureInfo> it = getCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            if (ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (Iterator<CreatureInfo> it = getRemovedCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            list.add(ci);
        }
        return list;
    }

    /** Return list of CreatureInfo where atSplit == false. */
    CreatureInfoList getAfterSplitCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator<CreatureInfo> it = getCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            if (!ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        return list;
    }

    /**
     * Return list of CreatureInfo where both certain and atSplit are true, plus
     * removed creatures.
     */
    CreatureInfoList getCertainAtSplitOrRemovedCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator<CreatureInfo> it = getCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            if (ci.isCertain() && ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (Iterator<CreatureInfo> it = getRemovedCreatures().iterator(); it
            .hasNext();)
        {
            CreatureInfo ci = it.next();
            list.add(ci);
        }
        return list;
    }

    String getOtherChildMarkerId()
    {
        if (!markerId.equals(child1.getMarkerId()))
        {
            return child1.getMarkerId();
        }
        else
        {
            return child2.getMarkerId();
        }
    }

    int getHeight()
    {
        return creatures.size();
    }

    /** Return true if big is a superset of little. */
    static boolean superset(List<String> big, List<String> little)
    {
        List<String> bigclone = new ArrayList<String>(big);
        for (Iterator<String> it = little.iterator(); it.hasNext();)
        {
            Object ob = it.next();
            if (!bigclone.remove(ob))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true iff new information was sent to this legion's parent.
     */
    void revealCreatures(List<String> cnl)
    {
        if (cnl == null)
        {
            // this means we are updating the parent, and the info gained is
            // computed from children
            cnl = new ArrayList<String>();
            cnl.addAll(child1.getCertainAtSplitOrRemovedCreatures()
                .getCreatureNames());
            cnl.addAll(child2.getCertainAtSplitOrRemovedCreatures()
                .getCreatureNames());
        }

        List<String> certainInfoGained = subtractLists(cnl,
            getCertainCreatures().getCreatureNames());

        if (!certainInfoGained.isEmpty())
        {
            for (Iterator<String> it = certainInfoGained.iterator(); it
                .hasNext();)
            {
                String name = it.next();
                this.creatures.add(new CreatureInfo(name, true, true));
            }
            this.parent.revealCreatures(null);
            // Note the parent is responsible for updating the CreatureInfo
            // for this node when calculating the predicted split.
        }
        else if (hasSplit())
        {
            reSplit();
        }
        else
        {
            // The reveal didn't contain any actual info; nothing to do.
        }

        assert this.creatures.size() == getHeight() : "Certainty error in revealCreatures -- size is "
            + this.creatures.size() + " height is " + getHeight();
    }

    // Hardcoded to default starting legion.
    public static boolean isLegalInitialSplitoff(List<String> names)
    {
        if (names.size() != 4)
        {
            return false;
        }
        int count = 0;
        if (names.contains("Titan"))
        {
            count++;
        }
        if (names.contains("Angel"))
        {
            count++;
        }

        return count == 1;
    }

    /**
     * Return a list of all legal combinations of splitoffs. Also update
     * knownKeep and knownSplit if we conclude that more creatures are certain.
     * 
     * @param childSize
     * @param knownKeep
     * @param knownSplit
     * @return
     */
    List<List<String>> findAllPossibleSplits(int childSize,
        List<String> knownKeep, List<String> knownSplit)
    {
        // Sanity checks
        assert knownSplit.size() <= childSize : "More known splitoffs than splitoffs";
        assert creatures.size() <= 8 : "> 8 creatures in legion";
        assert creatures.size() != 8 || childSize == 4 : "Illegal initial split";
        assert creatures.size() != 8
            || creatures.getCreatureNames().contains(Constants.titan) : "No titan in 8-high legion";
        assert creatures.size() != 8
            || creatures.getCreatureNames().contains(Constants.angel) : "No angel in 8-high legion";

        List<String> knownCombo = new ArrayList<String>();
        knownCombo.addAll(knownSplit);
        knownCombo.addAll(knownKeep);
        List<String> certain = getCertainCreatures().getCreatureNames();
        assert superset(certain, knownCombo) : "knownCombo contains uncertain creatures";

        // Now determine by count arguments if we can determine know keepers
        // or splits. (If parent contains 3 certain rangers, and we split 5-2
        // then the 5 split contains a ranger. By the same argument
        // if the 5 stack grows a griffon from 3 lions, then there are only 2
        // unkowns in there from the split, so the 2 stack must contain a
        // ranger.
        List<String> certainsToSplit = subtractLists(certain, knownCombo);
        Collections.sort(certainsToSplit);

        // Special code to take into account account the the first split
        // must include a lord in each stack
        int firstTurnUnknownLord = 0;
        if (this.turnCreated == 0)
        {

            boolean unknownTitan = certainsToSplit.remove("Titan");
            boolean unknownAngel = certainsToSplit.remove("Angel");
            if (unknownTitan && unknownAngel)
            {
                // ei. neither are positioned yet
                firstTurnUnknownLord = 1;
            }
            else if (unknownAngel)
            {
                // Titan known, set Angel certain
                if (knownKeep.contains("Titan"))
                {
                    knownSplit.add("Angel");
                }
                else
                {
                    knownKeep.add("Angel");
                }
            }
            else if (unknownTitan)
            {
                // Titan known, set Angel certain
                if (knownKeep.contains("Angel"))
                {
                    knownSplit.add("Titan");
                }
                else
                {
                    knownKeep.add("Titan");
                }
            }
        }

        int numUnknownsToKeep = creatures.size() - childSize
            - knownKeep.size();
        int numUnknownsToSplit = childSize - knownSplit.size();

        if (!certainsToSplit.isEmpty())
        {
            String nextCreature = "";
            String currCreature = "";
            int count = 0;

            Iterator<String> it = certainsToSplit.iterator();
            boolean done = false;
            while (!done)
            {
                currCreature = nextCreature;
                if (it.hasNext())
                {
                    nextCreature = it.next();
                }
                else
                {
                    nextCreature = "";
                    done = true;
                }

                if (!nextCreature.equals(currCreature))
                {
                    // Compute how many to keep or splt, and update the lists.
                    int numToKeep = count - numUnknownsToSplit
                        + firstTurnUnknownLord;
                    int numToSplit = count - numUnknownsToKeep
                        + firstTurnUnknownLord;
                    for (int i = 0; i < numToKeep; i++)
                    {
                        knownKeep.add(currCreature);
                        numUnknownsToKeep--;
                    }
                    for (int i = 0; i < numToSplit; i++)
                    {
                        knownSplit.add(currCreature);
                        numUnknownsToSplit--;
                    }
                    count = 1;
                }
                else
                {
                    count++;
                }
            }
        }

        List<String> unknowns = creatures.getCreatureNames();

        // update knownCombo because knownKeep or knownSplit may have changed
        knownCombo.clear();
        knownCombo.addAll(knownSplit);
        knownCombo.addAll(knownKeep);

        for (Iterator<String> it = knownCombo.iterator(); it.hasNext();)
        {
            String name = it.next();
            unknowns.remove(name);
        }

        Combos<String> combos = new Combos<String>(unknowns,
            numUnknownsToSplit);

        Set<List<String>> possibleSplitsSet = new HashSet<List<String>>();
        for (Iterator<List<String>> it = combos.iterator(); it.hasNext();)
        {
            List<String> combo = it.next();
            List<String> pos = new ArrayList<String>();
            pos.addAll(knownSplit);
            pos.addAll(combo);
            if (getHeight() != 8)
            {
                possibleSplitsSet.add(pos);
            }
            else
            {
                if (isLegalInitialSplitoff(pos))
                {
                    possibleSplitsSet.add(pos);
                }
            }
        }
        List<List<String>> possibleSplits = new ArrayList<List<String>>(
            possibleSplitsSet);
        return possibleSplits;
    }

    // TODO Use SimpleAI version?
    /**
     * Decide how to split this legion, and return a list of creatures names to
     * remove. Return empty list on error.
     */
    List<String> chooseCreaturesToSplitOut(List<List<String>> possibleSplits)
    {
        List<String> firstElement = possibleSplits.get(0);
        boolean maximize = (2 * firstElement.size() > getHeight());
        int bestKillValue = -1;
        List<String> creaturesToRemove = new ArrayList<String>();
        for (Iterator<List<String>> it = possibleSplits.iterator(); it
            .hasNext();)
        {
            List<String> li = it.next();
            int totalKillValue = 0;
            for (Iterator<String> it2 = li.iterator(); it2.hasNext();)
            {
                String name = it2.next();
                Creature creature = (Creature)VariantSupport
                    .getCurrentVariant().getCreatureByName(name);
                totalKillValue += creature.getKillValue();
            }
            if ((bestKillValue < 0)
                || (!maximize && totalKillValue < bestKillValue)
                || (maximize && totalKillValue > bestKillValue))
            {
                bestKillValue = totalKillValue;
                creaturesToRemove = li;
            }
        }
        return creaturesToRemove;
    }

    /** Return the number of times ob is found in li */
    int count(List<?> li, Object ob)
    {
        int num = 0;
        for (Iterator<?> it = li.iterator(); it.hasNext();)
        {
            if (ob.equals(it.next()))
            {
                num++;
            }
        }
        return num;
    }

    /**
     * Computes the predicted split of childsize, given that we may already know
     * some pieces that are keept or spilt. Also makes the new
     * CreatureInfoLists. Note that knownKeep and knownSplit will be altered,
     * and be empty after call
     * 
     * @param childSize
     * @param knownKeep
     *            certain creatures to keep
     * @param knownSplit
     *            certain creatures to split
     * @param keepList
     *            return argument
     * @param splitList
     *            return argument
     */
    void computeSplit(int childSize, List<String> knownKeep,
        List<String> knownSplit, CreatureInfoList keepList,
        CreatureInfoList splitList)
    {

        List<List<String>> possibleSplits = findAllPossibleSplits(childSize,
            knownKeep, knownSplit);

        List<String> splitoffNames = chooseCreaturesToSplitOut(possibleSplits);

        // We now know how we want to split, caculate certainty and
        // make the new creatureInfoLists
        for (Iterator<CreatureInfo> it = creatures.iterator(); it.hasNext();)
        {
            CreatureInfo ci = it.next();
            String name = ci.getName();
            CreatureInfo newinfo = new CreatureInfo(name, false, true);
            if (splitoffNames.contains(name))
            {
                splitList.add(newinfo);
                splitoffNames.remove(name);
                // If in knownSplit, set certain
                if (knownSplit.contains(name))
                {
                    knownSplit.remove(name);
                    newinfo.setCertain(true);
                }
            }
            else
            {
                keepList.add(newinfo);
                // If in knownKeep, set certain
                if (knownKeep.contains(name))
                {
                    knownKeep.remove(name);
                    newinfo.setCertain(true);
                }
            }
        }
    }

    /**
     * Perform the initial split of a stack, and creater the children
     * 
     * @param childSize
     * @param otherMarkerId
     * @param turn
     */
    void split(int childSize, String otherMarkerId, int turn)
    {
        assert creatures.size() <= 8 : "> 8 creatures in legion";
        assert !hasSplit() : "use reSplit to recalculate old splits";

        List<String> knownKeep = new ArrayList<String>();
        List<String> knownSplit = new ArrayList<String>();

        CreatureInfoList keepList = new CreatureInfoList();
        CreatureInfoList splitList = new CreatureInfoList();

        computeSplit(childSize, knownKeep, knownSplit, keepList, splitList);

        child1 = new PredictSplitNode(markerId, turn, keepList, this);
        child2 = new PredictSplitNode(otherMarkerId, turn, splitList, this);
        childSize2 = child2.getHeight();
    }

    /**
     * Recompute the split of a stack, taking advantage of any information
     * potentially gained from the children
     * 
     */
    void reSplit()
    {
        assert creatures.size() <= 8 : "> 8 creatures in legion";

        List<String> knownKeep = child1.getCertainAtSplitOrRemovedCreatures()
            .getCreatureNames();
        List<String> knownSplit = child2.getCertainAtSplitOrRemovedCreatures()
            .getCreatureNames();

        CreatureInfoList keepList = new CreatureInfoList();
        CreatureInfoList splitList = new CreatureInfoList();

        computeSplit(childSize2, knownKeep, knownSplit, keepList, splitList);

        // we have now predicted the split we need to inform the children
        child1.updateInitialSplitInfo(keepList);
        child2.updateInitialSplitInfo(splitList);
    }

    /**
     * This takes potentially new information about the legion's composition at
     * split and applies the later changes to the legion to get a new predicton
     * of contents. It then recursively resplits.
     * 
     * @param newList
     */
    void updateInitialSplitInfo(CreatureInfoList newList)
    {
        // TODO Check if any new information was gained and stop if not.
        newList.addAll(getAfterSplitCreatures());
        for (Iterator<CreatureInfo> it = getRemovedCreatures().iterator(); it
            .hasNext();)
        {
            newList.remove(it.next());
        }
        setCreatures(newList);

        // update children if we have any
        if (hasSplit())
        {
            reSplit();
        }
    }

    /**
     * Recombine this legion and other, because it was not possible to move.
     * They must share a parent. If either legion has the parent's markerId,
     * then that legion will remain. Otherwise this legion will remain. Also
     * used to undo splits.
     */
    void merge(PredictSplitNode other)
    {
        if (this.parent == other.parent)
        {
            assert getMarkerId().equals(parent.getMarkerId())
                || other.getMarkerId().equals(parent.getMarkerId()) : "None of the legions carry the parent maker";

            // this is regular merge, cancel split.
            parent.clearChildren();
        }
        else
        {
            // this must be a merge of a 3-way split
            // origNode -- father -- nodeB
            // \ nodeA \ third
            // this transforms into
            // origNode -- (nodeA + nodeB)
            // \ third

            PredictSplitNode nodeA = null;
            PredictSplitNode nodeB = null;

            if (this.parent == other.parent.parent)
            {
                nodeA = this;
                nodeB = other;
            }
            else if (this.parent.parent == other.parent)
            {
                nodeA = other;
                nodeB = this;
            }
            // check we got a valid combination, otherwise the nodes are not set
            assert (nodeA != null) && (nodeB != null) : "Illegal merge";

            PredictSplitNode father = nodeB.parent;
            PredictSplitNode origNode = nodeA.parent;
            PredictSplitNode thirdLegion;
            if (nodeB == father.child1)
            {
                thirdLegion = father.child2;
            }
            else
            {
                thirdLegion = father.child1;

            }

            if (origNode.getMarkerId().equals(thirdLegion.getMarkerId()))
            {
                // third is carries the original marker and nodeA is then
                // the splitoff from the origNode, just add creatures from nodeB
                nodeA.creatures.addAll(nodeB.creatures);
                origNode.childSize2 = nodeA.getHeight();
                origNode.child1 = thirdLegion;
            }
            else
            {
                // attach thirdLegion as the split from the node, and
                // nodeA+nodeB as the keep
                origNode.child2 = thirdLegion;
                origNode.childSize2 = thirdLegion.getHeight();
                if (origNode.getMarkerId().equals(nodeA.getMarkerId()))
                {
                    nodeA.creatures.addAll(nodeB.creatures);
                    origNode.child1 = nodeA;
                }
                else
                {
                    nodeB.creatures.addAll(nodeA.creatures);
                    origNode.child1 = nodeB;
                }
            }
        }
    }

    void addCreature(String creatureName)
    {
        if (creatureName.startsWith("Titan"))
        {
            creatureName = "Titan";
        }
        assert getHeight() < 7 || child1 == null : "Tried adding to 7-high legion";
        CreatureInfo ci = new CreatureInfo(creatureName, true, false);
        creatures.add(ci);
    }

    void removeCreature(String creatureName)
    {
        if (creatureName.startsWith("Titan"))
        {
            creatureName = "Titan";
        }
        assert getHeight() > 0 : "Tried removing from 0-high legion";

        List<String> cnl = new ArrayList<String>();
        cnl.add(creatureName);
        revealCreatures(cnl);

        // Find the creature to remove
        Iterator<CreatureInfo> it = creatures.iterator();
        // We have already checked height>0, so taking next is ok.
        CreatureInfo ci = it.next();
        while (!(ci.isCertain() && ci.getName().equals(creatureName)))
        {
            assert it.hasNext() : "Tried to remove nonexistant creature";
            ci = it.next();
        }

        // Only need to track the removed creature for future parent split
        // predictions if it was here at the time of the split.
        if (ci.isAtSplit())
        {
            removed.add(ci);
        }
        it.remove();
    }

    void removeCreatures(List<String> creatureNames)
    {
        revealCreatures(creatureNames);
        for (Iterator<String> it = creatureNames.iterator(); it.hasNext();)
        {
            String name = it.next();
            removeCreature(name);
        }
    }

    public int compareTo(PredictSplitNode other)
    {
        return toString().compareTo(other.toString());
    }

    static List<String> subtractLists(List<String> big, List<String> little)
    {
        ArrayList<String> li = new ArrayList<String>(big);
        for (Iterator<String> it = little.iterator(); it.hasNext();)
        {
            li.remove(it.next());
        }
        return li;
    }

    /** Return the number of times name occurs in li */
    static int count(List<String> li, String name)
    {
        int num = 0;
        for (Iterator<String> it = li.iterator(); it.hasNext();)
        {
            String s = it.next();
            if (s.equals(name))
            {
                num++;
            }
        }
        return num;
    }

    /**
     * lili is a list of lists. Return the minimum number of times name appears
     * in any of the lists contained in lili.
     */
    static int minCount(List<List<String>> lili, String name)
    {
        int min = Integer.MAX_VALUE;
        for (Iterator<List<String>> it = lili.iterator(); it.hasNext();)
        {
            List<String> li = it.next();
            min = Math.min(min, count(li, name));
        }
        if (min == Integer.MAX_VALUE)
        {
            min = 0;
        }
        return min;
    }
}
