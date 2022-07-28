import java.io.*;
import java.sql.SQLOutput;
import java.util.*;

import ViolationRepair.Util;


public class testJSDivergence {


    public static void main(String[] args) {
        HashMap<String, String> patient_collection = new HashMap<>();
        HashMap<String, HashMap<String,Double>> dosage_collection = new HashMap<>();
        readFromFile("/Users/lexie/Desktop/Master_Project/Violations/P1612/match_p1612_2191_01.txt",patient_collection,dosage_collection);


    }

    public static void readFromFile(String path, HashMap<String,String> patient_collection,HashMap<String, HashMap<String,Double>> dosage_collection) {
        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            String temp = "";
            String patient ="";
            String signatureY = "";
//            HashMap<String, Double> tempSet = new HashMap<>();
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
//                System.out.println(data);
                if(data.contains("admission_1.uri:")&& data.contains("admission_1.age:"))
                    temp = data.substring(data.indexOf("admission_1.uri:"),data.indexOf("admission_1.age:")-1);
                if(temp.length()>=1)
                    patient = temp.substring(temp.lastIndexOf(" ") + 1);
//                        System.out.println("patient is "+patient);
                if(data.contains("allSignatureY="))
                    temp = data.substring(data.indexOf("allSignatureY="));
                if(temp.length()>=1)
//                        System.out.println("signature is"+temp);
                    signatureY=temp;
                if (!patient.startsWith("2191")&&patient.length()>=1&&!patient_collection.containsKey(patient)){
                    patient_collection.put(patient,signatureY);
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        System.out.println("Print Collection");
        int i=1;
        for (Map.Entry<String, String> entry : patient_collection.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
//            System.out.println(i+".");
//            System.out.println("key "+key+" value "+value+"\n");
            i++;
        }

        getDosage(patient_collection,dosage_collection);

    }

    public static void getDosage(HashMap<String,String> patient_collection,
                                 HashMap<String, HashMap<String,Double>> dosage_collection) {

        for (Map.Entry<String, String> entry : patient_collection.entrySet()) {
            HashMap<String, Double> temp = new HashMap<>();
            String patient = entry.getKey();
//            System.out.println("Patient is "+patient);
            String allsignatureY = entry.getValue().substring(15);
            ArrayList<String> elephantList = new ArrayList<>(Arrays.asList(allsignatureY.split(",")));
//            System.out.println("elephant test");
            //item: 2191-01-04=12.5mg
            for (String s : elephantList) {
//                System.out.println("ele"+s);
                ArrayList<String> lionList = new ArrayList<>(Arrays.asList(s.split("=")));
                String date = lionList.get(0);
                if(date.startsWith(" ")){
                    date=date.substring(1);
                }
                String dosage = lionList.get(1);

                if(dosage.endsWith("}")){
                    dosage=dosage.substring(0,dosage.length()-2);
                }
                dosage = dosage.replace("mg","");
                Double dose = Double.valueOf(dosage);
//                System.out.println("date " + date + " dose " + dosage);
                if(!temp.containsKey(date)){
                    temp.put(date,dose);
                }
            }
            if(!dosage_collection.containsKey(patient)){
                dosage_collection.put(patient,temp);
            }

        }

        System.out.println("Dosage HashMap");
        for(Map.Entry<String, HashMap<String, Double>> entry : dosage_collection.entrySet()){
            System.out.println("Patient"+entry.getKey());
//            System.out.println("Related Dosage"+entry.getValue());
            HashMap<String,Double> temp = entry.getValue();
            for(Map.Entry<String,Double> entry1:temp.entrySet()){
                System.out.println("date"+entry1.getKey());
                System.out.println("dose "+entry1.getValue());
            }

        }

        HashMap<String,double[]> dosage_data = getDosageFromHashMap(dosage_collection);

    }


    public static HashMap<String, double[]> getDosageFromHashMap(HashMap<String,HashMap<String,Double>> dosage_collection){
        HashMap<String,double[]> dosage_data = new HashMap<String,double[]>();
        double[] temp = new double[32];
        for(Map.Entry<String,HashMap<String,Double>> entry:dosage_collection.entrySet()){
            for(int i=0;i<temp.length;i++){
                temp[i]=0;
            }
            String patient = entry.getKey();
            if(!entry.getKey().isEmpty()){

                HashMap<String,Double> temp_map = entry.getValue();
                System.out.println("patient"+patient);
                for(Map.Entry<String,Double> entry1:temp_map.entrySet()){
                    int date=Integer.parseInt(entry1.getKey().substring(8,10));
                    double value = entry1.getValue();
                    System.out.println("date is"+date+"value is"+value);
                    temp[date]=value;
                }
                System.out.println("patient"+patient+"array");
            }
            if(!dosage_data.containsKey(patient)){
                dosage_data.put(patient,temp);
            }
        }
        return dosage_data;
    }

    public static double CalculateJSDivergence(double[]p1, double[] p2){
        return Util.jsDivergence(p1,p2);
    }



}
