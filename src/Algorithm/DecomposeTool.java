package Algorithm;

import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.FitchTool;
import utils.TreeUtils;
import utils.UndoMachine;

import java.util.*;

public class DecomposeTool {
    ProblemInstance mainProblem;
    List<ProblemInstance> subProblems = new ArrayList<>();

    int t;
    int totalStatesExplored;
    String[] args;
    Random randomizer;

    List<Cut> trueCurrentCut;

    public DecomposeTool(ProblemInstance pi, int t) {
        this.mainProblem = pi;
        this.t = t;
    }

    public DecomposeTool(ProblemInstance pi, int t, String[] args) {
        this.mainProblem = pi;
        this.t = t;
        this.args = args;
    }

    public DecomposeTool(ProblemInstance pi, int t, String[] args, Random randomizer, List<Cut> currentCuts) {
        this.mainProblem = pi;
        this.t = t;
        this.args = args;
        this.randomizer = randomizer;
        this.trueCurrentCut = currentCuts;
        //System.out.println(Arrays.toString(args));
        //System.out.println("New decomposer created");
    }

    /**
     * Implementation of decompose recursion
     *
     * @return true if solution exists false if it doesn't
     */
    public boolean decomposeProblem(int k){
        if (k < 2) {
            return false;
        }

        // set up subproblems             done
        buildSubInstances();
//        for (ProblemInstance pi : subProblems) {
//            System.out.println("Printing sub instance");
//            pi.printTrees();
//        }
        // step 1 solve all subproblems to t
        Integer[] resultsList = new Integer[subProblems.size()];
        int cutsAddedCounter = 0;

        for (int i = 0; i < this.subProblems.size(); i++) {
            ProblemInstance subInstance = this.subProblems.get(i);
            MAFSolver solver = new MAFSolver(subInstance, randomizer);
            for (int j = 1; j <= t; j++) {
                boolean works = solver.search(j);
                this.totalStatesExplored+= solver.getNumStates();
                if (works) {
                    //System.out.println("Sub instance solved in " + j + " cuts");
                    cutsAddedCounter += solver.getCurrentCuts().size();
                    trueCurrentCut.addAll(solver.getCurrentCuts());
                    resultsList[i] = j;
                    break;
                }
            }

//            for (Cut cut : trueCurrentCut) {
//                if (cut.getProblemParent().getId() ==1) {
//                    System.out.println("break");
//                }
//            }

            if (resultsList[i] == null) {
                resultsList[i] = t+1;
            }

        }
        // step 2 for all subproblems that could not be solved to t, solve to depth k-(sum of returns from solveToT for all other subproblems)
        String[] args = new String[1];

        subProblems = new ArrayList<>();
        buildSubInstances();

        //System.out.println(Arrays.toString(resultsList));

        int[] finalResults = new int[resultsList.length];
        for (int i = 0; i < resultsList.length; i++) {
            if (resultsList[i] > t) {
                MAFSolver solver = new MAFSolver(subProblems.get(i), randomizer, args);
                int resultsSum = 0;
                for (int j = 0; j < resultsList.length; j++) {
                    if (j != i) {
                        resultsSum+= resultsList[j];
                    }
                }
                int newMaxDepth = k - resultsSum;

                if (newMaxDepth < 1) {
                    return false;
                }

                boolean isPossible = false;
                for (int j = 1; j <= newMaxDepth; j++) {
                    isPossible = solver.advancedSearch(j);
                    totalStatesExplored+= solver.getNumStates();
                    if (isPossible) {
                        cutsAddedCounter += solver.getCurrentCuts().size();
                        trueCurrentCut.addAll(solver.getCurrentCuts());
                        finalResults[i] = j;
                        break;
                    }
                }
                //isPossible = solver.advancedSearch(newMaxDepth, args);
                if (!isPossible) {
                    // step 3 if any of the subproblems from step 2 could not be solved returns false
                    trueCurrentCut.subList(trueCurrentCut.size()-cutsAddedCounter, trueCurrentCut.size()).clear();
                    return false;
                }

            } else {
                finalResults[i] = resultsList[i];
            }
        }
//        System.out.println("For budget of k in decompose");
//        System.out.println("Final result array: " + Arrays.toString(finalResults));
        if (Arrays.stream(finalResults).sum() <= k) {
            return true;
        } else {
            trueCurrentCut.subList(trueCurrentCut.size()-cutsAddedCounter, trueCurrentCut.size()).clear();
            return false;
        }



    }

    public void buildSubInstances() {
        // create copy of component, and copy of induced subtree;
        // link the 2 new forests
        // create problem instances and add to subProblems
        List<Set<String>> leafLabelsF2 = mainProblem.F2.getLeafLabelsList();
        List<Node> componentsInF2 = mainProblem.getF2().getComponents();

//        System.out.println("Size of F2");
//        System.out.println(componentsInF2.size());

//        mainProblem.printTrees();


        // TODO: PROBLEM WITH DECOMPOSE
        for (int i = 0; i < componentsInF2.size(); i++) {
            subProblems.add(mainProblem.makeSubProblem(componentsInF2.get(i), leafLabelsF2.get(i)));

            //subProblems.get(i).printTrees();
        }

    }


