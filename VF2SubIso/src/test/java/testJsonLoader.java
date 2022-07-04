

import ChangeExploration.ChangeLoader;

public class testJsonLoader {

    public static void main(String []args)
    {
        ChangeLoader loader=new ChangeLoader("D:\\Java\\TGFD-Project\\TGFD\\VF2SubIso\\out\\artifacts\\VF2SubIso_jar\\sample.json");
        loader.getAllChanges();
    }

}
