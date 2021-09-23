package com.x2ge.mqtt;

import com.x2ge.mqtt.utils.AsyncTask;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class PublishProcessor extends AsyncTask<String> {

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

    public String publish(Channel channel, String topic, String content, long timeout) throws Exception {
        int id = 0;
        String s;
        try {
            id = MqttMessageId.get();

            msgId = id;

            MqttPublishMessage msg = MqttProtocolUtil.publishMessage(topic,
                    content.getBytes(StandardCharsets.UTF_8),
                    1,
                    id,
                    false
            );
            channel.writeAndFlush(msg);
            s = execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MqttMessageId.release(id);
        }
        return s;
    }

    public void processAck(Channel channel, MqttPubAckMessage msg) {
        MqttMessageIdVariableHeader variableHeader = msg.variableHeader();
        if (variableHeader.messageId() == msgId) {
            accepted = true;
        }
    }
}
