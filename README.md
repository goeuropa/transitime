core
====

The complete core Java software for the Transitime real-time transit information project.

The software is checked in and made public only so that it can be viewed. While it has been used in a test environment it still is lacking significant features and is not ready for deployment.

<b>Build</b>

The software is made up of three modules which can each be built with maven.

The core fucntionality is in the transiTime project. The REST api is in transiTimeApi and the user Web applicaton is in transiTimeWebapp.

They should be built in this order.


transitTime

transitTimeApi

transitTimeWebApp


Each module can be built by change to the related directory and running "mvn install".

eg.

cd transitTime

mvn install

<b>Setup</b>

The main module is transitTime. This has several standalone programs in the org.transitime.applications package.

SchemaGenerator.java will generate the SQL to create the database structures you need to run on.<br/>
DBTest.java can be used to test that the database can be connected to.<br/>
GTFSFileProcessor.java will read a GTFS file into this database structure.<br/>
Core.java is as the name implies is the workhorse of the system. <br/>
RmiQuery.java allows you make queries to the server run in core from the command line.<br/>

Details on how to run each of these and their respective parameters are in the README for the transiTime module.

Once this is set up the next step is to set up the transiTimeApi which is a RESTful API. This API makes RMI calls to the RMI Server started by Core.java to provide results. This is a war file which can be deployed into Tomcat.  (I haven't got here yet but will fill in the details on setup in its README)

The transiTimeWebapp in turn is a web application which uses the transitTimeAPI to provided a user interface. This is a war file which can be deployed into tomcat. (I haven't got here yet but will fill in the details on setup in its README)



