package QPathBasedWorkload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class WorkloadPartitionerForJobs {

    private WorkloadEstimatorForJobs estimator;
    private HashMap<Integer, ArrayList<Job>> jobsByFragmentID;

    public WorkloadPartitionerForJobs(WorkloadEstimatorForJobs estimator)
    {
        this.estimator=estimator;
    }

    public HashMap<Integer, ArrayList<Job>> partition()
    {
        int numberOfJobs= estimator
                .getJobsByFragmentID()
                .keySet()
                .stream()
                .mapToInt(id -> id)
                .map(id -> estimator.getJobsByFragmentID().get(id).size())
                .sum();
        Job [] allJobs=new Job[numberOfJobs];
        int i=0;
        for (int id:estimator.getJobsByFragmentID().keySet()) {
            for (Job job:estimator.getJobsByFragmentID().get(id)) {
                allJobs[i]=job;
                i++;
            }
        }
        int []fragmentSize=new int[estimator.getNumberOfProcessors()];
        jobsByFragmentID=new HashMap<>();
        for (i=0;i< estimator.getNumberOfProcessors();i++)
        {
            jobsByFragmentID.put(i+1,new ArrayList<>());
        }
        HashSet<Integer> visited=new HashSet<>();
        while (true)
        {
            int id=-1;
            double min=Double.MAX_VALUE;
            for (i=0;i<allJobs.length;i++) {
                if(!visited.contains(i) && allJobs[i].getSize()<min)
                {
                    id=i;
                    min=allJobs[i].getSize();
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
                //System.out.println("\nNew load: " + allJobs[id].getSize() + " -> " + allJobs[id].getCenterNode().getVertexURI());
                fragmentSize[selectedFragment]+=allJobs[id].getSize();
                jobsByFragmentID.get(selectedFragment+1).add(allJobs[id]);
            }
            else
                break;
        }
        for (i=0;i<fragmentSize.length;i++)
            System.out.print("F"+(i+1) + ": " + fragmentSize[i] + "  **  ");
        System.out.println("\nPartitioner is Done. Number of jobs: " + allJobs.length);
        return jobsByFragmentID;
    }

}
