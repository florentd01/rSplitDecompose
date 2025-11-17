package Model;

import utils.FitchTool;
import utils.TreeUtils;
import utils.UndoMachine;

import java.util.*;

public class Node {
    private String label;
    private List<Node> children = new ArrayList<>();
    private Set<Node> descendantLeaves = new HashSet<>();
    private Node parent;
    private Node twin;
    private Node sibling = null;
    private int depth;
    private boolean isLeaf;
    private boolean isRoot;


    public Node(String label, List<Node> children, Node p, Node twin, int depth, boolean isLeaf, boolean isRoot){
        this.label = label;
        this.children = children;
        this.parent = p;
        this.twin = twin;
        this.depth = depth;
        this.isLeaf = isLeaf;
        this.isRoot = isRoot;
    }


    public Node(String label, int depth, boolean isLeaf, boolean isRoot){
        this.label = label;
        this.depth = depth;
        this.isLeaf = isLeaf;
        this.isRoot = isRoot;
    }

    public Node(int depth, boolean isRoot) {
        this.depth = depth;
        this.isRoot = isRoot;
    }

    public Node() {

    }


    public Node(String name){
        this.label = name;
    }

//    public Node deepClone(){
//        Node left = null;
//        Node right = null;
//        Node copy = new Node(this.label, this.depth, this.isLeaf, this.isRoot);
//
//        if (this.children.getFirst() != null) {
//            left = deepCloneHelper(this.children.getFirst(), copy);
//        }
//        if (this.children.getLast() != null) {
//            right = deepCloneHelper(this.children.getLast(), copy);
//        }
//        List<Node> cloneChildren = new ArrayList<>();
//        cloneChildren.add(left);
//        cloneChildren.add(right);
//        copy.setChildren(cloneChildren);
//
//        return copy;
//    }
//
    public Node(Node original) {
        this.label = original.getLabel();
        this.depth = original.getDepth();
        this.isLeaf = original.isLeaf();
        this.isRoot = original.isRoot();
        this.parent = null;
        for (Node child : original.getChildren()) {
            Node childCopy = new Node(child);
            childCopy.setParent(this);
            this.addChild(childCopy);
        }
    }

    public Node(Node original, Set<String> allowedLabels) {
        this.label = original.getLabel();
        this.depth = original.getDepth();
        this.isLeaf = original.isLeaf();
        this.isRoot = original.isRoot();
        this.parent = null;
        for (Node child : original.getChildren()) {
            if (!child.isLeaf() || allowedLabels.contains(child.getLabel())) {
                Node childCopy = new Node(child, allowedLabels);
                childCopy.setParent(this);
                this.addChild(childCopy);
            }

        }
    }
//
//    private Node copyHelper(Node original, Node copyOfParent) {
//
//        return null;
//    }
//
//    private Node deepCloneHelper(Node originalNode, Node copyOfParent) {
//        Node copy = new Node(originalNode.getLabel(), originalNode.getDepth(), originalNode.isLeaf(), originalNode.isRoot());
//        Node left = null;
//        Node right = null;
//        copy.setParent(copyOfParent);
//        List<Node> childrenOfOriginal = originalNode.getChildren();
//        if (childrenOfOriginal.getFirst() != null) {
//            left = deepCloneHelper(childrenOfOriginal.getFirst(), copy);
//        }
//        if (childrenOfOriginal.getLast() != null) {
//            right = deepCloneHelper(childrenOfOriginal.getLast(), copy);
//        }
//        List<Node> cloneChildren = new ArrayList<>();
//        cloneChildren.add(left);
//        cloneChildren.add(right);
//        copy.setChildren(cloneChildren);
//        return copy;
//    }

    public String getLabel(){
        return label;
    }

    public List<Node> getChildren(){
        return children;
    }

    public Set<Node> getDescendantLeaves() {
        return descendantLeaves;
    }

    public Node getParent(){
        return parent;
    }

    public Node getTwin(){
        return twin;
    }

    public Node getSibling() {
        return sibling;
    }

    public int getDepth(){
        return depth;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setChildren(List<Node> children) {
        for (Node child : children) {
            child.setParent(this);
        }
        this.children = children;
    }

    public void setDescendantLeaves(Set<Node> descendantLeaves) {
        this.descendantLeaves = descendantLeaves;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void setTwin(Node twin) {
        this.twin = twin;
    }

    public void setSibling(Node cherryPartner) {
        this.sibling = cherryPartner;
    }

    public boolean isRoot() {
        return this.isRoot;
    }

    public void setRoot(boolean root) {
        this.isRoot = root;
    }

    // TODO: prevent adding more than 2 children
    public void addChild(Node child) {
        this.children.add(child);
        child.setParent(this);
    }

    public void addChild(int index, Node child) {
        this.children.add(index, child);
        child.setParent(this);
    }

    private Set<Node> computeDescendantLeaves(Node node) {
        if (node == null) return Collections.emptySet();

        // Leaf node: descendant leaves = itself
        if (node.isLeaf()) {
            Set<Node> leaves = new HashSet<>();
            leaves.add(node);
            node.setDescendantLeaves(leaves);
            return leaves;
        }

        // Internal node: union of children's leaves
        Set<Node> leaves = new HashSet<>();
        for (Node child : node.getChildren()) {
            leaves.addAll(computeDescendantLeaves(child));
        }
        node.setDescendantLeaves(leaves);
        return leaves;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Node node = (Node) o;
//        if (node.isLeaf() && isLeaf) {
//            return Objects.equals(label, node.label);
//        } else {
//            return isLeaf == node.isLeaf && Objects.equals(label, node.label);
//        }
//
//
//    }


    public static void main(String[] args) {
        Forest F1 = Forest.readNewickFormat("((((1,2),(3,4)),((5,6),(7,8))),(((9,10),(11,12)),((13,14),(15,16))))");
        Forest F2 = Forest.readNewickFormat("(((7,8),((1,(2,(14,5))),(3,4))),(((11,(6,12)),10),((13,(15,16)),9)))");

        ProblemInstance instance = new ProblemInstance(F1, F2);
        instance.printTrees();
        //TreeUtils.printAsciiTree(F2.getComponent(0));
        TreeUtils.linkForests(F1, F2);
        TreeUtils.linkSiblings(F1);
        TreeUtils.linkSiblings(F2);

        Node root = F2.getComponents().removeFirst();

        F2.addComponent(root.getChildren().getFirst());
        F2.addComponent(root.getChildren().getLast());
        //TreeUtils.printAsciiTree(F2.getComponent(0));
        //instance.printTrees();

        Node copyOfRoot = new Node(root);
        System.out.println("copy of root");
        TreeUtils.printAsciiTree(copyOfRoot);



        FitchTool tool = new FitchTool(instance);
        System.out.println(tool.getPScore());
        System.out.println(tool.getColours().size());


        ProblemInstance subInstance = instance.makeSubProblem(F2.getComponent(0), tool.getColours().getFirst());

        System.out.println("Number of union events: " + tool.getPScore());
        //TreeUtils.printAsciiTree(subInstance.getF1().getComponent(0));
        //TreeUtils.printAsciiTree(subInstance.getF2().getComponent(0));

        System.out.println(tool.getColours().getFirst());



    }
}
