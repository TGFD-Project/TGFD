package changeExploration;

import infra.Attribute;
import infra.DataVertex;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ChangeLoader {

    private List<Change> allChanges;

    public ChangeLoader(String path)
    {
        this.allChanges=new ArrayList<>();
        loadChanges(path);
    }

    public List<Change> getAllChanges() {
        return allChanges;
    }

    private void loadChanges(String path) {

        JSONParser parser = new JSONParser();
        try
        {
            Object json = parser.parse(new FileReader(path));

            org.json.simple.JSONArray jsonArray = (org.json.simple.JSONArray) json;
            System.out.println("");
            Iterator iterator = jsonArray.iterator();
            while (iterator.hasNext())
            {
                JSONObject object= (JSONObject) iterator.next();

                org.json.simple.JSONArray allRelevantTGFDs=(org.json.simple.JSONArray)object.get("tgfds");
                HashSet <String> relevantTGFDs=new HashSet <>();
                for (Object TGFDName : allRelevantTGFDs)
                    relevantTGFDs.add((String) TGFDName);

                ChangeType type = ChangeType.valueOf((String) object.get("typeOfChange"));
                int id=Integer.parseInt(object.get("id").toString());
                if(type==ChangeType.deleteEdge || type==ChangeType.insertEdge)
                {
                    String src=(String) object.get("src");
                    String dst=(String) object.get("dst");
                    String label=(String) object.get("label");
                    Change change=new EdgeChange(type,id,src,dst,label);
                    change.addTGFD(relevantTGFDs);
                    allChanges.add(change);
                }
                else if(type==ChangeType.changeAttr || type==ChangeType.deleteAttr || type==ChangeType.insertAttr)
                {
                    String uri=(String) object.get("uri");
                    JSONObject attrObject=(JSONObject) object.get("attribute");
                    String attrName=(String) attrObject.get("attrName");
                    String attrValue=(String) attrObject.get("attrValue");
                    Change change=new AttributeChange(type,id,uri,new Attribute(attrName,attrValue));
                    change.addTGFD(relevantTGFDs);
                    allChanges.add(change);
                }
                else if(type==ChangeType.deleteVertex || type==ChangeType.insertVertex)
                {
                    JSONObject vertexObj=(JSONObject) object.get("vertex");
                    String uri=(String) vertexObj.get("vertexURI");
                    org.json.simple.JSONArray allTypes=(org.json.simple.JSONArray)vertexObj.get("types");

                    ArrayList<String> types=new ArrayList<>();
                    for (Object allType : allTypes)
                        types.add((String) allType);

                    ArrayList<Attribute> allAttributes=new ArrayList<>();
                    org.json.simple.JSONArray allAttributeLists=(org.json.simple.JSONArray)vertexObj.get("allAttributesList");
                    for (Object allAttributeList : allAttributeLists) {
                        JSONObject attrObject = (JSONObject) allAttributeList;
                        String attrName = (String) attrObject.get("attrName");
                        String attrValue = (String) attrObject.get("attrValue");
                        allAttributes.add(new Attribute(attrName, attrValue));
                    }

                    DataVertex dataVertex=new DataVertex(uri,types.get(0));
                    for (int i=1;i<types.size();i++)
                        dataVertex.addTypes(types.get(i));
                    for (Attribute attribute:allAttributes) {
                        dataVertex.addAttribute(attribute);
                    }
                    Change change=new VertexChange(type,id,dataVertex);
                    change.addTGFD(relevantTGFDs);
                    allChanges.add(change);
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
