package Violations;

import Infra.Match;
import ICs.TGFD;

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


//    public Match topViolation()
//    {
//        Match match=null;
//        int max=-1;
//
//        for (Match key:key_set) {
//            if(collection.get(key).size()>max) {
//                match = key;
//                max=collection.get(key).size();
//
//            }
//        }
//
//
//
//
//        return match;
//    }

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
                        System.out.println("m is"+m);
                        max=collection.get(m).size();
                        System.out.println("size is"+max);
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

/// Lejia Edit

//    public HashMap<Match, List<Violation>> sortViolationCollection()
//    {
//        HashMap<Match, List<Violation>> collection_sort = new HashMap<>();
//        while(!collection.isEmpty()){
//            Match max = topViolation();
//            collection_sort.put(max,collection.get(max));
//            collection.remove(max);
//        }
//
//        collection = collection_sort;
//        return collection_sort;
//    }

    public ArrayList<Match> sortViolationList(){
        for (Match match:collection.keySet()) {
            System.out.println("m1 is"+match);
            key_set.add(match.getSignatureX());
        }
        if(!key_set.isEmpty()){
            Match max_match=topViolation();
            sort_list.add(max_match);
            System.out.println("Max add is"+max_match);
            key_set.remove(max_match.getSignatureX());
        }
        return sort_list;
    }

//    public HashMap<Interval,List<Double>> calculate_ErrorMatchesRatio(){
//        HashMap<Interval,List<Double>> ErrorMatchesRatio = new HashMap<>();
//
//
//
//        return ErrorMatchesRatio;
//    }




}

