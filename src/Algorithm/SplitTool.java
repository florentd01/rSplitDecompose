package Algorithm;

/*
    Plan:
        In fitch tool keep track of unions for each colo


 */

import Model.Forest;
import Model.Node;
import Model.ProblemInstance;
import utils.FitchTool;
import utils.TreeUtils;
import utils.UndoMachine;

import java.util.*;

public class SplitTool {
    ProblemInstance problemInstance;
    List<Conflict> splittingCores = new ArrayList<>();
    Random random;

    int k;

    List<Cut> cutList;

    public SplitTool(ProblemInstance pI, int k, Random r) {
        problemInstance = pI;;
        //System.out.println("New splitter created");
        this.k = k;
        this.random = r;
    }


    //TODO: Main returns a List Conflict with all the cuts to be made in a component of F to complete split operation
    // each conflicts list of cuts corresponds to one splitting core on the component of F (the list includes splitting
    // cores for both branches of which overlapping component in T the selected edge was assigned to

    public List<Conflict> findSplitConflicts() {
        Random r = new Random(1);
        // chose 2 overlapping components
        List<OverlappingPair> overlappingPairsList = findOverlappingComponents();
        OverlappingPair chosenPair = overlappingPairsList.get(r.nextInt(overlappingPairsList.size()));

        List<Node> components = chosenPair.components();

        List<Node> commonNodesInT = chosenPair.commonNodes();

        if (commonNodesInT.isEmpty()) {
            TreeUtils.printAsciiTree(components.getFirst());
            TreeUtils.printAsciiTree(components.getLast());
            System.out.println("break here");
        }

        // chose a shared edge
        Node selectedEdge = commonNodesInT.get(r.nextInt(commonNodesInT.size()));
        //FsSSystem.out.println("Selected edge node: ");
        //TreeUtils.printAsciiTree(selectedEdge);


        // For each assignment of an edge do:
        //      Separate the losing component into 2 pieces,
        //      generate conflicts which have a list of cuts in order to separate those 2 pieces in F
        // Combine both lists of conflicts into one and return
        List<Conflict> splittingCores = new ArrayList<>();
        for (int i = 0; i<2; i++) {
            Node componentToSplit = components.get(i);
            Set<String> leafLabels = TreeUtils.getLeafSet(componentToSplit);
            List<Set<String>> splitLeafSets = separateComponent(selectedEdge, leafLabels);
//            for (Set<String> set : splitLeafSets) {
//                System.out.println(set);
//            }f
            SplitTreeNode root = new SplitTreeNode(componentToSplit);
            root.fillDescendantLeaves();
            List<Cut> cuts = new ArrayList<>();
            setSplittingCores(root, splitLeafSets, cuts);

        }

        return this.splittingCores;
    }




    /**
     * recursive Method that takes in 2 sets of leaf labels from a component in F that were separated by edge in T
     * assigned to different component and return a splitting core (list of conflicts)
     *
     *
     *
     * @param root        the root node of the tree
     * @param leafSets  2 sets of leaf node labels to be separated
     * Adds conflicts to list of cuts
     */

    public void setSplittingCores(SplitTreeNode root, List<Set<String>> leafSets, List<Cut> cutsToGetHere){
        //System.out.println("We took " + cutsToGetHere.size() + " cuts to get to this splitting core");
        if (cutsToGetHere.size() > this.k) {
            return;
        }
        if (areSeparatedAtRoot(root, leafSets)) {
            Conflict splittingCore = new Conflict(cutsToGetHere);
            this.splittingCores.add(splittingCore);
        } else {
            SplitTreeNode branchingNode = getBranchingNode(root, leafSets.getFirst(), leafSets.getLast());
            for (int i = 0; i < branchingNode.getChildren().size(); i++) {

                // try both cuts
                SplitTreeNode cutChild = branchingNode.getChildren().get(i);
                makeSplitCut(branchingNode, cutChild);

                UndoMachine um = new UndoMachine();
                //System.out.println("undoing cut");
                um.addEvent(um.new MakeSplitCut(branchingNode, cutChild, i));
                // update cut list for real tree
                cutsToGetHere.add(new Cut(branchingNode.realTwin, cutChild.realTwin, problemInstance.getF2(), i));

                // recurse to check if the component is now split
                setSplittingCores(root, leafSets, cutsToGetHere);

                // undo splitCut and remove real cut from cutsToGetHere list
                um.undoAll();
                cutsToGetHere.removeLast();

            }

        }

    }

