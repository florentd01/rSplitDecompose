
package Algorithm;

import Model.Cherry;
import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.DataTracker;
import utils.FitchTool;
import utils.TreeUtils;
import utils.UndoMachine;


import javax.net.ssl.SSLContext;
import java.util.*;


public class MAFSolver {
    private ProblemInstance problemInstance;
    private int numStates = 0;
    private Random randomizer;
    private String[] args;
    private int splitCounter;

    private int decomposeCounter;
    private List<Cut> currentCuts = new ArrayList<>();

    public DataTracker dt;

    private boolean trackData;
    private boolean useFastApprox;
    private boolean useWhiddemTrick;
    private boolean useParsimonyLowerBound;



    public MAFSolver(ProblemInstance problemInstance) {
        this.problemInstance = problemInstance;
        this.trackData = false;
    }

    public MAFSolver(ProblemInstance problemInstance, Random randomizer) {
        this.randomizer = randomizer;
        this.problemInstance = problemInstance;
        this.trackData = false;
    }

    public MAFSolver(ProblemInstance problemInstance, Random randomizer, String[] args) {
        this.randomizer = randomizer;
        this.problemInstance = problemInstance;
        this.args = args;
        this.trackData = false;
        if (Objects.equals(args[args.length - 1], "approx")) {
            this.useFastApprox = true;
        }
        //System.out.println(Arrays.toString(args));
    }

    public MAFSolver(ProblemInstance problemInstance, Random randomizer, String[] args, DataTracker dt) {
        this.randomizer = randomizer;
        this.problemInstance = problemInstance;
        this.args = args;
        //System.out.println(Arrays.toString(args));
        this.dt = dt;
        this.trackData = true;
        if (Objects.equals(args[2], "approx")) {
            this.useFastApprox = true;
        }
        if (Objects.equals(args[3], "3-2")) {
            this.useWhiddemTrick = true;
        }
        if (Objects.equals(args[4], "parsimony")) {
            this.useParsimonyLowerBound = true;
        }
    }

//    public MAFSolver(ProblemInstance problemInstance, Random randomizer, String[] args, DataTracker dt) {
//        this.randomizer = randomizer;
//        this.problemInstance = problemInstance;
//        this.args = args;
//        //System.out.println(Arrays.toString(args));
//        this.dt = dt;
//        this.trackData = true;
//        if (Objects.equals(args[2], "approx")) {
//            this.useFastApprox = true;
//        }
//        if (Objects.equals(args[3], "3-2")) {
//            this.useWhiddemTrick = true;
//        }
//        if (Objects.equals(args[4], "parsimony")) {
//            this.useParsimonyLowerBound = true;
//        }
//        //this.useWhiddemTrick = useWhiddemTrick;
//    }




    public Random getRandomizer() {
        return randomizer;
    }

    public List<Cut> getCurrentCuts() {
        return currentCuts;
    }

    public int getNumStates() {
        return numStates;
    }

    public int getDecomposeCounter() {
        return decomposeCounter;
    }

    public int getSplitCounter() {
        return splitCounter;
    }

    public boolean search(int k) {
        normalizeTree(new UndoMachine());
        //problemInstance.printTrees();
        return searchHelperV2(k);
    }



    public boolean advancedSearch(int k) {
        normalizeTree(new UndoMachine());
        //System.out.println("initial normalization");
        //problemInstance.printTrees();

//        switch (args[0]) {
//            case "split-decompose":
//                return searchHelperV2SplitDecompose(k, Integer.parseInt(args[1]));
//            default:
//                return searchHelperV2(k);
//        }
//        if (args[0] == null) {
//            System.out.println("break");
//        }



        return switch (args[0]) {
            case "split" -> searchOnlySplit(k);
            case "decompose" -> searchOnlyDecompose(k, Integer.parseInt(args[1]));
            case "split-decompose" -> searchHelperV2SplitDecompose(k, Integer.parseInt(args[1]));
            default -> searchHelperV2(k);
        };

    }







