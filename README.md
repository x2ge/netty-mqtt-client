# netty-mqtt-client

## 关于

基于netty实现的mqtt客户端，可用于Java、Android环境

## 如何使用

#### Gradle：

    repositories {
        mavenCentral()
    }
    
    dependencies {
        implementation 'io.github.x2ge:netty-mqtt-client:2.0.0'
    }

#### 连接

    MqttClient mqttClient = new MqttClient();
    MqttConnectOptions options = new MqttConnectOptions();
    options.setHost("localhost");
    options.setPort(1883);
    options.setClientIdentifier("netty_mqtt_c1");
    options.setUserName("testuser");
    options.setPassword("123456".getBytes(StandardCharsets.UTF_8));
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

        }

        @Override
        public void onReconnectStart(int cur) {

        }

        @Override
        public void onMessageArrived(String topic, String s) {

        }
    });

#### 订阅

    mqttClient.subscribe("netty_mqtt_c1");

#### 取消订阅

    mqttClient.unsubscribe("netty_mqtt_c1");

#### 发布消息

    mqttClient.publish("netty_mqtt_c1", "hello, netty mqtt!");

#### 关闭连接

    mqttClient.close();	