    // method that checks if the two pieces are separated
    public boolean areSeparatedAtRoot(SplitTreeNode root, List<Set<String>> leafSets) {

        Set<String> target = root.getDescendantLabels();
        if(target == null) {
            System.out.println("pause");
        }

        Set<String> s1 = new HashSet<>(leafSets.getFirst());
        Set<String> s2 = new HashSet<>(leafSets.getLast());

        s1.retainAll(target);
        s2.retainAll(target);

        boolean intersect1 = !s1.isEmpty();
        boolean intersect2 = !s2.isEmpty();


        return intersect1 ^ intersect2;
    }



//    public SplitTreeNode getBranchingNode(SplitTreeNode root, List<Set<String>> leafSets) {
//        // check if children are separate if they are return the current node, otherwise recurse on each child
//        boolean separated = true;
//        for (SplitTreeNode child : root.getChildren()) {
//            if (!areSeparatedAtRoot(child, leafSets)) {
//                separated = false;
//            }
//        }
//        if (separated) {
//            return root;
//        } else {
//            for (SplitTreeNode child : root.getChildren()) {
//                SplitTreeNode current = getBranchingNode(child, leafSets);
//                if (current != null) {
//                    return current;
//                }
//            }
//        }
//        return null;
//    }




    public void makeSplitCut(SplitTreeNode parent, SplitTreeNode child) {
        Set<String> cDescendants = child.getDescendantLabels();
        parent.getChildren().remove(child);

        SplitTreeNode temp = parent;

        while (temp != null) {
            temp.getDescendantLabels().removeAll(cDescendants);
            temp = temp.parent;
        }
    }


    /**
     * method for selecting a node to branch on for finding splitting cores
     * @param node      current node
     * @param setA  1st set of leaves
     * @param setB  2nd set of leaves
     * @return node in SplitTree on which to branch
     */


    private static SplitTreeNode getBranchingNode(SplitTreeNode node,
                                                           Set<String> setA,
                                                           Set<String> setB) {
        if (node == null || node.isLeaf()) {
            return null;
        }

        if (node.getChildren().size() == 2) {
            SplitTreeNode c1 = node.getChildren().get(0);
            SplitTreeNode c2 = node.getChildren().get(1);

            Set<String> d1 = c1.getDescendantLabels();
            Set<String> d2 = c2.getDescendantLabels();

            boolean c1InA = isSubsetOf(d1, setA);
            boolean c1InB = isSubsetOf(d1, setB);
            boolean c2InA = isSubsetOf(d2, setA);
            boolean c2InB = isSubsetOf(d2, setB);

            // Each child must belong entirely to exactly one of the sets
            boolean c1Valid = (c1InA ^ c1InB); // XOR: exactly one
            boolean c2Valid = (c2InA ^ c2InB);

            // And they must correspond to different sets
            if (c1Valid && c2Valid && (c1InA != c2InA)) {
                return node;
            }
        }

        // Recurse into children
        for (SplitTreeNode child : node.getChildren()) {
            SplitTreeNode result = getBranchingNode(child, setA, setB);
            if (result != null) return result;
        }

        return null;
    }

    /**
     * Returns true if all elements of subset are contained in superset.
     */
    private static boolean isSubsetOf(Set<String> subset, Set<String> superset) {
        return superset.containsAll(subset);
    }



    public class SplitTreeNode {
        // Helper class for finding splitting cores
        String label;
        Set<String> descendantLabels;
        List<SplitTreeNode> children = new ArrayList<>();
        boolean isLeaf;
        boolean isRoot;


        SplitTreeNode parent;

        Node realTwin;

        public SplitTreeNode(Node sourceRoot) {
            this.label = sourceRoot.getLabel();
            this.isLeaf = sourceRoot.isLeaf();
            this.isRoot = sourceRoot.isRoot();
            this.parent = null;
            this.realTwin = sourceRoot;
            for (Node child : sourceRoot.getChildren()) {
                if (child != null) {
                    SplitTreeNode childCopy = new SplitTreeNode(child);
                    childCopy.parent = this;
                    this.children.add(childCopy);
                }
            }
        }

