package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.x2ge.mqtt.utils.AsyncTask;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnsubscribeProcessor extends AsyncTask<String> {

    public int msgId;
    private final AtomicBoolean receivedAck = new AtomicBoolean(false);
    private Exception e;

    @Override
    public String call() throws Exception {
        while (!isCancelled() && !receivedAck.get()) {

            if (e != null) {
                throw e;
            }

            synchronized (receivedAck) {
                try {
                    receivedAck.wait(300L);
                } catch (Exception ex) {
//                    ex.printStackTrace();
                }
            }
        }
        return receivedAck.get() ? ProcessorResult.RESULT_SUCCESS : ProcessorResult.RESULT_FAIL;
    }

    public String unsubscribe(Channel channel, String[] topics, long timeout) throws Exception {
        int id = 0;
        String s;
        try {
            id = MessageIdFactory.get();

            msgId = id;

            MqttUnsubscribeMessage msg = ProtocolUtils.unsubscribeMessage(id, Arrays.asList(topics));
            channel.writeAndFlush(msg);
            s = execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MessageIdFactory.release(id);
        }
        return s;
    }

    public void processAck(Channel channel, MqttUnsubAckMessage msg) {
        MqttMessageIdAndPropertiesVariableHeader variableHeader = msg.idAndPropertiesVariableHeader();
        if (variableHeader.messageId() == msgId) {
            synchronized (receivedAck) {
                receivedAck.set(true);
                receivedAck.notify();
            }
        }
    }
}
