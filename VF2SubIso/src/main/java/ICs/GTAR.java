package ICs;

import Infra.Delta;
import Infra.VF2PatternGraph;

public class GTAR {

    private Delta delta;
    private VF2PatternGraph p1;
    private VF2PatternGraph p2;
    private String name="";

    public GTAR(VF2PatternGraph p1,VF2PatternGraph p2, Delta delta, String name)
    {
        this.delta=delta;
        this.p1=p1;
        this.p2=p2;
        this.name=name;
    }


    public void setDelta(Delta delta) {
        this.delta = delta;
    }

    public void setP1(VF2PatternGraph p1) {
        this.p1 = p1;
    }

    public void setP2(VF2PatternGraph p2) {
        this.p2 = p2;
    }

    public VF2PatternGraph getP1() {
        return p1;
    }

    public VF2PatternGraph getP2() {
        return p2;
    }

    public Delta getDelta() {
        return delta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "GTAR{" +
                "delta=" + getDelta() +
                ", p1=" + getP1() +
                ", p2=" + getP2() +
                '}';
    }
}