    public boolean searchOnlyDecompose(int k, int t) {
        numStates++;
        if (trackData) {
            dt.statesExplored++;
        }
        if (k < 0) {
            if (trackData) {
                dt.failedBranchCount++;
            }
            return false;
        } else if (problemInstance.getF1().getLeavesByLabel().size() <= 2) {
            return true;
        } else {
            if (useFastApprox) {
                FastApprox approxMachine = new FastApprox(new Random(1));
                if (approxMachine.testsFastApprox(new ProblemInstance(problemInstance)) / 3 > k) {
                    //System.out.println("Stop search do to lower bound from 3-approx");
                    if (trackData) {
                        dt.failedBranchCount++;
                    }
                    return false;
                }
            }
            FitchTool disjointChecker = new FitchTool(problemInstance);

            if (useParsimonyLowerBound) {
                //] break early if possible
            }

            List<Conflict> conflicts = new ArrayList<>();
            List<Cherry> cherries = findCherries();
            boolean skipDecompose = false;
            // todo: implement 3-2 reduction
            if (!useWhiddemTrick) {
                conflicts = findCherryConflicts(cherries.getFirst());
                // random cherry selection
//                    int index = randomizer.nextInt(cherries.size());
//                    conflicts = findCherryConflicts(cherries.get(index));

            } else {
                Map<Cherry, List<Conflict>> cherryMap = new HashMap<>();
                for (Cherry cherry : cherries) {
                    List<Conflict> branchingOptions = findCherryConflicts(cherry);

                    if (branchingOptions.size() == 1) {
                        conflicts = branchingOptions;
                        skipDecompose = true;
                        break;
                    }
                    cherryMap.put(cherry, branchingOptions);
                }

                if (conflicts.isEmpty()) {
                    conflicts = cherryMap.get(cherries.getFirst());
                }
            }

            if (problemInstance.getF2().getComponents().size() > 1 && disjointChecker.getPScore() == problemInstance.getF2().getComponents().size() - 1 && !skipDecompose) {
                // do decompose
                decomposeCounter++;
                //System.out.println("Before decompose");
                //printNumStates();
                DecomposeTool decomposeTool = new DecomposeTool(problemInstance, t, args, randomizer);
                long startTime = System.nanoTime();
                boolean isPossible = decomposeTool.decomposeProblem(k);
                long finishTime = System.nanoTime();
                double duration = (double) (finishTime - startTime) /1000000;

                if (isPossible) {
                    currentCuts.addAll(decomposeTool.getTrueCurrentCuts());
                }
                numStates+= decomposeTool.totalStatesExplored;

                if (trackData) {
                    dt.decomposeCounter++;
                    dt.statesExplored += decomposeTool.totalStatesExplored;
                    dt.decomposeTimes.add(duration);
                    if (dt.justSplit) {
                        dt.decomposeAfterSplitCounter++;
                    }
                }
                //System.out.println("After decompose");
                //printNumStates();
                return isPossible;
            }


         // HERE IS THE END
            boolean isPossible = false;
            for (Conflict conflict : conflicts) {
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
                    currentCuts.add(cut);
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
                //System.out.println("F2 has " + problemInstance.getF2().getComponents().size() + " components");
                if (problemInstance.getF2().getComponents().size() > 1 && conflict.getCuts().size() < 3) {
                    //problemInstance.printTrees();
                    //TreeUtils.printAsciiTree(problemInstance.getF1().getComponent(0));
//                    for (Node comp : problemInstance.getF2().getComponents()) {
//                        TreeUtils.printAsciiTree(comp);
//                    }
                    //System.out.println("pause at more than 1 component");
//                    if (problemInstance.getF2().getComponents().size() ==3){
//                        System.out.println("break");
//                    }
                }
//                problemInstance.printTrees();
                isPossible = searchOnlyDecompose(k - conflict.getCuts().size(), t);
                if (isPossible) {
                    return true;
                }
                int nCuts = conflict.getCuts().size();
                currentCuts.subList(currentCuts.size()-nCuts, currentCuts.size()).clear();
                um.undoAll();
            }
            return isPossible;
        }
    }

    public boolean searchOnlySplit(int k) {
        //        if (k%100 == 0) {
//            System.out.println("searching at k = " + k);
//        }

        if (trackData) {
            dt.statesExplored++;
        }

        numStates++;
//        if (k == 3) {
//            System.out.println("breakpoint");
//            problemInstance.printTrees();
//        }
        if (k < 0) {
            if (trackData) {
                dt.failedBranchCount++;
            }
            return false;
        } else if (problemInstance.getF1().getLeavesByLabel().size() <= 2) {
            return true;
        } else {
            if (useFastApprox) {
                FastApprox approxMachine = new FastApprox(new Random());
                if (approxMachine.fastApprox(0, new ProblemInstance(problemInstance)) / 3 > k) {
                    dt.failedBranchCount++;
                    return false;
                }
            }
            List<Conflict> conflicts;
            if (problemInstance.getF2().getComponents().size() > 1) {
                FitchTool disjointChecker = new FitchTool(problemInstance);
//                System.out.println("Fitch on " + problemInstance.getF2().getComponents().size() + " components");
//                System.out.println("Fitch result: " + disjointChecker.getPScore());
                if (disjointChecker.getPScore() != problemInstance.getF2().getComponents().size() - 1) {
                    // do split
                    splitCounter++;
                    long startTime = System.nanoTime();
                    SplitTool splitTool = new SplitTool(problemInstance, k, this.randomizer);
                    conflicts = splitTool.findSplitConflicts();
                    //System.out.println("# of splitting cores: " + conflicts.size());
                    long finishTime = System.nanoTime();
                    double duration = (double) (finishTime - startTime) /1000000;

                    if (trackData) {
                        dt.setSplitFlag(true);
                        dt.splitCounter++;
                        dt.splitTimes.add(duration);
                        dt.splittingCoreSizes.add((long) conflicts.size());

                    }
                } else {
                    if (trackData) {
                        dt.defaultWhiddenCounter++;
                    }
                    List<Cherry> cherries = findCherries();
                    int index = randomizer.nextInt(cherries.size());
                    conflicts = findCherryConflicts(cherries.get(index));
                }
            } else {
                if (trackData) {
                    dt.defaultWhiddenCounter++;
                }
                List<Cherry> cherries = findCherries();
                int index = randomizer.nextInt(cherries.size());
                conflicts = findCherryConflicts(cherries.get(index));
            } // HERE IS THE END

            boolean isPossible = false;
            for (Conflict conflict : conflicts) {
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
                    currentCuts.add(cut);
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
                //System.out.println("F2 has " + problemInstance.getF2().getComponents().size() + " components");
                if (problemInstance.getF2().getComponents().size() > 1 && conflict.getCuts().size() < 3) {
                    //problemInstance.printTrees();
                    //TreeUtils.printAsciiTree(problemInstance.getF1().getComponent(0));
//                    for (Node comp : problemInstance.getF2().getComponents()) {
//                        TreeUtils.printAsciiTree(comp);
//                    }
                    //System.out.println("pause at more than 1 component");
//                    if (problemInstance.getF2().getComponents().size() ==3){
//                        System.out.println("break");
//                    }
                }
//                problemInstance.printTrees();
                isPossible = searchOnlySplit(k - conflict.getCuts().size());
                if (isPossible) {
                    return true;
                }
                int nCuts = conflict.getCuts().size();
                currentCuts.subList(currentCuts.size()-nCuts, currentCuts.size()).clear();
                um.undoAll();
            }
            return isPossible;
        }
    }

