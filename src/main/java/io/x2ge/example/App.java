package io.x2ge.example;

import io.x2ge.mqtt.MqttClient;
import io.x2ge.mqtt.MqttConnectOptions;
import io.x2ge.mqtt.utils.Log;

import java.nio.charset.StandardCharsets;

public class App {

    public static void main(String[] args) {
        MqttClient mqttClient = new MqttClient();
        mqttClient.setCallback(new MqttClient.Callback() {
            @Override
            public void onConnected() {
                // test
                try {
                    mqttClient.subscribe("netty_mqtt_c1");
                    mqttClient.subscribe("testtopic/#");
                    mqttClient.publish("netty_mqtt_c1", "hello, netty mqtt!");
                    mqttClient.unsubscribe("netty_mqtt_c1");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConnectFailed(Throwable e) {

            }

            @Override
            public void onConnectLost(Throwable e) {
                Log.i("-->onConnectLost : " + e);
            }

            @Override
            public void onReconnectStart(int cur) {

            }

            @Override
            public void onMessageArrived(String topic, String s) {

            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setHost("localhost");
        options.setPort(1883);

        options.setClientIdentifier("netty_mqtt_c1");
        options.setUserName("testuser");
        options.setPassword("123456".getBytes(StandardCharsets.UTF_8));
        options.setKeepAliveTime(5);
        options.setCleanSession(true);
        // 配置动作超时时间
        mqttClient.setActionTimeout(3000);
        // 配置掉线重连
        mqttClient.setReconnectOnLost(5, 10000);
        try {
            mqttClient.connect(options);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        mqttClient.close();
        for (; ; ) ;

    }
}
