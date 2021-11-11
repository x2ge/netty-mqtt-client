package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.x2ge.mqtt.utils.AsyncTask;
import io.x2ge.mqtt.utils.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SubscribeProcessor extends AsyncTask<String> {

    private long timeout;
    private int msgId;
    private final AtomicBoolean receivedAck = new AtomicBoolean(false);

    @Override
    public String call() throws Exception {
        if (!isCancelled() && !receivedAck.get()) {
            synchronized (receivedAck) {
                receivedAck.wait(timeout);
            }
        }
        return receivedAck.get() ? ProcessorResult.RESULT_SUCCESS : ProcessorResult.RESULT_FAIL;
    }

    public String subscribe(Channel channel, String[] topics, long timeout) throws Exception {
        return subscribe(channel, 0, topics, timeout);
    }

    public String subscribe(Channel channel, int qos, String[] topics, long timeout) throws Exception {
        this.timeout = timeout;
        int id = 0;
        String s;
        try {
            id = MessageIdFactory.get();

            this.msgId = id;

            MqttSubscribeMessage msg = ProtocolUtils.subscribeMessage(id, qos, topics);
            Log.i("-->发起订阅：" + msg);
            channel.writeAndFlush(msg);
            s = execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MessageIdFactory.release(id);
        }
        return s;
    }

    public void processAck(Channel channel, MqttSubAckMessage msg) {
        MqttMessageIdAndPropertiesVariableHeader variableHeader = msg.idAndPropertiesVariableHeader();
        if (variableHeader.messageId() == msgId) {
            synchronized (receivedAck) {
                receivedAck.set(true);
                receivedAck.notify();
            }
        }
    }
}
