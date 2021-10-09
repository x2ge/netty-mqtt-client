package io.x2ge.example;

import io.x2ge.mqtt.MqttClient;
import io.x2ge.mqtt.MqttConnectOptions;
import io.x2ge.mqtt.utils.Log;

public class App {

    public static void main(String[] args) {
        MqttClient mqttClient = new MqttClient();
        MqttConnectOptions options = new MqttConnectOptions();

        // emqx
        options.setHost("broker-cn.emqx.io");
        options.setPort(1883);

        // apollo
//        options.setHost("localhost");
//        options.setPort(61613);
//        options.setUserName("admin");
//        options.setPassword("password".getBytes(StandardCharsets.UTF_8));

        // anoah
//        options.setHost("localhost");
//        options.setPort(30380);
//        options.setUserName("anoah");
//        options.setPassword("uclass2019".getBytes(StandardCharsets.UTF_8));
        options.setClientIdentifier("netty_mqtt_c1");
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
                    mqttClient.subscribe(1, "topic123");
                    mqttClient.publish("topic123", "hello, netty mqtt!");
                    // 订阅主题 topic 中可使用 /# ，表示模糊匹配该主题
                    // 示例：订阅主题 topic1/# ，可接收 topic1、
                    // topic1/aaa、topic1/bbb等主题下消息
//                    mqttClient.subscribe("topic1/#");
                    // 发布一个消息到主题topic1/aaa
//                    mqttClient.publish("topic1/aaa", "hello, netty mqtt!-2-");
                    // 取消订阅
                    mqttClient.unsubscribe("topic");
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
