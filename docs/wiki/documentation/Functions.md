## Built-in Functions

This section provides documentation for all built-in functions available with the reference implementation. 
For each function the documentation provides 

1. A one sentence explanation of the functions intent.
1. The function's **signature** that specifies data types and names for each input argument, and, the expected data type for the function's return value. A function signature consists of the function's name followed by a colon `:` then a space separated list of data types--one for each formal argument of the function--followed by an arrow `->` followed by the function's return type, e.g., `add: Integer Integer -> Integer` is the signature for `add` which accepts 2 inputs both `Integer`'s and returns one value of type `Integer`. 
1. The function's **header** that specifies names for each of the function's formal arguments. Any documentation following the header can refer
   to the function's formal arguments by name. 
1. The function's **purpose statement** that further expands on how the function behaves and specifies any pre- and/or post-conditions. 
1. A list of examples calling the function and their expected results. 


### Unknown (`null` and `missing`) propagation

Unless otherwise stated all functions listed below propagate `null` and `missing` argument values. Propagating `null` 
and `missing` values is defined as: if any function argument is either `null` or `missing` the function will return 
`null`, e.g., 

```sql 
CHAR_LENGTH(null)    -- `null`
CHAR_LENGTH(missing) -- `null` (also returns `null`)
```


### BIT_LENGTH -- since v0.10.0

Returns the number of bits in the input string.

Signature
: `BIT_LENGTH: String —> Int`

Header
: `BIT_LENGTH(str)`

Examples
: 

```sql
bit_length('jose') -- 32
```

### CAST -- since v0.1.0

Given a value and a target data type, attempt to coerce the value to the target data type. 

Signature 
: `CAST: Any DataType -> DataType` 

Where `DataType` is one of 

* `missing`
* `null`
* `integer`
* `boolean`
* `float`
* `decimal`
* `timestamp`
* `symbol`
* `string`
* `list`
* `struct`
* `bag`

Note for handling `CAST` Operation on `+inf`, `-inf`, and `nan`, see Special Float Value Handling below.

Header
: `CAST(exp AS dt)` 

Purpose
: Given an expression, `exp` and the data type name, `dt`, evaluate `expr` to a value, `v` and alter the data type of `v` to `DT(dt)`. If the conversion 
cannot be made the implementation signals an error. 


The runtime support for casts is 

* Casting to `null` from 
    * `null` is a no-op
    * `missing` returns `null`
    * else error 
* Casting to `missing` from
    * `missing` is a no-op
    * `null` returns `missing`
    * else error 
* Casting to `integer` from
    * Integer: is a no-op
    * Boolean: `true` returns 1, `false` returns 0
    * String or Symbol: attempt to parse the content as an Integer and return the Integer, else error. 
    * Float or Decimal: gets narrowed to Integer 
    * else error 
* Casting to `boolean` from
    * Boolean is a no-op
    * Integer or Decimal or Float: if `v` is a representation of the number `0` (e.g., `0` or `-0` or `0e0` or `0d0` et.) then `false` else `true`
    * String or Symbol: `true` unless `v` matches--ignoring character case--the Ion string `"false"` or matches--ignoring character case-- the Ion symbol `'false'` then return `false` 
    * else error 
* Casting to `float` from
    * Float is a no-op
    * Boolean: `false` return `0.0`, `true` returns `1.0`
    * Integer or Decimal: convert to Float and return 
    * String or Symbol: attempt to parse as Float and return the Float value, else error 
    * else error 
* Casting to `decimal` from
    * Decimal is a no-op
    * Boolean: return `1d0` if `true`, `0d0` if `false`
    * String or Symbol: attemp to parse as Decimal and return Decimal value, else error
    * else error 
* Casting to `timestamp` from
    * Timestamp is a no-op
    * String or Symbol: attemp to parse as Timestamp and return the Timestamp value, else error 
    * else error
* Casting to `symbol` from
    * Symbol is a no-op
    * Integer or Float or Decimal: narrow to Integer and return the value as a Symbol, i.e, a Symbol with the same sequence of digits as characters 
    * String: return the String as a Symbol, i.e., represent the same sequence of characters as a Symbol
    * Boolean: return `'true'` for `true` and `'false'` for `false`
    * Timestamp: return the Symbol representation of the Timestamp, i.e., represent the same sequence of digits and characters as a Symbol
    * else error 
* Casting to `string` from
    * String is a no-op
    * Integer or Float or Decimal: narrow to Integer and return the value as a String, i.e, a String with the same sequence of digits as characters
    * Symbol: return the String as a Symbol, i.e., represent the same sequence of characters as a String
    * Boolean: return `"true"` for `true` and `"false"` for `false`
    * Timestamp: return the String representation of the Timestamp, i.e., represent the same sequence of digits and characters as a String
    * else error 
* Casting to `list` from
    * List is a no-op
    * Bag: return a list with the same elements. The order of the elements in the resulting list is unspecified. 
    * else error 
* Casting to `struct` from
    * Struct is a no-op
    * else error 
* Casting to `bag` from
    * Bag is a no-op
    * List: return a list with the same elements. The order of the elements in the resulting bag is unspecified. 

#### Special Float Value Handling
The following table list explicitly on behavior of CAST Function when expression supplied is one of `+inf`, `-inf`, or `nan`.

| target type | can cast | cast quality |     expected result/error      | 
|:-----------:|:--------:|:------------:|:------------------------------:|
|   missing   |  false   |     N/A      |     EVALUATOR_INVALID_CAST     | 
|    null     |  false   |     N/A      |     EVALUATOR_INVALID_CAST     |
|   integer   |  false   |     N/A      |     EVALUATOR_CAST_FAILED      | 
|   boolean   |   true   |    LOSSY     |              true              |
|    float    |   true   |   LOSSLESS   |        +inf, -inf, nan         |
|   decimal   |  false   |     N/A      |     EVALUATOR_CAST_FAILED      | 
|  timestamp  |  false   |     N/A      |     EVALUATOR_INVALID_CAST     |
|   symbol    |   true   |   LOSSLESS   | 'Infinity', '-Infinity', 'NaN' | 
|   string    |   true   |   LOSSLESS   | "Infinity", "-Infinity", "NaN" |
|    list     |  false   |     N/A      |     EVALUATOR_INVALID_CAST     | 
|   struct    |  false   |     N/A      |     EVALUATOR_INVALID_CAST     | 
|     bag     |  false   |     N/A      |     EVALUATOR_INVALID_CAST     |


Examples
:

