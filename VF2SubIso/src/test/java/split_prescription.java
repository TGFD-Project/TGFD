import ICs.TGFD;
import Infra.Interval;
import Violations.Violation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.ScatteringByteChannel;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public class split_prescription {

    public static void main(String []args)
    {

        LocalDate start = LocalDate.parse("2191-01-01");
        LocalDate end = start.plusDays(1);
        for (int i=0;i<31;i++)
        {
            Interval intv=new Interval(start, end);
            StringBuilder sb  = readFromFile("E:\\MorteZa\\Datasets\\PDD\\Dataset\\pdd_nt\\prescriptions.nt",intv);
            writeToTheFile("E:\\MorteZa\\Datasets\\PDD\\Dataset\\pdd_nt\\"+ start +".nt",sb);
            start = start.plusDays(1);
            end = end.plusDays(1);
        }
    }

    public static StringBuilder readFromFile(String path, Interval interval)
    {
        StringBuilder ret = new StringBuilder();
        BufferedReader reader;
        String line = "";
        try {
            reader = new BufferedReader(new FileReader(path));
            line = reader.readLine().toLowerCase(Locale.ROOT);
            LocalDate startDate=null, endDate = null;
            StringBuilder sb=new StringBuilder();
            boolean hasToBeAdded = false, tobeIgnored = false;
            while (line != null) {
                if(line.contains("<http://pdd.wangmengsd.com/property/prescription_id>"))
                {
                    // new prescription
                    if(hasToBeAdded && !tobeIgnored)
                        ret.append(sb);
                    sb = new StringBuilder();
                    hasToBeAdded = false;
                    tobeIgnored = false;
                    startDate = null;
                    endDate = null;
                    sb.append(line).append("\n");
                }
                else
                {
                    sb.append(line).append("\n");
                    if(line.contains("<http://pdd.wangmengsd.com/property/start_date>"))
                    {
                        // start date
                        try {
                            String[] temp = line.split(" ");
                            if (temp.length > 2) {
                                startDate = LocalDate.parse(temp[2].substring(1, 11));
                            }
                        }
                        catch (Exception e)
                        {
                            tobeIgnored = true;
                        }
                    }
                    else if(line.contains("<http://pdd.wangmengsd.com/property/end_date>"))
                    {
                        // end date
                        try {
                            String[] temp = line.split(" ");
                            if (temp.length > 2) {
                                endDate = LocalDate.parse(temp[2].substring(1, 11));
                            }
                        }
                        catch (Exception e)
                        {
                            tobeIgnored = true;
                        }
                        if(!tobeIgnored)
                            hasToBeAdded = contains(interval, startDate, endDate);
                    }
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            System.out.println(line);
            e.printStackTrace();
        }
        return ret;
    }

    public static boolean contains(Interval interval, LocalDate startDate, LocalDate endDate)
    {
        boolean ret = false;
        while (startDate.isEqual(endDate) || startDate.isBefore(endDate))
        {
            ret = ret || interval.containsExcludeEnd(startDate);
            startDate = startDate.plusDays(1);
        }
        return ret;
    }

    private static void writeToTheFile(String path, StringBuilder sb)
    {
        try {
            FileWriter file = new FileWriter(path);
            file.write(sb.toString());
            file.close();
            System.out.println("Successfully wrote to the file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

}