    public boolean searchHelperV2(int k) {
        //System.out.println("searching at k = " + k);
        numStates++;
//        if (k == 3) {
//            System.out.println("breakpoint");
//            problemInstance.printTrees();
//        }

        if (trackData) {
            dt.statesExplored++;
        }
        if (k < 0) {
            if (trackData) {
                dt.failedBranchCount++;
            }
            return false;
        } else if (problemInstance.getF1().getLeavesByLabel().size() <= 2) {
            return true;
        } else {
            if (useFastApprox) {
                FastApprox approxMachine = new FastApprox(new Random());
                if (approxMachine.fastApprox(0, new ProblemInstance(problemInstance)) / 3 > k) {
                    dt.failedBranchCount++;
                    return false;
                }
            }
            boolean isPossible = false;
            List<Cherry> cherries = findCherries();
            // TODO: add 3-2 reduction prioritization

            List<Conflict> conflicts = new ArrayList<>();



            List<CherryConflicts> cherryConflicts = new ArrayList<>();
            for (int i = 0; i < cherries.size(); i++) {
                // loop over all cherries and get the conflicts (sets of cuts associated with one branch of the search tree)

                Cherry cherry = cherries.get(i);
                List<Conflict> branchingOptions = findCherryConflicts(cherry);

                if (!useWhiddemTrick) {
                    // if 3-2 reduction not in use, take the first cherry to branch on, no need to get the rest
                    conflicts = branchingOptions;
                    break;
                }

                // if the current cherry has only one conflict 3-2 reduction applies and exit the loop early
                if (branchingOptions.size() == 1) {
                    conflicts = branchingOptions;
                    break;
                }
                cherryConflicts.add(new CherryConflicts(cherry, branchingOptions, i));
            }


            if (conflicts.isEmpty()) {
                //int index = randomizer.nextInt(cherries.size());
                conflicts = cherryConflicts.getFirst().getConflicts();
            }


            //List<Conflict> conflicts = findCherryConflicts(cherries.get(index));
            for (Conflict conflict : conflicts) {
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
//                    if (problemInstance.getF2().getLeavesByLabel().get("430").getSibling() == null) {
//                        System.out.println("MASSIVE PROBLEM");
//                        throw new RuntimeException();
//                    }
                    List<Node> children = cut.getProblemParent().getChildren();
                    if (cut.getProblemParent().getId() == 1 && cut.getProblemChild().getLabel() == "5" ||cut.getProblemChild().getLabel() == "8"){
                        System.out.println("break for id = 1");
                    }





                    um.addEvent(um.new MakeCut(cut, problemInstance.getF2()));
                    currentCuts.add(cut);
                    cut.makeCut();
//                    if (problemInstance.getF2().getLeavesByLabel().get("430").getSibling() == null) {
//                        System.out.println("MASSIVE PROBLEM");
//                        //throw new RuntimeException();
//                    }
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
                isPossible = searchHelperV2(k - conflict.getCuts().size());
                if (isPossible) {
                    return true;
                }
                int nCuts = conflict.getCuts().size();
                currentCuts.subList(currentCuts.size()-nCuts, currentCuts.size()).clear();
                um.undoAll();
            }
            return isPossible;

        }
    }

    public boolean searchHelperV2SplitDecompose(int k, int t) {
        if (trackData) {
            dt.statesExplored++;
        }

        numStates++;

        if (k < 0) {
            if (trackData) {
                dt.failedBranchCount++;
            }
            return false;
        } else if (problemInstance.getF1().getLeavesByLabel().size() <= 2) {
            return true;
        } else {
            if (useFastApprox) {
                FastApprox approxMachine = new FastApprox(new Random());
                if (approxMachine.fastApprox(0, new ProblemInstance(problemInstance)) / 3 > k) {
                    dt.failedBranchCount++;
                    return false;
                }
            }
            List<Conflict> conflicts;


            if (problemInstance.getF2().getComponents().size() > 1) {
                FitchTool disjointChecker = new FitchTool(problemInstance);

                if (disjointChecker.getPScore() == problemInstance.getF2().getComponents().size() - 1) {
                    // do decompose
                    decomposeCounter++;
                    //System.out.println("Before decompose");
                    //printNumStates();
                    DecomposeTool decomposeTool = new DecomposeTool(problemInstance, t, args, randomizer);
                    long startTime = System.nanoTime();
                    boolean isPossible = decomposeTool.decomposeProblem(k);
                    long finishTime = System.nanoTime();
                    double duration = (double) (finishTime - startTime) /1000000;
                    if (isPossible) {
                        currentCuts.addAll(decomposeTool.getTrueCurrentCuts());
                    }
                    numStates += decomposeTool.totalStatesExplored;
                    if (trackData) {
                        dt.decomposeCounter++;
                        dt.statesExplored += decomposeTool.totalStatesExplored;
                        dt.decomposeTimes.add(duration);
                        if (dt.justSplit) {
                            dt.decomposeAfterSplitCounter++;
                        }
                    }
                    return isPossible;
                } else {
                    // do split
                    splitCounter++;

                    long startTime = System.nanoTime();
                    SplitTool splitTool = new SplitTool(problemInstance, k, this.randomizer);
                    conflicts = splitTool.findSplitConflicts();
                    long finishTime = System.nanoTime();
                    double duration = (double) (finishTime - startTime) /1000000;
                    if (trackData) {
                        dt.setSplitFlag(true);
                        dt.splitCounter++;
                        dt.splitTimes.add(duration);
                        dt.splittingCoreSizes.add((long) conflicts.size());

                    }
                }
            } else {
                if (trackData) {
                    dt.setSplitFlag(false);
                    dt.defaultWhiddenCounter++;
                }

                List<Cherry> cherries = findCherries();
                int index = randomizer.nextInt(cherries.size());
                conflicts = findCherryConflicts(cherries.get(index));
            } // HERE IS THE END

            boolean isPossible = false;
            for (Conflict conflict : conflicts) {
                UndoMachine um = new UndoMachine();
                for (Cut cut : conflict.getCuts()) {

                    um.addEvent(um.new MakeCut(cut, problemInstance.getF2()));
                    currentCuts.add(cut);
                    cut.makeCut();

                }
                normalizeTree(um);

                isPossible = searchHelperV2SplitDecompose(k - conflict.getCuts().size(), t);
                if (isPossible) {
                    return true;
                }
                int nCuts = conflict.getCuts().size();
                currentCuts.subList(currentCuts.size()-nCuts, currentCuts.size()).clear();
                um.undoAll();
            }
            return isPossible;
        }
    }

