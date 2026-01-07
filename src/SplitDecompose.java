import Algorithm.MAFSolver;
import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.DataTracker;
import utils.ExperimentTool;
import utils.TreeUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static utils.ExperimentTool.*;

public class SplitDecompose {



    public static void SplitDecomposeOnDB() throws IOException {
        File dir = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\kernelizing_agreement_forests_data\\maindataset");
        try {
            for (File f : dir.listFiles()) {
                String fName = f.getName();
                ProblemInstance pi = ExperimentTool.readProblemInstanceFromFileWithRho(f);


                String[] arguments = new String[2];
                arguments[0] = "split-decompose";
                arguments[1] = "2";


                MAFSolver solver = new MAFSolver(pi, new Random(1), arguments);

                long startSD = System.nanoTime();
                int trueSolution = -1;
                for (int i = 1; i < 15; i++) {
                    System.out.println("SEARCHING AT K = " + i);
                    boolean works = solver.advancedSearch(i);

                    if (works) {
                        System.out.println();
                        solver.printNumStates();
                        System.out.println();
                        System.out.println("---------------------------------");
                        System.out.println("Solvable in " + i + " cuts\n\n");

                        trueSolution = i;
                        break;
                    } else {
                        System.out.println(i + " cuts not enough\n\n");
                    }
                }
                long endSD = System.nanoTime();

                long durationSD = (endSD - startSD)/1000000;

                String dataEntry = fName + "," + trueSolution + "," + durationSD;
                ExperimentTool.appendLineToFile("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test\\kernelizationData.csv", dataEntry);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        List<ProblemInstance> problemInstances = ExperimentTool.readProblemInstancesFromDirectory(dir);

        System.out.println(problemInstances.size());

    }

    public static void rsprOnDBS() {
        File dir = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\kernelizing_agreement_forests_data\\maindataset");
        try {
            int i = 0;
            for (File f : dir.listFiles()) {
                String fName = f.getName();

                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line1 = br.readLine();
                    String line2 = br.readLine();


                    if (line1 == null || line2 == null) {
                        throw new IOException("File " + f.getName() + " must contain exactly two lines with Newick trees.");
                    }

                    line1 = "(" + line1.substring(0, line1.length()-1) + ",rho)";

                    line2 = "(" + line2.substring(0, line2.length()-1) + ",rho)";

                    String trees = line1 + System.lineSeparator() + line2;

//                    System.out.println(trees);
//                    System.out.println();

                    long startTimeRspr = System.nanoTime();
                    String rsprResultString = runRsprOnTrees(trees);
                    long endTimeRspr = System.nanoTime();

                    long rsprDuration = (endTimeRspr - startTimeRspr)/1000000;

                    int rsprResult = ExperimentTool.extractInteger(rsprResultString);

                    System.out.println(rsprResultString);

                    if (rsprResult != -1) {
                        String dataEntry = i + "," + fName + "," + rsprResult + "," + rsprDuration;
                        ExperimentTool.appendLineToFile("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test\\kernelizationDataRspr.csv", dataEntry);

                    }

                    i++;
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }



    public static void compareRsprSplitDecompose(int stepCounter, String[] treeGenArgs, String[] splitDecomposeArgs, String[] logFileNames) throws Exception {
        //ProblemInstance instance = problemInstanceWithRhoFromTreeStringArray(trees);
        String logFilePath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test_logs\\" + logFileNames[0];
        String resultsFilePath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test_logs\\" + logFileNames[1];
        String mismatchIdPath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test_logs\\" + logFileNames[2];

        String[] treeFilePaths = {"C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomTree.jar",
                "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomRSPR.jar"};


        String trees = ExperimentTool.generateTreePair(treeGenArgs[0], treeGenArgs[1], treeGenArgs[2], treeGenArgs[3], treeFilePaths, null);

        System.out.println(trees);

        System.out.println("Running rspr");

        long startTimeRspr = System.nanoTime();
        String rsprResultString = runRsprOnTrees(trees);
        long endTimeRspr = System.nanoTime();

        long rsprDuration = (endTimeRspr - startTimeRspr)/1000000;

        String[] rsprFinal3 = ExperimentTool.getLastThreeLines(rsprResultString);

        int rsprResult = -1;
        if (rsprFinal3.length == 3) {
            rsprResult = ExperimentTool.extractInteger(rsprFinal3[2]);
        }

        System.out.println(rsprResultString);

        if (rsprResult != -1) {


            //ProblemInstance instance = problemInstanceFromTreeStringArray(trees.split("\\r?\\n"));
            ProblemInstance instance = problemInstanceWithRhoFromTreeStringArray(trees.split("\\r?\\n"));
            instance.printTrees();

            MAFSolver solver = new MAFSolver(instance, new Random(1), splitDecomposeArgs);

            int trueSolution = -1;

            long startSD = System.nanoTime();
            for (int i = 10; i < 21; i++) {
                System.out.println("SEARCHING AT K = " + i);
                boolean works = solver.advancedSearch(i);

                if (works) {
                    System.out.println();
                    solver.printNumStates();
                    System.out.println();
                    System.out.println("---------------------------------");
                    System.out.println("Solvable in " + i + " cuts\n\n");

                    trueSolution = i;
                    break;
                } else {
                    System.out.println(i + " cuts not enough\n\n");
                }
            }
            long endSD = System.nanoTime();

            long durationSD = (endSD - startSD)/1000000;

//            Forest F2origin = instance.getOriginalF2();
//            F2origin.printForest();
//
//
//            TreeUtils.applySolution(F2origin, solver.getCurrentCuts());
//            for (Node root : F2origin.getComponents()) {
//                TreeUtils.removeRho(root);
//            }
//            List<String> newickComponents = F2origin.toNewickList();
//
//            String sdComponents = String.join(" ", newickComponents);


            StringBuilder sb = new StringBuilder("-----------------------------------------------------------------");
            if (trueSolution != rsprResult) {
                sb.append(System.lineSeparator()).append("RESULT MISMATCH").append(System.lineSeparator());

                ExperimentTool.appendLineToFile(mismatchIdPath, Integer.toString(stepCounter));
            } else {
                sb.append(System.lineSeparator()).append("SUCCESS NO MISMATCH").append(System.lineSeparator());
            }
            sb.append("Run number ").append(stepCounter).append(System.lineSeparator());
            sb.append("rspr solution: ").append(rsprResult).append(System.lineSeparator());
            sb.append(rsprFinal3[0]).append(System.lineSeparator());
            sb.append(rsprFinal3[1]).append(System.lineSeparator());
            sb.append("rspr completed in ").append(rsprDuration).append(" milliseconds").append(System.lineSeparator());
            sb.append("Split and Decompose solution: ").append(trueSolution).append(System.lineSeparator());
            //sb.append(sdComponents).append(System.lineSeparator());
            sb.append("Split and Decompose completed in ").append(durationSD).append(" milliseconds").append(System.lineSeparator());
            sb.append("Split and Decompose Counters").append(System.lineSeparator());
            sb.append("split: ").append(solver.getSplitCounter()).append(System.lineSeparator());
            sb.append("decompose: ").append(solver.getDecomposeCounter()).append(System.lineSeparator());
            sb.append("NEWICK TREES:").append(System.lineSeparator());
            sb.append(trees).append(System.lineSeparator());


            ExperimentTool.appendLineToFile(logFilePath, sb.toString());

            String bothResults = stepCounter + "," +trueSolution + "," + rsprResult;

            ExperimentTool.appendLineToFile(resultsFilePath, bothResults);
        }
    }

    public static void compareConfigs(String[] args1, String[] args2, int stepCounter, String[] treeGenArgs, String[] logFileNames) {

    }

    public static int testMain(String[] args) {
        File treeFile = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\trees1.txt");

        try {
            ProblemInstance pi = readProblemInstanceFromFileWithRho(treeFile);
            //ProblemInstance pi = ExperimentTool.readProblemFromFile(treeFile);
            //pi.printTrees();

            String[] arguments = new String[2];
            arguments[0] = "split-decompose";
            //arguments[1] = args[1];
            arguments[1] = "2";

            Date d1 = new Date();

            MAFSolver solver;
            if (args.length == 3) {
                solver = new MAFSolver(pi, new Random(Integer.parseInt(args[2])), arguments);
            } else {
                solver = new MAFSolver(pi, new Random(), arguments);
            }



            for (int i = 1; i < 18; i++) {
                System.out.println("SEARCHING AT K = " + i);
                boolean works = solver.advancedSearch(i);

                if (works) {
                    System.out.println();
                    solver.printNumStates();
                    System.out.println();
                    System.out.println("---------------------------------");
                    System.out.println("Solvable in " + i + " cuts\n\n");
                    return i;
                } else {
                    System.out.println(i + " cuts not enough\n\n");
                }
            }

//            Forest F2origin = pi.getOriginalF2();


//            TreeUtils.applySolution(F2origin, solver.getCurrentCuts());
//            for (Node root : F2origin.getComponents()) {
//                TreeUtils.removeRho(root);
//            }
//            List<String> newickComponents = F2origin.toNewickList();
//            System.out.println();
//            System.out.println("OPT:");
//            for (String s : newickComponents) {
//                System.out.println(s);
//            }
//
//            System.out.println("\n\n\nSplit " + solver.getSplitCounter() + " times");
//            System.out.println("Decomposed " + solver.getDecomposeCounter() + " times\n\n\n");
//
//
//            Date d2 = new Date();
//
//            System.out.println("Started at " + d1);
//            System.out.println("Finished at " + d2);


        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }


    public static void main (String[] args){
        //File treeFile = new File(args[0]);
        //File treeFile = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\rspr_test_trees\\trees_100_17.txt");
        //File treeFile = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\treesLARGE.txt");
        File treeFile = new File ("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\rspr_test_trees\\tree_2_test.txt");

        try {
            ProblemInstance pi = readProblemInstanceFromFileWithRho(treeFile);
            //ProblemInstance pi = ExperimentTool.readProblemFromFile(treeFile);
            pi.printTrees();

            String[] arguments = new String[] {"default", "2", "approx", "whidden-trick"};
//            arguments[0] = "split-decompose";
//            //arguments[0] = args[1];
//            //arguments[1] = args[2];
//            arguments[1] = "2";




            Date d1 = new Date();

            MAFSolver solver = new MAFSolver(pi, new Random(), arguments, new DataTracker("", ""));




            for (int i = 1; i < 71; i++) {
                System.out.println("SEARCHING AT K = " + i);
                System.out.println("Configuration: " + arguments[0]);
                boolean works = solver.advancedSearch(i);

                if (works) {
                    System.out.println();
                    solver.printNumStates();
                    System.out.println();
                    System.out.println("---------------------------------");
                    System.out.println("Solvable in " + i + " cuts\n\n");


                    break;
                } else {
                    solver.printNumStates();
                    System.out.println(i + " cuts not enough\n\n");
                }
            }

            Forest F2origin = pi.getOriginalF2();


            TreeUtils.applySolution(F2origin, solver.getCurrentCuts());
            for (Node root : F2origin.getComponents()) {
                TreeUtils.removeRho(root);
            }
            List<String> newickComponents = F2origin.toNewickList();
            System.out.println();
            System.out.println("OPT:");
            for (String s : newickComponents) {
                System.out.println(s);
            }

            System.out.println("\n\n\nSplit " + solver.getSplitCounter() + " times");
            System.out.println("Decomposed " + solver.getDecomposeCounter() + " times\n\n\n");


            Date d2 = new Date();

            System.out.println("Started at " + d1);
            System.out.println("Finished at " + d2);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
