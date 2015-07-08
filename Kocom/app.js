var vertx = require('vertx');
var container = require('vertx/container');
var eb = vertx.eventBus;

var webServerConf = {

  // Normal web server stuff

  port: 30001,
  host: '163.180.117.178',
  //ssl: true,

  bridge: true,

  inbound_permitted: [
    // Allow calls to login and authorise
    {
      address: 'vertx.basicauthmanager.login'
    },
    {
      address: 'mqttclient'
    },
    {
      address: 'pdCollector'
    },
    {
	address : 'mongo-persistor'
       }
  ],


  outbound_permitted: [
    {}
  ]
};
var dbconfig = {
    "host" : "localhost",
    "port" : 30000,
    "dbname" : "scconfig"
    };
var key = "kocom";

var mqttconf={
  host : 'localhost',
  port : 1883,
  key : key,
  clientID : "collector",
  dbConfig : dbconfig
};

container.deployModule('icns.kocom~mongo-persistor~1.0',dbconfig, function() {
  load('static_data.js');
});

container.deployModule('icns.kocom~publicdatacollector~1.0', function(err) { // deploy public data collector module
    if(err!=null){
        err.printStackTrace(); // error print;
    }
});

container.deployModule('icns.kocom~mqtt-client~0.1', mqttconf, 1, function(err) { // deploy public data collector module
    if(err!=null){
        err.printStackTrace(); // error print;
    }
    else
    {
      eb.send("mqttclient",{
        "action" : "subscribe",
        "topic" : "TGdata"
      });
    }
});

container.deployModule('io.vertx~mod-web-server~2.0.0-final', webServerConf);