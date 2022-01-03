package VF2BasedWorkload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class WorkloadPartitioner {

    private WorkloadEstimator estimator;
    private HashMap<Integer, ArrayList<Joblet>> jobletsByFragmentID;

    public WorkloadPartitioner(WorkloadEstimator estimator)
    {
        this.estimator=estimator;
    }

    public HashMap<Integer, ArrayList<Joblet>> partition()
    {
        int numberOfJoblets= estimator
                .getJobletsByFragmentID()
                .keySet()
                .stream()
                .mapToInt(id -> id)
                .map(id -> estimator.getJobletsByFragmentID().get(id).size())
                .sum();
        Joblet [] allJoblets=new Joblet[numberOfJoblets];
        int i=0;
        for (int id:estimator.getJobletsByFragmentID().keySet()) {
            for (Joblet job:estimator.getJobletsByFragmentID().get(id)) {
                allJoblets[i]=job;
                i++;
            }
        }
        int []fragmentSize=new int[estimator.getNumberOfProcessors()];
        jobletsByFragmentID=new HashMap<>();
        for (i=0;i< estimator.getNumberOfProcessors();i++)
        {
            jobletsByFragmentID.put(i+1,new ArrayList<>());
        }
        HashSet<Integer> visited=new HashSet<>();
        while (true)
        {
            int id=-1;
            double min=Double.MAX_VALUE;
            for (i=0;i<allJoblets.length;i++) {
                if(!visited.contains(i) && allJoblets[i].getSize()<min)
                {
                    id=i;
                    min=allJoblets[i].getSize();
                }
            }
            if(id!=-1)
            {
                visited.add(id);
                int minLoad=Integer.MAX_VALUE;
                int selectedFragment=-1;
                for (i=0;i<fragmentSize.length;i++)
                {
                    if(fragmentSize[i]<minLoad)
                    {
                        minLoad=fragmentSize[i];
                        selectedFragment=i;
                    }
                }
                //for (i=0;i<fragmentSize.length;i++)
                //{
                //    System.out.print("F"+(i+1) + ": " + fragmentSize[i] + "  **  ");
                //}
                //System.out.println("\nNew load: " + allJoblets[id].getSize() + " -> " + allJoblets[id].getCenterNode().getVertexURI());
                fragmentSize[selectedFragment]+=allJoblets[id].getSize();
                jobletsByFragmentID.get(selectedFragment+1).add(allJoblets[id]);
            }
            else
                break;
        }
        for (i=0;i<fragmentSize.length;i++)
            System.out.print("F"+(i+1) + ": " + fragmentSize[i] + "  **  ");
        System.out.println("\nPartitioner is Done. Number of joblets: " + allJoblets.length);
        return jobletsByFragmentID;
    }

}
