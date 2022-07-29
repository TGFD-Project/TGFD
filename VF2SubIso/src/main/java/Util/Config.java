package Util;

import com.amazonaws.regions.Regions;
import org.apache.activemq.ActiveMQConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class Config {

    private static HashMap<Integer, ArrayList<String>> typesPaths = new HashMap<>();
    private static HashMap<Integer, ArrayList<String>> dataPaths = new HashMap<>();
    private static HashMap <Integer, String> diffFilesPath=new HashMap<>();
    private static HashMap<Integer,LocalDate> timestamps=new HashMap<>();
    public static HashMap<LocalDate,Integer> timestampsReverseMap=new HashMap<>();
    private static ArrayList<Double> diffCaps=new ArrayList <>();

    public static String patternPath = "";
    public static ArrayList<String> workers=new ArrayList <>();
    public static String ActiveMQBrokerURL= ActiveMQConnection.DEFAULT_BROKER_URL;
    public static String ActiveMQUsername= "";
    public static String ActiveMQPassword= "";
    public static String nodeName="";
    public static boolean Amazon=false;
    public static String S3BucketName="imdb-141031";
    public static Regions region=Regions.US_EAST_2;
    public static String language="N-Triples";
    public static HashMap<String,String> jobs=new HashMap <>();
    public static dataset datasetName;
    public static long threadsIdleTime=3000;// in ms
    public static int supersteps=0;
    public static double zeta=-1;
    public static boolean gfd=false;
    public static boolean filterGraph = false;

    public static boolean optimizedLoadingBasedOnTGFD=false;
    public static boolean saveViolations=false;
    public static boolean debug =false;

    public static void parse(String input) throws FileNotFoundException {
        if(input.equals("--help")) {
            System.out.println("""
                     Expected arguments to parse:
                     -p <path to the patternFile> // in case of Amazon S3, it should be in the form of bucket_name/key
                     [-t<snapshotId> <typeFile>]
                     [-d<snapshotId> <dataFile>]
                     [-c<snapshotId> <diff file>]
                     [-s<snapshotId> <snapshot timestamp>]
                     -logcap List<double> // example: -diffCap 0.02,0.04,0.06,1
                     -optgraphload <true-false> // load parts of data file that are needed based on the TGFDs
                     -debug <true-false> // print details of matching
                     -mqurl <URL> // URL of the ActiveMQ Broker
                     -mqusername <Username> // Username to access ActiveMQ Broker
                     -mqpassword <Password> // Password of ActiveMQ Broker
                     -nodename <node name> // Unique node name for the workers
                     -workers List<names> // List of workers name. example: worker1,worker2,worker3
                     -amazon <true-false> // run on Amazon EC2
                     -region <region name> // Name of the region in Amazon EC2
                     -language <language name> // Names like "N-Triples", "TURTLE", "RDF/XML"
                     -job <worker name>,<job> // For example: -job worker1,pattern1.txt
                     -dataset <dataset name> // Options: imdb (default), dbpedia, synthetic, pdd
                     -idletime <time> // idle time in threads (in ms)
                     -superstep <integer> // number of supersteps
                     -zeta <double> // value of zeta
                     -gfd <true-false> // run GFD error detection
                     -filter <true-false> // Filter the loaded graph based on the constant literals in the TGFD (for debugging purposes)
                    """.indent(5));
        } else
            parseInputParams(input);
    }

    private static void parseInputParams(String pathToConfigFile) {
        Scanner scanner;
        try {
            scanner = new Scanner(new File(pathToConfigFile));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] conf = line.toLowerCase().split(" ");
                if (conf.length != 2)
                    continue;
                if (conf[0].equals("-optgraphload")) {
                    optimizedLoadingBasedOnTGFD = Boolean.parseBoolean(conf[1]);
                } else if (conf[0].equals("-debug")) {
                    debug = Boolean.parseBoolean(conf[1]);
                } else if (conf[0].equals("-logcap")) {
                    String[] temp = conf[1].split(",");
                    for (String diffCap : temp)
                        diffCaps.add(Double.parseDouble(diffCap));
                } else if(conf[0].equals("-mqurl")) {
                    ActiveMQBrokerURL=conf[1];
                }
                else if(conf[0].equals("-saveviolations")) {
                    saveViolations=Boolean.parseBoolean(conf[1]);
                }
                else if(conf[0].equals("-mqusername")) {
                    ActiveMQUsername=conf[1];
                }
                else if(conf[0].equals("-mqpassword")) {
                    ActiveMQPassword=conf[1];
                }else if(conf[0].equals("-nodename")) {
                    nodeName=conf[1];
                }else if(conf[0].equals("-superstep")) {
                    supersteps=Integer.parseInt(conf[1]);
                } else if (conf[0].equals("-workers")) {
                    String[] temp = conf[1].split(",");
                    workers.addAll(Arrays.asList(temp));
                } else if(conf[0].equals("-region")) {
                    region=Regions.fromName(conf[1]);
                } else if(conf[0].equals("-amazon")) {
                    Amazon=Boolean.parseBoolean(conf[1]);
                }else if(conf[0].equals("-language")) {
                    language=conf[1];
                }else if(conf[0].equals("-dataset")) {
                    try {
                        datasetName = dataset.valueOf(conf[1]);
                    }
                    catch (IllegalArgumentException exception)
                    {
                        System.out.println("Error in dataset name: " + exception.getMessage());
                        System.out.println("Dataset name is being set to default name: \"IMDB\"");
                        datasetName=dataset.imdb;
                    }
                }else if(conf[0].equals("-zeta")) {
                    zeta=Double.parseDouble(conf[1]);
                } else if(conf[0].equals("-gfd")) {
                    gfd=Boolean.parseBoolean(conf[1]);
                }
                else if(conf[0].equals("-filter")) {
                    filterGraph=Boolean.parseBoolean(conf[1]);
                }else if(conf[0].equals("-job")) {
                    String[] temp = conf[1].split(",");
                    if(temp.length !=2)
                        continue;
                    jobs.put(temp[0],temp[1]);
                }else if(conf[0].equals("-idletime")) {
                    threadsIdleTime=Long.parseLong(conf[1]);
                }else if (conf[0].startsWith("-t")) {
                    var snapshotId = Integer.parseInt(conf[0].substring(2));
                    if (!typesPaths.containsKey(snapshotId))
                        typesPaths.put(snapshotId, new ArrayList <>());
                    typesPaths.get(snapshotId).add(conf[1]);
                } else if (conf[0].startsWith("-d")) {
                    var snapshotId = Integer.parseInt(conf[0].substring(2));
                    if (!dataPaths.containsKey(snapshotId))
                        dataPaths.put(snapshotId, new ArrayList <>());
                    dataPaths.get(snapshotId).add(conf[1]);
                } else if (conf[0].startsWith("-c")) {
                    var snapshotId = Integer.parseInt(conf[0].substring(2));
                    if (snapshotId != 1)
                        diffFilesPath.put(snapshotId, conf[1]);
                } else if (conf[0].startsWith("-p")) {
                    patternPath = conf[1];
                } else if (conf[0].startsWith("-s")) {
                    var snapshotId = Integer.parseInt(conf[0].substring(2));
                    timestamps.put(snapshotId, LocalDate.parse(conf[1]));
                    timestampsReverseMap.put(LocalDate.parse(conf[1]),snapshotId);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList <String> getFirstDataFilePath() {
        return dataPaths.get(1);
    }

    public static ArrayList <String> getFirstTypesFilePath() {
        return typesPaths.get(1);
    }

    public static HashMap <Integer, ArrayList <String>> getAllDataPaths() {
        return dataPaths;
    }

    public static HashMap <Integer, ArrayList <String>> getAllTypesPaths() {
        return typesPaths;
    }

    public static HashMap <Integer, LocalDate> getTimestamps() {
        return timestamps;
    }

    public static HashMap <Integer, String> getDiffFilesPath() {
        return diffFilesPath;
    }

    public static ArrayList <Double> getDiffCaps() {
        return diffCaps;
    }

    public enum dataset
    {
        pdd, dbpedia, imdb, synthetic
    }
}