```sql
-- Unknowns propagation 
CAST(null    AS null)    -- null 
CAST(missing AS null)    -- null 
CAST(missing AS missing) -- null
CAST(null    AS missing) -- null
CAST(null    AS boolean) -- null (null AS any data type name result to null)
CAST(missing AS boolean) -- null (missing AS any data type name result to null)

-- any value that is not an unknown cannot be cast to `null` or `missing`
CAST(true AS null)    -- error 
CAST(true AS missing) -- error
CAST(1    AS null)    -- error
CAST(1    AS missing) -- error

-- AS boolean 
CAST(true      AS boolean) -- true no-op
CAST(0         AS boolean) -- false
CAST(1         AS boolean) -- true
CAST(`1e0`     AS boolean) -- true (float)
CAST(`1d0`     AS boolean) -- true (decimal)
CAST('a'       AS boolean) -- false
CAST('true'    AS boolean) -- true (PartiQL string 'true')
CAST(`'true'`  AS boolean) -- true (Ion symbol `'true'`)
CAST(`'false'` AS boolean) -- false (Ion symbol `'false'`)

-- AS integer
CAST(true   AS integer) -- 1
CAST(false  AS integer) -- 0
CAST(1      AS integer) -- 1
CAST(`1d0`  AS integer) -- 1
CAST(`1d3`  AS integer) -- 1000
CAST(1.00   AS integer) -- 1
CAST('12'   AS integer) -- 12
CAST('aa'   AS integer) -- error
CAST(`'22'` AS integer) -- 22
CAST(`'x'`  AS integer) -- error

-- AS flaot
CAST(true   AS float) -- 1e0
CAST(false  AS float) -- 0e0
CAST(1      AS float) -- 1e0
CAST(`1d0`  AS float) -- 1e0
CAST(`1d3`  AS float) -- 1000e0
CAST(1.00   AS float) -- 1e0
CAST('12'   AS float) -- 12e0
CAST('aa'   AS float) -- error
CAST(`'22'` AS float) -- 22e0
CAST(`'x'`  AS float) -- error

-- AS decimal
CAST(true   AS decimal) -- 1.
CAST(false  AS decimal) -- 0.
CAST(1      AS decimal) -- 1.
CAST(`1d0`  AS decimal) -- 1. (REPL printer serialized to 1.)
CAST(`1d3`  AS decimal) -- 1d3
CAST(1.00   AS decimal) -- 1.00
CAST('12'   AS decimal) -- 12.
CAST('aa'   AS decimal) -- error
CAST(`'22'` AS decimal) -- 22.
CAST(`'x'`  AS decimal) -- error

-- AS timestamp
CAST(`2001T`                      AS timestamp) -- 2001T
CAST('2001-01-01T'                AS timestamp) -- 2001-01-01
CAST(`'2010-01-01T00:00:00.000Z'` AS timestamp) -- 2010-01-01T00:00:00.000Z
CAST(true                         AS timestamp) -- error
CAST(2001                         AS timestamp) -- error

-- AS symbol 
CAST(`'xx'`                     AS symbol) -- xx (`'xx'` is an Ion symbol)
CAST('xx'                       AS symbol) -- xx ('xx' is a string)
CAST(42                         AS symbol) -- '42'
CAST(`1e0`                      AS symbol) -- '1'
CAST(`1d0`                      AS symbol) -- '1'
CAST(true                       AS symbol) -- 'true'
CAST(false                      AS symbol) -- 'false'
CAST(`2001T`                    AS symbol) -- '2001T'
CAST(`2001-01-01T00:00:00.000Z` AS symbol) -- '2001-01-01T00:00:00.000Z`

-- AS string 
CAST(`'xx'`                     AS string) -- "xx" (`'xx'` is an Ion symbol)
CAST('xx'                       AS string) -- "xx" ('xx' is a string)
CAST(42                         AS string) -- "42"
CAST(`1e0`                      AS string) -- "1.0"
CAST(`1d0`                      AS string) -- "1"
CAST(true                       AS string) -- "true"
CAST(false                      AS string) -- "false"
CAST(`2001T`                    AS string) -- "2001T"
CAST(`2001-01-01T00:00:00.000Z` AS string) -- "2001-01-01T00:00:00.000Z"

-- AS struct 
CAST(`{ a: 1 }` AS struct) -- { a:1 }
CAST(true       AS struct) -- err

-- AS list
CAST(`[1, 2, 3]`        AS list) -- [ 1, 2, 3 ] (REPL does not diplay the parens and commas)
CAST(<<'a', { 'b':2 }>> AS list) -- [ a, { 'b':2 } ] (REPL does not diplay the parens and commas)
CAST({ 'b':2 }          AS list) -- error

