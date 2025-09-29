package Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Node {
    private String label;
    private List<Node> children = new ArrayList<>();
    private Node parent;
    private Node twin;
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

    public Node deepClone(){
        Node left = null;
        Node right = null;
        Node copy = new Node(this.label, this.depth, this.isLeaf, this.isRoot);

        if (this.children.getFirst() != null) {
            left = deepCloneHelper(this.children.getFirst(), copy);
        }
        if (this.children.getLast() != null) {
            right = deepCloneHelper(this.children.getLast(), copy);
        }
        List<Node> cloneChildren = new ArrayList<>();
        cloneChildren.add(left);
        cloneChildren.add(right);
        copy.setChildren(cloneChildren);

        return copy;
    }

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

    private Node copyHelper(Node original, Node copyOfParent) {

        return null;
    }

    private Node deepCloneHelper(Node originalNode, Node copyOfParent) {
        Node copy = new Node(originalNode.getLabel(), originalNode.getDepth(), originalNode.isLeaf(), originalNode.isRoot());
        Node left = null;
        Node right = null;
        copy.setParent(copyOfParent);
        List<Node> childrenOfOriginal = originalNode.getChildren();
        if (childrenOfOriginal.getFirst() != null) {
            left = deepCloneHelper(childrenOfOriginal.getFirst(), copy);
        }
        if (childrenOfOriginal.getLast() != null) {
            right = deepCloneHelper(childrenOfOriginal.getLast(), copy);
        }
        List<Node> cloneChildren = new ArrayList<>();
        cloneChildren.add(left);
        cloneChildren.add(right);
        copy.setChildren(cloneChildren);
        return copy;
    }

    public String getLabel(){
        return label;
    }

    public List<Node> getChildren(){
        return children;
    }

    public Node getParent(){
        return parent;
    }

    public Node getTwin(){
        return twin;
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
        Node root = new Node("1", 0, false, true);
        Node node1 = new Node("2", 0, true, false);
        Node node2 = new Node("3", 0, true, false);
        root.addChild(node1);
        root.addChild(node2);
        Node root2 = new Node(0, true);
        Node root3 = root2;

        if (root2 == root3) {
            System.out.println("they are equal");
        }

        System.out.println(root.getLabel());
        Node copyOfRoot = new Node(root);
        System.out.println(copyOfRoot.getChildren());
        List<Node> childrenOfCopy = copyOfRoot.getChildren();
        System.out.println(root);
        System.out.println(copyOfRoot);
        for (Node n : childrenOfCopy) {
            System.out.println(n.getParent());
        }

    }
}
