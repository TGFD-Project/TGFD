import VF2Runner.VF2SubgraphIsomorphism;
import infra.VF2Graph;
import infra.attribute;
import infra.relationshipEdge;
import infra.vertex;

public class testVF2 {

    public static void main(String []args)
    {
        VF2SubgraphIsomorphism VF2=new VF2SubgraphIsomorphism();
        VF2.executeAndPrintResults(generateDataGraph(),generatePatternGraph());
    }

    public static VF2Graph generateDataGraph()
    {
        VF2Graph graph=new VF2Graph();

        vertex v1=new vertex("Frank_Lampard", "player");
        v1.addAttribute("name","lampard");
        v1.addAttribute("age","34");
        v1.addAttribute("number","11");
        graph.addVertex(v1);

        vertex v3=new vertex("Didier_Drogba","player");
        v3.addAttribute("name","Drogba");
        v3.addAttribute("age","36");
        graph.addVertex(v3);

        vertex v2=new vertex("Team_Chelsea","team");
        v2.addAttribute("name","Chelsea");
        v2.addAttribute("league","Premiere League");
        graph.addVertex(v2);

        graph.addEdge(v1,v2,new relationshipEdge("playing"));
        graph.addEdge(v3,v2,new relationshipEdge("playing"));

        return graph;
    }

    public static VF2Graph generatePatternGraph()
    {
        VF2Graph pattern=new VF2Graph();

        // isPatternNode=true for all pattern vertices
        vertex v1=new vertex("player",true);
        //variable literal
        v1.addAttribute(new attribute("number"));
        //constant literal
        v1.addAttribute(new attribute("age","34"));
        pattern.addVertex(v1);

        vertex v2=new vertex("team",true);
        //variable literal
        v2.addAttribute(new attribute("league"));
        pattern.addVertex(v2);

        pattern.addEdge(v1,v2,new relationshipEdge("playing"));

        return pattern;
    }

}