    public boolean searchHelperV2SplitDecomposeNew(int k, int t) {
        if (trackData) {
            dt.statesExplored++;
        }

        numStates++;

        if (k < 0) {
            if (trackData) {
                dt.failedBranchCount++;
            }
            return false;
        } else if (problemInstance.getF1().getLeavesByLabel().size() <= 2) {
            return true;
        } else {
            if (useFastApprox) {
                FastApprox approxMachine = new FastApprox(new Random());
                if (approxMachine.fastApprox(0, new ProblemInstance(problemInstance)) / 3 > k) {
                    dt.failedBranchCount++;
                    return false;
                }
            }
            List<Conflict> conflicts;

            if (problemInstance.getF2().getComponents().size() > 1) {
                FitchTool disjointChecker = new FitchTool(problemInstance);
                if (disjointChecker.getPScore() == problemInstance.getF2().getComponents().size() - 1) {
                    // do decompose
                    decomposeCounter++;
                    DecomposeTool decomposeTool = new DecomposeTool(problemInstance, t, args, randomizer);
                    long startTime = System.nanoTime();
                    boolean isPossible = decomposeTool.decomposeProblem(k);
                    long finishTime = System.nanoTime();
                    double duration = (double) (finishTime - startTime) /1000000;
                    if (isPossible) {
                        currentCuts.addAll(decomposeTool.getTrueCurrentCuts());
                    }
                    numStates += decomposeTool.totalStatesExplored;
                    if (trackData) {
                        dt.decomposeCounter++;
                        dt.statesExplored += decomposeTool.totalStatesExplored;
                        dt.decomposeTimes.add(duration);
                        if (dt.justSplit) {
                            dt.decomposeAfterSplitCounter++;
                        }
                    }

                    return isPossible;
                } else {
                    // do split
                    splitCounter++;

                    long startTime = System.nanoTime();
                    SplitTool splitTool = new SplitTool(problemInstance, k, this.randomizer);
                    conflicts = splitTool.findSplitConflicts();
                    long finishTime = System.nanoTime();
                    double duration = (double) (finishTime - startTime) /1000000;
                    if (trackData) {
                        dt.setSplitFlag(true);
                        dt.splitCounter++;
                        dt.splitTimes.add(duration);
                        dt.splittingCoreSizes.add((long) conflicts.size());

                    }
                }
            } else {
                if (trackData) {
                    dt.setSplitFlag(false);
                    dt.defaultWhiddenCounter++;
                }

                List<Cherry> cherries = findCherries();
                int index = randomizer.nextInt(cherries.size());
                conflicts = findCherryConflicts(cherries.get(index));
            }



            // Default Whidden
            boolean isPossible = false;
            for (Conflict conflict : conflicts) {
                UndoMachine um = new UndoMachine();
                for (Cut cut : conflict.getCuts()) {
                    um.addEvent(um.new MakeCut(cut, problemInstance.getF2()));
                    currentCuts.add(cut);
                    cut.makeCut();
                }
                normalizeTree(um);
                isPossible = searchHelperV2SplitDecompose(k - conflict.getCuts().size(), t);
                if (isPossible) {
                    return true;
                }
                int nCuts = conflict.getCuts().size();
                currentCuts.subList(currentCuts.size()-nCuts, currentCuts.size()).clear();
                um.undoAll();
            }
            return isPossible;
        }
    }

    public void printNumStates() {
        System.out.println("Explored " + numStates + " states");
    }





    private void normalizeTree(UndoMachine um) {
        boolean didChange = false;
        Forest F2 = problemInstance.getF2();
        Forest F1 = problemInstance.getF1();
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
            if (!useWhiddemTrick) {
                conflictList.addAll(findCutsSameComponentNoWhidden(aInF2, bInF2));
            } else {
                conflictList.addAll(findCutsSameComponent(aInF2, bInF2));
            }

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

    public List<Conflict> findCutsSameComponentNoWhidden(Node a, Node b) {
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
        MAFSolver algoBot = new MAFSolver(instance);


        boolean isPossible = algoBot.search(4);
        if (isPossible) {
            System.out.println("it works");
        } else {
            System.out.println("bing bong no luck this time");
        }

    }

    public static void test2() {
//        Forest F1 = Forest.readNewickFormat("((((6,(28,12)),15),((7,(19,14)),4)),((((16,((23,1),30)),((22,8),20)),((25,13),(11,26))),(((5,(3,10)),(18,27)),(((29,21),(24,2)),(17,9)))))");
//        Forest F2 = Forest.readNewickFormat("((((7,(6,19)),4),15),(((16,((23,(1,(11,26))),30)),((22,8),20)),(24,(18,(((((29,21),2),(17,9)),13),(((25,((5,(3,10)),14)),27),(28,12)))))))");
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");

        // True value = 2
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("((((7,8),((2,(11,1)),(3,4))),(6,5)),((12,(10,9)),((14,13),(15,16))))");
//        Forest F2origin = Forest.readNewickFormat("((((7,8),((2,(11,1)),(3,4))),(6,5)),((12,(10,9)),((14,13),(15,16))))");

        // true value = 4
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");
//        Forest F2origin = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");


        // trees3.txt true value 4
        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))));");
        Forest F2 = Forest.readNewickFormat("((((2,1),(((11,12),4),(8,(3,(6,5))))),7),((14,(10,9)),(13,(15,16))));");
        Forest F2origin = Forest.readNewickFormat("((((2,1),(((11,12),4),(8,(3,(6,5))))),7),((14,(10,9)),(13,(15,16))));");

        TreeUtils.assignUniqueIds(F2.getComponent(0), 0);

        TreeUtils.assignUniqueIds(F2origin.getComponent(0), 0);
