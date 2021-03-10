package IncrementalRunner;

import infra.Match;
import infra.RelationshipEdge;
import infra.VF2PatternGraph;
import infra.Vertex;
import org.jgrapht.GraphMapping;

import java.util.*;

public class IncrementalChange {

    //region Fields: Private
    private VF2PatternGraph pattern;
    private HashMap <String, GraphMapping <Vertex, RelationshipEdge>> newMatches;
    private ArrayList <String> removedMatchesSignatures;
    private HashMap<String, GraphMapping<Vertex, RelationshipEdge>> afterMatches;
    private HashSet<String> beforeMatchesSignatures;
    //endregion

    //region Constructors
    public IncrementalChange(Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeMatchIterator,VF2PatternGraph pattern)
    {
        newMatches=new HashMap<>();
        removedMatchesSignatures=new ArrayList <>();
        this.pattern=pattern;
        computeBeforeMatches(beforeMatchIterator);
    }
    //endregion

    //region Public Functions

    public void addAfterMatches(Iterator<GraphMapping<Vertex, RelationshipEdge>> afterMatchIterator)
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
            if(!beforeMatchesSignatures.contains(key))
                newMatches.put(key,afterMatches.get(key));
        }
        for (String key:beforeMatchesSignatures) {
            if(!afterMatches.containsKey(key))
                removedMatchesSignatures.add(key);
        }
        //System.out.print(beforeMatchesSignatures.size() + " -- " + newMatches.size() + " -- " + removedMatchesSignatures.size());
    }
    //endregion

    //region Private Functions
    private void computeBeforeMatches(Iterator<GraphMapping<Vertex, RelationshipEdge>> beforeMatchIterator)
    {
        beforeMatchesSignatures=new HashSet <>();
        if (beforeMatchIterator!=null)
        {
            while (beforeMatchIterator.hasNext())
            {
                beforeMatchesSignatures.add(Match.signatureFromPattern(pattern, beforeMatchIterator.next()));
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

    //endregion
}
