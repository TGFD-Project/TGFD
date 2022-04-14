package Infra;

import java.util.ArrayList;

public class DataDependency {

    private ArrayList<Literal> X;
    private ArrayList<Literal> Y;

    public DataDependency(ArrayList<Literal> X, ArrayList<Literal> Y)
    {
        this.X=X;
        this.Y=Y;
    }

    public DataDependency()
    {
        this.X=new ArrayList<>();
        this.Y=new ArrayList<>();
    }

    public void addLiteralToX(Literal l)
    {
        this.X.add(l);
    }

    public void addLiteralToY(Literal l)
    {
        this.Y.add(l);
    }

    public ArrayList<Literal> getX() {
        return X;
    }

    public ArrayList<Literal> getY() {
        return Y;
    }


    @Override
    public String toString() {
        return "Dependency{" +
                "X=" + X +
                ", Y=" + Y +
                '}';
    }
}
