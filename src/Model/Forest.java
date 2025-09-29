package Model;

import java.util.*;

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

    public void addComponent(Node root, int pos) {
        components.add(pos, root);
    }

    public void deleteSingletons(){
        for (Node component : components) {
            if (component.isLeaf()) {
                components.remove(component);
                // TODO: Check if Unnecessary
                leavesByLabel.remove(component.getLabel());
            }
        }

    }

    public void suppressDegreeTwo() {
        for (Node component : this.components) {
            suppressDegreeTwoHelper(component);
            if (component.isRoot() && component.getChildren().size() == 1) {
                components.add(component.getChildren().getFirst());
                components.remove(component);
            }
        }
    }

    private Node suppressDegreeTwoHelper(Node node) {
        List<Node> newChildren = new ArrayList<>();
        for (Node child : node.getChildren()) {
            Node suppressedChild = suppressDegreeTwoHelper(child);
            suppressedChild.setParent(node);
            newChildren.add(suppressedChild);
        }
        node.setChildren(newChildren);

        if (node.getParent() != null && node.getChildren().size() == 1) {
            Node onlyChild = node.getChildren().getFirst();

            // Reconnect parent <-> child
            Node parent = node.getParent();
            onlyChild.setParent(parent);

            List<Node> siblings = parent.getChildren();
            siblings.remove(node);
            siblings.add(onlyChild);
            return onlyChild;
        }
        return node;
    }

    public void reduceCommonCherries(Forest pairedForest) {
        for (Node leaf : leavesByLabel.values()) {
            Node parent = leaf.getParent();
            Node parentOfTwin = leaf.getTwin().getParent();
            Node twinNode = leaf.getTwin();
            if (sameCherry(parent,  parentOfTwin)) {
                for (Node child : parent.getChildren()) {
                    leavesByLabel.remove(child.getLabel());
                }
                for (Node child : parentOfTwin.getChildren()) {
                    pairedForest.getLeavesByLabel().remove(child.getLabel());
                }

                leavesByLabel.put(leaf.getLabel(), leaf);
                pairedForest.getLeavesByLabel().put(twinNode.getLabel(), twinNode);

                Node parentOfParentOfTwin = parentOfTwin.getParent();
                Node parentOfParent = parent.getParent();

                parentOfParent.getChildren().remove(parent);
                parentOfParent.addChild(leaf);

                parentOfParentOfTwin.getChildren().remove(parentOfTwin);
                parentOfParentOfTwin.addChild(twinNode);

            }
        }
    }

    public boolean sameCherry(Node parent1, Node parent2) {
        Set<String> leafSet1 = new HashSet<>();
        Set<String> leafSet2 = new HashSet<>();
        for (Node child : parent1.getChildren()) {
            leafSet1.add(child.getLabel());
        }
        for (Node child : parent2.getChildren()) {
            leafSet2.add(child.getLabel());
        }
        return leafSet1.equals(leafSet2);
    }


    public static void main(String[] args) {
        Node root = new Node("1", 0, false, true);
        Node node1 = new Node("2", 1, false, false);
        Node node2 = new Node("3", 2, true, false);
        Node node3 = new Node("4", 2, true, false);
        root.addChild(node1);
        node1.addChild(node2);
        node1.addChild(node3);
        System.out.println(root.getChildren());
        List<Node> components = new ArrayList<>();
        components.add(root);
        Forest F1 = new Forest(components);
        F1.suppressDegreeTwo();
        String label = F1.getComponent(0).getLabel();
        System.out.println(label);
        System.out.println(F1.getComponent(0).getChildren());
    }
}
