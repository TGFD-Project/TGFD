import KMeansClustering.Centroid;
import KMeansClustering.EuclideanDistance;
import KMeansClustering.KMeans;
import PDD.Record;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toSet;


public class testPDDClusters {
    public static void main(String[] args) {
        //read match from TGFD match results
        HashMap<String, double[]> patientsDose_fromFile=readDosageFromFile("/Users/lexie/Desktop/Master_Project/Violations/P1612/match_p1612.txt");
        saveDoses("/Users/lexie/Desktop/Master_Project/Clusters/doses",patientsDose_fromFile);
        HashMap<String,HashMap<String,Double>> patientsBio_fromFile = readBioFromFile("/Users/lexie/Desktop/Master_Project/Violations/P1612/match_p1612.txt");

        List<String> patients =getPatients(patientsDose_fromFile);
        System.out.println("Patients size is "+patients.size());
//
//        for(String patient:patients){
//            System.out.println("patient is"+patient);
//        }

        HashMap<String,HashMap<String,Double>> patientsWithTags=computeTaggedPatients(6,patientsDose_fromFile,patientsBio_fromFile);

//        for(Map.Entry<String,HashMap<String,Double>>i:patientsWithTags.entrySet()){
//            System.out.println("Patient"+i.getKey());
//            HashMap<String,Double> attributes = i.getValue();
//            for(Map.Entry<String,Double> r:attributes.entrySet()){
//                System.out.println("attribute"+r.getKey()+"with value"+r.getValue());
//            }
//        }
        //create patients records with URI and Features
        List<Record> records = datasetWithTaggedPatients(patientsWithTags);
//        for(Record r:records){
//            System.out.println(r.toString());
//        }
//

        computeKMeansClustering(records);



    }

    public static void computeKMeansClustering(List<Record> records){
        HashMap<String,HashMap<String,String>> clusters_collection = new HashMap<>();
        //form k-means Clustering
        Map<Centroid,List<Record>> clusters = KMeans.fit(records,7,new EuclideanDistance(),1000);

        // Printing the cluster configuration
        String number;
        clusters.forEach((key, value) -> {
            System.out.println("-------------------------- CLUSTER ----------------------------");

            // Sorting the coordinates to see the most significant tags first.
            System.out.println(key);
            //TODO: change stream
            String members = String.join(", ", value.stream().map(Record::getUri).collect(toSet()));
            System.out.print(members);
            System.out.println();
            System.out.println();

            HashMap<String,String> temp_attributes = new HashMap<>();

            temp_attributes.put("centroid",key.toString());
            temp_attributes.put("members",members);

            clusters_collection.put("Cluster"+key,temp_attributes);

        });
        saveClusters("/Users/lexie/Desktop/Master_Project/Clusters/cluster",clusters_collection);

    }

