package Algorithm;

import Model.*;
import utils.ExperimentTool;
import utils.TreeUtils;
import utils.UndoMachine;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static utils.ExperimentTool.readProblemInstanceFromFileWithRho;

public class MCTSSolver {
    /**
     * needs:
     * way to save states: map that maps from List<Integer> where each entry is the parent of a cut to
     *                      double which is the probability of choosing this set of cuts
     * */
    private double C;
    private ProblemInstance problemInstance;
    private MCTSNode rootMCTSNode;
    private int globalUpperBound;
    private List<Conflict> bestConflictSequence;
    private Random random;

    private String[] args;

    /// maps states represented by lists of cuts to probabilities of selecting each state
    /// Unneeded
    private Map<List<Integer>, Double> probabilityTable;

    public MCTSSolver(ProblemInstance pI, double C, String[] args, Random random) {
        this.problemInstance = pI;
        probabilityTable = new HashMap<>();
        rootMCTSNode = new MCTSNode();
        globalUpperBound = Integer.MAX_VALUE;
        bestConflictSequence = new ArrayList<>();
        this.C = C;
        this.args = args;
        this.random = random;
    }

    public int monteCarloSearch(int nIterations) {
        int bestCost = Integer.MAX_VALUE;
        normalizeTree(new UndoMachine());

        for (int i = 0; i < nIterations; i++) {
            if (i % 100 == 0) {
                System.out.println("run # " + i);
                System.out.println("Best Upper Bound: " + bestCost);
            }

            int rolloutVal = rollout(rootMCTSNode);
            if (rolloutVal < bestCost) {
                bestCost = rolloutVal;
            }
        }
        return bestCost;
    }

    public int findUpperBound(int nIterations) {
        // algorithm
        normalizeTree(new UndoMachine());

        for (int i = 0; i < nIterations; i++) {
            System.out.println("run # " + i);
            System.out.println("Best Upper Bound: " + globalUpperBound);
            //selection
            MCTSNode node = selection();
            MCTSNode expandedChild = expansion(node);

            int rolloutCost = rollout(expandedChild);

            backProp(expandedChild, rolloutCost);
        }

        return globalUpperBound;
    }

    // moved to MCTSNode internal method
    private double UCT(MCTSNode node) {
        return -node.bestCost + C * Math.sqrt(Math.log(node.parent.visits)/node.visits);
    }

    // unneeded
//    public ProblemInstance rebuildFromCuts() {
//        return null;
//    }


    private MCTSNode selection() {
        MCTSNode node = rootMCTSNode;
        // while node is not terminal and fully expanded
        while (node.isFullyExpanded() && !node.isTerminal()) {
            // argmax over UCT
            double bestUCB = Integer.MIN_VALUE;
            List<MCTSNode> children = node.children;
            for (MCTSNode child: children) {
                double childUCB = child.UCB();
                if (childUCB > bestUCB) {
                    bestUCB = childUCB;
                    node = child;
                }
            }
        }
        return node;
    }

    // TODO: implement expansion
    private MCTSNode expansion(MCTSNode node) {
        if (node.isTerminal) {
            return node;
        }

        if (node.unexpandedConflicts.isEmpty()) {
            node.computeConflicts();
        }

        Conflict conflict = node.unexpandedConflicts.removeFirst();
        MCTSNode child = new MCTSNode();
        child.conflictsSoFar = new ArrayList<>(node.conflictsSoFar);
        child.conflictsSoFar.add(conflict);

        node.children.add(child);
        child.parent = node;

        return child;
    }

    private int rollout(MCTSNode node) {
        int cutCount = 0;
        for (Conflict con : node.conflictsSoFar) {
            cutCount+= con.getCuts().size();
        }
        UndoMachine um = new UndoMachine();
        ProblemInstance pI = node.rebuildFromConflicts(um);

        List<Conflict> rolloutConflicts = new ArrayList<>();
        while (pI.getF1().getLeavesByLabel().size() > 2) {
            // do rollout
            FastApprox approxMachine = new FastApprox(new Random());
            int lowerBound = approxMachine.fastApprox(0, new ProblemInstance(problemInstance)) / 3;
            if (cutCount + lowerBound >= globalUpperBound) {
                um.undoAll();
                return globalUpperBound;
            }


            List<Cherry> cherries = findCherries();
            int index = random.nextInt(cherries.size());
            Cherry cherry = cherries.get(index);

            // TODO: improve to add split/decompose and other speed ups
            List<Conflict> cherryConflicts = findCherryConflicts(cherry);

            int conflictIndex = random.nextInt(cherryConflicts.size());
            Conflict conflict = cherryConflicts.get(conflictIndex);
            cutCount+= conflict.getCuts().size();
            rolloutConflicts.add(conflict);
            for (Cut cut : conflict.getCuts()) {
                cut.makeCut();
                um.addEvent(um.new MakeCut(cut, problemInstance.getF2()));
            }
            normalizeTree(um);
        }

        if (cutCount < globalUpperBound) {
            globalUpperBound = cutCount;
            bestConflictSequence = new ArrayList<>(node.conflictsSoFar);
            bestConflictSequence.addAll(rolloutConflicts);
        }

        um.undoAll();
        return cutCount;
    }

