var eb = require("vertx/event_bus");
var console = require("vertx/console");
var vertx = require("vertx")

var eb = vertx.eventBus;

  var jsonMsg = {
    service : 'process',
    operation : 'start',
    id : 'send-data.send-data-process',
    data: {"address":"my.custom.address","message":"testing jBPM with vert.x"}
  };
  eb.send('org.jbpm:send-data:1.0', jsonMsg, function(reply) {
  console.log("Received reply: " + JSON.stringify(reply));
  });