package Model;

import Algorithm.Cut;
import utils.TreeUtils;
import utils.UndoMachine;

import java.util.*;
import utils.TreeUtils.*;

public class Forest {
    private List<Node> components = new ArrayList<>();
    private Forest twin;
    private final Map<String, Node> leavesByLabel = new HashMap<>();

    public Forest() {
        this.twin = null;
    }

    public Forest(List<Node> rootNodes) {
        this.components = rootNodes;
        this.twin = null;
    }

    private void buildLeafMap() {
        for (Node component : components) {
            recursiveLeafMapBuilder(component);
        }
    }

    private void recursiveLeafMapBuilder(Node node) {
        if (node.isLeaf()) {
            leavesByLabel.put(node.getLabel(), node);
        } else {
            for (Node child : node.getChildren()) {
                recursiveLeafMapBuilder(child);
            }
        }
    }

    public Map<String, Node> getLeavesByLabel() {
        return leavesByLabel;
    }

    public void setTwin(Forest twin) {
        this.twin = twin;
    }

    public Forest getTwin() {
        return twin;
    }

    public Node getComponent(int index) {
        return components.get(index);
    }

    public List<Node> getComponents() {
        return components;
    }

    public void addComponent(Node root) {
        components.add(root);
    }

    public void addComponent(int pos, Node root) {
        components.add(pos, root);
    }

    /**
     * Removes all internal nodes (isLeaf == false) that have no children.
     * Traverses the tree recursively and cleans up in place.
     *
     * @param root The root of the tree.
     */
    public static void removeEmptyInternalNodes(Node root) {
        if (root == null) return;
        removeEmptyInternalNodesHelper(root);
    }

    private static boolean removeEmptyInternalNodesHelper(Node node) {
        // Leaf nodes are kept as-is
        if (node.isLeaf()) {
            return true;
        }

        // Process children first (post-order traversal)
        List<Node> children = node.getChildren();
        for (int i = 0; i < node.getChildren().size(); i++) {
            if (!removeEmptyInternalNodesHelper(node.getChildren().get(i))) {
                children.remove(i);
                i--;
            }
        }

        // If this node has no children and is not a leaf, remove it
        if (node.getChildren().isEmpty() && !node.isLeaf()) {
            // Never delete the root directly — handled by caller if needed
            return node.isRoot();
        }

        return true;
    }


    public boolean deleteSingletons(UndoMachine um){
        boolean didSomething = false;
        List<String> toBeRemoved = new ArrayList<>();
        Forest T1 = twin;
        for (int i = 0; i < components.size();i++) {
            Node component = components.get(i);
            if (component.isLeaf()){
                didSomething = true;
                components.remove(i);
                if (this.getLeavesByLabel().size() != T1.getLeavesByLabel().size()) {
                    System.out.println("Mismatch of leaves delete singletons start");
                }
                leavesByLabel.remove(component.getLabel());

                Node twin = component.getTwin();
                Node twinParent = twin.getParent();


                T1.getLeavesByLabel().remove(twin.getLabel());
                if (twinParent != null) {
                    if (this.getLeavesByLabel().size() != T1.getLeavesByLabel().size()) {
                        System.out.println("Mismatch of leaves delete singletons after remove from t1");
                    }
                    if (twinParent.isRoot()) {
                        Node newRoot = twin.getSibling();
                        newRoot.setRoot(true);
                        newRoot.setSibling(null);
                        newRoot.setParent(null);
                        T1.components.removeFirst();

                        T1.addComponent(newRoot);

                        um.addEvent(um. new DeleteSingleton(this, component, T1, twin, 1, i));
                    } else {
                        Node siblingOfRemoved = twin.getSibling();
                        Node twinParentParent = twinParent.getParent();

                        int index = twinParentParent.getChildren().indexOf(twinParent);
                        if (index == -1) {
                            System.out.println("Problem with child setting");
                        }
                        twinParentParent.getChildren().remove(index);

                        Node newSiblingOfSibling = twinParentParent.getChildren().getFirst();
                        newSiblingOfSibling.setSibling(siblingOfRemoved);

                        twinParentParent.getChildren().add(index, siblingOfRemoved);

                        siblingOfRemoved.setSibling(newSiblingOfSibling);
                        siblingOfRemoved.setParent(twinParentParent);

                        um.addEvent(um. new DeleteSingleton(this, component, T1, twin, 2, i));
                    }
                } else {
                    T1.components.removeFirst();
                    um.addEvent(um.new DeleteSingleton(this, component, T1, twin, 0, i));

                }
                i--;
            }
            if (this.getLeavesByLabel().size() != T1.getLeavesByLabel().size()) {
                System.out.println("Mismatch of leaves after delete singleton completed");
            }
        }
        return didSomething;
    }