    public static void saveClusters(String path,HashMap<String,HashMap<String,String>> clusters_collection) {
        int num=1;
        try {
            FileWriter file1 = new FileWriter(path + "_"+"1612"+".txt");
            for(Map.Entry<String,HashMap<String,String>> i:clusters_collection.entrySet()){
//                System.out.println(i.getKey());
                file1.write(num+".");
                HashMap<String,String> temp = i.getValue();
                for(Map.Entry<String,String> r:temp.entrySet()){
//                    System.out.println(r.getKey()+r.getValue());
                    file1.write(r.getKey()+r.getValue()+"\n");
                }
                num++;
            }
            System.out.println("Successfully write file"+path + "_"+"1612"+".txt");
            file1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void saveDoses(String path,HashMap<String, double[]> patient_collection){
        int num=1;
        try {
            FileWriter file1 = new FileWriter(path + "_"+"1612"+".txt");

            for(Map.Entry<String,double[]> e:patient_collection.entrySet()){
                String dose="dose:";
                String patient = e.getKey();
                file1.write(num+"."+patient+"\n");
                double[] doses = e.getValue();
                for(double i:doses){
                    dose = dose+ i +",";
                }
                file1.write(dose+"\n");
                num++;
            }
            System.out.println("Successfully write file"+path + "_"+"1612"+".txt");
            file1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        public static HashMap<String, HashMap<String,Double>> readBioFromFile(String path){
        HashMap<String, HashMap<String,Double>> patient_collection = new HashMap<>();
        String temp_age;
        String temp_gender;
        String temp;
        String patient;
        double age;
        double gender;


        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(data!=null) {
                    if (data.contains("Interval")){
//                        System.out.println("data is"+data);
                        temp = data.substring(data.indexOf("admission_2.uri: "), data.indexOf(",admission_1.age:"));
                        patient = temp.substring(temp.lastIndexOf(" ")+1);


                        temp_age = data.substring(data.indexOf("admission_1.age: ")+17,data.indexOf(",admission_2.age"));
                        age=0;
                        age = switch (temp_age) {
                            case "children" -> 1;
                            case "youth" -> 2;
                            case "adult" -> 3;
                            case "senior" -> 4;
                            default -> age;
                        };


                        temp_gender = data.substring(data.indexOf("admission_1.gender: ")+20,data.indexOf(",admission_2.gender"));
                        gender = 0;
                        if(temp_gender.equals("f")){
                            gender = 0;
                        }else if(temp_gender.equals("m")){
                            gender = 1;
                        }
                        putInBioHashMap(patient,patient_collection,age,gender);
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

    public static HashMap<String, double[]> readDosageFromFile(String path) {
        HashMap<String, double[]> patient_collection = new HashMap<>();
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
                    if (data.contains("Interval")){
//                        System.out.println("data is"+data);
                        temp = data.substring(data.indexOf("admission_2.uri: "), data.indexOf(",admission_1.age:"));
                        patient = temp.substring(temp.lastIndexOf(" ")+1);

                        //TODO: improve for multiple intervals
                        temp_interval = data.substring(data.indexOf("[Interval{start= "), data.indexOf("}"));
//                        System.out.println("Interval is" + temp_interval);

                        start_date = Integer.parseInt(temp_interval.substring(19, temp_interval.lastIndexOf(",")));
                        end_date = Integer.parseInt(temp_interval.substring(temp_interval.indexOf(", end= t_")+9));

                        temp_signatureY = data.substring(data.indexOf("signatureY=")+11, data.indexOf("mg"));
                        if(temp_signatureY.contains("-")){
                            temp_signatureY = temp_signatureY.substring(0,temp_signatureY.indexOf('-'));
                        }
                        signatureY = Double.parseDouble(temp_signatureY);
//                        System.out.println("signature" + "for patient"+patient+"is"+ signatureY);
                        putInDoseHashMap(patient,start_date,end_date,patient_collection,signatureY);
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

    public static HashMap<String,HashMap<String,Double>> computeTaggedPatients(int numOfWindows, HashMap<String,double[]> patient_collection,
                                                                                        HashMap<String,HashMap<String,Double>> bio_collection){
        HashMap<String,HashMap<String,Double>> taggedPatients = new HashMap<>();
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
//            System.out.println("For patient"+patient+"The number of valid winodws is"+valid_windows);
            double mean=mean_sum/valid_windows;
//            System.out.println("For patient"+patient+"The mean of all valid winodws is"+mean);

            HashMap<String,Double> attributes = bio_collection.get(patient);
            attributes.put(type,mean);
            taggedPatients.put(patient,attributes);
        }


        return taggedPatients;
    }


    public static void putInDoseHashMap(String patient,int start_date, int end_date, HashMap<String,double[]> patient_collection,double signatureY){
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

    public static void putInBioHashMap(String patient,HashMap<String,HashMap<String,Double>> patient_collection,
                                       double age, double gender){
        if(!patient_collection.containsKey(patient)){
            HashMap<String,Double> bio_collection = new HashMap<>();
            bio_collection.put("age",age);
            bio_collection.put("gender",gender);
            patient_collection.put(patient,bio_collection);
        }

    }


    public static List<Record> datasetWithTaggedPatients(HashMap<String, HashMap<String,Double>> patientsWithTags){
        List<Record> records = new ArrayList<>();
        for(Map.Entry<String,HashMap<String,Double>>i:patientsWithTags.entrySet()){
            String patient = i.getKey();
            HashMap<String,Double> tags = i.getValue();
            records.add(new Record(patient,tags));
        }
        return records;
    }

}
