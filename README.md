# fileshare-ws
A Wildfly Web service for managing a database of shared files to enable P2P file 
sharing.

# Dependencies
This service was developed and tested on a Wildfly 27.0.1.Final but should work
on any other versions of Wildfly newer than this. However, the functionality of
the service on any other versions cannot be assured.

This project is configured as a maven project and was developed and built using
Maven 3.9.12. This tool is used for packaging and deploying the project to an
existing Wildfly server. Alternatively, it may be imported to Eclipse as a Maven
project and deployed to a server via Eclipse's server environment manager as a
dynamic web module. Both approaches are detailed in the next section.

This service assumes Java 17 and is compiled against the jakarta namespace. Newer
versions of Java should work but are not assured to do so. Compiling against the
javax namespace for Web services is not supported.

In order to track files, this service must have a backing PostgreSQL database that
will hold file listings that clients can add and remove files to and query for
information that will enable a peer-to-peer share of a file. Maven handles JDBC
dependencies, but there must be a running PostgreSQL database running to receive
queries and updates.

# Building/Deploying
Choose **one** of the following methods for building and deploying the project.

### Building and Deploying with Maven
This approach allows for building and deploying the service from the command line
directly using Maven as the build tool. After downloading and configuring Wildfly,
run the following commands from the project root directory:

    mvn clean package
    mvn wildfly:deploy

This will generate the .war file and then deploy it to the running server. However,
the above deploy command will fail if the Wildfly server is configured to require
an admin username and password. In this case, use the following deploy command:

    mvn wildfly:deploy -Dwildfly.username=<user> -Dwildfly.password=<pass>

where <user> and <pass> are your admin username and password. If the Wildfly server
uses a non-standard port, also add the following option:

    -Dwildfly.port=<port>

When it is time to shut down the service run the following, with the appropriate
command line options as above:

    mvn wildfly:undeploy

### Building and Deploying via Eclipse
This approach allows for building and deploying using Eclipse to manage the server
and handle publishing the service. Follow these steps:

1. Download Wildfly and configure a server environment in Eclipse for it.
2. Run the Wildfly server and verify that it is reachable.
3. Import this project as a Maven project.
4. Right click on the project and click Project -> Run As -> Run on Server.

This should be sufficient to get the service deployed to the server. If there are
any issues, right clicking on the project and selcting Maven -> Update Project...
then checking the "Force Update of Snapshots/Releases" radio button and updating
the project will often fix them.

Another common issue is in incorrect mappings in the Deployment Assembly settings
of Eclipse. If there are problems, try right clicking the project and clicking
Properties -> Deployment Assembly. Be sure that src/main/webapp is mapped to the
project root and that Maven Dependencies is mapped to WEB-INF/lib.

# Configuration
In order to change certain operational aspects of the service, change the values
found in the src/main/resources/fileservice.properties file. Below is a listing
and explanation of what each of these properties does:

- db.stale_file_age: this controls the number of seconds a file can be left in the
database without receiving a heartbeat signal from the client before it is considered stale.
Files that are considered stale are valid targets for the reaper, which will remove them
on its next run.
- db.timezone: the timezone that the database should use. It is important that the
service and clients share a time zone to avoid mismatches between times that a file
was last seen or has received a heartbeat to avoid premature reaping.
- db.user: the username that the service will use when interacting with the backing PostgreSQL
database.
- db.password: the password that the service will use when interacting with the backing
PostgreSQL database.
- db.url: the location of the database. This must be configured to the settings appropriate
for the backing PostgreSQL server in use. The address should include the database
name as configured on your system. e.g. jdbc:postgresql://localhost:5432/<your_db_name>

# Usage
After starting the PostgreSQL server, deploy the project to a running Wildfly
server. Once this process is complete, the service is available for interfacing
with the clients, found in the fileshare-client project.

If run from a command shell the server will stream logs to the terminal. If run
from Eclipse, these will appear in the Eclipse terminal instead. At any time,
a log file may be found for inspection in the ${WildflyHome}/standalone/log
directory.

After the service is deployed, verify that it is active by accessing the WSDL
from a browser:

    http://localhost:8080/FileShare_Service/FileShareService?wsdl

If the server is running on a port other than 8080, substitute the port into the
above URL in place of '8080'.
