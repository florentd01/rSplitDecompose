package utils;


import Algorithm.Cut;
import Algorithm.SplitTool;
import Model.Forest;
import Model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UndoMachine {
    public List<Undoable> undoables = new ArrayList<>();
    public int size;


    public UndoMachine() {
        size = 0;
    }

    public void addEvent(Undoable event) {
        this.undoables.add(event);
        this.size++;
    }

    public void undoAll(){
        for (int i = undoables.size() - 1; i >= 0; i--) {
            undoables.get(i).undo();
        }

    }

    public class MakeSplitCut implements Undoable {
        SplitTool.SplitTreeNode root;
        SplitTool.SplitTreeNode parent;
        SplitTool.SplitTreeNode child;
        int index;

        public MakeSplitCut(SplitTool.SplitTreeNode parent, SplitTool.SplitTreeNode child, int index) {
            this.parent = parent;
            this.child =  child;
            this.index = index;
        }


        @Override
        public void undo() {

        }
    }



    public class MakeCut implements Undoable {
        Forest F;
        Node parent;
        Node child;
        int index;


        public MakeCut (Cut cut, Forest F) {
            this.parent = cut.getProblemParent();
            this.child = cut.getProblemChild();
            this.F = F;
            if (parent == null) {
                System.out.println("WEEWOO ALARM");
            }
            this.index = parent.getChildren().indexOf(child);
        }


        @Override
        public void undo() {
            parent.addChild(index, child);
            child.setParent(parent);
            child.setRoot(false);
            F.getComponents().remove(child);
            child.getSibling().setSibling(child);
        }
    }

    // TODO: finish new undo()
    public class ReduceCherry implements Undoable{
        Forest F;
        Node remainingNode;
        Node removedNode;
        Node parent;
        boolean onRoot;

        public ReduceCherry(Forest F, Node remainingNode, Node removedNode, Node parent, boolean onRoot) {
            this.F = F;
            this.remainingNode = remainingNode;
            this.removedNode = removedNode;
            this.parent = parent;
            this.onRoot = onRoot;
        }

//        public void undoOld() {
//            List<Node> cherrySiblings = new ArrayList<>();
//            cherrySiblings.add(remainingNode);
//            cherrySiblings.add(removedNode);
//            parent.setChildren(cherrySiblings);
//
//            remainingNode.setParent(parent);
//            remainingNode.setSibling(removedNode);
//
//            removedNode.setParent(parent);
//            removedNode.setSibling(remainingNode);
//
//            F.getLeavesByLabel().put(removedNode.getLabel(), removedNode);
//
//            if (onRoot) {
//                F.getComponents().remove(remainingNode);
//                F.addComponent(parent);
//
//                remainingNode.setRoot(false);
//
//            } else {
//
//
//                Node parentOfParent = parent.getParent();
//                parentOfParent.getChildren().remove(remainingNode);
//                parentOfParent.addChild(parent);
//            }
//        }

        public void undo() {
            F.getLeavesByLabel().put(removedNode.getLabel(), removedNode);
            if (onRoot) {
                int index = F.getComponents().indexOf(remainingNode);

                remainingNode.setSibling(removedNode);
                remainingNode.setRoot(false);
                remainingNode.setParent(parent);

                F.getComponents().remove(index);
                F.addComponent(index, parent);
            } else {
                Node grandParent = remainingNode.getParent();
                int index = grandParent.getChildren().indexOf(remainingNode);

                remainingNode.getSibling().setSibling(parent);
                remainingNode.setSibling(removedNode);
                remainingNode.setParent(parent);

                grandParent.getChildren().remove(index);
                grandParent.addChild(index, parent);
            }
        }
    }

    public class DeleteSingleton implements Undoable {
        Forest F2;
        Node component;

        Forest T1;
        Node componentInT1;

        int caseNum;
        int indexF2;

        public DeleteSingleton(Forest F , Node component, Forest T1, Node componentInT1, int caseNum, int indexF2) {
            this.F2 = F;
            this.component = component;
            this.T1 = T1;
            this.componentInT1 = componentInT1;
            this.caseNum = caseNum;
            this.indexF2 = indexF2;
        }

        @Override
        public void undo() {
            F2.getLeavesByLabel().put(component.getLabel(), component);
            F2.addComponent(indexF2, component);
            switch (caseNum) {
                case 0:
                    System.out.println("we got here somehow");
                    T1.addComponent(componentInT1);
                    T1.getLeavesByLabel().put(componentInT1.getLabel(), componentInT1);
                    break;
                case 1:
                    Node oldSibling = T1.getComponents().removeFirst();
                    oldSibling.setRoot(false);
                    oldSibling.setSibling(componentInT1);
                    oldSibling.setParent(componentInT1.getParent());
                    T1.addComponent(componentInT1.getParent());
                    T1.getLeavesByLabel().put(componentInT1.getLabel(), componentInT1);
                    break;
                case 2:
                    Node parentOfRemoved = componentInT1.getParent();
                    Node parentOfParentOfR = parentOfRemoved.getParent();

                    Node siblingOfRemoved = componentInT1.getSibling();
                    Node newSiblingOfSibling = siblingOfRemoved.getSibling();


                    int index = parentOfParentOfR.getChildren().indexOf(siblingOfRemoved);
                    parentOfParentOfR.getChildren().remove(index);
                    parentOfParentOfR.addChild(index, parentOfRemoved);

                    newSiblingOfSibling.setSibling(parentOfRemoved);

                    siblingOfRemoved.setSibling(componentInT1);
                    siblingOfRemoved.setParent(parentOfRemoved);
                    T1.getLeavesByLabel().put(componentInT1.getLabel(), componentInT1);
                    break;

            }
        }
    }


    // TODO: Write comments
    public class SuppressDegreeTwo implements Undoable {
        Forest F;
        Node parentOfSuppressed;
        Node suppressedNode;
        Node childOfSuppressed;
        int index;


        public SuppressDegreeTwo(Forest F, Node parent, Node suppressedNode, Node childOfSuppressed, int index) {
            this.F = F;
            this.parentOfSuppressed = parent;
            this.suppressedNode = suppressedNode;
            this.childOfSuppressed = childOfSuppressed;
            this.index = index;

        }

        @Override
        public void undo() {
            if (parentOfSuppressed == null) {

                F.getComponents().remove(index);
                F.getComponents().add(index, suppressedNode);

                childOfSuppressed.setParent(suppressedNode);
                childOfSuppressed.setRoot(false);
            } else {
                int index = parentOfSuppressed.getChildren().indexOf(childOfSuppressed);
                Node sibling = childOfSuppressed.getSibling();
                if (sibling!= null) {
                    childOfSuppressed.setSibling(null);
                    sibling.setSibling(suppressedNode);
                }
                parentOfSuppressed.getChildren().remove(index);
                parentOfSuppressed.getChildren().add(index, suppressedNode);
                childOfSuppressed.setParent(suppressedNode);
            }
        }
    }


    public interface Undoable {
        void undo();
    }
}
