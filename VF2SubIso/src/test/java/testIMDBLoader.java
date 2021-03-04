import graphLoader.GraphLoader;
import graphLoader.IMDBLoader;
import infra.TGFD;

import java.util.ArrayList;

public class testIMDBLoader {

    public static void main(String []args)
    {
        GraphLoader imdbLoader=new IMDBLoader(new ArrayList <TGFD>(),"C:\\Users\\admin\\Downloads\\actors-1894.nt");
    }

}
