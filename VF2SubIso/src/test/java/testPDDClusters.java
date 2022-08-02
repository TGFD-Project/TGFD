import KMeansClustering.Centroid;
import KMeansClustering.EuclideanDistance;
import KMeansClustering.KMeans;
import PDD.Record;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static java.util.stream.Collectors.toSet;


public class testPDDClusters {
    public static void main(String[] args) {
        //TODO:read match from TGFD match results
        HashMap<String, double[]> patient_collection = new HashMap<>();
        HashMap<String, double[]> patients_fromFile=readFromFile("/Users/lexie/Desktop/Master_Project/Violations/P1612/match_p1612.txt",patient_collection);
        List<String> patients =getPatients(patients_fromFile);

        for(String patient:patients){
            System.out.println("patient is"+patient);
        }
//        Set<String> features=;
        HashMap<String,HashMap<String,Double>> patientsWithTags=computeDoseSignatureFromWindow(6,patients_fromFile);

        //TODO:create patients records with URI and Features
        List<Record> records = datasetWithTaggedPatients(patientsWithTags);
        for(Record r:records){
            System.out.println(r.toString());
        }

        //TODO:form k-means Clustering
        Map<Centroid,List<Record>> clusters = KMeans.fit(records,7,new EuclideanDistance(),1000);
        // Printing the cluster configuration
        clusters.forEach((key, value) -> {
            System.out.println("-------------------------- CLUSTER ----------------------------");

            // Sorting the coordinates to see the most significant tags first.
//            System.out.println(sortedCentroid(key));
            //TODO: change stream
            String members = String.join(", ", value.stream().map(Record::getUri).collect(toSet()));
            System.out.print(members);
            System.out.println();
            System.out.println();
        });

    }

    public static void computeKMeansClustering(){

    }


    public static HashMap<String, double[]> readFromFile(String path, HashMap<String, double[]> patient_collection) {
        String temp;
        String temp_interval;
        String temp_signatureY;
        String patient;
        double signatureY;
        int start_date;
        int end_date;
        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(data!=null) {
//                    System.out.println("data is"+data);
                    if (data.contains("Interval")){
                        temp = data.substring(data.indexOf("admission_2.uri: "), data.indexOf(",admission_1.age:"));
                        patient = temp.substring(temp.lastIndexOf(" ")+1);


                        temp_interval = data.substring(data.indexOf("[Interval{start= "), data.indexOf("}], signatureX="));
//                        System.out.println("Interval is" + temp_interval);

                        start_date = Integer.parseInt(temp_interval.substring(19, temp_interval.lastIndexOf(",")));
                        end_date = Integer.parseInt(temp_interval.substring(temp_interval.indexOf(", end= t_")+9));

                        temp_signatureY = data.substring(data.indexOf("signatureY=")+11, data.indexOf("mg"));

                        signatureY = Double.parseDouble(temp_signatureY);
//                        System.out.println("signature" + "for patient"+patient+"is"+ signatureY);
                        putInHashMap(patient,start_date,end_date,patient_collection,signatureY);
                    }

                }

            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return patient_collection;
    }

    public static List<String> getPatients(HashMap<String, double[]> patients_collection){
        List<String> patients = new ArrayList<>(patients_collection.keySet());
        return  patients;
    }

    public static HashMap<String,HashMap<String,Double>> computeDoseSignatureFromWindow(int numOfWindows, HashMap<String,double[]> patient_collection){
        HashMap<String,HashMap<String,Double>> doseSignature = new HashMap<>();
        String type = "dose_signature";
        for(Map.Entry<String, double[]> i:patient_collection.entrySet()){
            String patient = i.getKey();
            double[] doses = i.getValue();
            double mean_sum = 0;
            int valid_windows = 0;
            for(int j=1;j<=doses.length-numOfWindows;j++){
                //compute single window size mean
                double single_sum = 0;
                for(int k=j;k<j+numOfWindows;k++){
                    single_sum=single_sum+doses[k];
                }
                double single_mean = single_sum/numOfWindows;

                //Add single window size mean to sum if the window is valid
                if(single_mean!=0){
                    mean_sum = mean_sum+single_mean;
                    valid_windows++;
                }

            }
            System.out.println("For patient"+patient+"The number of valid winodws is"+valid_windows);
            double mean=mean_sum/valid_windows;
            System.out.println("For patient"+patient+"The mean of all valid winodws is"+mean);

            HashMap<String,Double> mean_doseSignature = new HashMap<>();
            mean_doseSignature.put(type,mean);
            doseSignature.put(patient,mean_doseSignature);
        }
        return doseSignature;
    }


    public static void putInHashMap(String patient,int start_date, int end_date,HashMap<String,double[]>patient_collection,double signatureY){
//        System.out.println("patient is" + patient);
        double[] original = new double[32];
        for(int i=0;i<32;i++){
            original[i]=0;
        }

        if(!patient_collection.containsKey(patient)) {
            patient_collection.put(patient,original);
        }

        double[] temp_dosage=patient_collection.get(patient);

        for(int i=start_date;i<=end_date;i++){
            temp_dosage[i]=signatureY;
        }

        patient_collection.put(patient,temp_dosage);


    }

    public static List<Record> datasetWithTaggedPatients(HashMap<String,HashMap<String,Double>> patientsWithTags){
        List<Record> records = new ArrayList<>();
        for(Map.Entry<String,HashMap<String,Double>>i:patientsWithTags.entrySet()){
            String patient = i.getKey();
            HashMap<String,Double> tags = i.getValue();
            records.add(new Record(patient,tags));
        }
        return records;
    }

}
