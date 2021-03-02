package IncrementalRunner;

import infra.Match;
import infra.RelationshipEdge;
import infra.VF2PatternGraph;
import infra.Vertex;
import org.jgrapht.GraphMapping;

import java.util.HashMap;
import java.util.Iterator;

public class IncrementalChange {

    //region Fields: Private
    private Iterator <GraphMapping <Vertex, RelationshipEdge>> beforeMatchIterator;
    private Iterator <GraphMapping <Vertex, RelationshipEdge>> afterMatchIterator;
    private VF2PatternGraph pattern;
    private HashMap <String, GraphMapping <Vertex, RelationshipEdge>> newMatches, removedMatches;
    private HashMap<String, GraphMapping<Vertex, RelationshipEdge>> beforeMatches, afterMatches;
    private int error1=0,error2=0;
    //endregion

    //region Constructors
    public IncrementalChange(Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeMatchIterator,VF2PatternGraph pattern)
    {
        this.beforeMatchIterator=beforeMatchIterator;
        newMatches=new HashMap<>();
        removedMatches=new HashMap<>();
        this.pattern=pattern;

        computeBeforeMatches();

    }
    //endregion

    public void addAfterMatches(Iterator<GraphMapping<Vertex, RelationshipEdge>> afterMatchIterator)
    {
        this.afterMatchIterator=afterMatchIterator;
        computeAfterMatches();
    }

    //region Private Functions
    private void computeBeforeMatches()
    {
        beforeMatches=new HashMap<>();
        if (beforeMatchIterator!=null)
        {
            while (beforeMatchIterator.hasNext())
            {
                var mapping = beforeMatchIterator.next();
                var signatureFromPattern = Match.signatureFromPattern(pattern, mapping);

                beforeMatches.put(signatureFromPattern, mapping);
            }
        }
    }

    private void computeAfterMatches()
    {
        afterMatches=new HashMap<>();
        if(afterMatchIterator!=null) {
            while (afterMatchIterator.hasNext()) {
                var mapping = afterMatchIterator.next();
                var signatureFromPattern = Match.signatureFromPattern(pattern, mapping);

                afterMatches.put(signatureFromPattern, mapping);
            }
        }

        for (String key:afterMatches.keySet()) {
            if(!beforeMatches.containsKey(key))
                newMatches.put(key,afterMatches.get(key));
        }
        for (String key:beforeMatches.keySet()) {
            if(!afterMatches.containsKey(key))
                removedMatches.put(key,beforeMatches.get(key));
        }
    }
    //endregion

    //region Getters
    public HashMap <String, GraphMapping <Vertex, RelationshipEdge>> getNewMatches() {
        return newMatches;
    }

    public HashMap <String, GraphMapping <Vertex, RelationshipEdge>> getRemovedMatches() {
        return removedMatches;
    }

    public int getError1() {
        return error1;
    }

    public int getError2() {
        return error2;
    }

    //endregion
}
