package com.x2ge.mqtt;

import com.x2ge.mqtt.utils.StringUtils;
import io.netty.handler.codec.mqtt.MqttVersion;

public class MqttConnectOptions {
    private MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;

    private String host;
    private int port;

    private String clientIdentifier = "";
    private String userName = "";
    private byte[] password;
    private int keepAliveTime = 60;

    private boolean isWillFlag = false;
    private boolean isWillRetain = false;
    private int willQos = 0;
    private String willTopic = "";
    private byte[] willMessage;

    private boolean isCleanSession = false;

    public MqttConnectOptions() {

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

    public boolean isWillFlag() {
        return isWillFlag;
    }

    public void setWillFlag(boolean willFlag) {
        this.isWillFlag = willFlag;
    }

    public boolean isCleanSession() {
        return isCleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.isCleanSession = cleanSession;
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getWillQos() {
        return willQos;
    }

    public void setWillQos(int willQos) {
        this.willQos = willQos;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public byte[] getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(byte[] willMessage) {
        this.willMessage = willMessage;
    }

    public byte[] getPassword() {
        return password;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

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
}
