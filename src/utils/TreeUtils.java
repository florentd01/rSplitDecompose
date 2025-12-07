package utils;

import Algorithm.Cut;
import Algorithm.SplitTool;
import Model.Forest;
import Model.Node;

import java.util.*;

public class TreeUtils {
    public static void printAsciiTree(Node root) {
        printAsciiTree(root, "", true);
    }

    private static void printAsciiTree(Node node, String prefix, boolean isTail) {
        if (node == null) return;

        // Print this node
        String label = (node.getLabel() != null ? node.getLabel() : "[*]");
        System.out.println(prefix + (isTail ? "└── " : "├── ") + label);

        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean last = (i == children.size() - 1);
            printAsciiTree(children.get(i), prefix + (isTail ? "    " : "│   "), last);
        }
    }

    public static void printAsciiTree2(Node root) {
        printAsciiTree2(root, "", true);
    }

    private static void printAsciiTree2(Node node, String prefix, boolean isTail) {
        if (node == null) return;

        // Print this node
        String label = node.toString() + "  " +(node.getLabel() != null ? node.getLabel() : "[*]");
        System.out.println(prefix + (isTail ? "└── " : "├── ") + label);

        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            boolean last = (i == children.size() - 1);
            printAsciiTree2(children.get(i), prefix + (isTail ? "    " : "│   "), last);
        }
    }



    public static void linkForests(Forest F1, Forest F2) {
        F1.setTwin(F2);
        F2.setTwin(F1);
        for (Node node : F1.getLeavesByLabel().values()) {
            Node twin = F2.getLeavesByLabel().get(node.getLabel());
            node.setTwin(twin);
            twin.setTwin(node);
        }
    }

    public static void linkSiblings2(Forest F) {
        Set<Node> set = new HashSet<>();
        for (Node leaf : F.getLeavesByLabel().values()) {
            if (!set.contains(leaf)) {
                if (leaf.getParent() != null) {
                    System.out.println("Linking node");
                    List<Node> siblingPair = leaf.getParent().getChildren();

                    siblingPair.getFirst().setSibling(siblingPair.getLast());
                    siblingPair.getLast().setSibling(siblingPair.getFirst());
                    set.add(leaf.getSibling());

                }
            }

        }
    }

    public static void linkSiblings(Forest F) {
        Node root = F.getComponent(0);
        if (root == null) return;

        // Root has no sibling
        root.setSibling(null);

        // Traverse all nodes (DFS or BFS both work)
        for (Node child : root.getChildren()) {
            assignSiblingsHelper(child);
        }
    }

    private static void assignSiblingsHelper(Node node) {
        Node parent = node.getParent();

        if (parent != null) {
            List<Node> siblings = parent.getChildren();

            if (siblings.size() == 2) { // assuming binary tree
                // Set sibling to the other child
                if (siblings.get(0) == node) {
                    node.setSibling(siblings.get(1));
                } else {
                    node.setSibling(siblings.getFirst());
                }
            } else {
                // No sibling or more than 2 children — set to null or first alternative
                node.setSibling(null);
            }
        }

        // Recurse down
        for (Node child : node.getChildren()) {
            assignSiblingsHelper(child);
        }
    }

    public static boolean checkDescendantRelations(Forest F) {
        for (Node component : F.getComponents()) {
            if (!validateNode(component)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateNode(Node node) {
        if (node == null) {
            return true;
        }


        for (Node child : node.getChildren()) {
            if (child.getParent() != node) {
                System.out.println("Parent wrong at node: " + child);
                return false;

            }
            if (!validateNode(child)) {
                return false;
            }
        }
        return true;
    }

    public static HashSet<String> getLeafSet(Node root) {
        HashSet<String> leaves = new HashSet<>();
        collectLeafLabels(root, leaves);
        return leaves;
    }

    private static void collectLeafLabels(Node node, Set<String> leaves) {
        if (node == null) return;

        if (node.isLeaf()) {
            leaves.add(node.getLabel());
        } else {
            for (Node child : node.getChildren()) {
                collectLeafLabels(child, leaves);
            }
        }
    }

    /**
     * Sets the depth of every node in the tree.
     * The root node is assigned depth 0.
     *
     * @param root the root of the tree
     */
    public static void setNodeDepths(Node root) {
        if (root == null) return;
        setNodeDepthsHelper(root, 0);
    }

    private static void setNodeDepthsHelper(Node node, int depth) {
        node.setDepth(depth);

        for (Node child : node.getChildren()) {
            setNodeDepthsHelper(child, depth + 1);
        }
    }

    /**
     * Finds the deepest node in the tree that is an ancestor of all given leaf labels.
     *
     * @param root        the root node of the tree
     * @param leafLabels  the set of leaf labels to consider
     * @param labelToNode a map from leaf label -> corresponding Node in the tree
     * @return the deepest (maximum depth) common ancestor of all given leaves, or null if not all found
     */
    public static Node findDeepestCommonAncestor(Node root, Set<String> leafLabels, Map<String, Node> labelToNode) {
        if (root == null || leafLabels == null || leafLabels.isEmpty()) return null;

        // Collect the actual Node references for the labels
        List<Node> leafNodes = new ArrayList<>();
        for (String label : leafLabels) {
            Node leaf = labelToNode.get(label);
            if (leaf != null) leafNodes.add(leaf);
        }

        if (leafNodes.isEmpty()) return null;

        // Start from the LCA of the first two nodes, then merge with the rest
        setNodeDepths(root);
        Node currentLCA = leafNodes.getFirst();
        for (int i = 1; i < leafNodes.size(); i++) {
            currentLCA = findLCA(currentLCA, leafNodes.get(i));
            if (currentLCA == null) return null;
        }

        return currentLCA;
    }

    /**
     * Finds the LCA of two nodes using parent pointers and depth values.
     * Assumes setNodeDepths(root) has already been called.
     */
    private static Node findLCA(Node a, Node b) {
        if (a == null || b == null) return null;

        // Bring both to same depth
        while (a.getDepth() > b.getDepth()) a = a.getParent();
        while (b.getDepth() > a.getDepth()) b = b.getParent();

        // Walk upward until they meet
        while (a != null && b != null && a != b) {
            a = a.getParent();
            b = b.getParent();
        }

        return a;
    }

    public static Node readNewick(String newick) {
        newick = newick.trim();
        if (newick.endsWith(";")) {

            return readSubtree(newick.substring(0, newick.length() - 1), 0);
        } else {
            return readSubtree(newick, 0);
        }
    }




    private static Node readSubtree(String newick, int depth) {
        int leftParen = newick.indexOf('(');
        int rightParen = newick.lastIndexOf(')');

        if (leftParen != -1 && rightParen != -1) {

            String name = newick.substring(rightParen + 1);
            String[] childrenString = split(newick.substring(leftParen + 1, rightParen));

            Node node = new Node();
            node.setDepth(depth);

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

            return node;

        } else throw new RuntimeException("unbalanced ()'s");

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

    public static void assignUniqueIds(Node root, int init) {
        assignIdsDFS(root, new int[]{init});
    }

    private static void assignIdsDFS(Node node, int[] counter) {
        if (node == null) return;

        node.setId(counter[0]);
        counter[0]++;

        for (Node child : node.getChildren()) {
            assignIdsDFS(child, counter);
        }
    }

    public static Map<Integer, Node> createIdMap(Node root) {
        Map<Integer, Node> map = new HashMap<>();
        fillIdMap(root, map);
        return map;
    }

    public static Map<Integer, Node> createIdMap(Forest f) {

        Map<Integer, Node> map = new HashMap<>();
        for (Node comp : f.getComponents()) {
            fillIdMap(comp, map);
        }
        return map;
    }

    private static void fillIdMap(Node node, Map<Integer, Node> map) {
        if (node == null) return;

        map.put(node.getId(), node);

        for (Node child : node.getChildren()) {
            fillIdMap(child, map);
        }
    }

    public static void applySolution(Forest F2, List<Cut> cutList) {
        Map<Integer, Node> idMap = TreeUtils.createIdMap(F2);
        for (Cut cut : cutList) {
            Node parent = idMap.get(cut.getProblemParent().getId());
            int cutId = cut.getIndexInChildrenList();
            if (parent.getChildren().isEmpty()) {
                System.out.println("break");
            }
            Node cutChild = parent.getChildren().remove(cutId);
            cutChild.setRoot(true);
            F2.addComponent(cutChild);


        }
        //F2.printForest();
        F2.suppressDegreeTwo(new UndoMachine());
    }

    public static void printNodeIds(Node root) {
        if (root == null) return;
        printIdsRecursive(root);
    }

    private static void printIdsRecursive(Node node) {
        // Print this node's ID
        System.out.println(node.getId());

        // Recurse on children
        for (Node child : node.getChildren()) {
            printIdsRecursive(child);
        }
    }

    public static Node removeRho(Node root) {
        if (root == null) return null;

        // Case 1 — root is rho
        if ("rho".equals(root.getLabel())) {
            return null;  // the whole tree disappears
        }

        // Case 2 — remove rho somewhere below
        removeRhoRecursive(root);
        return root;
    }

    private static void removeRhoRecursive(Node node) {
        List<Node> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);

            // Found rho (guaranteed to be a leaf)
            if ("rho".equals(child.getLabel())) {
                children.remove(i);
                child.setParent(null);
                i--; // adjust index after removal
            }
            else {
                removeRhoRecursive(child);
            }
        }
    }



    public static void main(String[] args) {
        Node root = readNewick("(((1,2),(3,4)),((5,6),(7,8)))");
        printAsciiTree(root);


    }


}