    /**
     * method for generating subtree on T induced by the taxa of single component in F
     * @param T forest
     * @param leafSet set of taxa from component in F
     *
     * @return root node that is the root of induced subtree
     */
    public Forest induceSubtree(Forest T, Set<String> leafSet) {
        Node highestCommonAncestor = TreeUtils.findDeepestCommonAncestor(T.getComponent(0), leafSet, T.getLeavesByLabel());
        Node newRoot = new Node(highestCommonAncestor, leafSet);
        //normalize newRoot
        List<Node> component = new ArrayList<>();
        component.add(newRoot);
        Forest subF = new Forest(component);
        subF.suppressDegreeTwo(new UndoMachine());
        return subF;
    }


    /**
     * checks for solution for individual sub problem to given depth
     *
     * @param t determines to what depth subproblems are searched
     * @param subProblem given subproblem from disjoint components in T and F
     *
     * @return smallest number of cuts to solve given sub problem. returns t + 1 if solution not possible in at most t cuts
     */
    public int solveToT(ProblemInstance subProblem, int t) {
        // create new maf solver
        // run loop from i = 1 to t for basic search
        //      return i if solution is found at i

        //      if no solution return t + 1
        MAFSolver subProblemSolver = new MAFSolver(subProblem);
        for (int i = 1; i <= t; i++) {
            boolean hasSolution = subProblemSolver.search(i);
            if (hasSolution) return i;
        }
        return t + 1;
    }

    public static void main(String[] args) {
        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");


        Forest F2 = Forest.readNewickFormat("((1,3),(2,4))");
        TreeUtils.linkSiblings(F2);
        TreeUtils.assignUniqueIds(F2.getComponent(0), 0);
        Forest c2 = Forest.readNewickFormat("((5,8),(7,6))");
        TreeUtils.linkSiblings(c2);
        TreeUtils.assignUniqueIds(c2.getComponent(0), 7);
        Forest c3 = Forest.readNewickFormat("((9,11),(10,12))");
        TreeUtils.linkSiblings(c3);
        TreeUtils.assignUniqueIds(c3.getComponent(0), 14);
        Forest c4 = Forest.readNewickFormat("((13,15),(14,16))");
        TreeUtils.linkSiblings(c4);
        TreeUtils.assignUniqueIds(c4.getComponent(0), 21);

        F2.addComponent(c2.getComponent(0));
        F2.addComponent(c3.getComponent(0));
        F2.addComponent(c4.getComponent(0));

        F2.getLeavesByLabel().putAll(c2.getLeavesByLabel());
        F2.getLeavesByLabel().putAll(c3.getLeavesByLabel());
        F2.getLeavesByLabel().putAll(c4.getLeavesByLabel());

        TreeUtils.linkSiblings(F1);

        TreeUtils.linkForests(F1, F2);

        ProblemInstance pi = new ProblemInstance(F1, F2);
        pi.printTrees();

        String[] arguments = new String[2];
        arguments[0] = "split-decompose";
        arguments[1] = "2";

        MAFSolver solver = new MAFSolver(pi, new Random(), arguments);

        if (solver.advancedSearch(8)) {
            System.out.println("solution found");
        } else {
            System.out.println("no solution");
        }

        Forest F2origin = pi.getOriginalF2();

        System.out.println("Current cuts");
        System.out.println(solver.getCurrentCuts());

        F2origin.printForest();

        TreeUtils.applySolution(F2origin, solver.getCurrentCuts());

        F2origin.printForest();
        List<String> newickComponents = F2origin.toNewickList();
        for (String s : newickComponents) {
            System.out.println(s);
        }





//        pi.printTrees();
//
//        pi.getOriginalF2().printForest();
//        for (Node leaf : pi.getOriginalF2().getLeavesByLabel().values()) {
//            System.out.println("Leaf label and id");
//            System.out.println(leaf.getLabel());
//            System.out.println(leaf.getId());
//        }


//        System.out.println("ids in order for f2");
//        for (Node comp : pi.getF2().getComponents()) {
//            TreeUtils.printNodeIds(comp);
//        }
//
//        System.out.println("Ids in order for f2original");
//        for (Node comp : pi.getOriginalF2().getComponents()) {
//            TreeUtils.printNodeIds(comp);
//        }


//        if (solver.advancedSearch(3, new String[0])) {
//            System.out.println("works");
//        } else {
//            System.out.println("doesnt");
//        }

        FitchTool fitch = new FitchTool(pi);
        System.out.println(fitch.getPScore());
//        if (solver.advancedSearch(8, new String[0])) {
//            System.out.println("works in 8 cuts");
//        } else {
//            System.out.println("doesnt");
//        }








//        DecomposeTool decomposeTool = new DecomposeTool(pi, 1);
//
//        //decomposeTool.buildSubInstances();
//        if (decomposeTool.decomposeProblem(8)) {
//            System.out.println("decompose works");
//        } else {
//            System.out.println("doesnt work");
//        }

    }

}
