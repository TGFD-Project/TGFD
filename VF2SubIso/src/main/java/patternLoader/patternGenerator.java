package patternLoader;

import infra.VF2PatternGraph;
import infra.Attribute;
import infra.PatternVertex;
import infra.relationshipEdge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class patternGenerator {

     private List<VF2PatternGraph> allPatterns;

    public patternGenerator(String path)
    {
        allPatterns=new ArrayList<>();
        try {
            loadGraphPattern(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public List<VF2PatternGraph> getPattern() {
        return allPatterns;
    }

    private void loadGraphPattern(String path) throws FileNotFoundException {

        HashMap<String, PatternVertex> allVertices=new HashMap<>();
        VF2PatternGraph currentPattern=null;

        Scanner scanner = new Scanner(new File(path));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.startsWith("#pattern"))
            {
                if(currentPattern!=null)
                    allPatterns.add(currentPattern);
                currentPattern=new VF2PatternGraph();
                allVertices=new HashMap<>();
            }
            if(line.startsWith("#v"))
            {
                String args[] = line.split(" ")[1].split(",");
                PatternVertex v=new PatternVertex(args[1]);
                for (int i=2;i<args.length;i++)
                {
                    if(args[i].contains("@"))
                    {
                        String []attr=args[i].substring(1,args[i].length()-1).split("@");

                        v.addAttribute(new Attribute(attr[0],attr[1]));
                    }
                    else
                    {
                        v.addAttribute(new Attribute(args[i].substring(1,args[i].length()-1)));
                    }
                }
                currentPattern.addVertex(v);
                allVertices.put(args[0],v);
            }
            else if(line.startsWith("#e"))
            {
                String args[] = line.split(" ")[1].split(",");
                currentPattern.addEdge(allVertices.get(args[0]),allVertices.get(args[1]),new relationshipEdge(args[2]));
            }
        }
        if(currentPattern!=null)
            allPatterns.add(currentPattern);
    }

}
