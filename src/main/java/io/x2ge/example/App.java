package io.x2ge.example;

import io.x2ge.mqtt.MqttClient;
import io.x2ge.mqtt.MqttConnectOptions;
import io.x2ge.mqtt.utils.Log;

import java.nio.charset.StandardCharsets;

public class App {

    public static void main(String[] args) {
        MqttClient mqttClient = new MqttClient();
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

        mqttClient.setCallback(new MqttClient.Callback() {
            @Override
            public void onConnected() {
                // test
                try {
                    // 订阅主题
                    mqttClient.subscribe("testtopic");
                    // 订阅主题 topic 中可使用 /# ，表示模糊匹配该主题
                    // 示例：订阅主题 parenttopic/# ，可接收 parenttopic、
                    // parenttopic/c1、parenttopic/c2等主题下消息
                    mqttClient.subscribe("parenttopic/#");
                    // 发布一个消息到主题parenttopic/c2
                    mqttClient.publish("parenttopic/c2", "hello, netty mqtt!");
                    // 取消订阅
                    mqttClient.unsubscribe("testtopic");
//                    mqttClient.close();
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

        try {
            mqttClient.connect(options);
        } catch (Exception e) {
//            e.printStackTrace();
//            Log.i("--->连接失败了：" + e);
        }

        for (; ; ) ;

    }
}