    // Done
    public boolean suppressDegreeTwo(UndoMachine um) {
        boolean didSomething = false;
        for (int i = 0; i < components.size(); i++) {
            Node component = components.get(i);
            if (suppressDegTwo(component, um)) {
                didSomething = true;
            }
        }
        return didSomething;
    }

    private boolean suppressDegTwo(Node node, UndoMachine um) {
        boolean didSomething = false;
        if (node.getChildren().size() == 1) {
            Node child = node.getChildren().getFirst();
            if (node.isRoot()) {
                child.setParent(null);
                child.setRoot(true);

                int index = components.indexOf(node);
                components.remove(index);
                components.add(index, child);

                um.addEvent(um.new SuppressDegreeTwo(this, null, node, child, index));

            } else {

                Node parent = node.getParent();

                child.setParent(parent);
                if (node.getSibling() != null) {
                    child.setSibling(node.getSibling());
                    child.getSibling().setSibling(child);
                }


                int indexInChildList = parent.getChildren().indexOf(node);
                parent.getChildren().remove(indexInChildList);
                parent.getChildren().add(indexInChildList, child);

                um.addEvent(um.new SuppressDegreeTwo(this, parent, node, child, indexInChildList));

            }
            didSomething = true;
            suppressDegTwo(child, um);
        } else {
            for (int i = 0; i < node.getChildren().size(); i++) {
                Node child = node.getChildren().get(i);
                if (suppressDegTwo(child, um)){
                    didSomething = true;
                }
            }
        }
        return didSomething;
    }

//    public void suppressDegreeTwoOld(UndoMachine um) {
//        for (int i = 0; i < components.size(); i++) {
//            Node component = components.get(i);
//            suppressDegreeTwoHelper(component, um);
//            if (component.isRoot() && component.getChildren().size() == 1) {
//                components.add(component.getChildren().getFirst());
//                components.remove(component);
//            }
//        }
//    }
//
//    private Node suppressDegreeTwoHelper(Node node, UndoMachine um) {
//        List<Node> newChildren = new ArrayList<>();
//        for (Node child : node.getChildren()) {
//            Node suppressedChild = suppressDegreeTwoHelper(child, um);
//            suppressedChild.setParent(node);
//            newChildren.add(suppressedChild);
//        }
//        node.setChildren(newChildren);
//
//        if (node.getParent() != null && node.getChildren().size() == 1) {
//            Node onlyChild = node.getChildren().getFirst();
//
//            // Reconnect parent <-> child
//            Node parent = node.getParent();
//            onlyChild.setParent(parent);
//
//            return onlyChild;
//        }
//
//        if (node.getLabel() == "9") {
//            System.out.println("Pause here");
//        }
//        return node;
//    }

    public boolean reduceCommonCherries(Forest pairedForest, UndoMachine um) {
        boolean didReduce = false;
        List<String> removedNodes = new ArrayList<>();
        Set<Node> exploredNodes = new HashSet<>();
        for (Node leaf : leavesByLabel.values()) {
            if (!exploredNodes.contains(leaf)) {
                Node parent = leaf.getParent();
                Node parentOfTwin = leaf.getTwin().getParent();
                if (parent != null && parentOfTwin != null) {
                    if (sameCherry(leaf)) {
                        exploredNodes.add(leaf.getSibling());
                        removedNodes.add(leaf.getSibling().getLabel());
                        removeCherry(leaf, um);
                        didReduce = true;

                    }
                }
            }
        }
        for (String label : removedNodes) {
            this.leavesByLabel.remove(label);
            pairedForest.getLeavesByLabel().remove(label);
        }
        return didReduce;
    }

    public void removeCherry(Node leaf, UndoMachine um) {
        Node sibling = leaf.getSibling();
        Node parent = leaf.getParent();

        Node twin = leaf.getTwin();
        Node twinSibling = twin.getSibling();
        Node twinParent = twin.getParent();

        removeCherryHelper(leaf, sibling, parent, this, um);
        removeCherryHelper(twin, twinSibling, twinParent, this.getTwin(), um);
    }

