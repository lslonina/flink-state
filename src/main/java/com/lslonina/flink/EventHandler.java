package com.lslonina.flink;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import com.lslonina.model.WordData;

public class EventHandler extends KeyedProcessFunction<String, String, String> {

    private transient ValueState<WordData> count;

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
        super.open(parameters);
        count = getRuntimeContext().getState(new ValueStateDescriptor<>("count", WordData.class));
    }

    @Override
    public void processElement(String value, KeyedProcessFunction<String, String, String>.Context context,
            Collector<String> out) throws Exception {
        WordData wd = count.value();
        if (wd == null) {
            wd = new WordData();
            wd.setWord(context.getCurrentKey());
            wd.setCount(0);
        }
        wd.setCount(wd.getCount() + 1);
        count.update(wd);

        out.collect("(" + value + ", " + wd.getCount() + ")");
    }

}
