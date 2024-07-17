1. 初始化
 mqtt = new Mqtt.Builder()
            .setUri(url, deviceId)// url：mqtt接口 deviceId 唯一标识
            .setUserInfo(mqtt_uname, mqtt_pwd) // 用户名/密码
            .setUIFactory(new UIFactory())// 主线程回调
            .setLogger(new MqttLogger()) // 日志打印 默认是 System.out
            .setConnectListener(this) // 连接状态见监听
            .build();
2. 一般使用
注解
Topic:
     topics:此类绑定的topic，可以绑定多个
     autoCreate：是否自动实例化，如果是Activity等不能自己create的类，需要创建时调用Mqtt.bind(this)绑定，详细见3
     qos：消息质量
     thread：回调在onMessage的线程，默认io线程
Format:
    value:当Topic:topics 存再变量时，从format对应的函数或成员变量替换(推荐使用函数)

@Topic(topics = "pv/{id}/getMcuLog}", thread = Topic.Local.IO)
public class McuLogMessage implements IMessage {
    private static final String TAG = "McuLogMessage";

    @Format("{id}")
    private String id() {
        return Constants.getDeviceId();
    }
    
    @Override
    public void onMessage(String topic, String message) {
    }
}

当收到pv/{id}/getMcuLog的消息，会自动回调到onMessage函数

3. 在activity或者其他已经创建好的对象中使用
 autoCreate 必须为false，需要实现IMessage(未实现编译会爆错提示)
 @Topic(topics = "pv/id/getMcuLog}", autoCreate = false,thread = Topic.Local.MAIN)
 public class TestActivity extends Activity implements IMessage {
    private static final String TAG = "McuLogMessage";

    public void onCreate(Bundle xxx) {
        // ...
        Mqtt.bind(this)
    }   
    @Override
    public void onMessage(String topic, String message) {
    }
 }
