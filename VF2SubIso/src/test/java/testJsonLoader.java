import graphLoader.dbPediaChangeLoader;

public class testJsonLoader {

    public static void main(String []args)
    {
        dbPediaChangeLoader loader=new dbPediaChangeLoader("D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\out\\artifacts\\VF2SubIso_jar\\sample.json");
        loader.getAllChanges();
    }

}
