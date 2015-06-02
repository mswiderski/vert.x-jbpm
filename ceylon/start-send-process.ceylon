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
            "id"->"send-data.send-data-process",
            "data"->JsonObject {
              "address"->"my.custom.address",
              "message"->"testing jBPM with vert.x send by Ceylon"              
            }
          };
    
    vertx.eventBus.send<JsonObject>("org.jbpm:send-data:1.0", jsonMsg).onComplete((Message<JsonObject> msg) => print("Received reply: ``msg.body``"));
    
  }
}