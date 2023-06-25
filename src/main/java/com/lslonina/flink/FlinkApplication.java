package com.lslonina.flink;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.TaskManagerOptions;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.SavepointRestoreSettings;
import org.apache.flink.runtime.minicluster.MiniCluster;
import org.apache.flink.runtime.minicluster.MiniClusterConfiguration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FlinkApplication {
    private static final String CHECKPOINTS = "file:///home/lslonina/flink-checkpoints";
    private static final String SAVEPOINTS = "file:///home/lslonina/flink-savepoints";

    public static void main(String[] args) throws Exception {
        int jobManagerPort = 6123;
        int numTaskSlots = 8;
        Configuration conf = new Configuration();
        conf.setInteger(JobManagerOptions.PORT, jobManagerPort);
        conf.setInteger(TaskManagerOptions.NUM_TASK_SLOTS, numTaskSlots);
        MiniClusterConfiguration cfg = new MiniClusterConfiguration.Builder()
                .setConfiguration(conf)
                .setNumSlotsPerTaskManager(numTaskSlots)
                .build();
        MiniCluster cluster = new MiniCluster(cfg);
        cluster.start();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.createRemoteEnvironment("localhost",
                jobManagerPort);
        env.setStateBackend(new EmbeddedRocksDBStateBackend(true));
        var checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.setCheckpointInterval(5000);
        checkpointConfig.setCheckpointStorage(CHECKPOINTS);
        checkpointConfig.setExternalizedCheckpointCleanup(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        checkpointConfig.setMinPauseBetweenCheckpoints(1000);
        checkpointConfig.setTolerableCheckpointFailureNumber(0);
        checkpointConfig.setMaxConcurrentCheckpoints(1);

        DataStream<String> dataStream = env.socketTextStream("localhost", 9999);
        dataStream = dataStream
                .keyBy(value -> value)
                .process(new EventHandler()).uid("event-handler");
        dataStream.print();

        JobGraph jobGraph = env.getStreamGraph().getJobGraph();
        // env.getStreamGraph().setJobName("FlinkApplication-StateBackend");
        // var savepointPath = CHECKPOINTS + "/e9d5754f483f4afc2d6e181012533865/chk-3/_metadata";
        var savepointPath = SAVEPOINTS + "/1/savepoint-4c4943-0b0e155123bb/_metadata";
        jobGraph.setSavepointRestoreSettings(SavepointRestoreSettings.forPath(savepointPath));

        cluster.submitJob(jobGraph);
    }
}
