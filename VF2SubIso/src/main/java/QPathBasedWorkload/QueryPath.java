package QPathBasedWorkload;

import Infra.PatternVertex;

import java.util.ArrayList;

public class QueryPath {

    private ArrayList<Triple> triples;

    public QueryPath()
    {
        triples=new ArrayList<>();
    }

    public QueryPath(ArrayList<Triple> triples)
    {
        this.triples=triples;
    }

    public void addTriple(Triple triple)
    {
        this.triples.add(triple);
    }

    public void addTriple(PatternVertex src, PatternVertex dst, String edge)
    {
        this.triples.add(new Triple(src,dst,edge));
    }

    public String getCenterVertexType()
    {
        return triples.get(0).getSrc().getTypes().iterator().next();
    }

    public ArrayList<Triple> getTriples() {
        return triples;
    }

    public int getSize()
    {
        return this.triples.size();
    }

    @Override
    public String toString() {
        StringBuilder res= new StringBuilder("QueryPath{");
        for (Triple triple: triples) {
            res.append(triple.getSrc() + triple.getEdge() + triple.getDst() + "\n");
        }
        res.append('}');
        return res.toString();
    }

}
