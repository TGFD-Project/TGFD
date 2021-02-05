import VF2Runner.VF2SubgraphIsomorphism;
import infra.*;
import patternLoader.patternGenerator;

public class testVF2 {

    public static void main(String []args) {

        VF2SubgraphIsomorphism VF2=new VF2SubgraphIsomorphism();
        patternGenerator generator=new patternGenerator("D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt");
        for (VF2PatternGraph pattern:generator.getPattern()) {
            System.out.println("\n########## Graph pattern ##########");
            System.out.println(pattern.toString());
            VF2.executeAndPrintResults(generateDataGraph(),pattern);
        }
    }

    public static VF2DataGraph generateDataGraph()  {
        VF2DataGraph graph=new VF2DataGraph();

        dataVertex v1=new dataVertex("player","Frank_Lampard");
        v1.addAttribute("name","lampard");
        v1.addAttribute("age","34");
        v1.addAttribute("number","11");
        graph.addVertex(v1);

        dataVertex v3=new dataVertex("player","Didier_Drogba");
        v3.addAttribute("name","Drogba");
        v3.addAttribute("age","36");
        graph.addVertex(v3);

        dataVertex v2=new dataVertex("team","Team_Chelsea");
        v2.addAttribute("name","Chelsea");
        v2.addAttribute("league","Premiere League");
        graph.addVertex(v2);

        graph.addEdge(v1,v2,new relationshipEdge("playing"));
        graph.addEdge(v3,v2,new relationshipEdge("playing"));

        return graph;
    }

    public static VF2PatternGraph generatePatternGraph()
    {
        VF2PatternGraph pattern=new VF2PatternGraph();

        patternVertex v1=new patternVertex("player");
        //variable literal
        v1.addAttribute(new attribute("name"));
        //constant literal
        v1.addAttribute(new attribute("age","36"));

        pattern.addVertex(v1);

        patternVertex v2=new patternVertex("team");
        //variable literal
        v2.addAttribute(new attribute("league"));
        pattern.addVertex(v2);

        pattern.addEdge(v1,v2,new relationshipEdge("playing"));

        return pattern;
    }

}
