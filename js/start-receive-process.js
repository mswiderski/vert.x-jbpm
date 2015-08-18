var eb = require("vertx/event_bus");
var console = require("vertx/console");
var vertx = require("vertx")

var eb = vertx.eventBus;

  var jsonMsg = {
    service : 'process',
    operation : 'start',
    id : 'receive-data.receive-data-process',
    data: {"address":"my.custom.address"}
  };
  eb.send('org.jbpm:receive-data:1.0', jsonMsg, function(reply) {
  console.log("Received reply: " + JSON.stringify(reply));
  });
  console.log("Message sent");