        public SplitTreeNode getParent() {
            return parent;
        }

        public Set<String> getDescendantLabels() {
            return descendantLabels;
        }

        public SplitTreeNode findSplittingEdge(List<Set<String>> leafSets){

            return null;
        }
        public List<SplitTreeNode> getChildren() {
            return this.children;
        }

        public String getLabel() {
            return label;
        }

        public void printAsciiTree() {
            System.out.println("SplitTree");
            printAsciiTree(this, "", true);
        }

        private static void printAsciiTree(SplitTreeNode node, String prefix, boolean isTail) {
            if (node == null) return;

            // Print this node
            String label = (node.getLabel() != null ? node.getLabel() : "[*]");
            System.out.println(prefix + (isTail ? "└── " : "├── ") + label);

            List<SplitTreeNode> children = node.getChildren();
            for (int i = 0; i < children.size(); i++) {
                boolean last = (i == children.size() - 1);
                printAsciiTree(children.get(i), prefix + (isTail ? "    " : "│   "), last);
            }
        }


        public void setDescendantLeaves(Set<String> leaves) {
            this.descendantLabels = leaves;
        }

        public boolean isLeaf() {
            return this.isLeaf;
        }

        public boolean isRoot() {
            return isRoot;
        }

        /**
         * For every node in the tree, fills its descendantLeaves field with
         * the set of labels of all leaf descendants in its subtree.
         *
         */
        public void fillDescendantLeaves() {

            computeDescendantLeaves(this);
        }

        /**
         * Post-order helper that returns the set of leaf labels under this node
         * and also stores it via node.setDescendantLeaves(...).
         */
        private Set<String> computeDescendantLeaves(SplitTreeNode node) {
            Set<String> leaves = new HashSet<>();

            if (node.isLeaf()) {
                // Leaf: its descendant leaves set is just its own label
                if (node.getLabel() != null) {
                    leaves.add(node.getLabel());

                }
            } else {
                // Internal node: union of children's descendant leaves
                for (SplitTreeNode child : node.getChildren()) {
                    Set<String> childLeaves = computeDescendantLeaves(child);
                    leaves.addAll(childLeaves);
                }
            }

            node.setDescendantLeaves(leaves);
            return leaves;
        }
    }



    /* method takes in an edge in T assigned to component X and also takes component Y (did not get edge) leaf labels,
     *  returns 2 sets of leaf labels for the parts of Y that are separated by edge.
     */
    public List<Set<String>> separateComponent(Node edgeHead, Set<String> componentLabels) {
        List<Set<String>> postSplitLeafSets = new ArrayList<>();


        Set<String> childLabelsFromCutEdge = TreeUtils.getLeafSet(edgeHead);

        Set<String> componentFirstHalf = new HashSet<>(componentLabels);
        componentFirstHalf.retainAll(childLabelsFromCutEdge);

        Set<String> componentSecondHalf = new HashSet<>(componentLabels);
        componentSecondHalf.removeAll(componentFirstHalf);

        postSplitLeafSets.add(componentFirstHalf);

        postSplitLeafSets.add(componentSecondHalf);



        return postSplitLeafSets;
    }



    public List<OverlappingPair> findOverlappingComponents() {
        Map<Node, List<Node>> componentEmbeddings = new HashMap<>();
        //List<Set<String>> leafLabelList = new ArrayList<>();
        List<Node> componentsInF2 = problemInstance.getF2().getComponents();
        // Step 1: Build embedding for each component in F
        for (Node component : componentsInF2) {
            Set<String> leafLabels = TreeUtils.getLeafSet(component);

            List<Node> embeddedNodes = getEmbeddedNodesInT(leafLabels);
//            for (Node node : embeddedNodes) {
//                System.out.println("Node is: " + node);
//            }
            componentEmbeddings.put(component, embeddedNodes);
        }

        // Step 2: Check pairwise overlaps
        List<OverlappingPair> overlaps = new ArrayList<>();

        for (int i = 0; i < componentsInF2.size(); i++) {
            for (int j = i + 1; j < componentsInF2.size(); j++) {
                //System.out.println("splitter comparing subtree " + i + " and subtree " + j);
                Node c1 = componentsInF2.get(i);
                Node c2 = componentsInF2.get(j);

                List<Node> e1 = new ArrayList<>(componentEmbeddings.get(c1));
                List<Node> e2 = new ArrayList<>(componentEmbeddings.get(c2));

                e1.retainAll(e2);

                if (!e1.isEmpty()) {
                    List<Node> components = new ArrayList<>();
                    components.add(c1);
                    components.add(c2);
                    overlaps.add(new OverlappingPair(components, e1, i, j));
                }
            }
        }

        return overlaps;
    }

