package Algorithm;

import Model.Forest;
import Model.Node;

public class ApproxCut {
    private Forest forest;
    private final Node problemParent;
    private final Node problemChild;
    private int indexInChildrenList;

    public ApproxCut(Node problemParent, Node problemChild, Forest F) {
        this.problemParent = problemParent;
        this.problemChild = problemChild;
        this.forest = F;
        this.indexInChildrenList = problemParent.getChildren().indexOf(problemChild);
    }


    public void makeCut() {
        problemParent.getChildren().remove(problemChild);
        forest.addComponent(problemChild);
        problemChild.setRoot(true);
        problemChild.setParent(null);
        if (problemChild.getSibling() != null) {
            problemChild.getSibling().setSibling(null);
        }
    }

    public int getIndexInChildrenList() {
        return indexInChildrenList;
    }
}
