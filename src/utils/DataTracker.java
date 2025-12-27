package utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class DataTracker {
    public long statesExplored = 0;
    public long splitCounter = 0;
    public long decomposeCounter = 0;
    public long defaultWhiddenCounter = 0;
    public long failedBranchCount = 0;

    public long decomposeAfterSplitCounter = 0;


    public List<Double> splitTimes = new ArrayList<>();

    public List<Double> decomposeTimes = new ArrayList<>();
    
    public final List<Long> splittingCoreSizes = new ArrayList<>();
    public final List<Long> splittingCoreSearchTimes = new ArrayList<>();

    private final String logFilePath;

    public boolean justSplit = false;

    private String algType;

    public DataTracker(String logFilePath, String algType){
        this.logFilePath = logFilePath;
    }

    public void printToConsole() {
        StringBuilder sb = new StringBuilder();
        sb.append("Times split: ").append(splitCounter).append(System.lineSeparator());
        sb.append("Total time spent splitting: ").append(sumSafe(splitTimes)).append(System.lineSeparator());
        sb.append("Average timeToSplit: ").append(average(splitTimes)).append(System.lineSeparator());
        sb.append("Average splitting core size: ").append(longAverage(splittingCoreSizes)).append(System.lineSeparator()).append(System.lineSeparator());

        sb.append("Times decomposed: ").append(decomposeCounter).append(System.lineSeparator());
        sb.append("Total time spent decomposing: ").append(sumSafe(decomposeTimes)).append(System.lineSeparator());
        sb.append("Average decompose time: ").append(average(decomposeTimes)).append(System.lineSeparator()).append(System.lineSeparator());

        sb.append("Times splitting lead to decompose: ").append(decomposeAfterSplitCounter).append(System.lineSeparator());
        sb.append("Proportion of times splitting lead to decompose: ").append((double) decomposeAfterSplitCounter/splitCounter).append(System.lineSeparator());
        System.out.println(sb.toString());
    }

    public void printToLogs() {
        StringBuilder sb = new StringBuilder();
        sb.append("Times split: ").append(splitCounter).append(System.lineSeparator());
        sb.append("Total time spent splitting: ").append(sumSafe(splitTimes)).append(System.lineSeparator());
        sb.append("Average timeToSplit: ").append(average(splitTimes)).append(System.lineSeparator());
        sb.append("Average splitting core size: ").append(longAverage(splittingCoreSizes)).append(System.lineSeparator());

        sb.append("Times decomposed: ").append(decomposeCounter).append(System.lineSeparator());
        sb.append("Total time spent decomposing: ").append(sumSafe(decomposeTimes));
        sb.append("Average decompose time: ").append(average(decomposeTimes)).append(System.lineSeparator());

        sb.append("Times splitting lead to decompose: ").append(decomposeAfterSplitCounter);
        sb.append("Proportion of times splitting lead to decompose: ").append((double) decomposeAfterSplitCounter/splitCounter).append(System.lineSeparator());



        ExperimentTool.appendLineToFile(logFilePath, sb.toString());

    }

    public void toggleSplitFlag() {
        justSplit = !justSplit;
    }

    public void setSplitFlag(boolean newVal) {
        justSplit = newVal;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) return 0.0;

        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    public static double longAverage(List<Long> values) {
        if (values.isEmpty()) return 0.0;

        double sum = 0.0;
        for (long v : values) {
            sum += v;
        }
        return sum / values.size();
    }



    private double sumSafe(List<Double> values) {
        double sum = 0L;
        for (Double v : values) {
            if (v != null) {
                sum += v;
            }
        }
        return sum;
    }


    public void addSplittingCore(long size, long searchTime) {
        splittingCoreSizes.add(size);
        splittingCoreSearchTimes.add(searchTime);

    }



    public List<Long> getSplittingCoreSearchTimes() {
        return splittingCoreSearchTimes;
    }

    public List<Long> getSplittingCoreSizes() {
        return splittingCoreSizes;
    }
}
