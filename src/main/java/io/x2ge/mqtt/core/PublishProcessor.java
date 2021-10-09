package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.x2ge.mqtt.utils.AsyncTask;
import io.x2ge.mqtt.utils.Log;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PublishProcessor extends AsyncTask<String> {

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

    public String publish(Channel channel, String topic, String content, long timeout) throws Exception {
        int id = 0;
        String s;
        try {
            id = MessageIdFactory.get();

            msgId = id;

            MqttPublishMessage msg = ProtocolUtils.publishMessage(topic,
                    content.getBytes(StandardCharsets.UTF_8),
                    1,
                    id,
                    false
            );
            Log.i("-->发送消息：" + msg);
            channel.writeAndFlush(msg);
            s = execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MessageIdFactory.release(id);
        }
        return s;
    }

    public void processAck(Channel channel, MqttPubAckMessage msg) {
        MqttMessageIdVariableHeader variableHeader = msg.variableHeader();
        if (variableHeader.messageId() == msgId) {
            synchronized (receivedAck) {
                receivedAck.set(true);
                receivedAck.notify();
            }
        }
    }
}
