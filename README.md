# Newrelic Tools for Wildfly

## Newrelic log handler
A log handler which forwards log statements to the Newrelic log api. See https://docs.newrelic.com/docs/logs/log-api/introduction-log-api/ for details

### Version
The current version is 1.0-SNAPSHOT

### Installation:
Build the distribution by running this command:

``gradle jar``

Add the module to Wildfly

``module add --name=ch.performancebuildings.newrelic --dependencies=org.apache.logging.log4j.api,org.jboss.logmanager,jakarta.json.api --resources=~/YOUR_PATH/wildfly-newrelic-1.0-SNAPSHOT.jar``

This creates the module in ``$WILDFLY_HOME/modules``

Add the handler to the logging subsystem

``/subsystem=logging/custom-handler=PB-NEWRELIC:add(level=DEBUG, class=ch.performancebuildings.newrelic.NewrelicHandler,module=ch.performancebuildings.newrelic,properties={licenseKey=YOUR_LICENSE_KEY})``

Supported properties:

|Property| Description                                                                                                                                                                                 |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|licenseKey| must be a data ingest key                                                                                                                                                                   |
|hostname| will be sent to Newrelic as 'source'. Defaults to ``InetAddress.getLocalHost().getHostName()``                                                                                              |
|baseURI| the URI of the Newrelic Log-API. Defaults to https://log-api.newrelic.com/log/v1. <br/>Make sure your key matches the endpoint. FYI: an EU key can only be used at an EU endpoitn and vice versa |

Add the handler to a logger (example)
``/subsystem=logging/logger=ch.performancebuildings.control:add-handler(name=PB-NEWRELIC)``

To increase throughput consider using an async-handler
```
// create a new async handler
/subsystem=logging/async-handler=NEWRELIC-ASYNC:add(level=DEBUG, queue-length=1024, overflow-action=BLOCK)

// assign the previously created handler as a subhandler
/subsystem=logging/async-handler=NEWRELIC-ASYNC:assign-subhandler(name=PB-NEWRELIC)
```

### Testing & Debugging
For debugging set the logger ch.performancebuildings.newrelic.NewrelicHandler to DEBUG