    private Result rolloutSplitDecompose(MCTSNode headNode) {

        return null;
    }

    // TODO: implement backprop
    private void backProp(MCTSNode node, int rolloutCost) {
        while (node != null) {
            node.visits++;
            node.bestCost = Math.min(node.bestCost, rolloutCost);
            node = node.parent;
        }
    }

    public List<Cherry> findCherries() {
        Node root = problemInstance.getF1().getComponent(0);
        List<Cherry> cherries = new ArrayList<>();
        if (root == null) return cherries;


        collectCherries(root, cherries);
        return cherries;
    }


    private void collectCherries(Node node, List<Cherry> cherries) {
        if (node == null || (node.isLeaf() && node.getChildren().isEmpty())) return;

        List<Node> children = node.getChildren();
        if (children.size() == 2) {
            Node a = children.getFirst();
            Node b = children.getLast();

            if (a.isLeaf() && b.isLeaf()) {
                cherries.add(new Cherry(a, b));
            }
        }

        for (Node child : children) {
            collectCherries(child, cherries);
        }
    }

    public List<Conflict> findCherryConflicts(Cherry cherry) {
        List<Conflict> conflictList = new ArrayList<>();
        Node a = cherry.getA();
        Node b = cherry.getB();

        Node aInF2 = a.getTwin();
        Node bInF2 = b.getTwin();

        Node tempA = aInF2;
        Node tempB = bInF2;
        int depthA = 0;
        int depthB = 0;

        while (!tempA.isRoot()) {
            depthA++;
            tempA = tempA.getParent();
        }

        while (!tempB.isRoot()) {
            depthB++;
            tempB = tempB.getParent();
        }

        aInF2.setDepth(depthA);
        bInF2.setDepth(depthB);

        if (tempA.equals(tempB)){
            conflictList.addAll(findCutsSameComponent(aInF2, bInF2));
        } else {
            conflictList.addAll(findCutDifferentComponents(aInF2, bInF2));
        }
        return conflictList;
    }

    private List<Conflict> findCutDifferentComponents(Node a, Node b) {
        List<Conflict> conflicts = new ArrayList<>();
        Conflict conflictA = new Conflict(a, b);
        conflictA.addCut(new Cut(a.getParent(), a, problemInstance.getF2(), a.getParent().getChildren().indexOf(a)));
        conflicts.add(conflictA);

        Conflict conflictB = new Conflict(a, b);
        conflictB.addCut(new Cut(b.getParent(), b, problemInstance.getF2(), b.getParent().getChildren().indexOf(b)));
        conflicts.add(conflictB);

        return conflicts;
    }

    public List<Conflict> findCutsSameComponent(Node a, Node b) {
        List<Conflict> conflicts = new ArrayList<>();
        if (a == null || b == null) {
            return conflicts;
        }

        int da = a.getDepth(), db = b.getDepth();
        int diff = da - db;
        if (diff < 0) {
            Node temp = a;
            a = b;
            b = temp;
            diff = -diff;
        }

        Conflict conflictA = new Conflict(a, b);
        conflictA.addCut(new Cut(a.getParent(), a, problemInstance.getF2(), a.getParent().getChildren().indexOf(a)));

        Conflict conflictB = new Conflict(a, b);
        conflictB.addCut(new Cut(b.getParent(), b, problemInstance.getF2(), b.getParent().getChildren().indexOf(b)));

        Conflict conflictMiddle = new Conflict(a, b);
        while (diff-- != 0) {
            Cut cut = new Cut(a.getParent(), a.getSibling(), problemInstance.getF2(), a.getParent().getChildren().indexOf(a.getSibling()));
            a = a.getParent();
            conflictMiddle.addCut(cut);
        }

        while (a != null && b != null) {
            if (a == b) break;
            if (a.getParent() == b.getParent()) {
                break;
            } else {
                if (a.getParent() == null || b.getParent() == null) {
                    System.out.println("BIG PROBLEM DETECTED");
                }
                Cut aCut = new Cut(a.getParent(), a.getSibling(), problemInstance.getF2(), a.getParent().getChildren().indexOf(a.getSibling()));
                Cut bCut = new Cut(b.getParent(), b.getSibling(), problemInstance.getF2(), b.getParent().getChildren().indexOf(b.getSibling()));
                conflictMiddle.addCut(aCut);
                conflictMiddle.addCut(bCut);

                a = a.getParent();
                b = b.getParent();
            }
        }

        if (conflictMiddle.getCuts().size() > 1) {
            conflicts.add(conflictA);
            conflicts.add(conflictB);
        }

        conflicts.add(conflictMiddle);
        return conflicts;
    }

