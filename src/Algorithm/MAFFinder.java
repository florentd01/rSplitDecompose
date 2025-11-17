
package Algorithm;

import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.FitchTool;
import utils.TreeUtils;
import utils.UndoMachine;

import java.util.*;

public class MAFFinder {
    private ProblemInstance problemInstance;
    private int numStates = 0;

    public MAFFinder(ProblemInstance problemInstance) {
        this.problemInstance = problemInstance;
    }


    public boolean search(int k) {
        normalizeTree(new UndoMachine());
        problemInstance.printTrees();

        return searchHelper(k);
    }

    private void printNumStates() {
        System.out.println("Explored " + numStates + " states");
    }

    public boolean searchHelper(int k) {
        System.out.println("searching at k = " + k);
        numStates++;
        if (k == 2) {
            System.out.println("breakpoint");
        }
        if (k < 0) {
            return false;
        } else if (problemInstance.getF1().getLeavesByLabel().size() <= 2) {
            return true;
        } else {
            boolean isPossible = false;

            for (Conflict conflict : findConflicts()) {

                UndoMachine um = new UndoMachine();
                for (Cut cut : conflict.getCuts()) {
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF1())) {
//                        System.out.println("F1 parent problem");
//                    }
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF2())) {
//                        System.out.println("F2 parent problem");
//                    }
//                    if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                        System.out.println("Mismatch of leaves in search before cut");
//                    }
                    um.addEvent(um.new MakeCut(cut, problemInstance.getF2()));
                    cut.makeCut();
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF1())) {
//                        System.out.println("F1 parent problem");
//                    }
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF2())) {
//                        System.out.println("F2 parent problem");
//                    }
//                    if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                        System.out.println("Mismatch of leaves in search after cut");
//                    }
                }
                normalizeTree(um);
