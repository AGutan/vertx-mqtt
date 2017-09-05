package io.vertx.mqtt.test.server;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.routing.Route;
import io.vertx.mqtt.routing.Router;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MqttServerRouterTest {

  private static final Logger log = LoggerFactory.getLogger(MqttServerTest.class);

  protected static final String MQTT_SERVER_HOST = "localhost";
  protected static final int MQTT_SERVER_PORT = 1883;

  MqttEndpoint mqttEndpoint;

  private Vertx vertx;

  @Before
  public void before(TestContext context) {

    this.vertx = Vertx.vertx();
  }

  @After
  public void after(TestContext context) {

    this.vertx.close();
  }

  @Test
  public void messageRouting(TestContext context) {

    MqttServerOptions options = new MqttServerOptions()
      .setPort(8883)
      .setKeyCertOptions(new PemKeyCertOptions()
        .setKeyPath("/home/alex/ssltest/server2.pem")
        .setCertPath("/home/alex/ssltest/server-crt2.pem"))
      .setSsl(true);

    MqttServer mqttServer = MqttServer.create(vertx, options);

    Router router = Router.router(vertx);

    mqttServer.endpointHandler(endpoint -> {

      endpoint
        .publishHandler(router::accept)
        .publishReleaseHandler(endpoint::publishComplete);
      mqttEndpoint = endpoint;
      endpoint.accept(true);

    }).listen();

    Route tempsGroup1 = router.route().path("/foo/+/bar");

    tempsGroup1.mqttMessageHandler(message -> {

      Buffer payload = message.payload();
      // handle temperature data

      if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
        mqttEndpoint.publishAcknowledge(message.messageId());
      } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
        mqttEndpoint.publishRelease(message.messageId());
      }

    });




  }

}
