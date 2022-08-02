package PDD;

import java.util.HashMap;

public class Record {
    private final String uri;
    private final HashMap<String, Double> features;

    public Record(String uri, HashMap<String, Double> features) {
        this.uri = uri;
        this.features = features;
    }

    @Override
    public String toString() {
        return "Record{" +
                "uri='" + uri + '\'' +
                ", features=" + features +
                '}';
    }

    public HashMap<String, Double> getFeatures() {
        return features;
    }

    public String getUri() {
        return uri;
    }
}
