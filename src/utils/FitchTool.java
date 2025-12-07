package utils;

import Model.Forest;
import Model.Node;
import Model.ProblemInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FitchTool {
    List<Set<String>> colours = new ArrayList<>();
    List<Node> rootOfColoursF2 = new ArrayList<>();
    int pScore;

    public FitchTool(ProblemInstance pI) {
        for (Node component : pI.getF2().getComponents()) {
            colours.add(TreeUtils.getLeafSet(component));
            rootOfColoursF2.add(component);
        }
        pI.getF2().setLeafLabelsList(colours);
        fitchHelper(pI.getF1().getComponent(0));
    }

    public int getPScore() {
        return pScore;
    }

    public List<Set<String>> getColours() {
        return colours;
    }

    private Set<Integer> fitchHelper(Node node) {
        if (node.isLeaf()) {
            Set<Integer> colourSet = new HashSet<>();
            for (int i = 0; i < colours.size(); i++) {
                if (colours.get(i).contains(node.getLabel())) {
                    colourSet.add(i);
                }
            }
            return colourSet;
        } else {
            Set<Integer> s1 = fitchHelper(node.getChildren().getFirst());
            Set<Integer> s2 = fitchHelper(node.getChildren().getLast());

            Set<Integer> intersection = new HashSet<>(s1);

            intersection.retainAll(s2);

            if (!intersection.isEmpty()) {
                return intersection;
            } else {
                if (!s1.isEmpty() && !s2.isEmpty()) {pScore++;}
                Set<Integer> union = new HashSet<>();
                union.addAll(s1);
                union.addAll(s2);
                return union;
            }
        }
    }

    public static void main(String[] args) {
        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
        Forest F2 = Forest.readNewickFormat("((1,2),(3,4))");
        Forest c2 = Forest.readNewickFormat("((5,6),(7,8))");
        Forest c3 = Forest.readNewickFormat("(((9,10),(11,12)),((13,14),(15,16)))");

        F2.addComponent(c2.getComponent(0));
        F2.addComponent(c3.getComponent(0));

        F2.getLeavesByLabel().putAll(c2.getLeavesByLabel());
        F2.getLeavesByLabel().putAll(c3.getLeavesByLabel());

        TreeUtils.linkForests(F1, F2);
        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);

        ProblemInstance instance = new ProblemInstance(F1, F2);
        FitchTool fitch = new FitchTool(instance);

        System.out.println(fitch.pScore);
    }
}
