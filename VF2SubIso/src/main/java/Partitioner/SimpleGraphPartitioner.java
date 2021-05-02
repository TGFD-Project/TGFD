package Partitioner;

import graphLoader.GraphLoader;

public class SimpleGraphPartitioner{

    private GraphLoader loader;

    public SimpleGraphPartitioner(GraphLoader loader, int numberOfPartitions)
    {
        this.loader=loader;
    }

}