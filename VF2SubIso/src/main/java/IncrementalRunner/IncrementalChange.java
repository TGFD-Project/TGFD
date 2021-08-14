package IncrementalRunner;

import Infra.Match;
import Infra.RelationshipEdge;
import Infra.VF2PatternGraph;
import Infra.Vertex;
import org.jgrapht.GraphMapping;

import java.util.*;

public class IncrementalChange {

    //region Fields: Private
    private VF2PatternGraph pattern;
    private HashMap <String, GraphMapping <Vertex, RelationshipEdge>> newMatches;
    private HashMap <String, GraphMapping <Vertex, RelationshipEdge>> removedMatches;
    private ArrayList <String> removedMatchesSignatures;
    private HashMap<String, GraphMapping<Vertex, RelationshipEdge>> afterMatches;
    private HashMap<String, GraphMapping<Vertex, RelationshipEdge>> beforeMatches;
    //endregion

    //region Constructors
    public IncrementalChange(Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeMatchIterator,VF2PatternGraph pattern)
    {
        newMatches=new HashMap<>();
        removedMatchesSignatures=new ArrayList <>();
        removedMatches=new HashMap<>();
        this.pattern=pattern;
        computeBeforeMatches(beforeMatchIterator);
    }
    //endregion

    //region Public Functions

    public String addAfterMatches(Iterator<GraphMapping<Vertex, RelationshipEdge>> afterMatchIterator)
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
            {
                removedMatchesSignatures.add(key);
                removedMatches.put(key,beforeMatches.get(key));
            }
        }
        return beforeMatches.size() + " - " +afterMatches.size();
        //System.out.print(beforeMatchesSignatures.size() + " -- " + newMatches.size() + " -- " + removedMatchesSignatures.size());
    }
    //endregion

    //region Private Functions
    private void computeBeforeMatches(Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeMatchIterator)
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
    //endregion

    //region Getters
    public HashMap <String, GraphMapping <Vertex, RelationshipEdge>> getNewMatches() {
        return newMatches;
    }

    public ArrayList<String> getRemovedMatchesSignatures() {
        return removedMatchesSignatures;
    }

    public HashMap<String, GraphMapping<Vertex, RelationshipEdge>> getRemovedMatches() {
        return removedMatches;
    }

    //endregion
}
