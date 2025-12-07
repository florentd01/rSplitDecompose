package utils;

import Algorithm.MAFSolver;
import Model.Forest;
import Model.Node;
import Model.ProblemInstance;


import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLSyntaxErrorException;
import java.util.*;

public class ExperimentTool {
    List<ProblemInstance> problemInstances = new ArrayList<>();
    List<String> fileNames = new ArrayList<>();


    /**
     * Reads a single test file that contains two lines,
     * each line representing a tree in Newick format.
     * The two trees are converted into Forests and wrapped in a ProblemInstance.
     *
     * @param file the input .txt file
     * @return ProblemInstance created from the two trees in the file
     * @throws IOException if file cannot be read or has invalid format
     */
    public ProblemInstance readProblemInstanceFromFile(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line1 = br.readLine();
            String line2 = br.readLine();

            if (line1 == null || line2 == null) {
                throw new IOException("File " + file.getName() + " must contain exactly two lines with Newick trees.");
            }

            // Build forests from Newick strings
            Node tRoot = TreeUtils.readNewick(line1);
            Node fRoot = TreeUtils.readNewick(line2.trim());

            tRoot.setRoot(false);
            fRoot.setRoot(false);



            Node newRootT = new Node("rho");
            Node newRootF = new Node("rho");



            newRootT.addChild(tRoot);
            newRootF.addChild(fRoot);

            newRootT.setTwin(newRootF);
            newRootF.setTwin(newRootT);

            newRootT.setRoot(true);
            newRootF.setRoot(true);

            newRootT.setLeaf(true);
            newRootF.setLeaf(true);


            Forest t = new Forest(newRootT);
            Forest f = new Forest(newRootF);

            t.buildLeafMap();
            f.buildLeafMap();

            t.getLeavesByLabel().put("rho", newRootT);
            f.getLeavesByLabel().put("rho", newRootF);

            TreeUtils.linkSiblings(t);
            TreeUtils.linkSiblings(f);
            TreeUtils.linkForests(t, f);

            return new ProblemInstance(t, f);
        }
    }

    /**
     * Reads all .txt files from a directory and builds a list of ProblemInstances.
     * Each file is assumed to contain exactly two Newick trees on separate lines.
     *
     * @param directory directory containing test files
     * @return list of ProblemInstances
     * @throws IOException if directory is invalid or any file cannot be read
     */
    public static List<ProblemInstance> readProblemInstancesFromDirectory(File directory) throws IOException {
        if (directory == null || !directory.isDirectory()) {
            throw new IOException("Provided path is not a valid directory: " + directory);
        }

        File[] files = directory.listFiles();
        List<ProblemInstance> instances = new ArrayList<>();

        if (files == null) {
            return instances;
        }

        for (File f : files) {
            ProblemInstance instance = readProblemInstanceFromFileWithRho(f);
            instances.add(instance);
        }

        return instances;
    }

    public static ProblemInstance readProblemFromFile(File file)  throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line1 = br.readLine();
            String line2 = br.readLine();

            if (line1 == null || line2 == null) {
                throw new IOException("File " + file.getName() + " must contain exactly two lines with Newick trees.");
            }

            // Build forests from Newick strings
            Forest T1 = Forest.readNewickFormat(line1);
            Forest F2 = Forest.readNewickFormat(line2);

            TreeUtils.linkForests(T1, F2);
            TreeUtils.linkSiblings(T1);
            TreeUtils.linkSiblings(F2);

            return new ProblemInstance(T1, F2);
        }
    }

    public static ProblemInstance readProblemInstanceFromFileWithRho(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line1 = br.readLine();
            String line2 = br.readLine();

            if (line1 == null || line2 == null) {
                throw new IOException("File " + file.getName() + " must contain exactly two lines with Newick trees.");
            }
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
    }

    public static void splitAndDecompose(String filePath, String[] args) throws IOException {
        File treeFile = new File(filePath);

        ProblemInstance pi = readProblemInstanceFromFileWithRho(treeFile);

        MAFSolver solver = new MAFSolver(pi, new Random(), args);

        for (int i = 0; i < 18; i++) {
            System.out.println("SEARCHING AT K = " + i);
            boolean works = solver.advancedSearch(i);

            if (works) {
                System.out.println();
                solver.printNumStates();
                System.out.println();
                System.out.println("---------------------------------");
                System.out.println("Solvable in " + i + " cuts");

                break;
            } else {
                System.out.println(i + " cuts not enough\n\n");
            }
        }


    }





    public static String runExecutableWithInputFile(String exePath, String inputFilePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(exePath);
        pb.redirectErrorStream(true); // merge stdout + stderr
        Process process = pb.start();

        // Write file contents to the process's standard input
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
             BufferedReader fileReader = Files.newBufferedReader(Path.of(inputFilePath))) {

            String line;
            while ((line = fileReader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        }

        // Read process output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        process.waitFor();
        return output.toString();
    }

    public static String runJarAndGetOutput(String jarPath, String[] args) throws Exception {

        // Build: java -jar file.jar arg1 arg2 ...
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarPath);

        for (String arg : args) {
            command.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // merge stderr into stdout
        Process process = pb.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        process.waitFor();

        return output.toString();
    }

    public static String runJarWithInputFile(String jarPath, String[] jarArgs, String inputFilePath) throws Exception {

        // Build: java -jar jarfile.jar arg1 arg2 ...
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarPath);

        for (String arg : jarArgs) {
            command.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // merge stdout + stderr
        Process process = pb.start();

        // --- Write input file into the JAR's stdin ---
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
             BufferedReader fileReader = Files.newBufferedReader(Path.of(inputFilePath))) {

            String line;
            while ((line = fileReader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush(); // important!
        }

        // --- Read console output ---
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        process.waitFor();
        return output.toString();
    }

    public static String runJarWithInputLines(String jarPath, String[] jarArgs, String inputLine) throws Exception {

        // Build: java -jar jarfile.jar arg1 arg2 ...
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarPath);

        for (String arg : jarArgs) {
            command.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // merge stdout + stderr
        Process process = pb.start();

        // --- Write array lines to JAR's stdin ---
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(inputLine);
            writer.flush(); // ensure all data is sent
        }

        // --- Read console output ---
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        process.waitFor();
        return output.toString();
    }


    public static void compareToRspr() {
        // first create a tree file with two trees


        // run rspr on this tree


    }

    public static void testRandomTree() {
        String path = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomTree.jar";
        String[] jarArgs = new String[3];

        jarArgs[0] = "50";
        jarArgs[1] = "0";
        jarArgs[2] = "50";

        try {
            String result = runJarAndGetOutput(path, jarArgs);
            try {
                Files.writeString(Path.of("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\tree.txt"), result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testRandomRSPR() {
        String path = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomRSPR.jar";
        String inputFilePath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\trees1.txt";

        String[] jarArgs = new String[2];

        jarArgs[0] = "2";
        jarArgs[1] = "0";

        try {
            String result = runJarWithInputFile(path, jarArgs, inputFilePath);
            try {
                Files.writeString(Path.of("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\treeRSPRCopy.txt"), result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static String generateTreePair(String n, String s, String p, String rsprOperations, String[] jarFilePaths, String txtFilePath) {
        String[] argsTree = {n, s, p};
        String[] argsRSPR = {rsprOperations, s};

        String randomTreePath = jarFilePaths[0];
        String randomRSPRPath = jarFilePaths[1];
        String tree;
        String treeCopy;

        try {
            tree = runJarAndGetOutput(randomTreePath, argsTree);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            treeCopy = runJarWithInputLines(randomRSPRPath, argsRSPR, tree);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String treePair = tree + treeCopy;
        //System.out.println(treePair);

        if (txtFilePath != null) {
            try {
                Files.writeString(Path.of(txtFilePath), treePair);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return treePair;
    }

    public static String runExecutableWithInputLines(
            String exePath,
            String[] exeArgs,
            String inputLines) throws Exception {

        // Build command: exe arg1 arg2 ...
        List<String> command = new ArrayList<>();
        command.add(exePath);

        for (String arg : exeArgs) {
            command.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // merge stderr into stdout
        Process process = pb.start();

        // --- Write input lines to stdin (equivalent to < file.txt) ---
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream()))) {


            writer.write(inputLines);
            writer.newLine();

            writer.flush(); // ensure data is sent
        }

        // --- Read console output ---
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        process.waitFor();
        return output.toString();
    }

    public static void testTreeGen() {
        String[] filePaths = {"C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomTree.jar",
                "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomRSPR.jar"};
        String txtPath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\trees1.txt";

        generateTreePair("50", "0", "50", "10", filePaths, txtPath);
    }

    public static void RSPRWithTreeGen() throws Exception {
        String[] treeFilePaths = {"C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomTree.jar",
                "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomRSPR.jar"};

        String inputLines = generateTreePair("50", "0", "50", "10", treeFilePaths, null);

        System.out.println(inputLines);
        String[] lines = inputLines.split("\\r?\\n");

//        for (String line : lines) {
//            System.out.println("line");
//            System.out.println(line);
//        }

        String[] argumentsRSPR = new String[] {"-fpt"};



        String input = runExecutableWithInputLines("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\rspr-1.3.0\\rspr.exe", argumentsRSPR, inputLines).trim();

        System.out.println(input);
        int finalResult = extractInteger(input);

        System.out.println(finalResult);


    }

    public static String runRsprOnTrees(String trees) throws Exception {
        String[] argumentsRSPR = new String[] {"-fpt"};
        String input = runExecutableWithInputLines("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\rspr-1.3.0\\rspr.exe", argumentsRSPR, trees).trim();
        return input;
    }

    public static int extractInteger(String input) {
        if (input == null || input.isEmpty()) {
            return -1; // Case 1: empty
        }

//        // Case 2: String is just a plain integer
//        try {
//            return Integer.parseInt(input.trim());
//        } catch (NumberFormatException ignored) {
//            // Not a plain integer, continue to case 3
//        }

        // Case 3: Look for "drSPR=<number>" anywhere inside the string
        String marker = "exact drSPR=";
        int index = input.indexOf(marker);

        if (index != -1) {
            String after = input.substring(index + marker.length()).trim();

            // Extract the leading integer from the substring
            int end = 0;
            while (end < after.length() && Character.isDigit(after.charAt(end))) {
                end++;
            }

            if (end > 0) { // There was at least one digit
                try {
                    return Integer.parseInt(after.substring(0, end));
                } catch (NumberFormatException ignored) {}
            }
        }

        // No integer found
        return -1;
    }

    public static void appendLineToFile(String filePath, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.newLine();
            writer.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws Exception {
        //testRandomRSPR();
        //testTreeGen();
        RSPRWithTreeGen();

//        String inputLines = "((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))));\n" +
//                "(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)));";
//        String[] argumentsRSPR = new String[] {"-fpt", "-q"};
//        String rsprResult = runExecutableWithInputLines("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\rspr-1.3.0\\rspr.exe", argumentsRSPR, inputLines);
//        System.out.println(Integer.parseInt(rsprResult.trim()));
//        System.out.println(rsprResult.trim().length());

//        ExperimentTool ex = new ExperimentTool();
//
//        final File folder = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees");
//
//        File treeFile = new File("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\rspr_test_trees\\trees_100_17.txt");
//
//        ProblemInstance pi = readProblemInstanceFromFileWithRho(treeFile);
//        pi.printTrees();
//
//        String[] arguments = new String[2];
//        arguments[0] = "split-decompose";
//        arguments[1] = "2";
//
//        MAFSolver solver = new MAFSolver(pi, new Random(1), arguments);
//
//        for (int i = 0; i < 18; i++) {
//            System.out.println("SEARCHING AT K = " + i);
//            boolean works = solver.advancedSearch(i);
//
//            if (works) {
//                System.out.println();
//                solver.printNumStates();
//                System.out.println();
//                System.out.println("---------------------------------");
//                System.out.println("Solvable in " + i + " cuts");
//
//                break;
//            } else {
//                System.out.println(i + " cuts not enough\n\n");
//            }
//        }





//        try {
//            List<ProblemInstance> pr = ex.readProblemInstancesFromDirectory(folder);
//            pr.get(0).printTrees();
//            MAFSolver solver = new MAFSolver(pr.getFirst(), new Random(1), new String[2]);
//            if (solver.advancedSearch(10)) {
//                System.out.println("it works");
//            } else {
//                System.out.println("it doesnt work");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }
}
