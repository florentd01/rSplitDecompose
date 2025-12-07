import Algorithm.MAFSolver;
import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.ExperimentTool;
import utils.TreeUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static utils.ExperimentTool.*;

public class SplitDecompose {

    public static ProblemInstance problemInstanceWithRhoFromTreeStringArray(String[] trees) {
        String line1 = trees[0];
        String line2 = trees[1];


        //"(+(((1,2),(3,4)),((5,6),(7,8)))+,rho)"
        line1 = "(" + line1 + ",rho)";

        line2 = "(" + line2 + ",rho)";

        Forest T1 = Forest.readNewickFormat(line1);
        Forest F2 = Forest.readNewickFormat(line2);

        TreeUtils.linkForests(T1, F2);
        TreeUtils.linkSiblings(T1);
        TreeUtils.linkSiblings(F2);

        return new ProblemInstance(T1, F2);
    }

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



    public static void compareRsprSplitDecompose(int stepCounter) throws Exception {
        //ProblemInstance instance = problemInstanceWithRhoFromTreeStringArray(trees);
        String logFilePath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test\\logs.txt";
        String resultsFilePath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test\\results_comparison.csv";

        String[] treeFilePaths = {"C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomTree.jar",
                "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomRSPR.jar"};


        String trees = ExperimentTool.generateTreePair("50", "0", "50", "10", treeFilePaths, null);

        System.out.println(trees);

        System.out.println("Running rspr");

        long startTimeRspr = System.nanoTime();
        String rsprResultString = runRsprOnTrees(trees);
        long endTimeRspr = System.nanoTime();

        long rsprDuration = (endTimeRspr - startTimeRspr)/1000000;

        int rsprResult = ExperimentTool.extractInteger(rsprResultString);

        System.out.println(rsprResultString);

        if (rsprResult != -1) {
            String[] arguments = new String[2];
            arguments[0] = "split-decompose";
            arguments[1] = "2";

            ProblemInstance instance = problemInstanceWithRhoFromTreeStringArray(trees.split("\\r?\\n"));
            MAFSolver solver = new MAFSolver(instance, new Random(1), arguments);

            int trueSolution = -1;

            long startSD = System.nanoTime();
            for (int i = 1; i < 11; i++) {
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


            StringBuilder sb = new StringBuilder("-----------------------------------------------------------------");
            if (trueSolution != rsprResult) {
                sb.append(System.lineSeparator()).append("RESULT MISMATCH").append(System.lineSeparator());
                String mismatchIdPath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\Test\\mismatch_ids.txt";
                ExperimentTool.appendLineToFile(mismatchIdPath, Integer.toString(stepCounter));
            } else {
                sb.append(System.lineSeparator()).append("SUCCESS NO MISMATCH").append(System.lineSeparator());
            }
            sb.append("Run number ").append(stepCounter).append(System.lineSeparator());
            sb.append("rspr solution: ").append(rsprResult).append(System.lineSeparator());
            sb.append("rspr completed in ").append(rsprDuration).append(" milliseconds").append(System.lineSeparator());
            sb.append("Split and Decompose solution: ").append(trueSolution).append(System.lineSeparator());
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


        public static void main (String[]args){
            //File treeFile = new File(args[0]);
            File treeFile = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\trees1.txt");

            try {
                ProblemInstance pi = readProblemInstanceFromFileWithRho(treeFile);
                pi.printTrees();

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


                        break;
                    } else {
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
