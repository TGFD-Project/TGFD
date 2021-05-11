package Partitioner;

import infra.FocusNode;

import java.util.ArrayList;
import java.util.List;

public class MessageInterpreter {

    public static List <FocusNode> getFocusNodes(String message)
    {
        List <FocusNode> allFocusNodes=new ArrayList <>();
        for (String focusNodeMessage:message.split("\n")) {
            String []res=focusNodeMessage.split("#");
            if(res.length==3)
            {
                allFocusNodes.add(new FocusNode(res[0],res[1],Integer.parseInt(res[2])));
            }
        }
        return allFocusNodes;
    }

}
