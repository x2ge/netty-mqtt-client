package io.x2ge.mqtt;

import io.netty.handler.codec.mqtt.MqttVersion;
import io.x2ge.mqtt.utils.StringUtils;

public class MqttConnectOptions {
    private String host;
    private int port;

    // 可变报头部分
    private MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;
    private boolean isWillRetain = false;
    private int willQos = 0;
    private boolean isWillFlag = false;
    private boolean isCleanSession = false;
    private int keepAliveTime = 60;

    // 有效载荷 ：客户端标识符，遗嘱主题，遗嘱消息，用户名，密码
    private String clientIdentifier = "";
    private String willTopic = "";
    private byte[] willMessage;
    private String userName = "";
    private byte[] password;


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MqttVersion getMqttVersion() {
        return mqttVersion;
    }

    public boolean isHasUserName() {
        return !StringUtils.isEmpty(userName);
    }

    public boolean isHasPassword() {
        return password != null && password.length > 0;
    }

    public boolean isWillRetain() {
        return isWillRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.isWillRetain = willRetain;
    }

    public int getWillQos() {
        return willQos;
    }

    public void setWillQos(int willQos) {
        this.willQos = willQos;
    }

    public boolean isWillFlag() {
        return isWillFlag;
    }

    public void setWillFlag(boolean willFlag) {
        this.isWillFlag = willFlag;
    }

    public boolean isCleanSession() {
        return isCleanSession;
    }

    /**
     * 如果清理会话（CleanSession）标志被设置为true，
     * 客户端和服务端在重连后，会丢弃之前的任何会话相关内容及配置
     *
     * @param cleanSession true 重连后丢弃相关数据
     */
    public void setCleanSession(boolean cleanSession) {
        this.isCleanSession = cleanSession;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * @param keepAliveTime 维持连接时间，秒
     */
    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public byte[] getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(byte[] willMessage) {
        this.willMessage = willMessage;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }
}
