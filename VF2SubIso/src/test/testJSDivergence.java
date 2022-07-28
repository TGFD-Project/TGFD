import java.io.*;
import java.sql.SQLOutput;
import java.util.*;

import ViolationRepair.Util;


public class testJSDivergence {


    public static void main(String[] args) {
        HashMap<String, String> patient_collection = new HashMap<>();
        HashMap<String, HashMap<String,Double>> dosage_collection = new HashMap<>();
        readFromFile("/Users/lexie/Desktop/Master_Project/Violations/P1612/match_p1612_2191_01.txt",patient_collection,dosage_collection);
        double[]p1={0,0,0,0,0,0,0,0,25,25,50,75,100,25,5,25,25,25,100,25,0,0,0,0,0,0,0,0,0,0,0};
        double[]p2= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,50,75,50,50,50,0,0,0,0,0,0};
        double[]p3= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,25,25,75,75,25,25,25,25,50,25,100,0,0,0};
        double[]p4= {0,0,0,0,5,5,5,12.5,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        double[]p5= {12.5,12.5,50,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        double[]p6= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2.5,2.5,12.5,2.5,2.5,2.5,2.5,2.5,2.5,2.5,0,0,0,0};
        double[]p7= {0,0,0,0,0,0,0,12.5,12.5,0,0,5,5,37.5,100,100,100,100,100,100,100,0,0,0,0,0,0,0,0,0,0};
        double[]p8= {75,75,200,75,75,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        double[]p9= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,0,0,5,5,5,25,5,5,5,5,12.5,25,5,50,5};
        double[]p10= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,50,75,5,75,5,5,0,0,0,0,0,0,0,0,0,0};
        double[]p11= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,25,25,50,75};

        double[]c1={0,0,0,0,0,0,0,0,25,0,25,25,25,75,20,20,0,0,75,75,25,0,0,0,0,0,0,0,0,0,0};
        double[]c2= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,50,25,25,0,0,50,0,0,0,0,0};
        double[]c3= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,25,0,20,0,20,0,0,0,25,25,75,100,0,0};
        double[]c4= {0,0,0,0,5,0,0,7.5,7.5,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        double[]c5= {12.5,0,37.5,50,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        double[]c6= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2.5,0,10,10,0,0,0,0,0,0,2.5,0,0,0};
        double[]c7= {0,0,0,0,0,0,0,12.5,0,12.5,0,5,0,32.5,62.5,0,0,0,0,0,0,100,0,0,0,0,0,0,0,0,0};
        double[]c8= {75,0,125,125,0,75,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        double[]c9= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,5,0,5,0,0,20,20,0,0,0,7.5,12.5,20,45,45};
        double[]c10= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,45,25,70,70,70,0,5,0,0,0,0,0,0,0,0,0};
        double[]c11= {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,25,0,25,25};

        ArrayList<double[ ] > dosage = new ArrayList<double[ ] >();
        ArrayList<double[ ] > dosage_change = new ArrayList<double[ ] >();
        dosage.add(p1);
        dosage.add(p2);
        dosage.add(p3);
        dosage.add(p4);
        dosage.add(p5);
        dosage.add(p6);
        dosage.add(p7);
        dosage.add(p8);
        dosage.add(p9);
        dosage.add(p10);
        dosage.add(p11);

        dosage_change.add(c1);
        dosage_change.add(c2);
        dosage_change.add(c3);
        dosage_change.add(c4);
        dosage_change.add(c5);
        dosage_change.add(c6);
        dosage_change.add(c7);
        dosage_change.add(c8);
        dosage_change.add(c9);
        dosage_change.add(c10);
        dosage_change.add(c11);

        for(int i=0;i<dosage.size();i++){
            for(int j=i;j<dosage.size();j++){
                if(i==j){
                    System.out.println("");
                }else {
                    double[] p_1 = dosage.get(i);
                    double[] p_2 = dosage.get(j);
                    System.out.println("JS Divergence between " + (i + 1) + " and " + (j + 1) + " is ");
                    System.out.println(CalculateJSDivergence(p_1, p_2));
                }
            }
        }

        for(int i=0;i<dosage_change.size();i++){
            for(int j=i;j<dosage_change.size();j++){
                if(i==j){
                    System.out.println("");
                }else {
                    double[] p_1 = dosage_change.get(i);
                    double[] p_2 = dosage_change.get(j);
                    System.out.println("JS change Divergence between " + (i + 1) + " and " + (j + 1) + " is ");
                    System.out.println(CalculateJSDivergence(p_1, p_2));
                }
            }
        }


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
    }


    public static double[] getDosageFromHashMap(HashMap<String,HashMap<String,Double>> dosage_collection){
        double[] temp = new double[31];
        for (double i:temp){
            i = 0;
        }
        for(Map.Entry<String,HashMap<String,Double>> entry:dosage_collection.entrySet()){
            if(!entry.getKey().isEmpty()){
                HashMap<String,Double> temp_map = entry.getValue();
                for(Map.Entry<String,Double> entry1:temp_map.entrySet()){
                    int date=Integer.parseInt(entry1.getKey().substring(8,9));
                    double value = entry1.getValue();
                    System.out.println("date is"+date);

                }
            }
        }
        return temp;
    }

    public static double CalculateJSDivergence(double[]p1, double[] p2){
        return Util.jsDivergence(p1,p2);
    }



}
