package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.x2ge.mqtt.MqttConnectOptions;
import io.x2ge.mqtt.utils.AsyncTask;
import io.x2ge.mqtt.utils.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectProcessor extends AsyncTask<String> {

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

    public String connect(Channel channel, MqttConnectOptions options, long timeout) throws Exception {
        MqttConnectMessage msg = ProtocolUtils.connectMessage(options);
        Log.i("-->发起连接：" + msg);
        channel.writeAndFlush(msg);
        return execute().get(timeout, TimeUnit.MILLISECONDS);
    }

    public void processAck(Channel channel, MqttConnAckMessage msg) {
        MqttConnAckVariableHeader mqttConnAckVariableHeader = msg.variableHeader();
        String errormsg = "";
        switch (mqttConnAckVariableHeader.connectReturnCode()) {
            case CONNECTION_ACCEPTED:
                synchronized (receivedAck) {
                    receivedAck.set(true);
                    receivedAck.notify();
                }
                return;
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
                errormsg = "用户名密码错误";
                break;
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
                errormsg = "clientId不允许链接";
                break;
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
                errormsg = "服务不可用";
                break;
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                errormsg = "mqtt 版本不可用";
                break;
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
                errormsg = "未授权登录";
                break;
            default:
                errormsg = "未知问题";
                break;
        }

        synchronized (receivedAck) {
            e = new IOException(errormsg);
            receivedAck.notify();
        }
    }
}
