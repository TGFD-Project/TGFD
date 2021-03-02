package infra;

import org.jgrapht.GraphMapping;

import java.util.HashMap;
import java.util.Iterator;

public class IncrementalChange {

    //region Fields: Private
    private Iterator <GraphMapping <Vertex, RelationshipEdge>> beforeMatchIterator;
    private Iterator <GraphMapping <Vertex, RelationshipEdge>> afterMatchIterator;
    private VF2PatternGraph pattern;
    private HashMap <String, GraphMapping <Vertex, RelationshipEdge>> newMatches, removedMatches;
    //endregion

    //region Constructors
    public IncrementalChange(Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeChange,
                             Iterator<GraphMapping<Vertex, RelationshipEdge>> afterChange,
                             VF2PatternGraph pattern)
    {
        this.beforeMatchIterator=beforeChange;
        this.afterMatchIterator=afterChange;

        newMatches=new HashMap<>();
        removedMatches=new HashMap<>();
        this.pattern=pattern;

        computeDifferences();

    }
    //endregion

    //region Private Functions
    private void computeDifferences()
    {
        HashMap<String, GraphMapping<Vertex, RelationshipEdge>> beforeMatches, afterMatches;
        beforeMatches=new HashMap<>();
        afterMatches=new HashMap<>();

        try
        {
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
        catch (Exception e)
        {
            //System.out.println(e.getMessage());
        }

        try
        {
            if(afterMatchIterator!=null) {
                while (afterMatchIterator.hasNext()) {
                    var mapping = afterMatchIterator.next();
                    var signatureFromPattern = Match.signatureFromPattern(pattern, mapping);

                    afterMatches.put(signatureFromPattern, mapping);
                }
            }
        }
        catch (Exception e)
        {
            //System.out.println(e.getMessage());
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
    //endregion
}
