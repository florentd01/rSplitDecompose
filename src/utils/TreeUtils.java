package utils;

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



}