    public void removeCherryHelper(Node leaf, Node sibling, Node parent, Forest F, UndoMachine um) {
        if (parent.isRoot()) {
            int index = F.getComponents().indexOf(parent);
            F.getComponents().remove(index);
            F.addComponent(index, leaf);
            leaf.setParent(null);
            leaf.setRoot(true);
            leaf.setSibling(null);
            um.addEvent(um.new ReduceCherry(F, leaf, sibling, parent, true));
        } else {
            Node grandParent = parent.getParent();
            int index = grandParent.getChildren().indexOf(parent);
            grandParent.getChildren().remove(index);
            grandParent.addChild(index, leaf);

            leaf.setParent(grandParent);
            leaf.setSibling(parent.getSibling());
            leaf.getSibling().setSibling(leaf);
            um.addEvent(um.new ReduceCherry(F, leaf, sibling, parent, false));

        }
    }



//    public List<String> removeCherry(Node leaf, Node parent, Forest pairedForest, UndoMachine um) {
//        um.addEvent(um.new ReduceCherry(this, leaf, leaf.getSibling(), parent, false));
//
//        List<String> removedNodes = new ArrayList<>();
//
//        Node oldSibling = leaf.getSibling();
//        oldSibling.setSibling(null);
//
//        Node twinNode = leaf.getTwin();
//        Node parentOfTwin = twinNode.getParent();
//        Node oldTwinSibling = twinNode.getSibling();
//        oldTwinSibling.setSibling(null);
//
//        um.addEvent(um.new ReduceCherry(pairedForest, twinNode, oldTwinSibling, parentOfTwin, false));
//
//        removedNodes.add(leaf.getSibling().getLabel());
//        System.out.println("removing sibling of " + leaf.getLabel());
//        if (parent.isRoot()) {
//            removedNodes.add(parent.getLabel());
//            leaf.setRoot(true);
//            addComponent(leaf);
//        }
//        Node parentOfParentOfTwin = parentOfTwin.getParent();
//        Node parentOfParent = parent.getParent();
//
//        parentOfParent.getChildren().remove(parent);
//        parentOfParent.addChild(leaf);
//
//        leaf.setParent(parentOfParent);
//        twinNode.setParent(parentOfParentOfTwin);
//
//        parentOfParentOfTwin.getChildren().remove(parentOfTwin);
//        parentOfParentOfTwin.addChild(twinNode);
//
//
//
//        List<Node> siblings = parentOfParent.getChildren();
//        if (siblings.size() == 2) { // assuming binary tree
//            // Set sibling to the other child
//            if (siblings.get(0) == leaf) {
//                leaf.setSibling(siblings.get(1));
//                siblings.get(1).setSibling(leaf);
//            } else {
//                leaf.setSibling(siblings.getFirst());
//                siblings.get(0).setSibling(leaf);
//            }
//        } else {
//            // No sibling or more than 2 children — set to null or first alternative
//            leaf.setSibling(null);
//        }
//
//        List<Node> twinSiblings = parentOfParentOfTwin.getChildren();
//        if (twinSiblings.size() == 2) { // assuming binary tree
//            // Set sibling to the other child
//            if (twinSiblings.get(0) == twinNode) {
//                twinNode.setSibling(twinSiblings.get(1));
//                twinSiblings.get(1).setSibling(twinNode);
//            } else {
//                twinNode.setSibling(twinSiblings.getFirst());
//                twinSiblings.get(0).setSibling(twinNode);
//            }
//        } else {
//            // No sibling or more than 2 children — set to null or first alternative
//            twinNode.setSibling(null);
//        }
//
//
//        //TreeUtils.printAsciiTree(pairedForest.getComponent(0));
//        //TreeUtils.printAsciiTree(getComponent(0));
//        if (sameCherry(leaf)) {
//            removeCherry(leaf, parentOfParent, pairedForest, um);
//        }
//
//        return removedNodes;
//    }



    public boolean sameCherry(Node leaf) {
        if (leaf.getSibling().isLeaf() && leaf.getTwin().getSibling().isLeaf()) {
            return Objects.equals(leaf.getSibling().getLabel(), leaf.getTwin().getSibling().getLabel());
        }
        return false;
    }

    private static String[] split(String s) {
        ArrayList<Integer> splitIndices = getIntegers(s);

        int numSplits = splitIndices.size() + 1;
        String[] splits = new String[numSplits];

        if (numSplits == 1) {
            splits[0] = s;
        } else {

            splits[0] = s.substring(0, splitIndices.getFirst());

            for (int i = 1; i < splitIndices.size(); i++) {
                splits[i] = s.substring(splitIndices.get(i - 1) + 1, splitIndices.get(i));
            }

            splits[numSplits - 1] = s.substring(splitIndices.getLast() + 1);
        }

        return splits;
    }

