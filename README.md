# netty-mqtt-client

## 关于

基于netty实现的mqtt客户端，可用于Java、Android环境。持续开发中，现已完成基本框架及功能，目前仅支持qos1级别通讯，后期根据需要开发qos2级别。

## 如何使用

#### Gradle：

    repositories {
        mavenCentral()
    }
    
    dependencies {
        implementation 'io.github.x2ge:netty-mqtt-client:2.0.4'
    }

#### 连接

    MqttClient mqttClient = new MqttClient();
    MqttConnectOptions options = new MqttConnectOptions();
    options.setHost("localhost");
    options.setPort(1883);
    options.setUserName("testuser");
    options.setPassword("123456".getBytes(StandardCharsets.UTF_8));
    options.setClientIdentifier("netty_mqtt_c1");
    options.setKeepAliveTime(10);
    options.setCleanSession(true);
    // 配置动作超时时间
    mqttClient.setActionTimeout(3000);
    // 配置掉线重连
    mqttClient.setReconnectOnLost(5, 10000);
    mqttClient.connect(options);

#### 监听

    mqttClient.setCallback(new MqttClient.Callback() {
        @Override
        public void onConnected() {
            // test
            try {
                // 订阅主题
                mqttClient.subscribe("topic");
                // 订阅主题 topic 中可使用 /# ，表示模糊匹配该主题
                // 示例：订阅主题 topic1/# ，可接收 topic1、
                // topic1/aaa、topic1/bbb等主题下消息
                mqttClient.subscribe("topic1/#");
                // 发布一个消息到主题topic1/aaa
                mqttClient.publish("topic1/aaa", "hello, netty mqtt!");
                // 取消订阅
                mqttClient.unsubscribe("topic");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConnectFailed(Throwable e) {

        }

        @Override
        public void onConnectLost(Throwable e) {

        }

        @Override
        public void onReconnectStart(int cur) {

        }

        @Override
        public void onMessageArrived(String topic, String s) {

        }
    });

#### 订阅

    // 订阅主题
    mqttClient.subscribe("testtopic");
    // 订阅主题 topic 中可使用 /# ，表示模糊匹配该主题
    // 示例：订阅主题 parenttopic/# ，可接收 parenttopic、
    // parenttopic/c1、parenttopic/c2等主题下消息
    mqttClient.subscribe("parenttopic/#");

#### 取消订阅

    mqttClient.unsubscribe("testtopic");

#### 发布消息

    // 发布一个消息到主题parenttopic/c2
    mqttClient.publish("parenttopic/c2", "hello, netty mqtt!");

#### 关闭连接

    mqttClient.close();	