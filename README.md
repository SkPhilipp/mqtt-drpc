# MQTT DRPC

This library lets you make distributed remote procedure calls and get results back, with plain Java.

- Requires Java 8.
- Remote procedure calls go over MQTT, using Eclipse Paho.
- Remote procedure calls' content is streamed in JSON.
- You can publish and call standard Java interfaces.
- Source code builds with maven.

## References

- MQTT client implementation used is [Eclipse Paho](https://eclipse.org/paho/)
- MQTT broker implementation used for testing is [Mosquitto](http://mosquitto.org/)
- Build tool used is [Apache Maven](http://maven.apache.org/)

## Services

Define the interface of your service(s)

```java
public interface CalculatorService {

    public Integer calculate(Integer a, Integer b);

}
```

Instantiate a client, the client will require an MQTT broker URL.

```java
MqttDrpcClient client = new MqttDrpcClientBuilder().build("tcp://iot.eclipse.org:1883");
client.connect();
```

Publish the service, this will create MQTT subscriptions.

```java
client.publish(CalculatorService.class, // functionality to expose
               "remote-calculator",     // service identifier for rpc
               (a, b) -> a + b);        // implementation of our service
```

## Clients

Instantiate a client, the client will require an MQTT broker URL.

```java
MqttDrpcClient client = new MqttDrpcClientBuilder().build("tcp://iot.eclipse.org:1883");
client.connect();
```

Obtain a connector for your interface, this will allow you to make targeted and distributed remote procedure calls using the client's configuration.

```java
ServiceConnector<CalculatorService> connector = client.connector(CalculatorService.class);
```

With the connector we can make distributed calls, with regular interfaces, and nice Java 8 syntax to go along with it. This example will call every CalculatorService ( registered as described earlier ), and then logs any results it gets.

```java
connector.drpc(d -> d.calculate(1, 2),
               r -> LOG.info("CalculatorService#calculate(1,2) = {}", r));
```

And targeted calls.

```java
CalculatorService remoteCalculator = connector.connect("remote-calculator"); // connect to the calculator identified by the id we've registered with at the service side
remoteCalculator.calculate(1,2);
```