    private void normalizeTree(UndoMachine um) {
        boolean didChange = false;
        Forest F2 = problemInstance.getF2();
        Forest F1 = problemInstance.getF1();
        do {
            didChange = false;
            boolean f1Suppress = F1.suppressDegreeTwo(um);
            boolean f2Suppress = F2.suppressDegreeTwo(um);
            if (f1Suppress || f2Suppress) {
                didChange = true;
            }
            if (F2.reduceCommonCherries(F2.getTwin(), um)) {
                didChange = true;
            }
            if (F2.deleteSingletons(um)) {
                didChange = true;
            }
        } while (didChange);

    }


    private class MCTSNode {
        List<Conflict> conflictsSoFar;
        MCTSNode parent;
        List<MCTSNode> children;
        int visits;
        int bestCost;
        List<Conflict> unexpandedConflicts;

        boolean isTerminal;
        boolean initialized;

        MCTSNode() {
            conflictsSoFar = new ArrayList<>();
            children = new ArrayList<>();
            unexpandedConflicts = new ArrayList<>();
            this.bestCost = Integer.MAX_VALUE;
        }

        public boolean isFullyExpanded(){
            return unexpandedConflicts.isEmpty() && !children.isEmpty();
        }

        public ProblemInstance rebuildFromConflicts(UndoMachine um) {
            Forest F2 = problemInstance.getF2();
            for (Conflict conflict : conflictsSoFar) {
                for (Cut cut : conflict.getCuts()) {
                    cut.makeCut();
                    um.addEvent(um.new MakeCut(cut, F2));
                }
                normalizeTree(um);
            }
            this.isTerminal = F2.getLeavesByLabel().size() <= 2;
            return problemInstance;
        }

        public boolean isTerminal() {
            if (!initialized) {
                Forest F2 = problemInstance.getF2();
                UndoMachine um = new UndoMachine();
                for (Conflict conflict : conflictsSoFar) {
                    for (Cut cut : conflict.getCuts()) {
                        cut.makeCut();
                        um.addEvent(um.new MakeCut(cut, F2));
                    }
                    normalizeTree(um);
                }
                this.isTerminal = F2.getLeavesByLabel().size() <= 2;
                um.undoAll();
            }
            return isTerminal;
        }



        /// TODO: sets up unexpandedConflicts
        public void computeConflicts(){
            UndoMachine um = new UndoMachine();
            rebuildFromConflicts(um);
            Cherry cherry = findCherries().getFirst();
            this.unexpandedConflicts = findCherryConflicts(cherry);
            um.undoAll();
        }

        public double UCB() {
            return -this.bestCost + C * Math.sqrt(Math.log(parent.visits)/ this.visits);
        }
    }

    public static void main(String[] args) throws IOException {
        File treeFile = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\trees2_48rspr.txt");

        ProblemInstance pi = readProblemInstanceFromFileWithRho(treeFile);
//        Forest F1 = Forest.readNewickFormat("((((((31,82),((97,41),47)),(((88,43),(94,(55,(24,78)))),(((91,33),65),((36,79),5)))),((((6,90),95),((44,70),(((22,23),50),18))),((((60,54),(86,77)),98),(((64,46),67),57)))),((((12,((83,28),68)),(2,29)),(62,(92,72))),((((11,89),80),(15,(53,25))),((19,4),(((1,84),73),((58,39),37)))))),(((((((63,(75,14)),(85,96)),26),(81,59)),((17,34),(30,21))),((40,(13,(99,35))),((((93,71),74),7),((56,48),((32,(76,38)),45))))),(((9,(42,16)),((100,(10,87)),8)),(((3,69),(66,20)),((51,(52,27)),(61,49))))))");
//        Forest F2 = Forest.readNewickFormat("(((((31,82),(47,41)),(((88,43),(94,(55,(24,78)))),(((91,33),65),((36,79),5)))),(((((60,54),(86,77)),98),((64,46),67)),((44,70),(18,((85,(96,((63,(14,(((6,90),95),75))),(17,(26,((81,59),((25,(53,15)),((30,21),34)))))))),(50,(10,(22,23)))))))),((((2,29),(57,(12,((83,28),68)))),(62,(92,(97,72)))),(((19,4),(((84,((100,(87,(8,((9,(42,16)),((((3,69),(66,(89,20))),((51,(52,27)),(61,49))),((40,(13,(99,(80,35)))),((((93,71),74),7),((56,48),((32,(76,38)),45))))))))),1)),73),((58,39),37))),11)));");
//
//
//        TreeUtils.linkSiblings(F1);
//        TreeUtils.linkSiblings(F2);
//
//        TreeUtils.linkForests(F1, F2);
//        ProblemInstance pi = new ProblemInstance(F1, F2);
        pi.printTrees();
        MCTSSolver sol = new MCTSSolver(pi, 8, new String[2], new Random());
        //System.out.println(sol.findUpperBound(500));
        System.out.println(sol.monteCarloSearch(10000));
    }
}
