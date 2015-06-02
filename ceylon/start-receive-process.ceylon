import io.vertx.ceylon.platform {
  Verticle,
  Container
}
import io.vertx.ceylon.core {
  Vertx
}
import io.vertx.ceylon.core.eventbus {
  Message
}
import ceylon.json {
  JsonObject=Object,
  JsonArray=Array
}
shared class Sender() extends Verticle() {
  
  shared actual void start(Vertx vertx, Container container) {
    value jsonMsg = JsonObject {
            "service"->"process",
            "operation"->"start",
            "id"->"receive-data.receive-data-process",
            "data"->JsonObject {
              "address"->"my.custom.address"
            }
          };
    
    vertx.eventBus.send<JsonObject>("org.jbpm:receive-data:1.0", jsonMsg).onComplete((Message<JsonObject> msg) => print("Received reply: ``msg.body``"));
    
  }
}