-- AS bag
CAST([1,2,3]      AS bag) -- <<1,2,3>> (REPL does not display << >> and commas)
CAST([1,[2],3]    AS bag) -- <<1,[2],3>> (REPL does not display << >> and commas)
CAST(<<'a', 'b'>> AS bag) -- <<'a', 'b'>> (REPL does not display << >> and commas)
```

### CHAR_LENGTH, CHARACTER_LENGTH -- since v0.1.0

Counts the number of characters in the specified string, where 'character' is defined as a single unicode code point.

*Note:* `CHAR_LENGTH` and `CHARACTER_LENGTH` are synonyms.

Signature
: `CHAR_LENGTH: String -> Integer`

Header
: `CHAR_LENGTH(str)`

Signature
:   `CHARACTER_LENGTH: String -> Integer`

Header
:  `CHARACTER_LENGTH(str)`

Purpose
: Given a `String` value `str` return the number of characters (code points) in `str`.
  
Examples
: 

```sql  
CHAR_LENGTH('')          -- 0
CHAR_LENGTH('abcdefg')   -- 7
CHAR_LENGTH('😁😞😸😸') -- 4 (non-BMP unicode characters)
CHAR_LENGTH('eࠫ')         -- 2 (because 'eࠫ' is two codepoints: the letter 'e' and combining character U+032B)
```

### COALESCE -- since v0.1.0

Evaluates the arguments in order and returns the first non unknown, i.e. first non-`null` or non-`missing`. This function 
does **not** propagate `null` and `missing`.

Signature
: `COALESCE: Any Any ... -> Any`

Header
: `COALESCE(exp, [exp ...])`

Purpose
: Given a list of 1 or more arguments, evaluates the arguments left-to-right and returns the first value that is **not** an unknown (`missing` or
        `null`). 

Examples
: 

```sql  
COALESCE(1)                -- 1
COALESCE(null)             -- null
COALESCE(null, null)       -- null
COALESCE(missing)          -- null
COALESCE(missing, missing) -- null
COALESCE(1, null)          -- 1
COALESCE(null, null, 1)    -- 1
COALESCE(null, 'string')   -- 'string'
COALESCE(missing, 1)       -- 1
```

### DATE_ADD -- since v0.1.0

Given a data part, a quantity and a timestamp, returns an updated timestamp by altering datetime part by quantity

Signature 
: `DATE_ADD: DateTimePart Integer Timestamp -> Timestamp`

Where `DateTimePart` is one of 

* `year`
* `month`
* `day`
* `hour`
* `minute`
* `second`

Header
: `DATE_ADD(dp, q, timestamp)`

Purpose
:  Given a data part `dp`, a quantity `q`, and, an Ion timestamp `timestamp` returns an updated timestamp by applying the value for `q` to the `dp`
component of `timestamp`. Positive values for `q` add to the `timestamp`'s `dp`, negative values subtract. 

The value for `timestamp` as well as the return value from `DATE_ADD` must be a valid [Ion Timestamp](https://amzn.github.io/ion-docs/spec.html#timestamp)

Examples
: 

```sql  
DATE_ADD(year, 5, `2010-01-01T`)                -- 2015-01-01 (equivalent to 2015-01-01T)
DATE_ADD(month, 1, `2010T`)                     -- 2010-02T (result will add precision as necessary)
DATE_ADD(month, 13, `2010T`)                    -- 2011-02T
DATE_ADD(day, -1, `2017-01-10T`)                -- 2017-01-09 (equivalent to 2017-01-09T)
DATE_ADD(hour, 1, `2017T`)                      -- 2017-01-01T01:00-00:00
DATE_ADD(hour, 1, `2017-01-02T03:04Z`)          -- 2017-01-02T04:04Z
DATE_ADD(minute, 1, `2017-01-02T03:04:05.006Z`) -- 2017-01-02T03:05:05.006Z
DATE_ADD(second, 1, `2017-01-02T03:04:05.006Z`) -- 2017-01-02T03:04:06.006Z
```

### DATE_DIFF -- since v0.1.0

Given a datetime part and two valid timestamps returns the difference in datetime parts.

Signature
: `DATE_DIFF: DateTimePart Timestamp Timestamp -> Integer`

See [DATE_ADD](#date_add) for the definition of `DateTimePart`

Header
: `DATE_DIFF(dp, t1, t2)`


Purpose
: Given a datetime part `dp` and two timestamps `t1` and `t2` returns the difference in value for `dp` part of `t1` with `t2`. 
The return value is a negative integer when the `dp` value of `t1` is greater than the `dp` value of `t2`, and, a positive 
integer when the `dp` value of `t1` is less than the `dp` value of `t2`. 


Examples
:  

```sql  
DATE_DIFF(year, `2010-01-01T`, `2011-01-01T`)            -- 1
DATE_DIFF(year, `2010T`, `2010-05T`)                     -- 4 (2010T is equivalent to 2010-01-01T00:00:00.000Z)
DATE_DIFF(month, `2010T`, `2011T`)                       -- 12
DATE_DIFF(month, `2011T`, `2010T`)                       -- -12
DATE_DIFF(day, `2010-01-01T23:00T`, `2010-01-02T01:00T`) -- 0 (need to be at least 24h apart to be 1 day apart)
```


### EXISTS -- since v0.1.0

Given a PartiQL value returns `true` if and only if the value is a non-empty container(bag, sexp, list or struct), returns `false` otherwise. 

Signature
: `EXISTS: Container -> Boolean`

Header
: `EXISTS(val)`

Purpose 
: Given a PartiQL value `val`, if `val` is   

1. a container with size > 0, `EXISTS(val)` returns `true`; 
2. a container with size = 0, `EXISTS(val)` returns `false`; 
3. not a container, `EXISTS(val)` throws an error.

Note: 
1. This function does **not** propagate `null` and `missing`.

Examples
:  

```sql
EXISTS(`[]`)        -- false (empty list)
EXISTS(`[1, 2, 3]`) -- true (non-empty list)
EXISTS(`[missing]`) -- true (non-empty list)
EXISTS(`{}`)        -- false (empty struct)
EXISTS(`{ a: 1 }`)  -- true (non-empty struct)
EXISTS(`()`)        -- false (empty s-expression)
EXISTS(`(+ 1 2)`)   -- true (non-empty s-expression)
EXISTS(`<<>>`)      -- false (empty bag)
EXISTS(`<<null>>`)  -- true (non-empty bag)
EXISTS(1)           -- error
EXISTS(`2017T`)     -- error
EXISTS(null)        -- error
EXISTS(missing)     -- error
```

### EXTRACT -- since v0.1.0

Given a datetime part and a datetime type returns then datetime's datetime part value. 

Signature
: `EXTRACT: ExtractDateTimePart DateTime -> Integer`

where `ExtractDateTimePart` is one of 

* `year`
* `month`
* `day`
* `hour`
* `minute`
* `second`
* `timezone_hour`
* `timezone_minute`

and `DateTime` type is one of

* `DATE`
* `TIME`
* `TIMESTAMP` 
* `INTERVAL_YM`
* `INTERVAL_DT`

*Note* that `ExtractDateTimePart` **differs** from `DateTimePart` in [DATE_ADD](#date_add). 

Header
: `EXTRACT(edp FROM t)`

Purpose 
: Given a datetime part, `edp`, and a datetime type `t` return `t`'s value for `edp`. 
This function allows for `t` to be unknown (`null` or `missing`) but **not** `edp`.
If `t` is unknown the function returns `null`. 

Examples
: 

```sql
EXTRACT(YEAR FROM `2010-01-01T`)                           -- 2010
EXTRACT(MONTH FROM `2010T`)                                -- 1 (equivalent to 2010-01-01T00:00:00.000Z)
EXTRACT(MONTH FROM `2010-10T`)                             -- 10
EXTRACT(HOUR FROM `2017-01-02T03:04:05+07:08`)             -- 3
EXTRACT(MINUTE FROM `2017-01-02T03:04:05+07:08`)           -- 4
EXTRACT(TIMEZONE_HOUR FROM `2017-01-02T03:04:05+07:08`)    -- 7
EXTRACT(TIMEZONE_MINUTE FROM `2017-01-02T03:04:05+07:08`)  -- 8
EXTRACT(YEAR FROM DATE '2010-01-01')                       -- 2010
EXTRACT(MONTH FROM DATE '2010-01-01')                      -- 1
EXTRACT(DAY FROM DATE '2010-01-01')                        -- 1
EXTRACT(HOUR FROM DATE '2010-01-01')                       -- 0
EXTRACT(MINUTE FROM DATE '2010-01-01')                     -- 0
EXTRACT(SECOND FROM DATE '2010-01-01')                     -- 0
EXTRACT(HOUR FROM TIME '23:12:59')                         -- 23
EXTRACT(MINUTE FROM TIME '23:12:59')                       -- 12
EXTRACT(SECOND FROM TIME '23:12:59')                       -- 59
EXTRACT(SECOND FROM TIME (2) '23:12:59.128')               -- 59.13
EXTRACT(YEAR FROM INTERVAL '1-2' YEAR TO MONTH)            -- 1
EXTRACT(MONTH FROM INTERVAL '1-2' YEAR TO MONTH)           -- 2
EXTRACT(DAY FROM INTERVAL '3 4:5:6.789' DAY TO SECOND)     -- 3
EXTRACT(HOUR FROM INTERVAL '3 4:5:6.789' DAY TO SECOND)    -- 4
EXTRACT(MINUTE FROM INTERVAL '3 4:5:6.789' DAY TO SECOND)  -- 5
EXTRACT(SECOND FROM INTERVAL '3 4:5:6.789' DAY TO SECOND)           -- 6.789
EXTRACT(SECOND FROM INTERVAL '3 4:5:6.789' DAY TO SECOND(2))        -- 6.78
EXTRACT(TIMEZONE_HOUR FROM TIME WITH TIME ZONE '23:12:59-08:30')    -- -8
EXTRACT(TIMEZONE_MINUTE FROM TIME WITH TIME ZONE '23:12:59-08:30')  -- -30