//                problemInstance.printTrees();
                isPossible = searchHelper(k - conflict.getCuts().size());
                if (isPossible) {
                    return true;
                }
                um.undoAll();


            }
            return isPossible;

        }
    }

    public boolean searchHelperDecompose(int k) {
        System.out.println("searching at k = " + k);
        if (k < 0) {
            return false;
        } else if (problemInstance.getF1().getLeavesByLabel().size() <= 2) {
            return true;
        } else {

            FitchTool disjointChecker = new FitchTool(problemInstance);
            if (disjointChecker.getPScore() == problemInstance.getF2().getComponents().size()) {
                System.out.println("do decompose");
                return false;

            } else {
                boolean isPossible = false;

                for (Conflict conflict : findConflicts()) {

                    UndoMachine um = new UndoMachine();
                    for (Cut cut : conflict.getCuts()) {
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF1())) {
//                        System.out.println("F1 parent problem");
//                    }
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF2())) {
//                        System.out.println("F2 parent problem");
//                    }
//                    if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                        System.out.println("Mismatch of leaves in search before cut");
//                    }
                        um.addEvent(um.new MakeCut(cut, problemInstance.getF2()));
                        cut.makeCut();
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF1())) {
//                        System.out.println("F1 parent problem");
//                    }
//                    if (!TreeUtils.checkDescendantRelations(problemInstance.getF2())) {
//                        System.out.println("F2 parent problem");
//                    }
//                    if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                        System.out.println("Mismatch of leaves in search after cut");
//                    }
                    }
                    normalizeTree(um);
//                problemInstance.printTrees();
                    isPossible = searchHelper(k - conflict.getCuts().size());
                    if (isPossible) {
                        return true;
                    }
                    um.undoAll();


                }
                return isPossible;
            }
        }
    }



    private void normalizeTree(UndoMachine um) {
        boolean didChange = false;
        Forest F2 = problemInstance.getF2();
        Forest F1 = problemInstance.getF1();
        do {
            didChange = false;
//            if (!TreeUtils.checkDescendantRelations(F1)) {
//                System.out.println("F1 parent problem");
//            }
//            if (!TreeUtils.checkDescendantRelations(F2)) {
//                System.out.println("F2 parent problem");
//            }
//            if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                System.out.println("Mismatch of leaves before suppress deg 2");
//            }
            if (F1.suppressDegreeTwo(um) || F2.suppressDegreeTwo(um)) {
                didChange = true;
            }
//            if (!TreeUtils.checkDescendantRelations(F1)) {
//                System.out.println("F1 parent problem");
//            }
//            if (!TreeUtils.checkDescendantRelations(F2)) {
//                System.out.println("F2 parent problem");
//            }
//            if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                System.out.println("Mismatch of leaves after deg2 before redux cherries");
//            }
            if (F2.reduceCommonCherries(F2.getTwin(), um)) {
                didChange = true;
            }
//            if (!TreeUtils.checkDescendantRelations(F1)) {
//                System.out.println("F1 parent problem");
//            }
//            if (!TreeUtils.checkDescendantRelations(F2)) {
//                System.out.println("F2 parent problem");
//            }
//            if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                System.out.println("Mismatch of leaves after cherries before singletons");
//            }
            if (F2.deleteSingletons(um)) {
                didChange = true;
            }
//            if (!TreeUtils.checkDescendantRelations(F1)) {
//                System.out.println("F1 parent problem");
//            }
//            if (!TreeUtils.checkDescendantRelations(F2)) {
//                System.out.println("F2 parent problem");
//            }
//            if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                System.out.println("Mismatch of leaves after normalized done");
//            }
        } while (didChange);

    }

    public List<Conflict> findSingleConflict() {


        return null;
    }



    public List<Conflict> findConflicts() {
        List<Conflict> conflictList = new ArrayList<>();
        Set<Node> exploredNodes = new HashSet<>();
        for (Node leaf : problemInstance.getF1().getLeavesByLabel().values()) {
            if (!exploredNodes.contains(leaf)) {
                Node pairedNode = leaf.getSibling();
                exploredNodes.add(leaf);
                exploredNodes.add(pairedNode);

                if (!leaf.isRoot()) {
                    if (pairedNode != null && leaf.isLeaf() && pairedNode.isLeaf()) {
                        Node a = leaf.getTwin();
                        Node b = pairedNode.getTwin();
                        Node tempA = a;
                        Node tempB = b;
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

                        a.setDepth(depthA);
                        b.setDepth(depthB);

                        if (tempA.equals(tempB)){
                            conflictList.addAll(findCutsSameComponent(a, b));
                        } else {
                            conflictList.addAll(findCutDifferentComponents(a, b));
                        }

                    }
                }
            }
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
            Cut cut = new Cut(a.getParent(), a.getSibling(), problemInstance.getF2(), a.getParent().getChildren().indexOf(a));
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
                Cut aCut = new Cut(a.getParent(), a.getSibling(), problemInstance.getF2(), a.getParent().getChildren().indexOf(a));
                Cut bCut = new Cut(b.getParent(), b.getSibling(), problemInstance.getF2(), b.getParent().getChildren().indexOf(b));
                conflictMiddle.addCut(aCut);
                conflictMiddle.addCut(bCut);

                a = a.getParent();
                b = b.getParent();
            }
        }
        conflicts.add(conflictA);
        conflicts.add(conflictB);
        conflicts.add(conflictMiddle);





        return conflicts;
    }

    public static void test1(){
        Forest F1 = Forest.readNewickFormat("((((6,(28,12)),15),((7,(19,14)),4)),((((16,((23,1),30)),((22,8),20)),((25,13),(11,26))),(((5,(3,10)),(18,27)),(((29,21),(24,2)),(17,9)))))");
        //TreeUtils.printAsciiTree(F1.getComponent(0));
        Forest F2 = Forest.readNewickFormat("((((7,(6,19)),4),15),(((16,((23,(1,(11,26))),30)),((22,8),20)),(24,(18,(((((29,21),2),(17,9)),13),(((25,((5,(3,10)),14)),27),(28,12)))))))");
        TreeUtils.printAsciiTree(F2.getComponent(0));
        TreeUtils.linkForests(F1, F2);
        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);


        ProblemInstance instance = new ProblemInstance(F1, F2);
        MAFFinder algoBot = new MAFFinder(instance);


        boolean isPossible = algoBot.search(10);
        if (isPossible) {
            System.out.println("it works");
        } else {
            System.out.println("bing bong no luck this time");
        }

    }

    public static void testDecompose(){
        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
        Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");

        TreeUtils.printAsciiTree(F2.getComponent(0));
        TreeUtils.linkForests(F1, F2);
        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);

        ProblemInstance instance = new ProblemInstance(F1, F2);
        MAFFinder algoBot = new MAFFinder(instance);
        Node a = F2.getLeavesByLabel().get("7");
        Node b = F2.getLeavesByLabel().get("9");

        List<Conflict> conList = algoBot.findCutsSameComponent(a, b);
        for (Cut cut : conList.getLast().getCuts()) {
            System.out.println(cut.getProblemParent());
            System.out.println(cut.getProblemChild());
            cut.makeCut();
        }

        algoBot.normalizeTree(new UndoMachine());

        for (Node component : F2.getComponents()) {
            TreeUtils.printAsciiTree(component);
        }

        FitchTool fitch = new FitchTool(algoBot.problemInstance);
        for (Set<String> colourSet : fitch.getColours()) {
            System.out.println(colourSet);
        }

        ProblemInstance subProb = instance.makeSubProblem(instance.getF2().getComponent(2), fitch.getColours().get(2));

        TreeUtils.printAsciiTree(subProb.getF1().getComponent(0));
        TreeUtils.printAsciiTree(subProb.getF2().getComponent(0));

        MAFFinder subAlgoBot = new MAFFinder(subProb);

        if (subAlgoBot.search(1)) {
            System.out.println("found solution in 2");
        }
    }

    public static void main(String[] args) {
//        Forest F1 = Forest.readNewickFormat("((((6,(28,12)),15),((7,(19,14)),4)),((((16,((23,1),30)),((22,8),20)),((25,13),(11,26))),(((5,(3,10)),(18,27)),(((29,21),(24,2)),(17,9)))))");
//        Forest F2 = Forest.readNewickFormat("((((7,(6,19)),4),15),(((16,((23,(1,(11,26))),30)),((22,8),20)),(24,(18,(((((29,21),2),(17,9)),13),(((25,((5,(3,10)),14)),27),(28,12)))))))");
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");

        // True value = 2
        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
        Forest F2 = Forest.readNewickFormat("((((7,8),((2,(11,1)),(3,4))),(6,5)),((12,(10,9)),((14,13),(15,16))))");

//        Forest F1 = Forest.readNewickFormat("((1,2),(3,4))");
//        Forest F2 = Forest.readNewickFormat("((1,3),(4,2))");
        TreeUtils.linkForests(F1, F2);
        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);


        ProblemInstance instance = new ProblemInstance(F1, F2);
        MAFFinder algoBot = new MAFFinder(instance);


        boolean isPossible = algoBot.search(2);
        if (isPossible) {
            System.out.println("it works");
        } else {
            System.out.println("bing bong no luck this time");
        }



    }

}
