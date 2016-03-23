# Welcome to Brackit #

**Coming soon:** BrackitMR, the Hadoop extension of Brackit presented at the ADBIS conference on September 2013, will be released on this website soon. Send us an email to get notified!

**Brackit** is a flexible XQuery-based query engine developed at the [TU Kaiserslautern](http://wwwlgis.informatik.uni-kl.de/cms/dbis/projects/brackit/) in the context of our research in the field of query processing for semi-structured data. The system features a fast runtime and a flexible compiler backend, which is , e.g., able to rewrite queries for optimized join processing and efficient aggregation operations.

At the moment we support [XQuery 1.0](http://www.w3.org/TR/xquery/) including library module support, the [XQuery Update Facility 1.0](http://www.w3.org/TR/xquery-update-10/) and some features of [XQuery 3.0](http://www.w3.org/TR/xquery-30/) like the new FLWOR clauses `group by` and `count`.

As a speciality, Brackit comes with extensions to work natively with [JSON-style arrays and records](JSON.md). Another extension allows you to use a special [statement syntax](Statements.md) for writing XQuery programs in a script-like style.

Sounds interesting? Then have a look at the [Getting Started Guide](GettingStarted.md).

**BrackitDB** is an XML database management system, which is powered by the brackit query engine. Its native XML storage offers advanced indexing capabilities and full ACID transactions. Learn more about it [here](BrackitDB.md).


Enjoy!