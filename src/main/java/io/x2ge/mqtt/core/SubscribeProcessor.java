package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.x2ge.mqtt.utils.AsyncTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SubscribeProcessor extends AsyncTask<String> {

    public int msgId;
    private final AtomicBoolean accepted = new AtomicBoolean(false);
    private Exception e;

    @Override
    public String call() throws Exception {
        while (!isCancelled() && !accepted.get()) {

            if (e != null) {
                throw e;
            }

            synchronized (accepted) {
                try {
                    accepted.wait(100L);
                } catch (Exception ex) {
//                    ex.printStackTrace();
                }
            }
        }
        return accepted.get() ? ProcessorResult.RESULT_SUCCESS : ProcessorResult.RESULT_FAIL;
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
            synchronized (accepted) {
                accepted.set(true);
                accepted.notify();
            }
        }
    }
}
