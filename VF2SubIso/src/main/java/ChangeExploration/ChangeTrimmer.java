package ChangeExploration;

import ICs.TGFD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChangeTrimmer {

    private List <Change> changes;
    private List<TGFD> tgfds;
    private List<Change> vertexInsertionChanges;
    private HashMap<Integer, List<String>> TGFDsByChangeID;
    private HashMap<String,List<Change>> changesByTGFDs;
    private HashMap<String,Integer> TGFDCounts;
    private boolean []changeIDs;

    public ChangeTrimmer(List<Change> changes, List<TGFD> tgfds)
    {
        this.changes=changes;
        this.tgfds=tgfds;
    }

    public List<Change> trimChanges(int allowedNumberOfChanges)
    {
        initialize();
        List <Change> trimmedChanges = new ArrayList <>(vertexInsertionChanges);
        for (int i=0;i<allowedNumberOfChanges;i++)
        {
            Change change=getAChange();
            if(change!=null)
                trimmedChanges.add(change);
            else
                break;
        }
        return trimmedChanges;
    }

    private void initialize()
    {
        vertexInsertionChanges =new ArrayList <>();
        changeIDs=new boolean[changes.size()+1];
        Arrays.fill(changeIDs, Boolean.FALSE);
        for (Change change:changes) {
            if(change instanceof VertexChange && change.getTypeOfChange()==ChangeType.insertVertex)
            {
                vertexInsertionChanges.add(change);
                changeIDs[change.getId()]=true;
            }
            else if(change instanceof VertexChange && change.getTypeOfChange()==ChangeType.deleteVertex)
            {
                changeIDs[change.getId()]=true;
            }
        }
        TGFDCounts=new HashMap <>();
        for (TGFD tgfd:tgfds) {
            TGFDCounts.put(tgfd.getName(),0);
        }
        TGFDsByChangeID=new HashMap <>();
        changesByTGFDs=new HashMap <>();
        for (Change change:changes) {
            if(changeIDs[change.getId()])
                continue;
            for (String tgfdName:change.getTGFDs()) {
                if(!TGFDsByChangeID.containsKey(change.getId()))
                    TGFDsByChangeID.put(change.getId(),new ArrayList <>());
                TGFDsByChangeID.get(change.getId()).add(tgfdName);

                if(!changesByTGFDs.containsKey(tgfdName))
                    changesByTGFDs.put(tgfdName,new ArrayList <>());
                changesByTGFDs.get(tgfdName).add(change);
            }
        }
    }

    private Change getAChange()
    {
        // If TGFDCount is empty, then we have no specific TGFD,
        // We just have to return the changes in order.
        if(TGFDCounts.keySet().isEmpty()) {
            for (Change change : changes) {
                if (!changeIDs[change.getId()]) {
                    changeIDs[change.getId()] = true;
                    return change;
                }
            }
            return null;
        }

        // If we have a set of TGFDs as input and
        // we want to distribute the changes evenly between all TGFDs
        String tgfdName="";
        int min=Integer.MAX_VALUE;
        for (String name:TGFDCounts.keySet()) {
            if(TGFDCounts.get(name)<min && TGFDCounts.get(name)<changesByTGFDs.get(name).size())
            {
                min=TGFDCounts.get(name);
                tgfdName=name;
            }
        }
        if(tgfdName.equals(""))
            return null;
        for (Change change:changesByTGFDs.get(tgfdName)) {
            if(!changeIDs[change.getId()])
            {
                changeIDs[change.getId()]=true;
                for (String name:TGFDsByChangeID.get(change.getId()))
                    TGFDCounts.put(name,TGFDCounts.get(name) +1);
                return change;
            }
        }
        return null;
    }



}
