package IncrementalRunner;

import infra.Match;
import infra.RelationshipEdge;
import infra.VF2PatternGraph;
import infra.Vertex;
import org.jgrapht.GraphMapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class IncrementalChange {

    //region Fields: Private
    private Iterator <GraphMapping <Vertex, RelationshipEdge>> beforeMatchIterator;
    private Iterator <GraphMapping <Vertex, RelationshipEdge>> afterMatchIterator;
    private VF2PatternGraph pattern;
    private HashMap <String, GraphMapping <Vertex, RelationshipEdge>> newMatches;
    private Set<String> removedMatchesSignatures;
    private HashMap<String, GraphMapping<Vertex, RelationshipEdge>> beforeMatches, afterMatches;
    //endregion

    //region Constructors
    public IncrementalChange(Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeMatchIterator,VF2PatternGraph pattern)
    {
        this.beforeMatchIterator=beforeMatchIterator;
        newMatches=new HashMap<>();
        removedMatchesSignatures=new HashSet <>();
        this.pattern=pattern;
        computeBeforeMatches();
    }
    //endregion

    //region Public Functions

    public void addAfterMatches(Iterator<GraphMapping<Vertex, RelationshipEdge>> afterMatchIterator)
    {
        this.afterMatchIterator=afterMatchIterator;
        computeAfterMatches();
    }
    //endregion

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
                removedMatchesSignatures.add(key);
        }
    }
    //endregion

    //region Getters
    public HashMap <String, GraphMapping <Vertex, RelationshipEdge>> getNewMatches() {
        return newMatches;
    }

    public Set<String> getRemovedMatchesSignatures() {
        return removedMatchesSignatures;
    }

    //endregion
}
