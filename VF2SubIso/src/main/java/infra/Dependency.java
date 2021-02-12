package infra;

import java.util.ArrayList;

public class Dependency {

    private ArrayList<literal> X, Y;

    public Dependency(ArrayList<literal> X, ArrayList<literal> Y)
    {
        this.X=X;
        this.Y=Y;
    }

    public Dependency()
    {
        this.X=new ArrayList<>();
        this.Y=new ArrayList<>();
    }

    public void addLiteralToX(literal l)
    {
        this.X.add(l);
    }

    public void addLiteralToY(literal l)
    {
        this.Y.add(l);
    }

    public ArrayList<literal> getX() {
        return X;
    }

    public ArrayList<literal> getY() {
        return Y;
    }
}
