package Partitioner;

import VF2BasedWorkload.Joblet;

import java.util.ArrayList;
import java.util.List;

public class MessageInterpreter {

    public static List <Joblet> getFocusNodes(String message)
    {
        List <Joblet> allJoblets =new ArrayList <>();
        for (String focusNodeMessage:message.split("\n")) {
            String []res=focusNodeMessage.split("#");
            if(res.length==4)
            {
                //TODO: null is entered as centerNode. It has to be DataVertex from an input Graph.
                //TODO: null is entered for the TGFD as well
                allJoblets.add(new Joblet(0,null,null,Integer.parseInt(res[2]),Integer.parseInt(res[3])));
            }
        }
        return allJoblets;
    }

}
