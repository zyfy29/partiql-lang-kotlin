package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.value.PartiQLValue
import org.partiql.value.bagValue
import org.partiql.value.boolValue
import org.partiql.value.decimalValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import java.math.BigDecimal

/**
 * This holds sanity tests during the development of the [PartiQLCompiler.standard] implementation.
 */
class PartiQLEvaluatorTest {

    @ParameterizedTest
    @MethodSource("sanityTestsCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sanityTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("typingModeTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun typingModeTests(tc: TypingTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("subqueryTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun subqueryTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("aggregationTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun aggregationTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("joinTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun joinTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("globalsTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun globalsTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("castTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun castTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalAbsTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalAbsTests(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun castTestCases() = listOf(
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(10, 5));
                """.trimIndent(),
                expected = decimalValue(BigDecimal.valueOf(2000000, 5))
            ),
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(10, 3));
                """.trimIndent(),
                expected = decimalValue(BigDecimal.valueOf(20000, 3))
            ),
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(2, 0));
                """.trimIndent(),
                expected = decimalValue(BigDecimal.valueOf(20, 0))
            ),
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(1, 0));
                """.trimIndent(),
                expected = missingValue(),
                mode = Mode.PERMISSIVE()
            ),
            SuccessTestCase(
                input = """
                    1 + 2.0
                """.trimIndent(),
                expected = decimalValue(BigDecimal.valueOf(30, 1))
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t * 100 FROM <<0, 1, 2.0, 3.0>> AS t;",
                expected = bagValue(
                    int32Value(0),
                    int32Value(100),
                    decimalValue(BigDecimal.valueOf(2000, 1)),
                    decimalValue(BigDecimal.valueOf(3000, 1)),
                )
            ),
            // TODO: Use Datum for assertions. Currently, PartiQLValue doesn't support parameterized CHAR/VARCHAR
//            SuccessTestCase(
//                input = """
//                    CAST(20 AS CHAR(2));
//                """.trimIndent(),
//                expected = charValue("20"),
//            ),
        )

