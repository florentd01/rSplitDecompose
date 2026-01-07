package Algorithm;

import Model.Cherry;
import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.ExperimentTool;
import utils.TreeUtils;
import utils.UndoMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static utils.ExperimentTool.problemInstanceWithRhoFromTreeStringArray;

public class FastApprox {

    Random randomizer;
    int counter;

    public FastApprox(Random randomizer) {
        this.randomizer = randomizer;
        counter = 0;
    }

    public int testsFastApprox(ProblemInstance pI) {
        //System.out.println("Starting approx");
        //pI.printTrees();
        for (Node component : pI.getF2().getComponents()) {

            if (!TreeUtils.validateSiblingRelation(component)){
                System.out.println("Break bad siblings in approx before cut");
            }
        }
        return fastApprox(0, pI);
    }



    public int fastApprox(int cuts, ProblemInstance pI){
        //pI.printTrees();
        counter++;
        if (pI.getF1().getLeavesByLabel().size() <= 2) {
            return cuts;
        } else {
            List<Node> cutChildren = approxCutChildren(pI);
            int j = 0;
            for (Node child : cutChildren) {
                Node parent = child.getParent();

                ApproxCut cut = new ApproxCut(child.getParent(), child, pI.getF2());

                int k = 0;
                for (Node component : pI.getF2().getComponents()) {
                    //System.out.println("checking component: " + k);
                    if (!TreeUtils.validateSiblingRelation(component)){
                        System.out.println("Break bad siblings in approx before cut");
                    }
                }


                cut.makeCut();

                for (Node component : pI.getF2().getComponents()) {
                    if (!TreeUtils.validateSiblingRelation(component)){
                        System.out.println("Break bad siblings in approx after cut");
                    }
                }
                cuts++;
//                System.out.println("After Cut before Norm");
//                pI.printTrees();
                //approxNormalizeTree(new UndoMachine(), pI);
                // TODO: makes sure to remove empty internal nodes as well
                j++ ;
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
            problemChild.setSibling(null);
        }

        public int getIndexInChildrenList() {
            return indexInChildrenList;
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 1000; i++) {

            String[] treeFilePaths = {"C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomTree.jar",
                    "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomRSPR.jar"};

            String[] treeGenArgs = new String[] {"100", "0", "50", "15"};

            String trees = ExperimentTool.generateTreePair(treeGenArgs[0], treeGenArgs[1], treeGenArgs[2], treeGenArgs[3], treeFilePaths, null);
//            String trees = "((((((19,80),(87,56)),((((1,55),13),(43,(26,49))),((60,(83,65)),14))),(((34,(3,(((71,18),24),67))),92),((47,4),((69,63),70)))),(((((39,51),(81,28)),25),((((32,99),29),78),8)),(((53,12),(77,7)),((23,73),46)))),((((((59,15),66),(41,(76,37))),(((68,(38,(90,30))),(75,(64,2))),(((9,58),48),98))),((((57,(84,36)),(54,74)),((((21,17),82),(79,(72,35))),(27,16))),((88,((62,11),96)),((97,50),22)))),(((((61,45),94),(44,31)),52),((((91,40),((86,(10,95)),85)),((5,93),(6,42))),((100,33),(89,20))))));\n" +
//                    "((((((62,11),58),((44,27),9)),48),98),((((((((1,55),13),(43,(49,(((((39,51),(81,28)),25),((((32,99),29),78),8)),26)))),14),(19,80)),(((34,3),92),((47,4),((63,(5,69)),(30,70))))),((((53,12),(77,7)),(23,73)),(60,(83,65)))),((((((59,15),(46,66)),(41,(76,37))),((68,38),(75,(64,2)))),((((57,36),(54,74)),(90,((79,(72,35)),(((21,17),82),16)))),((88,96),((97,50),22)))),(((((61,45),94),(84,31)),52),((((91,((((71,18),24),67),40)),((86,(10,95)),85)),(93,((87,56),(6,42)))),((100,33),(89,20)))))));";
            System.out.println(trees);

            String[] splitDecomposeArgs = new String[] {"decompose", "2", "approx"};
            ProblemInstance instance = problemInstanceWithRhoFromTreeStringArray(trees.split("\\r?\\n"));
            MAFSolver solver = new MAFSolver(instance, new Random(), splitDecomposeArgs);

            boolean works = solver.advancedSearch(15);

            if (works) {
                System.out.println("Run " + i + " works!");
                System.out.println("States explored: " + solver.getNumStates());
            }else {
                System.out.println("Run " + i + " doesn't work :(");
            }

//            Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//            Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");
//
//            TreeUtils.linkSiblings(F1);
//            TreeUtils.linkSiblings(F2);
//
//            TreeUtils.linkForests(F1, F2);
//
//            ProblemInstance pI = new ProblemInstance(F1, F2);
//            //pI.printTrees();
//            FastApprox test = new FastApprox(new Random(i));
//
//            test.approxNormalizeTree(new UndoMachine(), pI);
//
//
//            int approxVal = test.fastApprox(0, pI);
//            System.out.println(approxVal);
//            if (approxVal > 12) {
//                System.out.println("BIG PROBLEM DETECTED at i = " + i);
//                break;
//            }
        }
    }
}
