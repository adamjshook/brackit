# JSON Extension (Beta) #

Brackit features a seamless integration of JSON-like objects and arrays directly at the language level.

You can easily mix arbitrary XML and JSON data in a single query or simply use brackit to convert data from one format into the other. This allows you to get the most out of your data.

The language extension allows you to construct and operate JSON data directly; additional utility functions help you to perform typical tasks.

Everything is designed to simplify joint processing of XDM and JSON and to maximize the freedom of developers. Thus, our extension effectively supports some sort of superset of XDM and JSON. That means, it is possible to create arrays and objects which do not strictly conform to the JSON RFC. It's up to you to decide how you want to have your data look like!

## Arrays ##

Arrays can be created using an extended version of the standard JSON array syntax:

```
(: statically create an array with 3 elements of different types: 1, 2.0, "3"  :)
[ 1, 2.0, "3" ]
(: for compliance with the JSON syntax the tokens 'true', 'false', and 'null' 
   are translated into the XML values xs:bool('true'), xs:bool('false') 
   and empty-sequence() :)
[ true, false, null ]
(: is different to :)
[ (true), (false), (null) ]
(: where each field is initialized as the result of a path expression 
   starting from the current context item, e,g., './true' :)

(: dynamically create an array by evaluating some expressions: :)
[ 1+1, substring("banana", 3, 5), () ]
(: yields the array [ 2, "nana", () ] :)

(: arrays can be nested and fields can be arbitrary sequences :)

[ (1 to 5) ]
(: yields an array of length 1: [(1,2,3,4,5)] :)

[ <entry tstmp='2012-03-29'>some text</entry> ]
(: yields an array of length 1 with an XML fragment as field value :)

[ 'x', [ 'y' ], 'z' ]
(: yields an array of length 3: [ 'x' , ['y'], 'z' ] :)

(: a preceding '=' distributes the items of a sequence to 
   individual array positions :)
[ =(1 to 5) ]
(: yields an array of length 5: [ 1, 2, 3, 4, 5 ] :)


(: array fields can be accessed by the '[[ ]]' postfix operator: :)
let $a := [ "Jim", "John", "Joe" ]
return $a[[1]]
(: yields the string "John" :)

(: the function bit:len() returns the length of an array :)
bit:len([ 1, 2, ])
(: yields 2 :)
```

## Records ##

Records provide an alternative to XML to represent structured data. Like with arrays we support an extended version of the standard JSON object syntax:

```
(: statically create a record with three fields named 'a', 'b' and 'c' :)
{ "a": 1, "b" : 2, "c" : 3 }

(: for compliance with the JSON syntax the tokens 'true', 'false', and 'null' 
   are translated into the XML values xs:bool('true'), xs:bool('false') 
   and empty-sequence() :)
{ "a": true, "b" : false, "c" : null}

(: field names are modelled as xs:QName and may be set in double quotes, single quotes or completely without quotes. :)
{ a : 1, 'b' : 2, "c" : 3 }

(: field values may be arbitrary expressions:)
{ a : concat('f', 'oo') , 'b' : 1+1, c : [1,2,3] }
(: yields {a : "foo", b : 2, c : [1,2,3]} :)

(: field values are defined by key-value pairs or by an expression that evaluates to a record :)
let $r := { x:1, y:2 }
return { $r, z:3}
(: yields {x : 1, y : 2, z : 3} :)

(: fields may be selectively projected into a new record :)
{x : 1, y : 2, z : 3}{z,y}
(: yields {z : 3, y : 2} :)

(: values of record field can be accessed using the deref operator '=>' :)
{ a : "hello" , b : "world" }=>b
(: yields the string "world" :)

(: the deref operator can be used to navigate into deeply nested record structures :)
let $n := <x><y>yval</y></x> 
let $r := {e : {m:'mvalue', n:$n}}
return $r=>e=>n/y
(: yields the XML fragment <y>yval</y> :)

(: the function bit:fields() returns the field names of a record :)
let $r := {x : 1, y : 2, z : 3}
return bit:fields($r)
(: yields the xs:QName array [ x, y, z ] :)

(: the function bit:values() returns the field values of a record :)
let $r := {x : 1, y : 2, z : (3, 4) }
return bit:values($r)
(: yields the array [ 1, 2, (2,4) ] :)
```

## Parsing JSON ##

```
(: the utility function json:parse() can be used to parse 
   JSON data dynamically from a given xs:string :)
let $s := io:read('/data/sample.json')
return json:parse($s)
```