//        Forest F1 = Forest.readNewickFormat("((1,2),(3,4))");
//        Forest F2 = Forest.readNewickFormat("((1,3),(4,2))");
        TreeUtils.linkForests(F1, F2);
        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);




        ProblemInstance instance = new ProblemInstance(F1, F2);
        instance.printTrees();
        MAFSolver algoBot = new MAFSolver(instance, new Random());


        boolean isPossible = algoBot.search(4);
        if (isPossible) {
            System.out.println("it works");
            System.out.println(algoBot.getNumStates());

        } else {
            System.out.println("bing bong no luck this time");
        }
        System.out.println(algoBot.getCurrentCuts());
        TreeUtils.printAsciiTree(instance.getOriginalF2().getComponent(0));


        TreeUtils.applySolution(F2origin, algoBot.getCurrentCuts());
        F2origin.printForest();
        List<String> newickComponents = F2origin.toNewickList();

        for (String s : newickComponents) {
            System.out.println(s);
        }


//        for (Cut cut : algoBot.getCurrentCuts()) {
//            Node parent = cut.getProblemParent();
//            System.out.println(parent);
//            System.out.println(parent.getId());
//            Node parentOrigin = idMap.get(parent.getId());
//            System.out.println(parentOrigin);
//            TreeUtils.printAsciiTree(parentOrigin);
//
//            int cutIndex = cut.getIndexInChildrenList();
//            Node cutChild = parentOrigin.getChildren().get(cutIndex);
//            parentOrigin.getChildren().remove(cutIndex);
//
//            System.out.println("parent in F2 after cut");
//            TreeUtils.printAsciiTree(parentOrigin);
//            System.out.println("child in f2 after cut");
//            TreeUtils.printAsciiTree(cutChild);
//
//
//
//            System.out.println("\n");
//
//        }
//        F2origin.suppressDegreeTwo(new UndoMachine());
//        TreeUtils.printAsciiTree(F2origin.getComponent(0));
    }




    public static void testDecompose(int i){
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("((((1,3),(2,4)),((5,8),(7,6))),(((9,11),(10,12)),((13,15),(14,16))))");



//        Forest F2 = Forest.readNewickFormat("((1,3),(2,4))");
//        TreeUtils.linkSiblings(F2);
//        Forest c2 = Forest.readNewickFormat("((5,8),(7,6))");
//        TreeUtils.linkSiblings(c2);
//        Forest c3 = Forest.readNewickFormat("((9,11),(10,12))");
//        TreeUtils.linkSiblings(c3);
//        Forest c4 = Forest.readNewickFormat("((13,15),(14,16))");
//        TreeUtils.linkSiblings(c4);
//
//        F2.addComponent(c2.getComponent(0));
//        F2.addComponent(c3.getComponent(0));
//        F2.addComponent(c4.getComponent(0));
//
//        F2.getLeavesByLabel().putAll(c2.getLeavesByLabel());
//        F2.getLeavesByLabel().putAll(c3.getLeavesByLabel());
//        F2.getLeavesByLabel().putAll(c4.getLeavesByLabel());

//        Forest F1 = Forest.readNewickFormat("((((6,(28,12)),15),((7,(19,14)),4)),((((16,((23,1),30)),((22,8),20)),((25,13),(11,26))),(((5,(3,10)),(18,27)),(((29,21),(24,2)),(17,9)))))");
//        Forest F2 = Forest.readNewickFormat("((((7,(6,19)),4),15),(((16,((23,(1,(11,26))),30)),((22,8),20)),(24,(18,(((((29,21),2),(17,9)),13),(((25,((5,(3,10)),14)),27),(28,12)))))))");

//        Forest F1 = Forest.readNewickFormat("(((((((((303,(126,157)),(488,246)),((140,(24,485)),113)),((((288,(471,6)),294),((90,348),155)),(((309,(450,210)),110),51))),(((394,(230,357)),271),(((208,395),(184,((462,135),118))),((194,14),(156,211))))),((((174,430),((207,125),((254,358),(145,(142,454))))),(((85,327),((163,412),144)),((336,(350,122)),94))),(((433,((137,(197,121)),273)),(((334,177),463),((380,335),229))),((((331,176),374),(284,27)),(384,(119,(116,363))))))),(((((((152,445),(434,(12,202))),(388,(421,223))),((70,87),117)),((((425,265),(132,460)),337),((372,91),252))),(((281,39),(189,(75,44))),((((369,7),435),(361,332)),((276,68),(20,470))))),(((((499,323),(295,322)),((439,191),(424,(149,209)))),(275,((457,59),(452,267)))),((((420,(136,356)),(96,(159,308))),(321,449)),(((378,(179,15)),(186,451)),(80,(175,399))))))),(((((490,((((11,199),456),84),((272,418),423))),((387,360),((242,447),((432,52),414)))),(((109,(280,256)),(((312,(383,150)),104),(196,270))),(((206,(290,396)),169),(398,351)))),((((218,(461,188)),((154,346),(183,57))),(((82,277),((487,255),56)),45)),((((498,472),389),((476,251),(264,386))),((492,333),((324,(329,38)),47))))),(((((((101,408),54),(366,(234,86))),(97,307)),((((61,253),(66,367)),172),158)),((((79,444),300),(((320,62),459),(92,243))),((291,99),((33,2),151)))),((((220,(160,285)),(141,(419,305))),(((31,(180,114)),(448,385)),437)),((111,293),((((442,313),401),231),(((258,1),(131,405)),497))))))),((((((((484,124),((215,(249,46)),310)),(((478,(403,48)),(441,143)),41)),(((26,120),226),(123,67))),(((((353,25),481),(422,((446,181),(415,105)))),((187,8),(127,480))),(((319,266),162),((354,35),(458,(390,(429,(342,301)))))))),((((204,261),((393,(338,190)),(213,((13,138),198)))),((((195,212),(165,364)),(468,43)),((355,5),(466,(477,371))))),((((377,(465,340)),381),182),((411,(464,102)),167)))),(((((221,(81,(22,153))),((326,248),(((9,203),(227,49)),373))),(148,((115,(171,469)),(192,440)))),(((64,106),((3,95),103)),(((89,185),(244,(392,278))),(93,((19,410),330))))),((((224,259),((98,379),(345,397))),((37,237),(73,(233,107)))),(((483,(370,436)),(228,391)),((42,347),((362,173),(283,368))))))),(((((((438,(214,83)),(352,(493,491))),((262,(343,467)),(279,314))),(((306,239),(417,(32,495))),(((16,146),274),225))),((((250,28),(298,426)),(260,(325,55))),(((344,(297,(282,100))),(292,268)),((241,133),222)))),(((((475,482),((299,69),232)),((88,(238,219)),(247,(60,407)))),((((486,339),(236,406)),427),((341,(65,130)),494))),((((164,(286,431)),304),(40,((193,108),349))),((((317,(10,18)),(129,128)),(134,112)),((50,216),413))))),(((((((416,(245,455)),23),296),((240,63),(((359,496),428),77))),((376,(402,53)),((217,72),(139,201)))),((((443,17),(78,(318,161))),((76,235),((365,382),168))),(((400,289),489),(((205,269),316),((((287,479),257),404),(74,34)))))),((((((315,170),328),375),(473,58)),(409,(453,147))),((((500,21),4),((200,(30,263)),((302,474),29))),(178,(((36,166),71),311))))))));");
//        Forest F2 = Forest.readNewickFormat("(((((((((303,(126,157)),((373,(((9,203),(227,49)),((326,248),((221,(81,(22,153))),((148,((89,185),(192,440))),(((((224,259),((98,379),(345,397))),((37,237),(73,(233,107)))),((((370,436),(146,483)),(228,391)),((42,347),((362,173),(283,368))))),((106,((289,400),64)),((3,((430,174),95)),103)))))))),(488,(318,246)))),113),((((309,(450,210)),110),(((284,27),(((331,176),374),(384,(119,(116,363))))),51)),((96,(218,(288,(471,6)))),((93,(((19,410),330),(244,(392,278)))),294)))),(((394,(230,357)),271),(((208,((372,(91,252)),395)),(184,((462,135),118))),((194,14),(156,211))))),(((((85,327),((163,412),144)),((336,(350,122)),94)),(207,125)),((((380,335),229),((((137,(197,121)),140),(24,485)),((334,177),463))),(484,(433,273))))),((((((152,445),(434,(12,202))),(388,(421,223))),((70,((132,460),87)),117)),(337,265)),(((281,39),(189,(75,44))),((((369,7),435),(361,332)),((276,68),(20,470)))))),(((((490,((((11,199),456),84),((272,418),423))),((387,((324,(329,38)),360)),((242,447),((432,52),414)))),(((109,(280,256)),(((312,(383,150)),104),270)),(((206,(290,396)),169),(398,351)))),(((((154,346),(183,57)),(461,188)),((((487,255),56),(196,(82,277))),45)),((((498,472),389),((476,251),(264,386))),((492,333),47)))),(((((220,(160,285)),(141,(419,305))),(((31,(180,114)),(448,385)),437)),((111,293),((((442,313),401),231),(((258,1),131),497)))),(((291,99),(151,((195,(212,((165,364),((468,43),(((355,5),(466,(477,371))),((204,261),((393,(338,190)),(213,((13,138),198))))))))),2))),((79,444),300))))),((((((((438,(214,83)),(352,(493,491))),((262,(343,467)),(279,314))),(((306,239),(417,(32,495))),(((16,(76,235)),274),(348,225)))),((((250,28),(298,426)),(260,(325,55))),(((344,(297,(282,100))),(292,268)),((241,133),222)))),(((((88,(238,219)),(247,(60,407))),(108,((475,482),((299,69),232)))),((((486,339),(236,406)),427),((341,(65,130)),(166,494)))),((((164,(286,431)),304),(40,(349,193))),(254,((((317,(10,18)),(129,128)),(134,112)),((50,216),413)))))),(((((((416,(245,455)),((158,((((61,253),(66,367)),172),((54,((90,155),(366,(234,86)))),(97,307)))),23)),296),((240,63),(((359,496),428),77))),((376,(402,53)),((139,201),217))),((((443,17),((115,78),161)),((365,382),168)),((((((287,479),257),404),(74,34)),(((205,269),316),(33,405))),489))),((((((315,170),328),375),(473,58)),(409,(453,147))),((((500,21),4),((200,(30,263)),((302,474),29))),(178,((71,36),311)))))),((((((310,(72,(215,(249,46)))),124),(((478,(403,(425,(((159,308),((420,(136,356)),((321,449),((((186,451),(((145,(142,454)),358),(378,(179,15)))),(80,(175,399))),((((499,323),(295,322)),((439,((243,(92,((320,62),459))),191)),(424,(149,209)))),(275,((457,59),(452,((171,469),267))))))))),48)))),(441,143)),41)),(((26,120),226),(123,67))),(((((353,25),481),(422,((415,105),((408,101),(446,181))))),((187,8),(127,480))),(((319,266),162),((354,35),(458,(390,(429,(342,301)))))))),((((377,(465,340)),381),182),((411,(464,102)),167)))));");

//        Forest F1 = Forest.readNewickFormat("((((6,4),8),(((17,10),5),((9,20),15))),((((12,16),(2,(19,(13,7)))),(1,3)),(18,(14,11))));");
//        Forest F2 = Forest.readNewickFormat("((((6,4),(20,8)),(15,(((19,(13,((18,(14,11)),7))),(((12,16),2),(1,3))),((17,10),5)))),9);");

        //k = 4 needed
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");

        //15 needed
        Forest F1 = Forest.readNewickFormat("((((((31,82),((97,41),47)),(((88,43),(94,(55,(24,78)))),(((91,33),65),((36,79),5)))),((((6,90),95),((44,70),(((22,23),50),18))),((((60,54),(86,77)),98),(((64,46),67),57)))),((((12,((83,28),68)),(2,29)),(62,(92,72))),((((11,89),80),(15,(53,25))),((19,4),(((1,84),73),((58,39),37)))))),(((((((63,(75,14)),(85,96)),26),(81,59)),((17,34),(30,21))),((40,(13,(99,35))),((((93,71),74),7),((56,48),((32,(76,38)),45))))),(((9,(42,16)),((100,(10,87)),8)),(((3,69),(66,20)),((51,(52,27)),(61,49))))))");
        Forest F2 = Forest.readNewickFormat("(((((31,82),(47,41)),(((88,43),(94,(55,(24,78)))),(((91,33),65),((36,79),5)))),(((((60,54),(86,77)),98),((64,46),67)),((44,70),(18,((85,(96,((63,(14,(((6,90),95),75))),(17,(26,((81,59),((25,(53,15)),((30,21),34)))))))),(50,(10,(22,23)))))))),((((2,29),(57,(12,((83,28),68)))),(62,(92,(97,72)))),(((19,4),(((84,((100,(87,(8,((9,(42,16)),((((3,69),(66,(89,20))),((51,(52,27)),(61,49))),((40,(13,(99,(80,35)))),((((93,71),74),7),((56,48),((32,(76,38)),45))))))))),1)),73),((58,39),37))),11)));");


        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);

        TreeUtils.linkForests(F1, F2);

        //ProblemInstance pi = new ProblemInstance(F1, F2);

        String[] arguments = new String[] {"default", "2", "no-approx", "no-whidden-trick"};


        ProblemInstance instance = new ProblemInstance(F1, F2);
        instance.printTrees();

        System.out.println("Random Seed : " + 217);

        DataTracker dt = new DataTracker("", "wew");

        MAFSolver algoBot = new MAFSolver(instance, new Random(217), arguments, dt);

        long t1 = System.nanoTime();
        boolean isPossible = algoBot.advancedSearch(i);
        long t2 = System.nanoTime();
        double tfull = (double) (t2 - t1) / 1000000;

        //boolean isPossible = algoBot.search(8);

        System.out.println();
        algoBot.printNumStates();
        System.out.println();

        if (isPossible) {
            System.out.println("ITS POSSIBLE!!!");
            dt.printToConsole();
            System.out.println("default whidden count: " + dt.defaultWhiddenCounter);
            System.out.println("Failed branch count (k<0): " + dt.failedBranchCount);
            System.out.println("total time " + tfull);
        } else {
            System.out.println("not possible");
        }

        Date d1 = new Date();
        System.out.println("Finished at " + d1);
        System.out.println("Split " + algoBot.getSplitCounter() + " times");
        System.out.println("Decomposed " + algoBot.getDecomposeCounter() +" times");