    /**
     * Given a set of leaf labels, finds all nodes in T that form the minimal subtree connecting them.
     * Assumes that all nodes have depth set correctly
     */
    private List<Node> getEmbeddedNodesInT(Set<String> leafLabels) {
        Node deepestAncestor = TreeUtils.findDeepestCommonAncestor(problemInstance.getF1().getComponent(0), leafLabels, problemInstance.getF1().getLeavesByLabel());

        // Collect all nodes on the paths between these leaves (using LCAs)
        List<Node> embedded = new ArrayList<>();



        for (String label : leafLabels) {
            Node n = problemInstance.getF1().getLeavesByLabel().get(label);

            while (n != deepestAncestor) {
                if (!embedded.contains(n)) {
                    embedded.add(n);
                }
                n = n.getParent();
            }

        }
        return embedded;
    }

    public static class OverlappingPair {
        List<Node> components = new ArrayList<>();
        List<Node> commonNodes = new ArrayList<>();
        int firstIndex;
        int secondIndex;

        public OverlappingPair(List<Node> components, List<Node> commonNodes, int firstIndex, int secondIndex) {
            this.components = components;
            this.commonNodes.addAll(commonNodes);
            this.firstIndex = firstIndex;
            this.secondIndex = secondIndex;
        }

        public List<Node> commonNodes() {
            return commonNodes;
        }

        public List<Node> components() {
            return components;
        }
    }




    public static void main(String[] args) {
        Forest F1 = Forest.readNewickFormat("(((1,2),(3,4)),((5,6),(7,8)))");
        Forest F2 = Forest.readNewickFormat("((1,7),(5,3))");
        Forest FTemp = Forest.readNewickFormat("((2,8),(6,4))");

        for (Node leaf : FTemp.getLeavesByLabel().values()) {
            F2.getLeavesByLabel().put(leaf.getLabel(), leaf);
        }

        F2.addComponent(FTemp.getComponent(0));

        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);
        TreeUtils.linkForests(F1, F2);

        ProblemInstance pi = new ProblemInstance(F1, F2);

        pi.printTrees();


        FitchTool fitchTool = new FitchTool(pi);
        System.out.println("p-score: " + fitchTool.getPScore());


        SplitTool tool = new SplitTool(pi, 7, new Random());
        tool.findSplitConflicts();

        System.out.println("Number of splitting cores");
        System.out.println(tool.splittingCores.size());

        

        //System.out.println(tool.splittingCores.get(0).getCuts().size());




//        SplitTreeNode copy = tool.new SplitTreeNode(F1.getComponent(0));
//        copy.printAsciiTree();
//
//        Set<String> s1 = new HashSet<>();
//
//        s1.add("2");
//        s1.add("4");
//        s1.add("6");
//        s1.add("8");
//
//
//        Set<String> s2 = new HashSet<>();
//        s2.add("1");
//        s2.add("3");
//        s2.add("5");
//        s2.add("7");
//
//        List<Set<String>> leafSets = new ArrayList<>();
//        leafSets.add(s1);
//        leafSets.add(s2);
//
//        copy.fillDescendantLeaves();
//        boolean test = tool.areSeparatedAtRoot(copy, leafSets);
//        if (test) {
//            System.out.println("2345678 separated from 1");
//        }
//
//        SplitTreeNode node = tool.getBranchingNode(copy, leafSets);
//        node.printAsciiTree();
//        for (SplitTreeNode child : node.getChildren()) {
//            System.out.println(child.getLabel());
//        }

    }

}
