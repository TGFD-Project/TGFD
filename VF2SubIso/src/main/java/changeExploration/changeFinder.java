package changeExploration;

import graphLoader.dbPediaLoader;
import infra.*;

import java.util.ArrayList;
import java.util.List;

public class changeFinder {

    VF2DataGraph g1,g2;
    private List<change> allChanges=new ArrayList<>();

    public changeFinder(dbPediaLoader db1, dbPediaLoader db2)
    {
        g1=db1.getGraph();
        g2=db2.getGraph();
    }

    public List<change> findAllChanged()
    {
        findChanges(g1,g2,changeType.deleteEdge,changeType.deleteVertex,changeType.deleteAttr,changeType.changeAttr);
        findChanges(g2,g1,changeType.insertEdge, changeType.insertVertex, changeType.insertAttr,null);
        return allChanges;
    }

    private void findChanges(VF2DataGraph first, VF2DataGraph second, changeType edgeType,
                             changeType vertexType, changeType attrType,changeType attrChange)
    {
        for (Vertex v:first.getGraph().vertexSet()) {
            DataVertex v1=(DataVertex) v;
            for (RelationshipEdge e:first.getGraph().outgoingEdgesOf(v)) {
                DataVertex dst=(DataVertex)e.getTarget();
                if(second.getNode(v1.getVertexURI())==null)
                {
                    change eChange=new edgeChange(edgeType,v1,dst,e.getLabel());
                    allChanges.add(eChange);
                }
                else if(second.getNode(dst.getVertexURI())==null)
                {
                    change eChange=new edgeChange(edgeType,v1,dst,e.getLabel());
                    allChanges.add(eChange);
                }
                else
                {
                    boolean exist=false;
                    Vertex v1_prime=second.getNode(v1.getVertexURI());
                    for (RelationshipEdge e2:second.getGraph().outgoingEdgesOf(v1_prime)) {
                        DataVertex dst2=(DataVertex) e2.getTarget();
                        if(e.getLabel().equals(e2.getLabel()) && dst2.getVertexURI().equals(dst.getVertexURI()))
                        {
                            exist=true;
                            break;
                        }
                    }
                    if(!exist)
                    {
                        change eChange=new edgeChange(edgeType,v1,dst,e.getLabel());
                        allChanges.add(eChange);
                    }
                }
            }
        }

        for (Vertex v:first.getGraph().vertexSet()) {
            DataVertex v1=(DataVertex) v;
            DataVertex v2= (DataVertex) second.getNode(v1.getVertexURI());
            if(v2==null)
            {
                change vChange=new vertexChange(vertexType,v1);
                allChanges.add(vChange);
                continue;
            }
            for (Attribute attr:v.getAllAttributesList()) {
                if(!v2.hasAttribute(attr.getAttrName()))
                {
                    change changeOfAttr=new attributeChange(attrType,v1,attr);
                    allChanges.add(changeOfAttr);
                }
                else if(attrChange!=null && !v2.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                {
                    change changeOfAttr=new attributeChange(changeType.changeAttr,v1,attr);
                    allChanges.add(changeOfAttr);
                }
            }
        }
    }
}
