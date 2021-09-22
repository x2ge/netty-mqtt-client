package com.x2ge.mqtt;

import com.x2ge.mqtt.utils.AsyncTask;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;

import java.util.concurrent.TimeUnit;

public class SubscribeProcessor extends AsyncTask<String> {

    public int msgId;
    private boolean accepted = false;
    private Exception e;

    @Override
    public String call() throws Exception {
        while (!isCancelled() && !accepted) {

            if (e != null) {
                throw e;
            }

            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
//                e.printStackTrace();
                break;
            }
        }
        return accepted ? ProcessorResult.RESULT_SUCCESS : ProcessorResult.RESULT_FAIL;
    }

    public String subscribe(Channel channel, String[] topics, long timeout) throws Exception {
        int id = 0;
        String s;
        try {
            id = MqttMessageId.get();

            msgId = id;

            MqttSubscribeMessage msg = MqttProtocolUtil.subscribeMessage(id, topics);
            channel.writeAndFlush(msg);
            s = execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MqttMessageId.release(id);
        }
        return s;
    }

    public void processAck(Channel channel, MqttSubAckMessage msg) {
        MqttMessageIdAndPropertiesVariableHeader variableHeader = msg.idAndPropertiesVariableHeader();
        if (variableHeader.messageId() == msgId) {
            accepted = true;
        }
    }
}
