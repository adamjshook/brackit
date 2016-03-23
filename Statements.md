# Statement Syntax Extension (Beta) #


---

**IMPORTANT NOTE**:

This extension is only a syntax extension to simplify programmer's life when writing XQuery. It is neither a subset of nor an equivalent to the [XQuery Scripting Extension 1.0](http://www.w3.org/TR/xquery-sx-10/).

---


Almost any non-trivial data processing task consists of a series of consecutive steps. Unfortunately, the functional style of XQuery makes it a bit cumbersome to write code in a convenient, script-like fashion. Instead, the standard way to express a linear multi-step process (with access to intermediate results) is to write a FLWOR expression with a series of `let`-clauses.

As a shorthand, Brackit allows you to write such processes as a sequence of '`;`'-terminated _statements_, which most developers are familiar with:

```
(: declare external input :)
declare variable $file external;

(: read input data :)
$events := fn:collection('events');

(: join the two inputs :)
$incidents := for $e in $events
              where $e/@severity = 'critical'
              let $ip := x/system/@ip
              group by $ip
              order by count($e)
              return <stats>
                        <system>{$ip}</system>
                        <critical-events>count($e)</critical-events>
                     </stats>;

(: store report to file :)
$report := <report date="{current-date()}">{$incidents}</report>;
$output := bit:serialize($report);
io:write($file, $output);

(: return a short message as result :)
<msg>
Generated '{count($incidents)}' incident entries to report '{$file}'
</msg>
```

Internally, the compiler treats this as a FLWOR expression with `let`-bindings. The result, i.e., the `return` expression, is the result of the last statement. Accordingly, the previous example is equivalent to:

```
(: declare external input :)
declare variable $file external;

(: read input data :)
let $events := fn:collection('events')

(: join the two inputs :)
let $incidents := for $e in $events
                  where $e/@severity = 'critical'
                  let $ip := x/system/@ip
                  group by $ip
                  order by count($e)
                  return <stats>
                            <system>{$ip}</system>
                            <critical-events>count($e)</critical-events>
                         </stats>

(: store report to file :)
let $report := <report date="{current-date()}">{$incidents}</report>
let $output := bit:serialize($report)
let $written := io:write($file, $output)

(: return a short message as result :)
return 
<msg>
Generated '{count($incidents)}' incident entries to report '{$file}'
</msg>
```

The statement syntax is especially helpful to improve readability of user-defined functions. The following example shows an - admittedly rather slow - implementation of the quicksort algorithm:

```
declare function local:qsort($values) {
  $len := count($values);
  if ($len <= 1) then (
      $values    
  ) else (
      $pivot := $values[$len idiv 2];
      $less := $values[. < $pivot];
      $greater := $values[. > $pivot];
      (local:qsort($less), $pivot, local:qsort($greater))
  )
};

local:qsort((7,8,4,5,6,9,3,2,0,1))
```