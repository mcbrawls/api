package net.mcbrawls.api.database

import org.jetbrains.exposed.sql.ComplexExpression
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.append

class CaseWhenNoElse<T> : Expression<T>(), ComplexExpression {
    /**
     * The boolean conditions to check and their resulting expressions if the condition is met.
     */
    val cases: MutableList<Pair<Expression<Boolean>, Expression<out T>>> = mutableListOf()

    /**
     * Adds a conditional expression with a [result] if the expression evaluates to `true`.
     */
    fun andWhen(cond: Expression<Boolean>, result: Expression<T>): CaseWhenNoElse<T> {
        cases.add(cond to result)
        return this
    }

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append("CASE")

            for ((first, second) in cases) {
                append(" WHEN ", first, " THEN ", second)
            }

            append(" END")
        }
    }

    companion object {
        /**
         * Compares] against any chained conditional expressions.
         */
        fun <T> caseNoElse(): CaseWhenNoElse<T> = CaseWhenNoElse()
    }
}
