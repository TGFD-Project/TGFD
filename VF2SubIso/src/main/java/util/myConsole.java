package util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class myConsole {


    private static StringBuilder builder=new StringBuilder();

    public static void print(String message)
    {
        builder.append(message + "\n");
        System.out.println(message);
    }

    public static void print(String message, long runTimeInMS)
    {
        String msg=message + " time: " + runTimeInMS + "(ms) ** " +
                TimeUnit.MILLISECONDS.toSeconds(runTimeInMS) + "(sec) ** " +
                TimeUnit.MILLISECONDS.toMinutes(runTimeInMS) +  "(min)";
        builder.append(msg + "\n");

        System.out.println(msg);
    }

    public static void saveLogs(String path)
    {
        try {
            FileWriter file = new FileWriter(path);
            file.write(builder.toString());
            file.close();
            System.out.println("Successfully wrote the log file: " + path);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

}