    private static ArrayList<Integer> getIntegers(String s) {
        ArrayList<Integer> splitIndices = new ArrayList<>();
        int rightParenCount = 0;
        int leftParenCount = 0;
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '(':
                    leftParenCount++;
                    break;
                case ')':
                    rightParenCount++;
                    break;
                case ',':
                    if (leftParenCount == rightParenCount) splitIndices.add(i);
                    break;
            }
        }
        return splitIndices;
    }

    public static Forest readNewickFormat(String newick) {
        return new Forest().innerReadNewickFormat(newick);
    }

    private Forest innerReadNewickFormat(String newick) {
        newick = newick.trim();
        if (newick.endsWith(";")) {

            this.components.add(readSubtree(newick.substring(0, newick.length() - 1), 0));
        } else {
            this.components.add(readSubtree(newick, 0));
        }
        return this;
    }

    private Node readSubtree(String newick, int depth) {
        int leftParen = newick.indexOf('(');
        int rightParen = newick.lastIndexOf(')');

        if (leftParen != -1 && rightParen != -1) {

            String name = newick.substring(rightParen + 1);
            String[] childrenString = split(newick.substring(leftParen + 1, rightParen));

            Node node = new Node();
            node.setDepth(depth);
            List<Node> children = new ArrayList<>();
            node.setRoot(depth == 0);

            for (String sub : childrenString) {
                Node child = readSubtree(sub, depth+1);
                node.addChild(child);
                child.setParent(node);
            }
            return node;
        } else if (leftParen == rightParen) {

            Node node = new Node(newick);
            node.setDepth(depth);
            node.setLeaf(true);
            node.setRoot(false);
            this.leavesByLabel.put(newick, node);

            return node;

        } else throw new RuntimeException("unbalanced ()'s");

    }



    public static void test1(){
        Forest F2 = readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
        Forest F3 = readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");

        UndoMachine um = new UndoMachine();
        TreeUtils.linkForests(F2, F3);
        TreeUtils.linkSiblings(F3);
        TreeUtils.linkSiblings(F2);
        System.out.println("Initial Forests --------------------------------------------------------");
        if (TreeUtils.checkDescendantRelations(F2)) {
            System.out.println("correct relations");
        } else {
            System.out.println("NOT CORRECT RELATIONS");
        }
//        System.out.println("    F1");
////        TreeUtils.printAsciiTree(F2.getComponent(0));
//        System.out.println("    F2");
//        TreeUtils.printAsciiTree(F3.getComponent(0));
        F3.reduceCommonCherries(F2, um);
        System.out.println("After cherry reduction --------------------------------------------------------");
//        System.out.println("    F1");
//        TreeUtils.printAsciiTree(F2.getComponent(0));
//        TreeUtils.printAsciiTree(F3.getComponent(0));
        if (TreeUtils.checkDescendantRelations(F2)) {
            System.out.println("correct relations");
        } else {
            System.out.println("NOT CORRECT RELATIONS");
        }
        um.undoAll();
        System.out.println("after Undo");
        //TreeUtils.printAsciiTree(F3.getComponent(0));
        //System.out.println("    F2");
        //TreeUtils.printAsciiTree(F2.getComponent(0));

        if (TreeUtils.checkDescendantRelations(F2)) {
            System.out.println("correct relations");
        } else {
            System.out.println("NOT CORRECT RELATIONS");
        }
        Cut cut = new Cut(F3.getLeavesByLabel().get("9").getParent(), F3.getLeavesByLabel().get("9"), F3, F3.getLeavesByLabel().get("9").getParent().getChildren().indexOf(F3.getLeavesByLabel().get("9")));


        cut.makeCut();
        System.out.println("after cut");
        if (TreeUtils.checkDescendantRelations(F2)) {
            System.out.println("correct relations");
        } else {
            System.out.println("NOT CORRECT RELATIONS");
        }
//        TreeUtils.printAsciiTree(F3.getComponent(0));
//        TreeUtils.printAsciiTree(F3.getComponent(1));


        F3.deleteSingletons(um);



        System.out.println("after singleton removed");
        if (TreeUtils.checkDescendantRelations(F2)) {
            System.out.println("correct relations");
        } else {
            System.out.println("NOT CORRECT RELATIONS");
        }
        //TreeUtils.printAsciiTree(F3.getComponent(0));
        if (F3.getComponents().size() == 1) {System.out.println("successfully removed singleton");}
        //TreeUtils.printAsciiTree(F2.getComponent(0));
        //um.undoAll();

        F3.suppressDegreeTwo(um);


        System.out.println("after deg 2 suppressed");
        if (TreeUtils.checkDescendantRelations(F2)) {
            System.out.println("correct relations");
        } else {
            System.out.println("NOT CORRECT RELATIONS");
        }
        //TreeUtils.printAsciiTree(F3.getComponent(0));
        //TreeUtils.printAsciiTree(F3.getComponent(1));
        //TreeUtils.printAsciiTree(F2.getComponent(0));
        System.out.println("wew lad");
    }

    public static void testDegree2(){
        Forest F2 = readNewickFormat("((((((((1,2)))),(3,4)),((5,6),(7,8)))))");
        TreeUtils.printAsciiTree(F2.getComponent(0));
        TreeUtils.linkSiblings(F2);
        UndoMachine um = new UndoMachine();
        F2.suppressDegreeTwo(um);
        TreeUtils.printAsciiTree(F2.getComponent(0));

        um.undoAll();

        TreeUtils.printAsciiTree(F2.getComponent(0));
    }

    public static void testCut() {
        Forest F1 = readNewickFormat("(((1,2),(3,4)),((5,6),(7,8)))");
        Forest F2 = readNewickFormat("((7,8),((1,(2,(3,5))),(6,4)))");
        F1.setTwin(F2);
        F2.setTwin(F1);
        UndoMachine um = new UndoMachine();
        TreeUtils.linkForests(F1, F2);
        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);
        System.out.println("Initial Forests --------------------------------------------------------");
        System.out.println("    F1");
        TreeUtils.printAsciiTree(F1.getComponent(0));
        System.out.println("    F2");
        TreeUtils.printAsciiTree(F2.getComponent(0));

        F2.reduceCommonCherries(F1, um);

        System.out.println("Reduced --------------------------------------------------------");
        System.out.println("    F1");
        TreeUtils.printAsciiTree(F1.getComponent(0));
        System.out.println("    F2");
        TreeUtils.printAsciiTree(F2.getComponent(0));

        um.undoAll();

        System.out.println("Undo Reduced --------------------------------------------------------");
        System.out.println("    F1");
        TreeUtils.printAsciiTree(F1.getComponent(0));
        System.out.println("    F2");
        TreeUtils.printAsciiTree(F2.getComponent(0));

    }


    public static void main(String[] args) {
        Node root = new Node("0", 0, false, true);
        Node node1 = new Node("1", 1, false, false);
        Node node2 = new Node("3", 2, true, false);
        Node node3 = new Node("4", 2, true, false);

        root.addChild(node1);
        node1.addChild(node2);
        node1.addChild(node3);

        Node croot = new Node("0", 0, false, true);
        Node cnode1 = new Node("1", 1, false, false);
        Node cnode2 = new Node("3", 2, true, false);
        Node cnode3 = new Node("4", 2, true, false);

        root.setTwin(croot);
        croot.setTwin(root);

        node1.setTwin(cnode1);
        cnode1.setTwin(node1);
        node2.setTwin(cnode2);
        cnode2.setTwin(node2);
        node3.setTwin(cnode3);
        cnode3.setTwin(node3);



//        System.out.println(root.getChildren());
//        List<Node> components = new ArrayList<>();
//        components.add(root);
//        Forest F1 = new Forest(components);
//        F1.suppressDegreeTwo(new UndoMachine());
//        String label = F1.getComponent(0).getLabel();
//        System.out.println(label);
//        System.out.println(F1.getComponent(0).getChildren());
//        TreeUtils.printAsciiTree(root);
        //Forest.test1();
        //testCut();

        Forest F1 = readNewickFormat("(((1,2),(3,4)),((5,6),(7,8)))");
        TreeUtils.linkSiblings(F1);

        Node remove = F1.getLeavesByLabel().get("2");
        Node pRemove = remove.getParent();

        UndoMachine um = new UndoMachine();

        Cut cut = new Cut(pRemove, remove, F1, 2);
        um.addEvent(um.new MakeCut(cut, F1));
        if (!TreeUtils.checkDescendantRelations(F1)) {
            System.out.println("F1 parent problem");
        }

        cut.makeCut();

        if (!TreeUtils.checkDescendantRelations(F1)) {
            System.out.println("F1 parent problem");
        }



        System.out.println(F1.getComponent(0).getChildren().get(0).getDepth());


    }
}
