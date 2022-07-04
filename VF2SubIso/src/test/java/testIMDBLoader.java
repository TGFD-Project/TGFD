

import Loader.GraphLoader;
import Loader.IMDBLoader;
import ICs.TGFD;

import java.util.ArrayList;
import java.util.Collections;

public class testIMDBLoader {

    public static void main(String []args)
    {
        GraphLoader imdbLoader=new IMDBLoader(new ArrayList <TGFD>(), Collections.singletonList("F:\\MorteZa\\Datasets\\IMDB\\rdf\\template.nt"));

        //GraphLoader syntheticLoader=new SyntheticLoader(new ArrayList <TGFD>(), Collections.singletonList("C:\\Users\\admin\\Downloads\\social-a-graph.txt0.txt"));
    }

}
