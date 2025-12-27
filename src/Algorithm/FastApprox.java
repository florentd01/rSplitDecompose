package Algorithm;

import Model.Cherry;
import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.UndoMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FastApprox {

    Random randomizer;

    public int fastApprox(int cuts, ProblemInstance pI){
        //pI.printTrees();
        if (pI.getF1().getLeavesByLabel().size() <= 2) {
            return cuts;
        } else {
            List<Node> cutChildren = approxCutChildren(pI);
            for (Node child : cutChildren) {
                Node parent = child.getParent();
                Algorithm.ApproxCut cut = new Algorithm.ApproxCut(child.getParent(), child, pI.getF2());
                cut.makeCut();
                cuts++;
//                System.out.println("After Cut before Norm");
//                pI.printTrees();
                //approxNormalizeTree(new UndoMachine(), pI);
                // TODO: makes sure to remove empty internal nodes as well
            }

            for (Node component : pI.getF2().getComponents()) {
                Forest.removeEmptyInternalNodes(component);
            }
            approxNormalizeTree(new UndoMachine(), pI);
//            System.out.println("CUTS DONE");
//            pI.printTrees();

            return fastApprox(cuts, pI);
        }
    }

    private List<Node> approxCutChildren(ProblemInstance pI) {
        List<Node> cutChildren = new ArrayList<>();
        List<Cherry> cherries = findCherries(pI);

        int index = randomizer.nextInt(cherries.size());
        Cherry cherry = cherries.get(index);
//        System.out.println("Selected Cherry: ");
//        System.out.println(cherry.getA().getLabel() + ", " + cherry.getB().getLabel());

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

        cutChildren.add(aInF2);
        cutChildren.add(bInF2);

        if (tempA.equals(tempB)) {
            if (depthA < depthB) {
                cutChildren.add(bInF2.getSibling());
            } else {
                cutChildren.add(aInF2.getSibling());
            }
        }



        return cutChildren;
    }


    public List<Cherry> findCherries(ProblemInstance pI) {
        Node root = pI.getF1().getComponent(0);
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



//    private List<Conflict> approxFindCutDifferentComponents(Node a, Node b, ProblemInstance pI) {
//        List<Conflict> conflicts = new ArrayList<>();
//        Conflict conflictA = new Conflict(a, b);
//        conflictA.addCut(new Cut(a.getParent(), a, pI.getF2(), a.getParent().getChildren().indexOf(a)));
//        conflicts.add(conflictA);
//
//        Conflict conflictB = new Conflict(a, b);
//        conflictB.addCut(new Cut(b.getParent(), b, pI.getF2(), b.getParent().getChildren().indexOf(b)));
//        conflicts.add(conflictB);
//
//        return conflicts;
//    }
//
//    /**
//     *
//     * @param a Node a
//     * @param b Node b
//     * @return List of cuts to be made for one step of fast 3 approx algorithm
//     */
//
//    private List<Cut> approxFindCutsSameComponent(Node a, Node b, ProblemInstance problemInstance) {
//        List<Cut> cuts = new ArrayList<>();
//        if (a == null || b == null) {
//            return cuts;
//        }
//
//        int da = a.getDepth(), db = b.getDepth();
//        int diff = da - db;
//        if (diff < 0) {
//            Node temp = a;
//            a = b;
//            b = temp;
//            diff = -diff;
//        }
//
//        cuts.add(new Cut(a.getParent(), a, problemInstance.getF2(), a.getParent().getChildren().indexOf(a)));
//        cuts.add(new Cut(b.getParent(), b, problemInstance.getF2(), b.getParent().getChildren().indexOf(b)));
//
//        int middleCutCounter = 0;
//        Cut middleCut = null;
//        while (diff-- != 0) {
//            if (middleCutCounter <1) {
//                middleCut = new Cut(a.getParent(), a.getSibling(), problemInstance.getF2(), a.getParent().getChildren().indexOf(a.getSibling()));
//            }
//            middleCutCounter++;
//            a = a.getParent();
//        }
//
//        while (a != null && b != null) {
//            if (a == b) break;
//            if (a.getParent() == b.getParent()) {
//                break;
//            } else {
//                if (a.getParent() == null || b.getParent() == null) {
//                    System.out.println("BIG PROBLEM DETECTED");
//                }
//                if (middleCutCounter < 1) {
//                    middleCut = new Cut(a.getParent(), a.getSibling(), problemInstance.getF2(), a.getParent().getChildren().indexOf(a.getSibling()));
//                }
//                a = a.getParent();
//                b = b.getParent();
//            }
//        }
//
//
//
//        cuts.addFirst(middleCut);
//
//        if (cuts.size() <= 3) {
//            return cuts;
//        } else {
//            return cuts.subList(0,4);
//        }
//    }

    private void approxNormalizeTree(UndoMachine um, ProblemInstance pI) {
        boolean didChange = false;
        Forest F2 = pI.getF2();
        Forest F1 = pI.getF1();
        do {
            didChange = false;
//            if (F2.getLeavesByLabel().get("430").getSibling() == null) {
//                System.out.println("MASSIVE PROBLEM");
//                //throw new RuntimeException();
//            }

//            if (!TreeUtils.checkDescendantRelations(F1)) {
//                System.out.println("F1 parent problem");
//            }
//            if (!TreeUtils.checkDescendantRelations(F2)) {
//                System.out.println("F2 parent problem");
//            }
//            if (problemInstance.getF1().getLeavesByLabel().size() != problemInstance.getF2().getLeavesByLabel().size()) {
//                System.out.println("Mismatch of leaves before suppress deg 2");
//            }
            boolean f1Suppress = F1.suppressDegreeTwo(um);
            boolean f2Suppress = F2.suppressDegreeTwo(um);
            if (f1Suppress || f2Suppress) {
                didChange = true;
            }

//            if (F2.getLeavesByLabel().get("430").getSibling() == null) {
//                TreeUtils.printAsciiTree(F2.getComponent(0));
//                TreeUtils.printAsciiTree(F2.getComponent(1));
//                System.out.println("MASSIVE PROBLEM");
//            }
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
//            if (F2.getLeavesByLabel().get("430").getSibling() == null) {
//                System.out.println("MASSIVE PROBLEM");
//            }
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
//            if (F2.getLeavesByLabel().get("430").getSibling() == null) {
//                System.out.println("MASSIVE PROBLEM");
//            }
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


    private class ApproxCut {
        private Forest forest;
        private final Node problemParent;
        private final Node problemChild;
        private int indexInChildrenList;

        public ApproxCut(Node problemParent, Node problemChild, Forest F) {
            this.problemParent = problemParent;
            this.problemChild = problemChild;
            this.forest = F;
            this.indexInChildrenList = problemParent.getChildren().indexOf(problemChild);
        }


        public void makeCut() {
            problemParent.getChildren().remove(problemChild);
            forest.addComponent(problemChild);
            problemChild.setRoot(true);
            problemChild.setParent(null);
            if (problemChild.getSibling() != null) {
                problemChild.getSibling().setSibling(null);
            }
        }

        public int getIndexInChildrenList() {
            return indexInChildrenList;
        }
    }
}
