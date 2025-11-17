package Model;

import utils.TreeUtils;
import utils.UndoMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProblemInstance {
    public Forest F1, F2;

    public ProblemInstance(Forest F1, Forest F2) {
        this.F1 = F1;
        this.F2 = F2;
    }

    public ProblemInstance makeSubProblem(Node rootInF2, Set<String> leafSet) {
        List<Node> F2Components = new ArrayList<>();
        F2Components.add(new Node(rootInF2));
        Forest F2ofSubproblem = new Forest(F2Components);

        Node rootOFSubT1 = new Node(F1.getComponent(0), leafSet);
        List<Node> F1Components = new ArrayList<>();
        F1Components.add(rootOFSubT1);
        Forest T1ofSubproblem = new Forest(F1Components);
        //TreeUtils.printAsciiTree(rootOFSubT1);
        Forest.removeEmptyInternalNodes(T1ofSubproblem.getComponent(0));
        //TreeUtils.printAsciiTree(rootOFSubT1);
        T1ofSubproblem.suppressDegreeTwo(new UndoMachine());
        //TreeUtils.printAsciiTree(rootOFSubT1);
        TreeUtils.linkSiblings(F2ofSubproblem);
        TreeUtils.linkSiblings(T1ofSubproblem);
        TreeUtils.linkForests(T1ofSubproblem, F2ofSubproblem);
        return new ProblemInstance(T1ofSubproblem, F2ofSubproblem);
    }

    public Forest getF1() {
        return F1;
    }

    public Forest getF2() {
        return F2;
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
}
