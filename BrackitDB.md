# Installation #

## Compiling from source ##

To build brackit from source you need to have [Mercurial](http://mercurial.selenic.com/) and [Maven](http://maven.apache.org) installed.

### Step 1: Get the brackit query engine ###

To build and use brackitdb you need to have the brackit query engine installed to your local Maven repository. Simply follow the steps 1 to 3 of the [installation instructions](GettingStarted.md).

### Step 2: Get the source ###
To get a clone of the current source tree type:
```
hg clone http://code.google.com/p/brackit.brackitdb brackitdb
```

### Step 3: Build ###
To build and package change into the root directy of the clone, e.g.,
```
cd brackitdb
```
and then type:
```
mvn package
```
To skip running the unit tests run instead
```
mvn -DskipTests package
```
The subdirectory _./brackitdb-server/target/brackitdb_ contains now everything you need to use brackitdb. You can copy the contents of this directory to a more suitable place like _/opt/brackitdb_.

# Database Administration #

You can start the database through the shell script in _./bin/server_ (_.\bin\server.sh_ on windows systems).

**Hint:** On UNIX-based systems you may want to make the script executable by issuing
```
chmod u+x ./bin/server
```

## Creating an empty database ##

To use brackitdb the first time, you must create an empty database with the command
```
bash ./bin/server install
```
When everything runs fine, you are ready to use brackitdb

## Starting/stopping the database server ##

You can start the database by issuing
```
bash ./bin/server start
```
and stop it again with
```
bash ./bin/server stop
```
or by sending a SIGQUIT signal (Ctrl+D) to the server process.

## The Command Line Processor ##
BrackitDB ships with a command line processor to issue queries against the database:
```
bash ./bin/server clp
```

# Client programs #

The database driver jar file _brackitdb-driver-X.Y.Z.jar_ resides in the subdirectory _./brackitdb-driver/target/_

The following code fragment demonstrates how to connect to run queries against the database:

```
// open a new connection to a locally running brackitdb server
BrackitConnection con = new BrackitConnection("localhost", 11011);
try {
    // run a simple query
    // and writ the result
    // to standard out
    con.query("1+1", System.out);
} finally {
   // close the connection again
   con.close();
}
```

In our [sample project](http://code.google.com/p/brackit/source/browse/?repo=dbexamples) you find examples to more advanced topics like document storage, indexing and transaction management.