        @JvmStatic
        fun globalsTestCases() = listOf(
            SuccessTestCase(
                input = """
                    SELECT VALUE t.a
                    FROM t;
                """.trimIndent(),
                expected = bagValue(
                    int64Value(1),
                    int64Value(2),
                ),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """
                            [
                                { "a": 1 },
                                { "a": 2 }
                            ]
                        """
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t1.a
                    FROM t AS t1, t AS t2;
                """.trimIndent(),
                expected = bagValue(
                    int64Value(1),
                    int64Value(1),
                    int64Value(2),
                    int64Value(2),
                ),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """
                            [
                                { "a": 1 },
                                { "a": 2 }
                            ]
                        """
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT o.name AS orderName,
                        (SELECT c.name FROM customers c WHERE c.id=o.custId) AS customerName
                    FROM orders o
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "orderName" to stringValue("foo")
                    ),
                    structValue(
                        "orderName" to stringValue("bar"),
                        "customerName" to stringValue("Helen")
                    ),
                ),
                globals = listOf(
                    Global(
                        name = "customers",
                        value = """
                            [{id:1, name: "Mary"},
                            {id:2, name: "Helen"},
                            {id:1, name: "John"}
                            ]
                        """
                    ),
                    Global(
                        name = "orders",
                        value = """
                            [{custId:1, name: "foo"},
                            {custId:2, name: "bar"}
                            ]
                        """
                    ),
                )
            ),
        )

        @JvmStatic
        fun joinTestCases() = listOf(
            // LEFT OUTER JOIN -- Easy
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    LEFT OUTER JOIN << 0, 2, 3 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue(
                    listValue(int32Value(0), int32Value(0)),
                    listValue(int32Value(1), int32Value(null)),
                    listValue(int32Value(2), int32Value(2)),
                )
            ),
            // LEFT OUTER JOIN -- RHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM
                        << 0, 1, 2 >> lhs
                    LEFT OUTER JOIN (
                        SELECT VALUE n
                        FROM << 0, 2, 3 >> AS n
                        WHERE n > 100
                    ) rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue(
                    listValue(int32Value(0), int32Value(null)),
                    listValue(int32Value(1), int32Value(null)),
                    listValue(int32Value(2), int32Value(null)),
                )
            ),
            // LEFT OUTER JOIN -- LHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM <<>> lhs
                    LEFT OUTER JOIN << 0, 2, 3>> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue<PartiQLValue>()
            ),
            // LEFT OUTER JOIN -- No Matches
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    LEFT OUTER JOIN << 3, 4, 5 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue(
                    listValue(int32Value(0), int32Value(null)),
                    listValue(int32Value(1), int32Value(null)),
                    listValue(int32Value(2), int32Value(null)),
                )
            ),
            // RIGHT OUTER JOIN -- Easy
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    RIGHT OUTER JOIN << 0, 2, 3 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue(
                    listValue(int32Value(0), int32Value(0)),
                    listValue(int32Value(2), int32Value(2)),
                    listValue(int32Value(null), int32Value(3)),
                )
            ),
            // RIGHT OUTER JOIN -- RHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    RIGHT OUTER JOIN <<>> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue<PartiQLValue>()
            ),
            // RIGHT OUTER JOIN -- LHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM (
                        SELECT VALUE n
                        FROM << 0, 1, 2 >> AS n
                        WHERE n > 100
                    ) lhs RIGHT OUTER JOIN
                        << 0, 2, 3>> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue<PartiQLValue>(
                    listValue(int32Value(null), int32Value(0)),
                    listValue(int32Value(null), int32Value(2)),
                    listValue(int32Value(null), int32Value(3)),
                )
            ),
            // RIGHT OUTER JOIN -- No Matches
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    RIGHT OUTER JOIN << 3, 4, 5 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = bagValue(
                    listValue(int32Value(null), int32Value(3)),
                    listValue(int32Value(null), int32Value(4)),
                    listValue(int32Value(null), int32Value(5)),
                )
            ),
            // LEFT OUTER JOIN -- LATERAL
            SuccessTestCase(
                input = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    LEFT OUTER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                expected = bagValue(
                    int32Value(2),
                    int32Value(12),
                    int32Value(22),
                )
            ),
            // INNER JOIN -- LATERAL
            SuccessTestCase(
                input = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    INNER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                expected = bagValue(
                    int32Value(2),
                    int32Value(12),
                    int32Value(22),
                )
            ),
        )

        @JvmStatic
        fun subqueryTestCases() = listOf(
            SuccessTestCase(
                input = """
                    SELECT VALUE (
                        SELECT VALUE t1 + t2
                        FROM <<5, 6>> AS t2
                    ) FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = bagValue(
                    bagValue(int32Value(5), int32Value(6)),
                    bagValue(int32Value(15), int32Value(16))
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE (
                        SELECT t1 + t2
                        FROM <<5>> AS t2
                    ) FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = bagValue(int32Value(5), int32Value(15))
            ),
            SuccessTestCase(
                input = """
                    SELECT (
                        SELECT VALUE t1 + t2
                        FROM <<5>> AS t2
                    ) AS t1_plus_t2
                    FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = bagValue(
                    structValue("t1_plus_t2" to bagValue(int32Value(5))),
                    structValue("t1_plus_t2" to bagValue(int32Value(15)))
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT
                        (
                            SELECT (t1 + t2) * (
                                SELECT t1 + t3 + t2
                                FROM <<7>> AS t3
                            )
                            FROM <<5>> AS t2
                        ) AS t1_plus_t2
                    FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = bagValue(
                    structValue("t1_plus_t2" to int32Value(60)),
                    structValue("t1_plus_t2" to int32Value(330))
                )
            ),
            SuccessTestCase(
                input = """
                    1 + (SELECT t.a FROM << { 'a': 3 } >> AS t)
                """.trimIndent(),
                expected = int32Value(4)
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE element
                    FROM << { 'a': [0, 1, 2] }, { 'a': [3, 4, 5] } >> AS t, t.a AS element
                """.trimIndent(),
                expected = bagValue(
                    int32Value(0),
                    int32Value(1),
                    int32Value(2),
                    int32Value(3),
                    int32Value(4),
                    int32Value(5),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE element
                    FROM << { 'a': { 'c': [0, 1, 2] } }, { 'a': { 'c': [3, 4, 5] } } >> AS t, t.a AS b, b.c AS element
                """.trimIndent(),
                expected = bagValue(
                    int32Value(0),
                    int32Value(1),
                    int32Value(2),
                    int32Value(3),
                    int32Value(4),
                    int32Value(5),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c
                    FROM << { 'a': { 'b': [100, 200], 'c': [0, 1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4, 5] } } >>
                        AS t, t.a AS t_a, t_a.b AS t_a_b, t_a.c AS t_a_c
                """.trimIndent(),
                expected = bagValue(
                    int32Value(100),
                    int32Value(101),
                    int32Value(102),
                    int32Value(200),
                    int32Value(201),
                    int32Value(202),
                    int32Value(303),
                    int32Value(304),
                    int32Value(305),
                    int32Value(403),
                    int32Value(404),
                    int32Value(405),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c + t_a_c_original
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t, t.a AS t_a, t_a.b AS t_a_b, t_a.c AS t_a_c, t.a.c AS t_a_c_original
                """.trimIndent(),
                expected = bagValue(
                    int32Value(102),
                    int32Value(103),
                    int32Value(103),
                    int32Value(104),
                    int32Value(202),
                    int32Value(203),
                    int32Value(203),
                    int32Value(204),
                    int32Value(306),
                    int32Value(307),
                    int32Value(307),
                    int32Value(308),
                    int32Value(406),
                    int32Value(407),
                    int32Value(407),
                    int32Value(408),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c + t_a_c_original
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t, t.a AS t_a, t_a.b AS t_a_b, t_a.c AS t_a_c, (SELECT VALUE d FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = bagValue(
                    int32Value(102),
                    int32Value(103),
                    int32Value(103),
                    int32Value(104),
                    int32Value(202),
                    int32Value(203),
                    int32Value(203),
                    int32Value(204),
                    int32Value(306),
                    int32Value(307),
                    int32Value(307),
                    int32Value(308),
                    int32Value(406),
                    int32Value(407),
                    int32Value(407),
                    int32Value(408),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c + t_a_c_original
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t,
                        t.a AS t_a,
                        t_a.b AS t_a_b,
                        t_a.c AS t_a_c,
                        (SELECT VALUE d + (SELECT b_og FROM t.a.b AS b_og WHERE b_og = 200 OR b_og = 400) FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = bagValue(
                    int32Value(302),
                    int32Value(303),
                    int32Value(303),
                    int32Value(304),
                    int32Value(402),
                    int32Value(403),
                    int32Value(403),
                    int32Value(404),
                    int32Value(706),
                    int32Value(707),
                    int32Value(707),
                    int32Value(708),
                    int32Value(806),
                    int32Value(807),
                    int32Value(807),
                    int32Value(808),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE
                        t_a_b + t_a_c + t_a_c_original + (
                            SELECT t_a_c_inner
                            FROM t.a.c AS t_a_c_inner
                            WHERE t_a_c_inner = 2 OR t_a_c_inner = 4
                        )
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t,
                        t.a AS t_a,
                        t_a.b AS t_a_b,
                        t_a.c AS t_a_c,
                        (SELECT VALUE d + (SELECT b_og FROM t.a.b AS b_og WHERE b_og = 200 OR b_og = 400) FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = bagValue(
                    int32Value(304),
                    int32Value(305),
                    int32Value(305),
                    int32Value(306),
                    int32Value(404),
                    int32Value(405),
                    int32Value(405),
                    int32Value(406),
                    int32Value(710),
                    int32Value(711),
                    int32Value(711),
                    int32Value(712),
                    int32Value(810),
                    int32Value(811),
                    int32Value(811),
                    int32Value(812),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE
                        t_a_b + t_a_c + t_a_c_original + (
                            SELECT t_a_c_inner + t_a_c
                            FROM t.a.c AS t_a_c_inner
                            WHERE t_a_c_inner = 2 OR t_a_c_inner = 4
                        )
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t,
                        t.a AS t_a,
                        t_a.b AS t_a_b,
                        t_a.c AS t_a_c,
                        (SELECT VALUE d + (SELECT b_og + t_a_c FROM t.a.b AS b_og WHERE b_og = 200 OR b_og = 400) FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = bagValue(
                    int32Value(306),
                    int32Value(307),
                    int32Value(309),
                    int32Value(310),
                    int32Value(406),
                    int32Value(407),
                    int32Value(409),
                    int32Value(410),
                    int32Value(716),
                    int32Value(717),
                    int32Value(719),
                    int32Value(720),
                    int32Value(816),
                    int32Value(817),
                    int32Value(819),
                    int32Value(820),
                )
            )
        )

        @JvmStatic
        fun aggregationTestCases() = kotlin.collections.listOf(
            SuccessTestCase(
                input = """
                    SELECT VALUE { 'sensor': sensor,
                          'readings': (SELECT VALUE v.l.co FROM g AS v)
                    }
                    FROM [{'sensor':1, 'co':0.4}, {'sensor':1, 'co':0.2}, {'sensor':2, 'co':0.3}] AS l
                    GROUP BY l.sensor AS sensor GROUP AS g
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "sensor" to int32Value(1),
                        "readings" to bagValue(
                            decimalValue(0.4.toBigDecimal()),
                            decimalValue(0.2.toBigDecimal())
                        )
                    ),
                    structValue(
                        "sensor" to int32Value(2),
                        "readings" to bagValue(
                            decimalValue(0.3.toBigDecimal())
                        )
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT col1, g
                    FROM [{ 'col1':1 }, { 'col1':1 }] simple_1_col_1_group
                    GROUP BY col1 GROUP AS g
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "col1" to int32Value(1),
                        "g" to bagValue(
                            structValue(
                                "simple_1_col_1_group" to structValue("col1" to int32Value(1))
                            ),
                            structValue(
                                "simple_1_col_1_group" to structValue("col1" to int32Value(1))
                            ),
                        )
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT p.supplierId_mixed
                    FROM [
                        { 'productId': 5,  'categoryId': 21, 'regionId': 100, 'supplierId_nulls': null, 'price_nulls': null },
                        { 'productId': 4,  'categoryId': 20, 'regionId': 100, 'supplierId_nulls': null, 'supplierId_mixed': null, 'price_nulls': null, 'price_mixed': null }
                    ] AS p
                    GROUP BY p.supplierId_mixed
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "supplierId_mixed" to nullValue(),
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT *
                    FROM << { 'a': 1, 'b': 2 } >> AS t
                    GROUP BY a, b, a + b GROUP AS g
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to int32Value(2),
                        "_3" to int32Value(3),
                        "g" to bagValue(
                            structValue(
                                "t" to structValue(
                                    "a" to int32Value(1),
                                    "b" to int32Value(2),
                                )
                            )
                        ),
                    ),
                )
            ),
        )

        @JvmStatic
        fun sanityTestsCases() = listOf(
            SuccessTestCase(
                input = "SELECT VALUE 1 FROM <<0, 1>>;",
                expected = bagValue(int32Value(1), int32Value(1))
            ),
            SuccessTestCase(
                input = "SELECT VALUE t FROM <<10, 20, 30>> AS t;",
                expected = bagValue(int32Value(10), int32Value(20), int32Value(30))
            ),
            SuccessTestCase(
                input = "SELECT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t;",
                expected = bagValue(boolValue(true), boolValue(true))
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t, << { 'b': 2 } >> s;",
                expected = bagValue(structValue("a" to int32Value(1), "b" to int32Value(2)))
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t LEFT JOIN << { 'b': 2 } >> s ON false;",
                expected = bagValue(structValue("a" to int32Value(1), "b" to nullValue())),
                mode = Mode.STRICT()
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON false;",
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to nullValue()
                    ),
                    structValue(
                        "a" to nullValue(),
                        "b" to int32Value(2)
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        { 'b': TRUE },
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = structValue(
                    "a" to int32Value(1),
                    "b" to boolValue(true),
                    "c" to stringValue("hello")
                )
            ),
            SuccessTestCase(
                input = """
                    CASE
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN FALSE THEN 'isFalse'
                        WHEN TRUE THEN 'isTrue'
                    END
                    ;
                """.trimIndent(),
                expected = stringValue("isTrue")
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON TRUE;",
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to int32Value(2)
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        NULL,
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = structValue<PartiQLValue>(null)
            ),
            SuccessTestCase(
                input = """
                    CASE
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN FALSE THEN 'isFalse'
                    END
                    ;
                """.trimIndent(),
                expected = stringValue(null)
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        5,
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = missingValue()
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1, 'b': FALSE },
                        { 'b': TRUE },
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = structValue(
                    "a" to int32Value(1),
                    "b" to boolValue(false),
                    "b" to boolValue(true),
                    "c" to stringValue("hello")
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT * FROM
                    <<
                        { 'a': 1, 'b': FALSE }
                    >> AS t,
                    <<
                        { 'b': TRUE }
                    >> AS s
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to boolValue(false),
                        "b" to boolValue(true)
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE {
                        'a': 1,
                        'b': NULL,
                        t.c : t.d
                    }
                    FROM <<
                        { 'c': 'hello', 'd': 'world' }
                    >> AS t
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to nullValue(),
                        "hello" to stringValue("world")
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT v, i FROM << 'a', 'b', 'c' >> AS v AT i",
                expected = bagValue(
                    structValue(
                        "v" to stringValue("a"),
                    ),
                    structValue(
                        "v" to stringValue("b"),
                    ),
                    structValue(
                        "v" to stringValue("c"),
                    ),
                )
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t FROM <<true, false, true, false, false, false>> AS t;",
                expected = bagValue(boolValue(true), boolValue(false))
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t = TRUE;",
                expected = bagValue(boolValue(true))
            ),
            SuccessTestCase(
                input = "100 + 50;",
                expected = int32Value(150)
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t * 100 FROM <<0, 1, 2, 3>> AS t;",
                expected = bagValue(int32Value(0), int32Value(100), int32Value(200), int32Value(300))
            ),
            SuccessTestCase(
                input = """
                    PIVOT x.v AT x.k FROM << 
                        { 'k': 'a', 'v': 'x' },
                        { 'k': 'b', 'v': 'y' },
                        { 'k': 'c', 'v': 'z' }
                    >> AS x
                """.trimIndent(),
                expected = structValue(
                    "a" to stringValue("x"),
                    "b" to stringValue("y"),
                    "c" to stringValue("z"),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT t
                    EXCLUDE t.a.b
                    FROM <<
                        {'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'}
                    >> AS t
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "t" to structValue(
                            "a" to structValue<PartiQLValue>(
                                // field `b` excluded
                            ),
                            "foo" to stringValue("bar"),
                            "foo2" to stringValue("bar2")
                        )
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT *
                    EXCLUDE
                        t.a.b.c[*].field_x
                    FROM [{
                        'a': {
                            'b': {
                                'c': [
                                    {                    -- c[0]; field_x to be removed
                                        'field_x': 0,
                                        'field_y': 0
                                    },
                                    {                    -- c[1]; field_x to be removed
                                        'field_x': 1,
                                        'field_y': 1
                                    },
                                    {                    -- c[2]; field_x to be removed
                                        'field_x': 2,
                                        'field_y': 2
                                    }
                                ]
                            }
                        }
                    }] AS t
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "a" to structValue(
                            "b" to structValue(
                                "c" to listValue(
                                    structValue(
                                        "field_y" to int32Value(0)
                                    ),
                                    structValue(
                                        "field_y" to int32Value(1)
                                    ),
                                    structValue(
                                        "field_y" to int32Value(2)
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    CASE (1)
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN 2 THEN 'isTwo'
                    END
                    ;
                """.trimIndent(),
                expected = stringValue(null)
            ),
            SuccessTestCase(
                input = """
                    CASE (1)
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN 2 THEN 'isTwo'
                        WHEN 1 THEN 'isOne'
                    END
                    ;
                """.trimIndent(),
                expected = stringValue("isOne")
            ),
            SuccessTestCase(
                input = """
                    `null.bool` IS NULL
                """.trimIndent(),
                expected = boolValue(true)
            ),
            // SELECT * without nested coercion
            SuccessTestCase(
                input = """
                    SELECT *
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "first" to int32Value(3),
                        "second" to int32Value(5)
                    )
                )
            ),
            // SELECT list without nested coercion
            SuccessTestCase(
                input = """
                    SELECT "first", "second"
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = bagValue(
                    structValue(
                        "first" to int32Value(3),
                        "second" to int32Value(5)
                    )
                )
            ),
            // SELECT value without nested coercion
            SuccessTestCase(
                input = """
                    SELECT VALUE "first"
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = bagValue(
                    int32Value(3),
                )
            ),
            // TODO port `IS <boolean value>` tests to conformance tests
            // IS TRUE
            SuccessTestCase(
                input = "TRUE IS TRUE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "FALSE IS TRUE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "NULL IS TRUE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "MISSING IS TRUE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "'foo' IS TRUE",
                expected = Datum.missing(),
                mode = Mode.PERMISSIVE()
            ),
            // IS NOT TRUE
            SuccessTestCase(
                input = "TRUE IS NOT TRUE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "FALSE IS NOT TRUE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "NULL IS NOT TRUE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "MISSING IS NOT TRUE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "'foo' IS NOT TRUE",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS FALSE
            SuccessTestCase(
                input = "TRUE IS FALSE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "FALSE IS FALSE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "NULL IS FALSE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "MISSING IS FALSE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "'foo' IS FALSE",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS NOT FALSE
            SuccessTestCase(
                input = "TRUE IS NOT FALSE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "FALSE IS NOT FALSE;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "NULL IS NOT FALSE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "MISSING IS NOT FALSE;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "'foo' IS NOT FALSE",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS UNKNOWN
            SuccessTestCase(
                input = "TRUE IS UNKNOWN;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "FALSE IS UNKNOWN;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "NULL IS UNKNOWN;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "MISSING IS UNKNOWN;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "'foo' IS UNKNOWN",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS NOT UNKNOWN
            SuccessTestCase(
                input = "TRUE IS NOT UNKNOWN;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "FALSE IS NOT UNKNOWN;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "NULL IS NOT UNKNOWN;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "MISSING IS NOT UNKNOWN;",
                expected = boolValue(false)
            ),
            SuccessTestCase(
                input = "'foo' IS NOT UNKNOWN",
                expected = Datum.missing(),
                mode = Mode.PERMISSIVE()
            ),
            SuccessTestCase(
                input = "MISSING IS MISSING;",
                expected = boolValue(true)
            ),
            SuccessTestCase(
                input = "MISSING IS MISSING;",
                expected = boolValue(true),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                input = "SELECT VALUE t.a IS MISSING FROM << { 'b': 1 }, { 'a': 2 } >> AS t;",
                expected = bagValue(boolValue(true), boolValue(false))
            ),
            // PartiQL Specification Section 7.1.1 -- Equality
            SuccessTestCase(
                input = "5 = 'a';",
                expected = boolValue(false),
            ),
            // PartiQL Specification Section 7.1.1 -- Equality
            SuccessTestCase(
                input = "5 = 'a';",
                expected = boolValue(false), // TODO: Is this correct?
                mode = Mode.STRICT(),
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "NULL IS MISSING;",
                expected = boolValue(false),
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "NULL IS MISSING;",
                expected = boolValue(false),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': 10, 'b': 1}, {'a': 1, 'b': 2}>> AS t ORDER BY t.a;",
                expected = listValue(
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to int32Value(10), "b" to int32Value(1))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': 10, 'b': 1}, {'a': 1, 'b': 2}>> AS t ORDER BY t.a DESC;",
                expected = listValue(
                    structValue("a" to int32Value(10), "b" to int32Value(1)),
                    structValue("a" to int32Value(1), "b" to int32Value(2))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a NULLS LAST;",
                expected = listValue(
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to int32Value(3), "b" to int32Value(4)),
                    structValue("a" to nullValue(), "b" to int32Value(1))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a NULLS FIRST;",
                expected = listValue(
                    structValue("a" to nullValue(), "b" to int32Value(1)),
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to int32Value(3), "b" to int32Value(4))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a DESC NULLS LAST;",
                expected = listValue(
                    structValue("a" to int32Value(3), "b" to int32Value(4)),
                    structValue("a" to int32Value(1), "b" to int32Value(2)),
                    structValue("a" to nullValue(), "b" to int32Value(1))
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a DESC NULLS FIRST;",
                expected = listValue(
                    structValue("a" to nullValue(), "b" to int32Value(1)),
                    structValue("a" to int32Value(3), "b" to int32Value(4)),
                    structValue("a" to int32Value(1), "b" to int32Value(2))
                )
            ),
            SuccessTestCase( // use multiple sort specs
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 1, 'b': 4}>> AS t ORDER BY t.a DESC NULLS FIRST, t.b DESC;",
                expected = listValue(
                    structValue("a" to nullValue(), "b" to int32Value(1)),
                    structValue("a" to int32Value(1), "b" to int32Value(4)),
                    structValue("a" to int32Value(1), "b" to int32Value(2))
                )
            ),
            // PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (1)
            // According to the Specification, in permissive mode, functions/operators return missing when one of
            //  the parameters is missing.
            SuccessTestCase(
                input = "SELECT VALUE 5 + v FROM <<1, MISSING>> AS v;",
                expected = bagValue(int32Value(6), missingValue())
            ),
            // PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (1)
            // See https://github.com/partiql/partiql-tests/pull/118 for more information.
            SuccessTestCase(
                input = "SELECT VALUE 5 + v FROM <<1, MISSING>> AS v;",
                expected = bagValue(int32Value(6), missingValue()),
                mode = Mode.STRICT(),
            ),
        )

        @JvmStatic
        fun typingModeTestCases() = listOf(
            TypingTestCase(
                name = "Expected missing value in collection",
                input = "SELECT VALUE t.a FROM << { 'a': 1 }, { 'b': 2 } >> AS t;",
                expectedPermissive = bagValue(int32Value(1), missingValue())
            ),
            TypingTestCase(
                name = "Expected missing value in tuple in collection",
                input = "SELECT t.a AS \"a\" FROM << { 'a': 1 }, { 'b': 2 } >> AS t;",
                expectedPermissive = bagValue(
                    structValue(
                        "a" to int32Value(1),
                    ),
                    structValue(),
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 4.2 -- index negative",
                input = "[1,2,3][-1];",
                expectedPermissive = missingValue()
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 4.2 -- out of bounds",
                input = "[1,2,3][3];",
                expectedPermissive = missingValue()
            ),
            TypingTestCase(
                name = "PartiQL Spec Section 5.1.1 -- Position variable on bags",
                input = "SELECT v, p FROM << 5 >> AS v AT p;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to int32Value(5)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a scalar value",
                input = "SELECT v FROM 0 AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to int32Value(0)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a scalar value (with at)",
                input = "SELECT v, p FROM 0 AS v AT p;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to int32Value(0)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a tuple value",
                input = "SELECT v.a AS a FROM { 'a': 1 } AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "a" to int32Value(1)
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over an absent value (missing)",
                input = "SELECT v AS v FROM MISSING AS v;",
                expectedPermissive = bagValue(structValue<PartiQLValue>())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over an absent value (null)",
                input = "SELECT v AS v FROM NULL AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "v" to nullValue()
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing tuples",
                input = "SELECT VALUE {'a':v.a, 'b':v.b} FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = bagValue(
                    structValue(
                        "a" to int32Value(1),
                        "b" to int32Value(1),
                    ),
                    structValue(
                        "a" to int32Value(2),
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing bags (1)",
                input = "SELECT VALUE v.b FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = bagValue(
                    int32Value(1),
                    missingValue()
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing bags (2)",
                input = "SELECT VALUE <<v.a, v.b>> FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = bagValue(
                    bagValue(
                        int32Value(1),
                        int32Value(1),
                    ),
                    bagValue(
                        int32Value(2),
                        missingValue()
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.2 -- Pivoting a Collection into a Variable-Width Tuple",
                input = "PIVOT t.price AT t.\"symbol\" FROM [{'symbol':25, 'price':31.52}, {'symbol':'amzn', 'price':840.05}] AS t;",
                expectedPermissive = structValue(
                    "amzn" to decimalValue(BigDecimal.valueOf(840.05))
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (3)",
                input = "SELECT VALUE NOT v FROM << false, {'a':1} >> AS v;",
                expectedPermissive = bagValue(boolValue(true), missingValue())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (2)",
                input = "SELECT VALUE 5 > v FROM <<1, 'a'>> AS v;",
                expectedPermissive = bagValue(boolValue(true), missingValue())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 9.1",
                input = """
                    SELECT
                        o.name AS orderName,
                        (SELECT c.name FROM << { 'name': 'John', 'id': 1 }, { 'name': 'Alan', 'id': 1 } >> c WHERE c.id=o.custId) AS customerName
                        FROM << { 'name': 'apples', 'custId': 1 } >> o
                """.trimIndent(),
                expectedPermissive = bagValue(
                    structValue(
                        "orderName" to stringValue("apples")
                    )
                )
            )
        )

        @JvmStatic
        fun intervalAbsTestCases() = listOf(
            // Year-Month interval tests
            SuccessTestCase(
                input = "ABS(INTERVAL '1-6' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(1, 6, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '+1-6' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(1, 6, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1-6' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(1, 6, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100-6' YEAR(3) TO MONTH)",
                expected = Datum.intervalYearMonth(100, 6, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100-6' YEAR(3) TO MONTH)",
                expected = Datum.intervalYearMonth(100, 6, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '5' YEAR)",
                expected = Datum.intervalYear(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-5' YEAR)",
                expected = Datum.intervalYear(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100' MONTH(3))",
                expected = Datum.intervalMonth(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100' MONTH(3))",
                expected = Datum.intervalMonth(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '0-0' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(0, 0, 2)
            ),
            // Day-Time interval tests  
            SuccessTestCase(
                input = "ABS(INTERVAL '5' DAY)",
                expected = Datum.intervalDay(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-5' DAY)",
                expected = Datum.intervalDay(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100' DAY(3))",
                expected = Datum.intervalDay(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100' DAY(3))",
                expected = Datum.intervalDay(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '3' HOUR)",
                expected = Datum.intervalHour(3, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-3' HOUR)",
                expected = Datum.intervalHour(3, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '30' MINUTE)",
                expected = Datum.intervalMinute(30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-30' MINUTE)",
                expected = Datum.intervalMinute(30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '45.123' SECOND)",
                expected = Datum.intervalSecond(45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-45.123' SECOND)",
                expected = Datum.intervalSecond(45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '123.456789' SECOND(3,6))",
                expected = Datum.intervalSecond(123, 456789000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-123.456789' SECOND(3,6))",
                expected = Datum.intervalSecond(123, 456789000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '1 2' DAY TO HOUR)",
                expected = Datum.intervalDayHour(1, 2, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1 2' DAY TO HOUR)",
                expected = Datum.intervalDayHour(1, 2, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '1 2:30' DAY TO MINUTE)",
                expected = Datum.intervalDayMinute(1, 2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1 2:30' DAY TO MINUTE)",
                expected = Datum.intervalDayMinute(1, 2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100 10:30' DAY(3) TO MINUTE)",
                expected = Datum.intervalDayMinute(100, 10, 30, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100 10:30' DAY(3) TO MINUTE)",
                expected = Datum.intervalDayMinute(100, 10, 30, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '1 2:30:45.567' DAY TO SECOND)",
                expected = Datum.intervalDaySecond(1, 2, 30, 45, 567000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1 2:30:45.567' DAY TO SECOND)",
                expected = Datum.intervalDaySecond(1, 2, 30, 45, 567000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100 10:30:45.123456' DAY(3) TO SECOND(6))",
                expected = Datum.intervalDaySecond(100, 10, 30, 45, 123456000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100 10:30:45.123456' DAY(3) TO SECOND(6))",
                expected = Datum.intervalDaySecond(100, 10, 30, 45, 123456000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '2:30' HOUR TO MINUTE)",
                expected = Datum.intervalHourMinute(2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-2:30' HOUR TO MINUTE)",
                expected = Datum.intervalHourMinute(2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '2:30:45.789' HOUR TO SECOND)",
                expected = Datum.intervalHourSecond(2, 30, 45, 789000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-2:30:45.789' HOUR TO SECOND)",
                expected = Datum.intervalHourSecond(2, 30, 45, 789000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '30:45.123' MINUTE TO SECOND)",
                expected = Datum.intervalMinuteSecond(30, 45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-30:45.123' MINUTE TO SECOND)",
                expected = Datum.intervalMinuteSecond(30, 45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '0 0:0:0' DAY TO SECOND)",
                expected = Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)
            )
        )
    }

    @Test
    fun proveThatRowWorksWhenDynamic() {
        val tc =
            SuccessTestCase(
                input = "t.a = 3",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
                globals = listOf(
                    Global(
                        name = "t",
                        type = PType.dynamic(),
                        value = Datum.row(Field.of("a", Datum.integer(3)))
                    )
                )
            )
        tc.run()
    }

    @Test
    fun proveThatRowWorks() {
        val tc =
            SuccessTestCase(
                input = "t.a = 3",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
                globals = listOf(
                    Global(
                        name = "t",
                        type = PType.row(PTypeField.of("a", PType.integer())),
                        value = Datum.row(Field.of("a", Datum.integer(3)))
                    ),
                )
            )
        tc.run()
    }

    @Test
    // @Disabled
    fun developmentTest() {
        val tc =
            SuccessTestCase(
                input = """
                    non_existing_column = 1
                """.trimIndent(),
                expected = Datum.nullValue(PType.bool())
            )
        tc.run()
    }

    @Test
    @Disabled("We need to support section 5.1")
    fun testTypingOfPositionVariable() = TypingTestCase(
        name = "PartiQL Spec Section 5.1.1 -- Position variable on bags",
        input = "SELECT v, p FROM << 5 >> AS v AT p;",
        expectedPermissive = bagValue(
            structValue(
                "v" to int32Value(5)
            )
        )
    ).run()

    @Test
    @Disabled("This is just a placeholder. We should add support for this. Grouping is not yet supported.")
    fun test3() =
        TypingTestCase(
            name = "PartiQL Specification Section 11.1",
            input = """
                    PLACEHOLDER FOR THE EXAMPLE IN THE RELEVANT SECTION. GROUPING NOT YET SUPPORTED.
            """.trimIndent(),
            expectedPermissive = missingValue()
        ).run()

    @Test
    @Disabled("The planner fails this, though it should pass for permissive mode.")
    fun test5() =
        TypingTestCase(
            name = "PartiQL Specification Section 5.2.1 -- Mistyping Cases",
            input = "SELECT v, n FROM UNPIVOT 1 AS v AT n;",
            expectedPermissive = bagValue(
                structValue(
                    "v" to int32Value(1),
                    "n" to stringValue("_1")
                )
            )
        ).run()

    @Test
    @Disabled("We don't yet support arrays.")
    fun test7() =
        TypingTestCase(
            name = "PartiQL Specification Section 6.1.4 -- when constructing arrays",
            input = "SELECT VALUE [v.a, v.b] FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
            expectedPermissive = bagValue(
                listValue(
                    int32Value(1),
                    int32Value(1),
                ),
                listValue(
                    int32Value(2),
                    missingValue()
                )
            )
        ).run()

    @Test
    @Disabled("There is a bug in the planner which makes this always return missing.")
    fun test8() =
        TypingTestCase(
            name = "PartiQL Specification Section 4.2 -- non integer index",
            input = "SELECT VALUE [1,2,3][v] FROM <<1, 1.0>> AS v;",
            expectedPermissive = bagValue(int32Value(2), missingValue())
        ).run()

    @Test
    @Disabled("CASTs aren't supported yet.")
    fun test9() =
        TypingTestCase(
            name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 27",
            input = "SELECT VALUE {'a':3*v.a, 'b':3*(CAST (v.b AS INTEGER))} FROM [{'a':1, 'b':'1'}, {'a':2}] v;",
            expectedPermissive = bagValue(
                structValue(
                    "a" to int32Value(3),
                    "b" to int32Value(3),
                ),
                structValue(
                    "a" to int32Value(6),
                ),
            )
        ).run()

    @Test
    @Disabled("Arrays aren't supported yet.")
    fun test10() =
        SuccessTestCase(
            input = "SELECT v, i FROM [ 'a', 'b', 'c' ] AS v AT i",
            expected = bagValue(
                structValue(
                    "v" to stringValue("a"),
                    "i" to int64Value(0),
                ),
                structValue(
                    "v" to stringValue("b"),
                    "i" to int64Value(1),
                ),
                structValue(
                    "v" to stringValue("c"),
                    "i" to int64Value(2),
                ),
            )
        ).run()

    @Test
    @Disabled(
        """
            We currently do not have support for consolidating collections containing MISSING/NULL. The current
            result (value) is correct. However, the types are slightly wrong due to the SUM__ANY_ANY being resolved.
        """
    )
    fun aggregationOnLiteralBagOfStructs() = SuccessTestCase(
        input = """
            SELECT
                gk_0, SUM(t.c) AS t_c_sum
            FROM <<
                { 'b': NULL, 'c': 1 },
                { 'b': MISSING, 'c': 2 },
                { 'b': 1, 'c': 1 },
                { 'b': 1, 'c': 2 },
                { 'b': 2, 'c': NULL },
                { 'b': 2, 'c': 2 },
                { 'b': 3, 'c': MISSING },
                { 'b': 3, 'c': 2 },
                { 'b': 4, 'c': MISSING },
                { 'b': 4, 'c': NULL }
            >> AS t GROUP BY t.b AS gk_0;
        """.trimIndent(),
        expected = bagValue(
            structValue(
                "gk_0" to int32Value(1),
                "t_c_sum" to int32Value(3)
            ),
            structValue(
                "gk_0" to int32Value(2),
                "t_c_sum" to int32Value(2)
            ),
            structValue(
                "gk_0" to int32Value(3),
                "t_c_sum" to int32Value(2)
            ),
            structValue(
                "gk_0" to int32Value(4),
                "t_c_sum" to int32Value(null)
            ),
            structValue(
                "gk_0" to nullValue(),
                "t_c_sum" to int32Value(3)
            ),
        ),
        mode = Mode.PERMISSIVE()
    ).run()

    // PartiQL Specification Section 8
    @Test
    @Disabled("Currently, .check(<PartiQLValue>) is failing for MISSING. This will be resolved by Datum.")
    fun missingAndTruePermissive() =
        SuccessTestCase(
            input = "MISSING AND TRUE;",
            expected = boolValue(null),
        ).run()

    // PartiQL Specification Section 8
    @Test
    @Disabled("Currently, .check(<PartiQLValue>) is failing for MISSING. This will be resolved by Datum.")
    fun missingAndTrueStrict() = SuccessTestCase(
        input = "MISSING AND TRUE;",
        expected = boolValue(null), // TODO: Is this right?
        mode = Mode.STRICT()
    ).run()

    @Test
    @Disabled("Support for ORDER BY needs to be added for this to pass.")
    // PartiQL Specification says that SQL's SELECT is coerced, but SELECT VALUE is not.
    fun selectValueNoCoercion() =
        SuccessTestCase(
            input = """
                (4, 5) < (SELECT VALUE t.a FROM << { 'a': 3 }, { 'a': 4 } >> AS t ORDER BY t.a)
            """.trimIndent(),
            expected = boolValue(false)
        ).run()

    @Test
    @Disabled("This is appropriately coerced, but this test is failing because LT currently doesn't support LISTS.")
    fun rowCoercion() =
        SuccessTestCase(
            input = """
                (4, 5) < (SELECT t.a, t.a FROM << { 'a': 3 } >> AS t)
            """.trimIndent(),
            expected = boolValue(false)
        ).run()

    @Test
    @Disabled("This broke in its introduction to the codebase on merge. See 5fb9a1ccbc7e630b0df62aa8b161d319c763c1f6.")
    // TODO: Add to conformance tests
    fun wildCard() =
        SuccessTestCase(
            input = """
             [
               { 'id':'5',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': [{'name': 'John'}, {'name': 'Doe'}]
                   },
                   { 'title':'B',
                     'price':2.0,
                     'authors': [{'name': 'Zoe'}, {'name': 'Bill'}]
                   }
                 ]
               },
               { 'id':'6',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': [{'name': 'John'}, {'name': 'Doe'}]
                   },
                   { 'title':'E',
                     'price':2.0,
                     'authors': [{'name': 'Zoe'}, {'name': 'Bill'}]
                   }
                 ]
               },
               { 'id':7,
                 'books':[]
               }
             ][*].books[*].authors[*].name
            """.trimIndent(),
            expected = bagValue(
                listOf(
                    stringValue("John"), stringValue("Doe"), stringValue("Zoe"), stringValue("Bill"),
                    stringValue("John"), stringValue("Doe"), stringValue("Zoe"), stringValue("Bill")
                )
            )
        ).run()

    @Test
    @Disabled("This broke in its introduction to the codebase on merge. See 5fb9a1ccbc7e630b0df62aa8b161d319c763c1f6.")
    // TODO: add to conformance tests
    // Note that the existing pipeline produced identical result when supplying with
    // SELECT VALUE v2.name FROM e as v0, v0.books as v1, unpivot v1.authors as v2;
    // But it produces different result when supplying with e[*].books[*].authors.*
    // <<
    //  <<{ 'name': 'John'},{'name': 'Doe'} >>,
    //  ...
    // >>
    fun unpivot() =
        SuccessTestCase(
            input = """
             [
               { 'id':'5',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': {
                      'first': {'name': 'John'},
                      'second': {'name': 'Doe'}
                     }
                   },
                   { 'title':'B',
                     'price':2.0,
                     'authors': {
                      'first': {'name': 'Zoe'}, 
                      'second': {'name': 'Bill'}
                     }
                   }
                 ]
               },
               { 'id':'6',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': {
                      'first': {'name': 'John'},
                      'second': {'name': 'Doe'}
                     }
                   },
                   { 'title':'E',
                     'price':2.0,
                     'authors': {
                      'first': {'name': 'Zoe'}, 
                      'second': {'name': 'Bill'}
                     }
                   }
                 ]
               },
               { 'id':7,
                 'books':[]
               }
             ][*].books[*].authors.*.name
            """.trimIndent(),
            expected = bagValue(
                listOf(
                    stringValue("John"), stringValue("Doe"), stringValue("Zoe"), stringValue("Bill"),
                    stringValue("John"), stringValue("Doe"), stringValue("Zoe"), stringValue("Bill")
                )
            )
        ).run()
}
