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
               r -> System.out.println("CalculatorService#calculate(1,2) = " + r));
```

And targeted calls.

```java
CalculatorService remoteCalculator = connector.connect("remote-calculator"); // connect to the calculator identified by the id we've registered with at the service side
remoteCalculator.calculate(1,2);
```

## Protocol

MQTT is used to relay messages, JSON is used as data format. The internals of the remote procedure protocol is described here. If you intend to only use this Java library, the library takes care of this for you.

### Publishing

To publish a service, a service must subscribe itself to multiple topics. Two for each method to expose. One of them for receiving global calls, and the other for calls by the service's identifier. The convention is as follows:

- `s/PACKAGE.INTERFACE/METHOD` for global calls.
- `s/PACKAGE.INTERFACE/METHOD/IDENTIFIER` for calls by identifier.

For example using the `CalculatorService` example from earlier, with its method `calculate`, and identifier `remote-calculator. The topics to subscribe on will be:

- `s/com.hileco.mqtt.example.CalculatorService/calculate` for global calls.
- `s/com.hileco.mqtt.example.CalculatorService/calculate/remote-calculator` for calls by identifier.

## Requests and Responses

Request bodies are defined as a JSON array containing:

- The UUID of the client's callback topic
- The UUID of the request
- The arguments, as part of the array

Response bodies are defined as a JSON array containing:

- The UUID of the request
- The result

In order to call a service, a client must first subscribe itself to a topic to receive results on. The convention is:

- `c/UUID` for callbacks.

For example, in a test scenario a client subscribed itself to:

- `c/c744e0a4-2274-4a4d-948b-4fd4e74ecc86`

To then essentially invoke `calculate(1, 2)` on all published calculators, a client must send:

- On the topic of the service, for example: `s/com.hileco.mqtt.example.CalculatorService/calculate`.
- With body containing callback UUID, request UUID, and arguments, for example: `["c744e0a4-2274-4a4d-948b-4fd4e74ecc86","3abfe6a4-1d48-40e5-a904-70b1f9267463",1,2]`.

The service must then respond on the callback topic:

- On the topic of the callback, for example: `c/c744e0a4-2274-4a4d-948b-4fd4e74ecc86`
- With body containing request UUID, and result, for example: ["3abfe6a4-1d48-40e5-a904-70b1f9267463",3]
