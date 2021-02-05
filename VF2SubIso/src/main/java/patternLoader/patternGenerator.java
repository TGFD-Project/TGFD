package patternLoader;

import infra.VF2PatternGraph;
import infra.attribute;
import infra.patternVertex;
import infra.relationshipEdge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class patternGenerator {

    VF2PatternGraph pattern;

    public patternGenerator(String path)
    {
        pattern=new VF2PatternGraph();
        try {
            loadGraphPattern(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public VF2PatternGraph getPattern() {
        return pattern;
    }

    private void loadGraphPattern(String path) throws FileNotFoundException {

        HashMap<String, patternVertex> allVertices=new HashMap<>();

        Scanner scanner = new Scanner(new File(path));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.startsWith("#v"))
            {
                String args[] = line.split(" ")[1].split(",");
                patternVertex v=new patternVertex(args[1]);
                for (int i=2;i<args.length;i++)
                {
                    if(args[i].contains("@"))
                    {
                        String []attr=args[i].substring(1,args[i].length()-1).split("@");

                        v.addAttribute(new attribute(attr[0],attr[1]));
                    }
                    else
                    {
                        v.addAttribute(new attribute(args[i].substring(1,args[i].length()-1)));
                    }
                }
                pattern.addVertex(v);
                allVertices.put(args[0],v);
            }
            else if(line.startsWith("#e"))
            {
                String args[] = line.split(" ")[1].split(",");
                pattern.addEdge(allVertices.get(args[0]),allVertices.get(args[1]),new relationshipEdge(args[2]));
            }
        }

    }

}
