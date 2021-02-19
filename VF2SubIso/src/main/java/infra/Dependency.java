package infra;

import java.util.ArrayList;

public class Dependency {

    private ArrayList<Literal> X;
    private ArrayList<Literal> Y;

    public Dependency(ArrayList<Literal> X, ArrayList<Literal> Y)
    {
        this.X=X;
        this.Y=Y;
    }

    public Dependency()
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
