package changeExploration;

import graphLoader.dbPediaLoader;
import infra.*;

import java.util.ArrayList;
import java.util.List;

public class ChangeFinder {

    VF2DataGraph g1,g2;
    private List<Change> allChanges=new ArrayList<>();

    public ChangeFinder(dbPediaLoader db1, dbPediaLoader db2)
    {
        g1=db1.getGraph();
        g2=db2.getGraph();
    }

    public List<Change> findAllChanged()
    {
        findChanges(g1,g2, ChangeType.deleteEdge, ChangeType.deleteVertex, ChangeType.deleteAttr, ChangeType.changeAttr);
        findChanges(g2,g1, ChangeType.insertEdge, ChangeType.insertVertex, ChangeType.insertAttr,null);
        return allChanges;
    }

    private void findChanges(VF2DataGraph first, VF2DataGraph second, ChangeType edgeType,
                             ChangeType vertexType, ChangeType attrType, ChangeType attrChange)
    {
        for (Vertex v:first.getGraph().vertexSet()) {
            DataVertex v1=(DataVertex) v;
            for (RelationshipEdge e:first.getGraph().outgoingEdgesOf(v)) {
                DataVertex dst=(DataVertex)e.getTarget();
                if(second.getNode(v1.getVertexURI())==null)
                {
                    Change eChange=new EdgeChange(edgeType,v1.getVertexURI(),dst.getVertexURI(),e.getLabel());
                    allChanges.add(eChange);
                }
                else if(second.getNode(dst.getVertexURI())==null)
                {
                    Change eChange=new EdgeChange(edgeType,v1.getVertexURI(),dst.getVertexURI(),e.getLabel());
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
                        Change eChange=new EdgeChange(edgeType,v1.getVertexURI(),dst.getVertexURI(),e.getLabel());
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
                Change vChange=new VertexChange(vertexType,v1);
                allChanges.add(vChange);
                continue;
            }
            for (Attribute attr:v.getAllAttributesList()) {
                if(!v2.hasAttribute(attr.getAttrName()))
                {
                    Change changeOfAttr=new AttributeChange(attrType,v1.getVertexURI(),attr);
                    allChanges.add(changeOfAttr);
                }
                else if(attrChange!=null && !v2.getAttributeValueByName(attr.getAttrName()).equals(attr.getAttrValue()))
                {
                    Change changeOfAttr=new AttributeChange(ChangeType.changeAttr,v1.getVertexURI(),attr);
                    allChanges.add(changeOfAttr);
                }
            }
        }
    }
}
