package io.lumify.twitter;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import io.lumify.core.exception.LumifyException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class TweetFileSpout extends BaseRichSpout {
    public static final String JSON_OUTPUT_FIELD = "json";
    private final String fileName;
    private TweetStreamReader tweetStreamReader;
    private SpoutOutputCollector collector;

    public TweetFileSpout(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(JSON_OUTPUT_FIELD));
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector collector) {
        this.collector = collector;
        try {
            tweetStreamReader = new TweetStreamReader(new FileInputStream(fileName));
        } catch (IOException e) {
            throw new LumifyException("Could not read file: " + fileName);
        }
    }

    @Override
    public void nextTuple() {
        try {
            JSONObject tweetJson = tweetStreamReader.read();
            if (tweetJson == null) {
                Utils.sleep(10000);
                return;
            }
            String messageId = tweetJson.optString("id");
            if (messageId == null) {
                return;
            }
            this.collector.emit(new Values(tweetJson.toString()), messageId);
        } catch (IOException e) {
            throw new LumifyException("Could not read file: " + fileName);
        }
    }
}