//        for (int i = 0; i < 99999999; i++) {
//
//        }


    }

    public static void testApprox() {
        //15 needed
        Forest F1 = Forest.readNewickFormat("((((((31,82),((97,41),47)),(((88,43),(94,(55,(24,78)))),(((91,33),65),((36,79),5)))),((((6,90),95),((44,70),(((22,23),50),18))),((((60,54),(86,77)),98),(((64,46),67),57)))),((((12,((83,28),68)),(2,29)),(62,(92,72))),((((11,89),80),(15,(53,25))),((19,4),(((1,84),73),((58,39),37)))))),(((((((63,(75,14)),(85,96)),26),(81,59)),((17,34),(30,21))),((40,(13,(99,35))),((((93,71),74),7),((56,48),((32,(76,38)),45))))),(((9,(42,16)),((100,(10,87)),8)),(((3,69),(66,20)),((51,(52,27)),(61,49))))))");
        Forest F2 = Forest.readNewickFormat("(((((31,82),(47,41)),(((88,43),(94,(55,(24,78)))),(((91,33),65),((36,79),5)))),(((((60,54),(86,77)),98),((64,46),67)),((44,70),(18,((85,(96,((63,(14,(((6,90),95),75))),(17,(26,((81,59),((25,(53,15)),((30,21),34)))))))),(50,(10,(22,23)))))))),((((2,29),(57,(12,((83,28),68)))),(62,(92,(97,72)))),(((19,4),(((84,((100,(87,(8,((9,(42,16)),((((3,69),(66,(89,20))),((51,(52,27)),(61,49))),((40,(13,(99,(80,35)))),((((93,71),74),7),((56,48),((32,(76,38)),45))))))))),1)),73),((58,39),37))),11)));");

        // trees3.txt true value 4
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))));");
//        Forest F2 = Forest.readNewickFormat("((((2,1),(((11,12),4),(8,(3,(6,5))))),7),((14,(10,9)),(13,(15,16))));");

        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);

        TreeUtils.linkForests(F1, F2);

        //ProblemInstance pi = new ProblemInstance(F1, F2);

        String[] arguments = new String[2];
        arguments[0] = "split";
        arguments[1] = "2";

        ProblemInstance instance = new ProblemInstance(F1, F2);
        instance.printTrees();
        System.out.println("Random Seed : " + 217);

        DataTracker dt = new DataTracker("", "split-decompose");

        MAFSolver algoBot = new MAFSolver(instance, new Random(217), arguments, dt);

