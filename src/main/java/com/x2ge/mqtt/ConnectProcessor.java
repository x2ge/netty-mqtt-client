package com.x2ge.mqtt;

import com.x2ge.mqtt.utils.AsyncTask;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ConnectProcessor extends AsyncTask<String> {

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

    public String connect(Channel channel, MqttConnectOptions options, long timeout) throws Exception {
        channel.writeAndFlush(MqttProtocolUtil.connectMessage(options));
        return execute().get(timeout, TimeUnit.MILLISECONDS);
    }

    public void processAck(Channel channel, MqttConnAckMessage msg) {
        MqttConnAckVariableHeader mqttConnAckVariableHeader = msg.variableHeader();
        String errormsg = "";
        switch (mqttConnAckVariableHeader.connectReturnCode()) {
            case CONNECTION_ACCEPTED:
                accepted = true;
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

        e = new IOException(errormsg);
    }
}
