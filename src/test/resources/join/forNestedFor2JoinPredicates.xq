for $a in (1 to 5)
let $b:= for $c in ((2 to 3),(5 to 8))
         where $a = $c
         where $c = $a
         return $c
return $b 