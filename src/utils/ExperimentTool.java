package utils;

import Algorithm.MAFSolver;
import Model.Forest;
import Model.Node;
import Model.ProblemInstance;


import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
                Files.writeString(Path.of(txtFilePath), treePair, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
        String txtPath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\treesLARGE.txt";

        generateTreePair("500", "0", "50", "70", filePaths, txtPath);
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
        String[] lastLines = getLastThreeLines(input);

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



    public static String[] getLastThreeLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0]; // no lines
        }

        // Split into lines
        String[] lines = text.split("\\R");  // \R matches any line break

        // Check final line for marker
        String finalLine = lines[lines.length - 1];
        if (!finalLine.contains("exact drSPR")) {
            return new String[0]; // marker not present
        }

        // Determine how many lines to return
        int count = Math.min(3, lines.length);

        // Copy last 'count' lines
        String[] result = new String[count];
        System.arraycopy(lines, lines.length - count, result, 0, count);

        return result;
    }

    public static void appendLineToFile(String filePath, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.newLine();
            writer.write(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readFileInPairs(Path filePath) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        List<String> result = new ArrayList<>();

        for (int i = 0; i < lines.size(); i += 2) {
            String first = lines.get(i);
            String second = (i + 1 < lines.size()) ? lines.get(i + 1) : "";

            result.add(first + System.lineSeparator() + second);
        }

        return result;
    }

    public static void testReadTreePairsFromLargeFile() throws IOException {
        String path = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated\\trees_100_16";

        List<String> ls = readFileInPairs(Path.of(path));

        System.out.println("Size: "+ ls.size());
        for (String tree : ls) {
            System.out.println("Printing treepair");
            System.out.println(tree);
        }

    }
    public static void generateTreeSet(String[] randOp) {
        String[] treeFilePaths = {"C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomTree.jar",
                "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\TreeGen\\RandomRSPR.jar"};
        String[] treeGenArgs = new String[] {"300", "0", "50", "-1"};
        //String[] randOp = new String[] {"10", "11", "12", "13", "17", "18"};
        List<String> randomOperations = new ArrayList<>(List.of(randOp));
        for (String opCount : randomOperations) {
            treeGenArgs[3] = opCount;
            for (int i = 0; i < 300; i++) {
                String filePath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated\\trees_100_" + opCount;
                String treesString = ExperimentTool.generateTreePair(treeGenArgs[0], treeGenArgs[1], treeGenArgs[2], treeGenArgs[3], treeFilePaths, filePath);
                System.out.println("Trees:");
                System.out.println(treesString);

            }
        }
    }





    public static void test1() throws Exception {
        String trees = Files.readString(Path.of("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\rspr_test_trees\\trees_100_17.txt"));

        String result = runRsprOnTrees(trees);

        String[] final3 = getLastThreeLines(result);

        int trueRes = extractInteger(final3[2]);

        System.out.println(final3[0]);
        System.out.println(final3[1]);

        System.out.println(trueRes);


    }


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


    public static ProblemInstance problemInstanceFromTreeStringArray(String[] trees) {
        String line1 = trees[0];
        String line2 = trees[1];


        Forest T1 = Forest.readNewickFormat(line1);
        Forest F2 = Forest.readNewickFormat(line2);

        TreeUtils.linkForests(T1, F2);
        TreeUtils.linkSiblings(T1);
        TreeUtils.linkSiblings(F2);

        return new ProblemInstance(T1, F2);
    }

    public static void reassignTrees(String string) throws IOException {
        String path = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated\\trees_100_" + string;

        List<String> ls = readFileInPairs(Path.of(path));

        String[] args = new String[] {"decompose", "2", "approx", "no"};

        int counter = 0;
        for (String trees : ls) {

            if (counter == 1000) {
                break;
            }
            ProblemInstance pI = ExperimentTool.problemInstanceWithRhoFromTreeStringArray(trees.split("\\r?\\n"));
            DataTracker dt = new DataTracker("", "decompose");
            MAFSolver solver = new MAFSolver(pI, new Random(), args, dt);
            int trueSolution = -1;
            for (int i = 0; i < 41; i++) {
                boolean works = solver.advancedSearch(i);

                if (works) {
                    solver.printNumStates();
                    System.out.println("---------------------------------");
                    System.out.println("Solvable in " + i + " cuts\n\n");

                    trueSolution = i;


                    try {
                        String txtFilePath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated_final\\trees_100_" + i;
                        Files.writeString(Path.of(txtFilePath), trees + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }


                    break;
                } else {
                    System.out.println(i + " cuts not enough");
                }

            }

            counter++;

        }

    }


    public static List<String> removeDuplicates(List<String> input) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String s : input) {
            if (seen.add(s)) { // add() returns false if already present
                result.add(s);
            }
        }

        return result;
    }

    public static void writeLinesToFile(List<String> lines, Path filePath) throws IOException {
        Files.write(filePath, lines);
    }

    public static void fixDuplicates() {
        String destPath = "C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated_final_final\\";
       // String sourcePath = ""
    }

    public static void processDirectoryFiles() throws IOException {
        Path inputDir = Path.of("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated_final");
        Path outputDir = Path.of("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated_final_final");
        if (!Files.isDirectory(inputDir)) {
            throw new IllegalArgumentException("Input path is not a directory");
        }

        Files.createDirectories(outputDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir)) {
            for (Path file : stream) {

                // Skip directories
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                // 1. Read file in pairs
                List<String> lines = Files.readAllLines(file);
                List<String> unique = getStrings(lines);

                // 3. Build output filename (append "_final")
                String originalName = file.getFileName().toString();
                Path outputFile = outputDir.resolve(originalName + "_final");

                // 4. Write result to file
                Files.write(outputFile, unique);
            }
        }
    }

    private static List<String> getStrings(List<String> lines) {
        List<String> pairedLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i += 2) {
            String first = lines.get(i);
            String second = (i + 1 < lines.size()) ? lines.get(i + 1) : "";
            pairedLines.add(first + System.lineSeparator() + second);
        }

        // 2. Remove duplicates (manual loop, preserve order)
        List<String> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (String entry : pairedLines) {
            if (seen.add(entry)) {
                unique.add(entry);
            }
        }
        return unique;
    }

    public static void validateTrees() throws IOException {
        Path inputDir = Path.of("C:\\Users\\Florent\\IdeaProjects\\rSplitDecompose\\test_trees\\generated_final_final");

        if (!Files.isDirectory(inputDir)) {
            throw new IllegalArgumentException("Input path is not a directory");
        }


        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDir)) {
            int counter = 0;
            String[] args = new String[] {"decompose", "2", "approx"};
            for (Path file : stream) {
                List<String> lines = Files.readAllLines(file);
                List<String> unique = getStrings(lines);

                for (String trees : unique) {
                    ProblemInstance pI = ExperimentTool.problemInstanceWithRhoFromTreeStringArray(trees.split("\\r?\\n"));
                    DataTracker dt = new DataTracker("", "decompose");
                    MAFSolver solver = new MAFSolver(pI, new Random(), args, dt);
                    int trueSolution = -1;
                    for (int i = 5; i < 41; i++) {
                        boolean works = solver.advancedSearch(i);

                        if (works) {
                            solver.printNumStates();
                            System.out.println("---------------------------------");
                            System.out.println("Solvable in " + i + " cuts\n\n");

                            if (i != counter) {
                                System.out.println("Problem in file with " + counter + " rspr operations");
                                System.out.println(trees);
                                System.out.println("true sol = " + i);
                                throw new RuntimeException();
                            }
                            counter++;



                            break;
                        } else {
                            System.out.println(i + " cuts not enough");
                        }

                    }


                }
                counter++;
            }

        }
    }







    public static void main(String[] args) throws Exception {
        //testRandomRSPR();
        //testTreeGen();
        //RSPRWithTreeGen();
        //test1();


        String[] randOp = new String[] {"2", "3", "4", "5", "6", "7", "8", "9"};
        //generateTreeSet(randOp);

        //String[] randOp = new String[] {"10", "11", "12", "13", "17", "18"};
        for (String count : randOp) {
            reassignTrees(count);
        }
//        testReadTreePairsFromLargeFile();
//        for (int i = 14; i <17; i+=1) {
//            ;
//        }

        //processDirectoryFiles();
        //validateTrees();
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
