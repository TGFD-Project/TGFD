import graphLoader.DBPediaChangeLoader;

public class testJsonLoader {

    public static void main(String []args)
    {
        DBPediaChangeLoader loader=new DBPediaChangeLoader("D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\out\\artifacts\\VF2SubIso_jar\\sample.json");
        loader.getAllChanges();
    }

}
