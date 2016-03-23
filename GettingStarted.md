# Installation #

## Compiling from source ##

To build brackit from source you need to have [Mercurial](http://mercurial.selenic.com/) and [Maven](http://maven.apache.org) installed.

### Step 1: Get the source ###
To get a clone of the current source tree type:
```
hg clone http://code.google.com/p/brackit
```

### Step 2: Build ###
To build and package change into the root directy of the clone, e.g.,
```
cd brackit
```
and then type:
```
mvn package
```
To skip running the unit tests run instead
```
mvn -DskipTests package
```
That's all. You find the ready-to-use jar file in the subdirectory ./target

### Step 3: Install (optional) ###
If you want to use brackit in your other Maven-based projects, you need to install brackit in your local maven repository:
```
mvn install
```

# First Steps #

## Running from the command line ##
Brackit ships with a rudimentary command line interface to run ad-hoc queries. Invoke it with
```
java -jar brackit-x.y.z.jar
```
where _x.y.z_ is the version number of brackit.

### Simple queries ###
The simplest way to run a query is by passing it via _stdin_:
```
echo "1+1" | java -jar brackit-x.y.z.jar
```
This returns as output
```
2
```
If the query is stored in a separate file, let's say _test.xq_, type:
```
java -jar brackit-x.y.z.jar -q test.xq
```
or use the file redirection of your shell:
```
java -jar brackit-x.y.z.jar < test.xq
```
Easy, isn't it?

### Querying documents ###
Querying documents is as simple as running any other query. The default "storage" module resolves any referred documents accessed by the XQuery functions _fn:doc()_ and _fn:collection()_ at query runtime. To query a document in your local filesytem simply type use the path to this document in the _fn:doc()_ function:
```
echo "doc('products.xml')//product[@prodno = '4711']" | java -jar brackit-x.y.z.jar
```
Of course, you can also directly query documents via _http(s)_, or _ftp_. For example:
```
echo "count(doc('http://example.org/foo.xml')//bar)" | java -jar brackit-x.y.z.jar
```

## Coding with Brackit ##
Running XQuery embedded in a Java program requires only a few lines of code:
```
String query = "for $i in (1 to 4)\n" +
               "let $d := <no>{$i}</no>\n" +
               "return $d";

// initialize a query context
QueryContext ctx = new QueryContext();

// compile the query
XQuery xq = new XQuery(query);

// enable formatted output
xq.setPrettyPrint(true);

// run the query and write the result
// to System.out
xq.serialize(ctx, System.out);
```
For more examples covering also the topics document storage and library modules have a look at the [example project](http://code.google.com/p/brackit/source/browse/?repo=examples).