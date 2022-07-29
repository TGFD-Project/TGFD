package Violations;

import Infra.Match;
import ICs.TGFD;
import Util.Config;

import java.time.LocalDate;
import java.util.*;

public class ViolationCollection {

    private HashMap<Match, List<Violation>> collection;
    private HashMap<String, HashMap<Match, List<Violation>>> collectionByTGFDs;

    public ViolationCollection()
    {
        collection = new HashMap<>();
        collectionByTGFDs=new HashMap<>();
    }


    private Set<String> key_set = new HashSet<>();
    private ArrayList<Match> sort_list = new ArrayList<>();

    public Set<Match> getKeySet(){
        return collection.keySet();
    }


    private void addViolation(HashMap<Match, List<Violation>> collection,Violation violation)
    {
        if(!collection.containsKey(violation.getMatch1()))
            collection.put(violation.getMatch1(),new ArrayList<>());
        collection.get(violation.getMatch1()).add(violation);

        if(!collection.containsKey(violation.getMatch2()))
            collection.put(violation.getMatch2(),new ArrayList<>());
        collection.get(violation.getMatch2()).add(violation);
    }

    public void addViolations(Set<Violation> violations)
    {
        for (Violation violation:violations) {
            addViolation(collection, violation);
        }
    }

    public void addViolations(TGFD tgfd, Set<Violation> violations)
    {
        for (Violation violation:violations) {
            addViolation(collection, violation);
        }
        if(!collectionByTGFDs.containsKey(tgfd.getName()))
            collectionByTGFDs.put(tgfd.getName(),new HashMap<>());
        for (Violation violation:violations)
        {
            addViolation(collectionByTGFDs.get(tgfd.getName()), violation);
        }
    }

    public HashMap<Match, List<Violation>> getCollection() {
        return collection;
    }



    public Match topViolation()
    {
        Match match=null;
        int max=-1;

        for (String key:key_set) {
            for (Match m:collection.keySet()) {
                if(m.getSignatureX().equals(key))
                {
                    if(collection.get(m).size()>max) {
                        match = m;
//                        System.out.println("m is"+m);
                        max=collection.get(m).size();
//                        System.out.println("size is"+max);
                    }
                }
            }

        }




        return match;
    }

    public List<Violation> getViolation(Match match)
    {
        List<Violation> vio = collection.get(match);
//        System.out.println("Vio is"+vio.toString());
        return vio;
    }


//    public ArrayList<Match> sortViolationList(){
//        for (Match match:collection.keySet()) {
//            System.out.println("m1 is"+match);
//            key_set.add(match.getSignatureX());
//            System.out.println("signature x is"+match.getSignatureX());
//            System.out.println("signature with interval is"+match.getSignatureYWithInterval());
//            System.out.println("interval is"+match.getIntervals());
//            List<Interval> vio_intervals = match.getIntervals();
//            System.out.println("1st interval"+ vio_intervals.get(0));
//
//            for(int i=0;i<vio_intervals.size();i++){
//                System.out.println("signatureY "+match.getSignatureY(vio_intervals.get(i).getStart()));
//            }
//
////            System.out.println("signature y is"+match.getSignatureY());
//        }
//        if(!key_set.isEmpty()){
//            Match max_match=topViolation();
//            sort_list.add(max_match);
////            System.out.println("Max add is"+max_match);
//            key_set.remove(max_match.getSignatureX());
//        }
//        return sort_list;
//    }

    public ArrayList<Match> sortMatchList(){
        ArrayList<String> sort_match = new ArrayList<>();
        for (Match match:collection.keySet()) {

            System.out.println("m1 is"+match);
            List<Violation> s_vio = new ArrayList<>();
            s_vio=collection.get(match);

//            for(Violation vio:s_vio){
//                LocalDate date=vio.getInterval().getStart();
//                String signatureX = match.getSignatureX();
//                String signatureY=match.getSignatureY(date);
//                String s_match=signatureX+signatureY+vio.getInterval().getStart1();
//                System.out.println("viovio is"+" "+s_match);
//                sort_match.add(s_match);
//            }

            for(int i=0;i<s_vio.size();i+=2){
                Violation vio = s_vio.get(i);
                LocalDate date1=vio.getInterval().getStart();
                LocalDate date2=vio.getInterval().getEnd();

                String signatureX = match.getSignatureX();
                //TODO: signatureY_1 and signatureY_2 would be the same
                String signatureY_1=match.getSignatureY();
                String signatureY_2=match.getSignatureY();

                String s_match_1="X: "+signatureX+"->"+ signatureY_1+ "t_"+ Config.timestampsReverseMap.get(vio.getInterval().getStart()) ;
                String s_match_2="X: "+signatureX+"->"+ signatureY_2+ "t_"+Config.timestampsReverseMap.get(vio.getInterval().getEnd());


                sort_match.add(s_match_1);
                sort_match.add(s_match_2);
            }
            System.out.println("Done.");
            key_set.add(match.getSignatureX());

        }





        Map<String, Integer> mapL = new HashMap<>();
        for (String current : sort_match) {
            int count = mapL.getOrDefault(current, 0);
            mapL.put(current, count + 1);
        }

        SortComparator1 compL = new SortComparator1(mapL);
        Collections.sort(sort_match, compL);
//        for(String i: sort_match){
//            System.out.println("\n---------------------------------------------------\n");
//            System.out.println(i+" "+"frequency"+mapL.get(i));
//        }

        Set<String> distinct = new HashSet<>(sort_match);
        for(String s:distinct){
            System.out.println("\n---------------------------------------------------\n");
            System.out.println(s+" frequency:" +Collections.frequency(sort_match,s));

        }


        if(!key_set.isEmpty()){
            Match max_match=topViolation();
            sort_list.add(max_match);
//            System.out.println("Max add is"+max_match);
            key_set.remove(max_match.getSignatureX());
        }
        return sort_list;
    }

    // Implement Comparator Interface to sort the values
    class SortComparator1 implements Comparator<String> {
        private final Map<String, Integer> freqMap;

        // Assign the specified map
        SortComparator1(Map<String, Integer> tFreqMap) {
            this.freqMap = tFreqMap;
        }

        // Compare the values
        @Override
        public int compare(String s1, String s2) {

            // Compare value by frequency
            int freqCompare = freqMap.get(s2).compareTo(freqMap.get(s1));

            // Compare value if frequency is equal
            int valueCompare = s1.compareTo(s2);

            // If frequency is equal, then just compare by value, otherwise -
            // compare by the frequency.
            if (freqCompare == 0)
                return valueCompare;
            else
                return freqCompare;
        }
    }


}

