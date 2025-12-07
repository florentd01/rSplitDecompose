package Algorithm;

import Model.Forest;
import Model.Node;
import utils.UndoMachine;

public class Cut {
    private Forest forest;
    private final Node problemParent;
    private final Node problemChild;
    // TODO: fix index
    private int indexInChildrenList;

    public Cut(Node parent, Node child, Forest F, int index) {
        problemParent = parent;
        problemChild = child;
        forest = F;
        this.indexInChildrenList = index;


    }

    public int getIndexInChildrenList() {
        return indexInChildrenList;
    }

    // TODO: properly set parent for child
    public void makeCut() {
        problemParent.getChildren().remove(problemChild);
        forest.addComponent(problemChild);
        problemChild.setRoot(true);
        problemChild.setParent(null);
        //TODO: make sure that the sibling of the sibling of problem child is set to null
        problemChild.getSibling().setSibling(null);
    }


    public Forest getForest() {
        return forest;
    }

    public Node getProblemParent() {
        return problemParent;
    }

    public Node getProblemChild() {
        return problemChild;
    }
}
