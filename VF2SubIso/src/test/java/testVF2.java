
import VF2Runner.VF2SubgraphIsomorphism;
import Infra.*;
import Loader.TGFDGenerator;

public class testVF2 {

    public static void main(String []args) {
        VF2SubgraphIsomorphism VF2=new VF2SubgraphIsomorphism();
        TGFDGenerator tgfd=new TGFDGenerator("D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\src\\test\\java\\samplePatterns\\pattern1.txt");
        System.out.println("\n########## Graph pattern ##########");
        System.out.println(tgfd.getTGFDs().get(0).getPattern().toString());
        VF2.execute(generateDataGraph(),tgfd.getTGFDs().get(0).getPattern(),true);
    }

    public static VF2DataGraph generateDataGraph()  {
        VF2DataGraph graph=new VF2DataGraph();

        DataVertex v1=new DataVertex("Frank_Lampard","player");
        v1.addAttribute("name","lampard");
        v1.addAttribute("age","34");
        v1.addAttribute("number","11");
        graph.addVertex(v1);

        DataVertex v3=new DataVertex("Didier_Drogba","player");
        v3.addAttribute("name","Drogba");
        v3.addAttribute("age","36");
        graph.addVertex(v3);

        DataVertex v2=new DataVertex("Team_Chelsea","team");
        v2.addAttribute("name","Chelsea");
        v2.addAttribute("league","Premiere League");
        graph.addVertex(v2);

        graph.addEdge(v1,v2,new RelationshipEdge("playing"));
        graph.addEdge(v3,v2,new RelationshipEdge("play"));

        return graph;
    }

    public static VF2PatternGraph generatePatternGraph()
    {
        VF2PatternGraph pattern=new VF2PatternGraph();

        PatternVertex v1=new PatternVertex("player");
        //variable literal
        v1.addAttribute(new Attribute("name"));
        //constant literal
        v1.addAttribute(new Attribute("age","36"));

        pattern.addVertex(v1);

        PatternVertex v2=new PatternVertex("team");
        //variable literal
        v2.addAttribute(new Attribute("league"));
        pattern.addVertex(v2);

        pattern.addEdge(v1,v2,new RelationshipEdge("playing"));

        return pattern;
    }
}