```
*Note* that `timezone_hour` and `timezone_minute` are **not supported** for `DATE` and `TIME` (without time zone) types, as well as for `INTERVAL` types which do not have timezone information.

### `FILTER_DISTINCT` -- since v0.7.0

Signature
: `FILTER_DISTINCT: Container -> Bag|List`

Header
: `FILTER_DISTINCT(c)`

Purpose
: Returns a bag or list of distinct values contained within a bag, list, sexp, or struct.  If the container is a struct,
the field names are not considered. A list will be returned if and only if the input is a list.

Examples
:

```sql
FILTER_DISTINCT([0, 0, 1])                  -- [0, 1]
FILTER_DISTINCT(<<0, 0, 1>>)                -- <<0, 1>>
FILTER_DISTINCT(SEXP(0, 0, 1))              -- <<0, 1>>
FILTER_DISTINCT({'a': 0, 'b': 0, 'c': 1})   -- <<0, 1>>
```

### LOWER  -- since v0.1.0

Given a string convert all upper case characters to lower case characters.

Signature
: `LOWER: String -> String`

Header
: `LOWER(s)`

Purpose
: Given a string, `s`, alter every upper case character in `s` to lower case. Any non-upper cased characters 
remain unchanged. This operation does rely on the locale specified by the runtime configuration. 
The implementation, currently, relies on Java's 
[String.toLowerCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toLowerCase()) documentation.

Examples
: 

```sql
LOWER('AbCdEfG!@#$') -- 'abcdefg!@#$'
```

### MAKE_DATE -- since v0.3.0

Given the integer values for the year, month and day returns the associated date.

Signature
: `MAKE_DATE: Year_Int Month_Int Day_Int -> Date`

where `Year_Int`, `Month_Int`, `Day_Int` are the Integers representing `year`, `month` and `day` respectively
 for the date.

Header
: `MAKE_DATE(year, month, day)`

Purpose
: Given integer values for `year`, `month` and `day`, returns an associated `Date`. This function allows arguments to be 
`unknown`s i.e. (`null` or `missing`).

Examples
:

```sql
MAKE_DATE(2021, 02, 28)                  -- 2021-02-28
MAKE_DATE(2020, 02, 29)                  -- 2020-02-29
MAKE_DATE(2021, 12, 31)                  -- 2021-12-31
MAKE_DATE(null, 02, 28)                  -- null
MAKE_DATE(2021, null, 28)                -- null
MAKE_DATE(2021, 02, null)                -- null
MAKE_DATE(missing, 02, 28)               -- null
MAKE_DATE(2021, missing, 28)             -- null
MAKE_DATE(2021, 02, missing)             -- null
```
### MAKE_TIME -- since v0.3.0

Given the values for the hour (int), minute (int), second (BigDecimal) and optionally for timezone minutes (int) returns the associated time.

Signature
: `MAKE_TIME: Hour_Int Minute_Int Second_BigDecimal (optional)TimezoneMinutes_Int -> Time`

where `Hour_Int`, `Minute_Int`, `TimezoneMinutes_Int` are the Integers representing `hour`, `minute` and `timezone minutes` respectively
 for the time. `Second_BigDecimal` is the BigDecimal representing the `second` with fraction of the time. 

Header
: `MAKE_TIME(hour, minute, second, timezoneMinutes?)`

Purpose
: Given values for `hour` (int), `minute` (int), `second` (BigDecimal) and optionally for `timezone minutes` (int), returns an associated `Time`. This function allows arguments to be 
`unknown`s i.e. (`null` or `missing`).

Examples
:

```sql
MAKE_TIME(23, 59, 59.)                   -- 23:59:59
MAKE_TIME(23, 59, 59.12345, 330)         -- 23:59:59.12345+05:30
MAKE_TIME(23, 59, 59.12345, -330)        -- 23:59:59.12345-05:30
MAKE_TIME(null, 02, 28.)                 -- null
MAKE_TIME(12, null, 28.)                 -- null
MAKE_TIME(21, 02, null)                  -- null
MAKE_TIME(missing, 02, 28.)              -- null
MAKE_TIME(23, missing, 59.)              -- null
MAKE_TIME(21, 02, missing)               -- null
MAKE_TIME(21, 02, 28., null)             -- null
MAKE_TIME(21, 02, 28., missing)          -- null
```

### SIZE -- since v0.1.0, CARDINALITY -- since v0.10.0

Given any container data type (i.e., list, structure or bag) return the number of elements in the container. 

Signature
: `SIZE: Container -> Integer`

Header
: `SIZE(c)`

Signature
: `CARDINALITY: Container -> Integer`

Header
: `CARDINALITY(c)`

Purpose 
: Given a container, `c`, return the number of elements in the container. 
If the input to `SIZE/CARDINALITY` is not a container 
the implementation throws an error. 

Examples
: 

```sql
SIZE(`[]`)                   -- 0
SIZE(`[null]`)               -- 1
SIZE(`[1,2,3]`)              -- 3
SIZE(<<'foo', 'bar'>>)       -- 2
SIZE(`{foo: bar}`)           -- 1 (number of key-value pairs)
SIZE(`[{foo: 1}, {foo: 2}]`) -- 2
SIZE(12)                     -- error
```

### NULLIF -- since v0.1.0

Given two expressions return `null` if the two expressions evaluate to the same value, else, returns the result of evaluating 
the first expression 

Signature 
: `NULLIF: Any Any -> Any`

Header
: `NULLIF(e1, e2)`

Purpose
: Given two expression, `e1` and `e2`, evaluate both expression to get `v1` and `v2` respectively, and return `null` if and only if 
`v1` equals `v2`, else, return `v1`. 
The implementation of `NULLIF` uses `=` for equality, i.e., `v1` and `v2` are considered equal by `NULLIF` if and only if `v1 = v2` is true. 


*Note*, `NULLIF` does **not** propagate unknowns (`null` and `missing`).

Examples
: 

```sql  
NULLIF(1, 1)             -- null
NULLIF(1, 2)             -- 1
NULLIF(1.0, 1)           -- null
NULLIF(1, '1')           -- 1
NULLIF([1], [1])         -- null
NULLIF(1, NULL)          -- 1
NULLIF(NULL, 1)          -- null
NULLIF(null, null)       -- null
NULLIF(missing, null)    -- null
NULLIF(missing, missing) -- null
```

### OCTET_LENGTH -- since v0.10.0

Returns the number of bytes in the input string.

Signature
: `OCTET_LENGTH: String —> Int`

Header
: `OCTET_LENGTH(str)`

Examples
: 

```sql
octet_length('jose') -- 4
```

### OVERLAY -- since v0.10.0

OVERLAY modifies a string argument by replacing a given substring of the string, which is specified by a given numeric
starting position and a given numeric length, with another string (called the replacement string). When the length of
the substring is zero, nothing is removed from the original string and the string returned by the
function is the result of inserting the replacement string into the original string at the starting position.

Signature
: `OVERLAY: String, String, Int —> String`

Header
: `OVERLAY(str1 PLACING str2 FROM pos)`

Signature
: `OVERLAY: String, String, Int, Int —> String`

Header
: `OVERLAY(str1 PLACING str2 FROM pos FOR for)`

Examples
: 

```sql
overlay('hello' placing '' from 1)              -- "hello
overlay('hello' placing '' from 2 for 3)         -- "ho
overlay('hello' placing '' from 2 for 4)        -- "h
overlay('hello' placing 'XX' from 1)            -- "XXllo
overlay('hello' placing 'XX' from 1 for 3)      -- "XXlo
overlay('hello' placing 'XX' from 1 for 1)      -- "XXello
overlay('hello' placing 'XX' from 1 for 100)    -- "XX
overlay('hello' placing 'XX' from 1 for 0)      -- "XXhello
overlay('hello' placing 'XX' from 7)            -- "helloXX
overlay('hello' placing 'XX' from 100 for 100)  -- "helloXX
overlay('hello' placing 'XX' from 2 for 1)      -- "hXXllo
overlay('hello' placing 'XX' from 2 for 3)      -- "hXXo
```

### POSITION -- since v0.10.0

Position determines the first position (counting from 1), if any, at which one string, str1, occurs within
another, str2. If str1 is of length zero, then it occurs at position 1 (one) for any value of str2. If str1
does not occur in str2, then zero is returned. The declared type of a <position expression> is exact numeric

Signature
: `POSITION: String, String —> Int`

Header
: `POSITION(str1 IN str2)`

Header
: `POSITION(str1, str2)`

Examples
: 

```sql
position('foo' in 'hello')     -- 0
position('' in 'hello')        -- 1
position('h' in 'hello')       -- 1
position('o' in 'hello')       -- 5
position('ll' in 'hello')      -- 3
position('lo' in 'hello')      -- 4
position('hello' in 'hello')   -- 1
position('xx' in 'xxyyxxyy')   -- 1
position('yy' in 'xxyyxxyy')   -- 3
```
    
### SUBSTRING -- since v0.1.0

Given a string, a start index and optionally a length, returns the
substring from the start index up to the end of the string, or, up to
the length provided.

Signature 
: `SUBSTRING: String Integer [ NNegInteger ] -> String` 

Where `NNegInteger` is a non-negative integer, i.e., 0 or greater. 

Header
: `SUBSTRING(str, start [ , length ])` 

`SUBSTRING(str FROM start [ FOR  length ])` 

Purpose 
: Given a string, `str`, a start position, `start` and optionally a length, `length`, 
extract the characters (code points) starting at index `start` and ending at (`start` + `length`) - 1. 
If `length` is omitted, then proceed till the end of `str`. 

The first character of `str` has index 1. 

Examples
: 

```sql
SUBSTRING("123456789", 0)      -- "123456789"
SUBSTRING("123456789", 1)      -- "123456789"
SUBSTRING("123456789", 2)      -- "23456789"
SUBSTRING("123456789", -4)     -- "123456789"
SUBSTRING("123456789", 0, 999) -- "123456789" 
SUBSTRING("123456789", 0, 2)   -- "1"
SUBSTRING("123456789", 1, 999) -- "123456789"
SUBSTRING("123456789", 1, 2)   -- "12"
SUBSTRING("1", 1, 0)           -- ""
SUBSTRING("1", 1, 0)           -- ""
SUBSTRING("1", -4, 0)          -- ""
SUBSTRING("1234", 10, 10)      -- ""
```

### TEXT_REPLACE -- since v0.10.0

In `string`, replaces all occurrences of substring `from` with another string `to`.

Signature
: `TEXT_REPLACE: String, String, String -> String`

Header
: `TEXT_REPLACE(string, from, to)`

Examples
:

```sql
text_replace('abcdefabcdef', 'cd', 'XX')       -- 'abXXefabXXef'
text_replace('abcdefabcdef', 'xyz', 'XX')      -- 'abcdefabcdef'
text_replace('abcdefabcdef', 'defab', '')      -- 'abccdef'
text_replace('abcabcabcdef', 'abcabc', 'XXX')  -- 'XXXabcdef'
text_replace('abcabcabcdef', '', 'X')          -- 'XaXbXcXaXbXcXaXbXcXdXeXfX'
text_replace('', 'abc', 'XX')                  -- ''
text_replace('', '', 'XX')                     -- 'XX'
```
    
### TO_STRING -- since v0.1.0

Given a timestamp and a format pattern return a string representation of the timestamp in the given format. 

Signature
: `TO_STRING: Timestamp TimeFormatPattern -> String`

Where `TimeFormatPattern` is a String with the following special character interpretations 

 | Format           | Example     | Description
 |:-----------------|:------------|:--------------------------------------------------------------------------------
 | `yy`             | 69          | 2-digit year
 | `y`              | 1969        | 4-digit year
 | `yyyy`           | 1969        | Zero padded 4-digit year
 | `M`              | 1           | Month of year
 | `MM`             | 01          | Zero padded month of year
 | `MMM`            | Jan         | Abbreviated month year name
 | `MMMM`           | January     | Full month of year name
 | `MMMMM`          | J           | Month of year first letter (NOTE: not valid for use with to_timestamp function)
 | `d`              | 2           | Day of month (1-31)
 | `dd`             | 02          | Zero padded day of month (01-31)
 | `a`              | AM          | AM or PM of day
 | `h`              | 3           | Hour of day (1-12)
 | `hh`             | 03          | Zero padded hour of day (01-12)
 | `H`              | 3           | Hour of day (0-23)
 | `HH`             | 03          | Zero padded hour of day (00-23)
 | `m`              | 4           | Minute of hour (0-59)
 | `mm`             | 04          | Zero padded minute of hour (00-59)
 | `s`              | 5           | Second of minute (0-59)
 | `ss`             | 05          | Zero padded second of minute (00-59)
 | `S`              | 0           | Fraction of second (precision: 0.1, range: 0.0-0.9)
 | `SS`             | 06          | Fraction of second (precision: 0.01, range: 0.0-0.99)
 | `SSS`            | 060         | Fraction of second (precision: 0.001, range: 0.0-0.999)
 | ...              | ...         | ...
 | `SSSSSSSSS`      | 060000000   | Fraction of second (maximum precision: 1 nanosecond, range: 0.0-0.999999999)
 | `n`              | 60000000    | Nano of second
 | `X`              | +07 or Z    | Offset in hours or "Z" if the offset is 0
 | `XX` or `XXXX`   | +0700 or Z  | Offset in hours and minutes or "Z" if the offset is 0
 | `XXX` or `XXXXX` | +07:00 or Z | Offset in hours and minutes or "Z" if the offset is 0
 | `x`              | +07         | Offset in hours
 | `xx` or `xxxx`   | +0700       | Offset in hours and minutes
 | `xxx` or `xxxxx` | +07:00      | Offset in hours and minutes
 


Header
: `TO_STRING(t,f)` 

Purpose
: Given a timestamp, `t`, and a format pattern, `f`, as a String, return a string representation of `t`
 in format `f`.

Examples
: 

```sql
TO_STRING(`1969-07-20T20:18Z`,  'MMMM d, y')                    -- "July 20, 1969"
TO_STRING(`1969-07-20T20:18Z`, 'MMM d, yyyy')                   -- "Jul 20, 1969"
TO_STRING(`1969-07-20T20:18Z`, 'M-d-yy')                        -- "7-20-69"
TO_STRING(`1969-07-20T20:18Z`, 'MM-d-y')                        -- "07-20-1969"
TO_STRING(`1969-07-20T20:18Z`, 'MMMM d, y h:m a')               -- "July 20, 1969 8:18 PM"
TO_STRING(`1969-07-20T20:18Z`, 'y-MM-dd''T''H:m:ssX')           -- "1969-07-20T20:18:00Z"
TO_STRING(`1969-07-20T20:18+08:00Z`, 'y-MM-dd''T''H:m:ssX')     -- "1969-07-20T20:18:00Z"
TO_STRING(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXX')   -- "1969-07-20T20:18:00+0800"
TO_STRING(`1969-07-20T20:18+08:00`, 'y-MM-dd''T''H:m:ssXXXXX')  -- "1969-07-20T20:18:00+08:00"
```
       
    
### TO_TIMESTAMP -- since v0.1.0

Given a string convert it to a timestamp. This is the inverse operation of [`TO_STRING`](#to_string)


Signature 
: `TO_TIMESTAMP: String [ TimeFormatPattern ] -> Timestamp`

See definition of `TimeFormatPattern` in [TO_STRING](#to_string). 

Header 
: `TO_TIMESTAMP(str[ , f ])` 

Purpose 
: Given a string, `str`, and an optional format pattern, `f`, as a String return a timestamp 
whose values are extracted from `str` using `f`. 

If the `<format pattern>` argument is omitted, `<string>` is assumed to be in the format of a 
[standard Ion timestamp](https://amzn.github.io/ion-docs/spec.html#timestamp).  This is the only recommended 
way to parse an Ion timestamp using this function.

Zero padding is optional when using a single format symbol (e.g. `y`, `M`, `d`, `H`, `h`, `m`, `s`) but required
for their zero padded variants (e.g. `yyyy`, `MM`, `dd`, `HH`, `hh`, `mm`, `ss`).

Special treatment is given to 2-digit years (format symbol `yy`).  1900 is added to values greater than or equal to 70
and 2000 is added to values less than 70.

Month names and AM/PM specifiers are case-insensitive.  

Examples
: 

Single argument parsing an Ion timestamp:
```sql
TO_TIMESTAMP('2007T')                         -- `2007T`
TO_TIMESTAMP('2007-02-23T12:14:33.079-08:00') -- `2007-02-23T12:14:33.079-08:00`
TO_TIMESTAMP('2016', 'y')                     -- `2016T`
TO_TIMESTAMP('2016', 'yyyy')                  -- `2016T`
TO_TIMESTAMP('02-2016', 'MM-yyyy')            -- `2016-02T`
TO_TIMESTAMP('Feb 2016', 'MMM yyyy')          -- `2016-02T`
TO_TIMESTAMP('Febrary 2016', 'MMMM yyyy')     -- `2016-02T`
```
Notes:

[All issues for PartiQL's `TO_TIMESTAMP` function](https://github.com/partiql/partiql-lang-kotlin/issues?utf8=%E2%9C%93&q=is%3Aissue+is%3Aopen+TO_TIMESTAMP+).

Internally, this is implemented with Java 8's `java.time` package.  There are a few differences between Ion's 
timestamp and the `java.time` package that create a few hypothetically infrequently encountered caveats that do not 
really have good workarounds at this time.    

- The Ion specification allows for explicitly signifying an unknown timestamp with a negative zero offset 
(i.e. the `-00:00` at the end of `2007-02-23T20:14:33.079-00:00`) but Java 8's `DateTimeFormatter` doesn't recognize 
this. **Hence, unknown offsets specified in this manner will be parsed as if they had an offset of `+00:00`, i.e. UTC.** 
To avoid this issue when parsing Ion formatted timestamps, use the single argument variant of `TO_TIMESTAMP`.  There 
is no workaround for custom format patterns at this time.
- `DateTimeFormatter` is capable of parsing UTC offsets to the precision of seconds, but Ion Timestamp's precision for 
offsets is minutes. TimestampParser currently handles this by throwing an exception when an attempt is made to parse a 
timestamp with an offset that does does not land on a minute boundary.  For example, parsing this timestamp would 
throw an exception:  `May 5, 2017 8:52pm +08:00:01` while `May 5, 2017 8:52pm +08:00:00` would not. 
- Ion Java's Timestamp allows specification of offsets up to +/- 23:59, while an exception is thrown by 
`DateTimeFormatter` for any attempt to parse an offset greater than +/- 18:00.  For example, attempting to parse: 
`May 5, 2017 8:52pm +18:01` would cause and exception to be thrown.  (Note: the Ion specification does 
indicate minimum and maximum allowable values for offsets.) In practice this will not be an issue for systems that do 
not abuse the offset portion of Timestamp because real-life offsets do not exceed +/- 12h.  


### TRIM -- since v0.1.0
 
Trims leading and/or trailing characters from a String. 


Signature
: `TRIM: [ String ] String -> String` 

Header
: `TRIM([[LEADING|TRAILING|BOTH r] FROM] str)`

Purpose
: Given a string, `str`, and an optional *set* of characters to remove, `r`, specified as a string, return the string 
with any character from `r` found at the beginning or end of `str` removed. 

If `r` is not provided it defaults to `' '`. 

Examples
: 

```sql
TRIM('       foobar         ')               -- 'foobar'
TRIM('      \tfoobar\t         ')            -- '\tfoobar\t'
TRIM(LEADING FROM '       foobar         ')  -- 'foobar         '
TRIM(TRAILING FROM '       foobar         ') -- '       foobar'
TRIM(BOTH FROM '       foobar         ')     -- 'foobar'
TRIM(BOTH '😁' FROM '😁😁foobar😁😁')         -- 'foobar'
TRIM(BOTH '12' FROM '1112211foobar22211122') -- 'foobar'
```

### UPPER -- since v0.1.0

Given a string convert all lower case characters to upper case characters.

Signature
: `UPPER: String -> String`

Header
: `UPPER(str)` 

Purpose
: Given a string, `str`, alter every upper case character is `str` to lower case. Any non-lower cases characters remain 
unchanged.  This operation does rely on the locale specified by the runtime configuration. 
The implementation, currently, relies on Java's 
[String.toLowerCase()](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#toLowerCase()) documentation.

Examples
: 

```sql
UPPER('AbCdEfG!@#$') -- 'ABCDEFG!@#$'
```    

### UTCNOW -- since v0.1.0

Returns the current time in UTC as a timestamp. 

Signature
: `UTCNOW:  -> Timestamp`

Header
: `UTCNOW()`

Purpose
: Return the current time in UTC as a timestamp 

Examples
: 

```sql
UTCNOW() -- 2017-10-13T16:02:11.123Z 
```

### UNIX_TIMESTAMP -- since v0.2.6

With no `timestamp` argument, returns the number of seconds since the last epoch ('1970-01-01 00:00:00' UTC).

With a `timestamp` argument, returns the number of seconds from the last epoch to the given `timestamp` 
(possibly negative).

Signature 
: `UNIX_TIMESTAMP: [Timestamp] -> Integer|Decimal`

Header 
: `UNIX_TIMESTAMP([timestamp])`

Purpose 
: `UNIX_TIMESTAMP()` called without a `timestamp` argument returns the number of whole seconds since the last 
epoch ('1970-01-01 00:00:00' UTC) as an Integer using `UTCNOW`.

`UNIX_TIMESTAMP()` called with a `timestamp` argument returns returns the number of seconds from the last epoch to the 
`timestamp` argument.
If given a `timestamp` before the last epoch, `UNIX_TIMESTAMP` will return the number of seconds before the last epoch as a negative 
number.
The return value will be a Decimal if and only if the given `timestamp` has a fractional seconds part.

Examples
: 

```sql
UNIX_TIMESTAMP()                            -- 1507910531 (if current time is `2017-10-13T16:02:11Z`; # of seconds since last epoch as an Integer)
UNIX_TIMESTAMP(`2020T`)                     -- 1577836800 (seconds from 2020 to the last epoch as an Integer)
UNIX_TIMESTAMP(`2020-01T`)                  -- ''
UNIX_TIMESTAMP(`2020-01-01T`)               -- ''
UNIX_TIMESTAMP(`2020-01-01T00:00Z`)         -- ''
UNIX_TIMESTAMP(`2020-01-01T00:00:00Z`)      -- ''
UNIX_TIMESTAMP(`2020-01-01T00:00:00.0Z`)    -- 1577836800. (seconds from 2020 to the last epoch as a Decimal)
UNIX_TIMESTAMP(`2020-01-01T00:00:00.00Z`)   -- ''
UNIX_TIMESTAMP(`2020-01-01T00:00:00.000Z`)  -- ''
UNIX_TIMESTAMP(`2020-01-01T00:00:00.100Z`)  -- 1577836800.1
UNIX_TIMESTAMP(`1969T`)                     -- -31536000 (timestamp is before last epoch)
```

### FROM_UNIXTIME -- since v0.2.6

Converts the given unix epoch into a timestamp.

Signature 
: `FROM_UNIXTIME: Integer|Decimal -> Timestamp`

Header 
: `FROM_UNIXTIME(unix_timestamp)`

Purpose 
: When given a non-negative numeric value, returns a timestamp after the last epoch.
When given a negative numeric value, returns a timestamp before the last epoch.
The returned timestamp has fractional seconds depending on if the value is a decimal.

Examples
: 

```sql
FROM_UNIXTIME(-1)           -- `1969-12-31T23:59:59-00:00`      (negative unix_timestamp; returns timestamp before last epoch)
FROM_UNIXTIME(-0.1)         -- `1969-12-31T23:59:59.9-00:00`    (unix_timestamp is decimal so timestamp has fractional seconds)
FROM_UNIXTIME(0)            -- `1970-01-01T00:00:00.000-00:00`
FROM_UNIXTIME(0.001)        -- `1970-01-01T00:00:00.001-00:00`  (decimal precision to fractional second precision) 
FROM_UNIXTIME(0.01)         -- `1970-01-01T00:00:00.01-00:00`
FROM_UNIXTIME(0.1)          -- `1970-01-01T00:00:00.1-00:00`
FROM_UNIXTIME(1)            -- `1970-01-01T00:00:01-00:00`
FROM_UNIXTIME(1577836800)   -- `2020-01-01T00:00:00-00:00`      (unix_timestamp is Integer so no fractional seconds)
```

## Math Functions

### CEIL / CEILING -- since v0.7.0

Returns the nearest integer greater than or equal to the input.

Signature 
: `CEIL/CEILING: Numeric -> Numeric`

Examples
: 

```sql
CEIL(1.1) = 2
CEIL(-42.8) = -42
CEIL(`+inf`) = `+inf` -- Float
CEIL(`-inf`) = `-inf` -- Float
CEIL(`nan`) = `nan` -- Float
```

### FLOOR -- since v0.10.0

Returns the nearest integer less than or equal to the input.

Signature 
: `FLOOR: Numeric -> Numeric`

Examples
: 

```sql
FLOOR(1.1) = 1
FLOOR(-42.8) = -43
FLOOR(`+inf`) = `+inf` -- Float
FLOOR(`-inf`) = `-inf` -- Float
FLOOR(`nan`) = `nan` -- Float
```

### ABS -- since v0.10.0

Returns the absolute value of the given number. 

Note that abs(n) will throw an EVALUATOR_INTEGER_OVERFLOW when n is both of type `INT` and n = `INT.MIN_VALUE`.

Signature 
: `ABS: Numeric -> Numeric`

| Number Type     | Result Type |
|-----------------|-------------|
| INT             | INT         |
| FLOAT           | FLOAT       | 
| DECIMAL         | DECIMAL     |
| +inf, nan, -inf | FLOAT       |

Examples
: 

```sql
abs(-4) = 4
abs(-4.0) = 4.0
abs(`-4e0`) = 4e0
abs(`-inf`) = `+inf`
abs(`nan`) = `nan`
```

### MOD -- since v0.10.0

MOD operates on two exact numeric arguments with scale 0 (zero) and returns
the modulus (remainder) of the first argument divided by the second argument as an exact numeric with scale 0 (zero).

If the second argument is zero, an EVALUATOR_ARITHMETIC_EXCEPTION will be thrown.

Signature 
: `MOD: Int, Int -> Int`

Examples
: 

```sql
mod(1, 1)      -- 0
mod(10, 1)     -- 0
mod(17, 1)     -- 0
mod(-17, 4)    -- -1
mod(17, -4)    -- 1
mod(10, 3)     -- 1
mod(17, 1)     -- 0
mod(17, 3)     -- 2
```

### SQRT -- since v0.10.0

Returns the square root of the given number.

The input number is required to be non-negative.

Signature 
: `SQRT: Numeric -> Numeric`

| Number Type | Result Type |
|-------------|-------------|
| INT         | FLOAT       |
| FLOAT       | FLOAT       | 
| DECIMAL     | DECIMAL     |
| +inf, nan   | FLOAT       |

Examples
: 

```sql
sqrt(4) = `2e0` 
sqrt(4.0) = 2.0000000000000000000000000000000000000 -- DECIMAL
sqrt(`+inf`) = `+inf`
sqrt(`nan`) = `nan`
```

### LN -- since v0.10.0

Returns the natural log of the given number.

Signature 
: `LN: Numeric -> Numeric`

The input number is required to be a positive number, otherwise an EVALUATOR_ARITHMETIC_EXCEPTION will be thrown.

Special cases:

ln(NaN) is NaN

ln(+Inf) is +Inf


| Number Type | Result Type |
|-------------|-------------|
| INT         | FLOAT       |
| FLOAT       | FLOAT       | 
| DECIMAL     | DECIMAL     |
| +inf, nan   | FLOAT       |

Examples
: 

```sql
ln(2) = `0.6931471805599453e0`
ln(2.0) = 0.69314718055994530941723212145817656808
ln(`+inf`) = `+inf`
ln(`nan`) = `nan`
```

### EXP -- since v0.10.0

Returns e^x for a given x.

Signature 
: `SQRT: Numeric -> Numeric`

Special Case: 

exp(NaN) is NaN

exp(+Inf) is +Inf

exp(-Inf) is 0.0

| Number Type    | Result Type |
|----------------|-------------|
| INT            | FLOAT       |
| FLOAT          | FLOAT       | 
| DECIMAL        | DECIMAL     |
| +inf, nan, -inf | FLOAT       |

Examples
: 

```sql
exp(1) = `2.718281828459045e0`
exp(1.0) = 2.7182818284590452353602874713526624978 -- DECIMAL
exp(`+inf`) = `inf` 
exp(`-inf`) = `0e0` 
exp(`nan`) = `nan`
```

### POWER -- since v0.10.0

POW(x,y) return x^y.

Note that if x is a negative number, than y must be an integer value, (not necessarily integer type), otherwise an EVALUATOR_ARITHMETIC_EXCEPTION will be thrown. 

Special Case: 

pow(x, 0.0) is 1.0;

pow(x, 1.0) == x;

pow(x, NaN) is NaN;

pow(NaN, x) is NaN for x != 0.0;

pow(x, Inf) is NaN for abs(x) == 1.0

Signature 
: `POWER: (Numeric, Numeric) -> Numeric`


|          x Type           |          y Type           | Result Type |
|:-------------------------:|:-------------------------:|:-----------:| 
|            INT            |            INT            |    FLOAT    | 
|            INT            |           FLOAT           |    FLOAT    |
|            INT            |          DECIMAL          |   DECIMAL   | 
|           FLOAT           |            INT            |    FLOAT    |
|           FLOAT           |           FLOAT           |    FLOAT    | 
|           FLOAT           |          DECIMAL          |   DECIMAL   |
|          DECIMAL          |            INT            |   DECIMAL   |
|          DECIMAL          |           FLOAT           |   DECIMAL   |
|          DECIMAL          |          DECIMAL          |   DECIMAL   |
| `+inf` or `-inf` or `nan` |          Numeric          |    FLOAT    |
|          NUMERIC          | `+inf` or `-inf` or `nan` |    FLOAT    |


Examples
: 

```sql
pow(2,2) = `4e0`
pow(2,`2e0`) = `4e0`
pow(2, 2.0) = 4.0000000000000000000000000000000000000
pow(`2e0`, 2) =  `4e0`
pow(`2e0`, `2e0`) =  `4e0`
pow(`2e0`, 2.0) = 4.0000000000000000000000000000000000000
pow(2.0, 2) = 4.0000000000000000000000000000000000000
pow(2.0, `2e0`) = 4.0000000000000000000000000000000000000
pow(2.0, 2.0) = 4.0000000000000000000000000000000000000
-- special rule
pow(`+inf`, 0) = `1e0`
pow(`+inf`, 1) = `+inf`
pow(`+inf`, `nan`) = `nan`;  
pow(`nan`, 0) = `1e0`
pow(`nan`, 1) = `nan`
pow(1, `+inf`) = `nan`
```

<!--
This is the template for writing documentations for an PartiQL built-in function. 

There are 6 parts to a function's documentation 

1. Function name with the first release version (exclude -SNAPSHOT)
1. One sentence statement -- much like the first sentence of a Java method's Javadoc
1. Signature -- the type signature of the function should use data types already defined on define a local data type using a WHERE clause 
1. Header -- function with formal parameters only that we will use in the next step 
1. Purpose -- english explanation of what the function does referring to formal argument names from Header. State any pre- and post-conditions as well as any exceptions to default behaviour e.g., propagation of unknowns `null` and `missing` 
1. Examples -- examples and their expected results. Make sure the examples cover the preceding explanations and **always** include examples for uncommon behaviour 

Here is an example for the imaginary function `add` 

### ADD -- since vX.Y.Z

Given 1 or more values return ther sum 

Signature
: `ADD: PosInt PosInt -> PosInt`

where `PosInt` is a positive `Integer`

Header
: `ADD(v1, v2 ... vn)`

Purpose
: Given 1 or more values `v1 .. vn` return their sum. The summation proceeds from left-to-right. If any of the values passed is **not** a `PosInt` the 
function returns the current sum up to that point. 
  
Examples
: 

```sql  
ADD(1)         -- 1 (wrap extra explanations with parens)
ADD(1,2)       -- 3
ADD(1,2,"a",3) -- 3
ADD()          -- 0
ADD("a")       -- 0
```

-->