//        algoBot.approxNormalizeTree(new UndoMachine(), instance);
//        int approxVal = algoBot.fastApprox(0, instance);
//        System.out.println("Approx val: " + approxVal);


    }

    public static void main(String[] args) {
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("((((7,8),((2,(11,1)),(3,4))),(6,5)),((12,(10,9)),((14,13),(15,16))))");

//        Forest F1 = Forest.readNewickFormat("((1,2),(3,4))");
//        Forest F2 = Forest.readNewickFormat("((1,3),(4,2))");


      //k = 4 needed
//        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
//        Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");


        //testApprox();S
        testDecompose(15);
        //test2();
////        //k = 8
//        Forest F1 = Forest.readNewickFormat("(((((1,(((2,(3,((4,5),(6,(7,8))))),9),((10,11),((12,13),14)))),(((15,16),((17,18),19)),20)),((21,(((22,23),(24,25)),(26,((27,28),29)))),((30,31),(32,((33,(34,(35,36))),(37,38)))))),(39,40)),((41,42),(((43,44),(45,(46,(47,48)))),(((49,50),(((51,(52,53)),(((54,55),56),((57,(58,59)),60))),((61,62),63))),((((((64,65),(66,67)),(((68,69),70),71)),(72,(73,(74,75)))),76),(((77,78),79),(80,(((81,82),((83,((84,85),(86,(87,((88,89),90))))),(((91,92),(93,(94,95))),(96,97)))),(98,(99,100))))))))))");
//        Forest F2 = Forest.readNewickFormat("(((((1,((2,9),((10,11),((12,(13,75)),14)))),20),(((21,(((22,23),(24,25)),(26,((27,28),29)))),39),(((30,(41,(33,(34,(35,36))))),31),(32,(37,38))))),40),(42,(((49,50),(((51,(52,53)),(((54,55),56),(((57,(58,59)),60),92))),((61,62),63))),((((((64,65),(66,67)),(((68,69),70),71)),(72,(73,((15,(16,74)),((17,18),19))))),76),(((77,(78,(3,((4,5),(6,(7,8)))))),79),(80,(((81,82),((83,((84,85),(86,(87,(((88,((43,44),(45,(46,(47,48))))),89),90))))),((91,(93,(94,95))),(96,97)))),(98,(99,100)))))))))");
//
//        // k = 9
////        Forest F1 = Forest.readNewickFormat("((((6,(28,12)),15),((7,(19,14)),4)),((((16,((23,1),30)),((22,8),20)),((25,13),(11,26))),(((5,(3,10)),(18,27)),(((29,21),(24,2)),(17,9)))))");
////        Forest F2 = Forest.readNewickFormat("((((7,(6,19)),4),15),(((16,((23,(1,(11,26))),30)),((22,8),20)),(24,(18,(((((29,21),2),(17,9)),13),(((25,((5,(3,10)),14)),27),(28,12)))))))");
//
////        Forest F1 = Forest.readNewickFormat("(((((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16)))),((17,18),19)),(((((21,22),(23,24)),((25,26),(27,28))),(((29,30),(31,32)),((33,34),(35,36)))),((37,38),39))),((((((41,42),(43,44)),((45,46),(47,48))),(((49,50),(51,52)),((53,54),(55,56)))),((57,58),59)),(((((61,62),(63,64)),((65,66),(67,68))),(((69,70),(71,72)),((73,74),(75,76)))),((77,78),79))))");
////        Forest F2 = Forest.readNewickFormat("(((((((35,36),(34,33)),((32,31),(30,29))),(((21,22),(24,23)),((25,26),(28,27)))),((38,37),39)),((18,19),((((14,13),(15,16)),((11,12),(9,10))),(((7,8),(6,5)),((3,4),(1,2)))))),((((((72,71),(70,69)),((73,74),(76,75))),(((63,64),(61,62)),((67,68),(66,65)))),(79,(77,78))),(((58,57),59),((((17,(52,51)),(50,49)),((56,55),(54,53))),(((48,47),(45,46)),((44,43),(41,42)))))))");
//
//        TreeUtils.linkForests(F1, F2);
//        TreeUtils.linkSiblings(F1);
//        TreeUtils.linkSiblings(F2);
//
//
//        String[] arguments = new String[2];
//        arguments[0] = "split-decompose";
//        arguments[1] = "2";
//
//        ProblemInstance instance = new ProblemInstance(F1, F2);
//        MAFSolver algoBot = new MAFSolver(instance, new Random(11), arguments);
//
//        List<Cherry> cherries = algoBot.findCherries();
//
////        int i = 0;
////        for (Cherry c : cherries) {
////            System.out.println("Cherry " + i);
////            i++;
////            System.out.println("a: " +c.getA().getLabel());
////            System.out.println("b: " +c.getB().getLabel());
////        }
//
//
//        boolean isPossible = algoBot.advancedSearch(9);
//        //boolean isPossible = algoBot.search(9);
//
//        System.out.println();
//        algoBot.printNumStates();
//        System.out.println();
//
//        if (isPossible) {
//            System.out.println("ITS POSSIBLE!!!");
//        } else {
//            System.out.println("not possible");
//        }
//
//        Map<Integer, Integer> counter = algoBot.decomposeCounters;
//        for (int f2Size : counter.keySet()) {
//            System.out.println(counter.get(f2Size) +" occurrences of decompose on " + f2Size + " components in F2");
//        }
    }

}
