package Algorithm;

import Model.Node;

import java.util.ArrayList;
import java.util.List;

public class Conflict {
    private List<Cut> cuts;
    private Node a;
    private Node b;

    public Conflict(Node a, Node b){
        cuts = new ArrayList<>();
        this.a = a;
        this.b = b;
    }

    public Conflict(){
        this.cuts = new ArrayList<>();
    }

    public Conflict(List<Cut> cuts){
        this.cuts = new ArrayList<>();
        this.cuts.addAll(cuts);
    }

    public void addCut(Cut cut){
        cuts.add(cut);
    }

    public List<Cut> getCuts(){
        return cuts;
    }

    public void setCuts(List<Cut> cuts) {

        ;
        this.cuts = cuts;
    }
}
