package Model;

import utils.TreeUtils;
import utils.UndoMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProblemInstance {
    public Forest F1, F2;
    public Forest originalF2;

    public ProblemInstance(Forest F1, Forest F2) {
        this.F1 = F1;
        this.F2 = F2;
        this.originalF2 = new Forest(F2);
        TreeUtils.assignUniqueIds(F2.getComponent(0), 0);
        TreeUtils.assignUniqueIds(originalF2.getComponent(0), 0);
    }

    public ProblemInstance(Forest F1, Forest F2, boolean a) {
        this.F1 = F1;
        this.F2 = F2;
    }

    public ProblemInstance(ProblemInstance original) {
        Forest T1 = new Forest(original.getF1());
        Forest F2 = new Forest(original.getF2());
        TreeUtils.linkSiblings(T1);
        TreeUtils.linkSiblings(F2);
        TreeUtils.linkForests(T1, F2);

        this.F1 = T1;
        this.F2 = F2;
        this.originalF2 = null;


    }

    public ProblemInstance makeSubProblem(Node rootInF2, Set<String> leafSet) {
//        System.out.println("Printing trees for decompose");
//        printTrees();
        List<Node> F2Components = new ArrayList<>();
        F2Components.add(new Node(rootInF2));
        Forest F2ofSubproblem = new Forest(F2Components);

        Node rootOFSubT1 = new Node(F1.getComponent(0), leafSet);
        List<Node> F1Components = new ArrayList<>();
        F1Components.add(rootOFSubT1);
        Forest T1ofSubproblem = new Forest(F1Components);

        T1ofSubproblem.buildLeafMap();
        F2ofSubproblem.buildLeafMap();

        //TreeUtils.printAsciiTree(rootOFSubT1);
        Forest.removeEmptyInternalNodes(T1ofSubproblem.getComponent(0));
        //TreeUtils.printAsciiTree(rootOFSubT1);
        T1ofSubproblem.suppressDegreeTwo(new UndoMachine());
        //TreeUtils.printAsciiTree(rootOFSubT1);
        TreeUtils.linkSiblings(F2ofSubproblem);
        TreeUtils.linkSiblings(T1ofSubproblem);
        // TODO PROBLEM HERE FROM DECOMPOSE
        TreeUtils.linkForests(T1ofSubproblem, F2ofSubproblem);


        return new ProblemInstance(T1ofSubproblem, F2ofSubproblem, true);
    }

    public Forest getF1() {
        return F1;
    }

    public Forest getF2() {
        return F2;
    }

    public Forest getOriginalF2() {
        return originalF2;
    }

    public void printTrees() {
        System.out.println("T1:");
        if (F1.getComponents().isEmpty()) {
            System.out.println("Tree is empty");
        } else {
            TreeUtils.printAsciiTree(F1.getComponent(0));
        }

        System.out.println("F2:");
        if (F2.getComponents().isEmpty()) {
            System.out.println("Tree is empty");
        } else {
            for (Node component : F2.getComponents()) {
                TreeUtils.printAsciiTree(component);
            }
        }
    }

    public static void main(String[] args) {
        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");


        Forest F2 = Forest.readNewickFormat("((1,3),(2,4))");
        TreeUtils.linkSiblings(F2);
        Forest c2 = Forest.readNewickFormat("((5,8),(7,6))");
        TreeUtils.linkSiblings(c2);
        Forest c3 = Forest.readNewickFormat("((9,11),(10,12))");
        TreeUtils.linkSiblings(c3);
        Forest c4 = Forest.readNewickFormat("((13,15),(14,16))");
        TreeUtils.linkSiblings(c4);

        F2.addComponent(c2.getComponent(0));
        F2.addComponent(c3.getComponent(0));
        F2.addComponent(c4.getComponent(0));

        F2.getLeavesByLabel().putAll(c2.getLeavesByLabel());
        F2.getLeavesByLabel().putAll(c3.getLeavesByLabel());
        F2.getLeavesByLabel().putAll(c4.getLeavesByLabel());

        TreeUtils.linkSiblings(F1);

        TreeUtils.linkForests(F1, F2);

        ProblemInstance pi = new ProblemInstance(F1, F2);

        ProblemInstance sub = pi.makeSubProblem(F2.getComponent(2), TreeUtils.getLeafSet(F2.getComponent(2)));

        sub.printTrees();
    }
}
