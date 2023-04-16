package fuzzd.interpreter.value

import fuzzd.generator.ast.ExpressionAST
import fuzzd.generator.ast.ExpressionAST.BooleanLiteralAST
import fuzzd.generator.ast.ExpressionAST.ExpressionListAST
import fuzzd.generator.ast.ExpressionAST.IntegerLiteralAST
import fuzzd.generator.ast.ExpressionAST.MapConstructorAST
import fuzzd.generator.ast.ExpressionAST.SequenceDisplayAST
import fuzzd.generator.ast.ExpressionAST.SetDisplayAST
import fuzzd.generator.ast.ExpressionAST.StringLiteralAST
import fuzzd.generator.ast.FunctionMethodSignatureAST
import fuzzd.generator.ast.MethodSignatureAST
import fuzzd.generator.ast.SequenceAST
import fuzzd.utils.reduceLists
import java.lang.Integer.min

fun <T> multisetDifference(m1: Map<T, Int>, m2: Map<T, Int>): Map<T, Int> {
    val diff = mutableMapOf<T, Int>()
    m1.entries.forEach { (k, v) ->
        if (k !in m2) {
            diff[k] = v
        } else if (k in m2 && m2[k]!! < v) {
            diff[k] = v - m2[k]!!
        }
    }
    return diff
}

fun <T> multisetIntersect(m1: Map<T, Int>, m2: Map<T, Int>): Map<T, Int> {
    val intersect = mutableMapOf<T, Int>()
    m1.entries.forEach { (k, v) ->
        if (k in m2) {
            intersect[k] = min(v, m2[k]!!)
        }
    }
    return intersect
}

sealed class Value {
    abstract fun toExpressionAST(): ExpressionAST

    data class MultiValue(val values: List<Value>) : Value() {
        override fun toExpressionAST(): ExpressionAST = ExpressionListAST(values.map { it.toExpressionAST() })
    }

    data class ClassValue(
        val fields: ValueTable,
        val functions: Map<FunctionMethodSignatureAST, ExpressionAST>,
        val methods: Map<MethodSignatureAST, SequenceAST>,
    ) : Value() {
        override fun toExpressionAST(): ExpressionAST = throw UnsupportedOperationException()
    }

    class ArrayValue(length: Int) : Value() {
        val arr = Array<Value?>(length) { null }

        fun setIndex(index: Int, value: Value) {
            arr[index] = value
        }

        fun getIndex(index: Int): Value =
            arr[index] ?: throw UnsupportedOperationException("Array index $index was null")

        fun length(): IntValue = IntValue(arr.size.toLong())

        override fun toExpressionAST(): ExpressionAST = throw UnsupportedOperationException()
    }

    sealed class DataStructureValue : Value() {
        abstract fun contains(item: Value): BoolValue
        abstract fun notContains(item: Value): BoolValue
        abstract fun modulus(): IntValue
    }

    data class SequenceValue(val seq: List<Value>) : DataStructureValue() {
        override fun contains(item: Value): BoolValue = BoolValue(item in seq)
        override fun notContains(item: Value): BoolValue = BoolValue(item !in seq)
        override fun modulus(): IntValue = IntValue(seq.size.toLong())
        fun properSubsetOf(other: SequenceValue): BoolValue =
            BoolValue(other.seq.containsAll(seq) && (other.seq - seq.toSet()).isNotEmpty())

        fun subsetOf(other: SequenceValue): BoolValue = BoolValue(other.seq.containsAll(seq))
        fun supersetOf(other: SequenceValue): BoolValue = BoolValue(seq.containsAll(other.seq))
        fun properSupersetOf(other: SequenceValue): BoolValue =
            BoolValue(seq.containsAll(other.seq) && (seq - other.seq.toSet()).isNotEmpty())

        fun union(other: SequenceValue): SequenceValue = SequenceValue(seq + other.seq)

        fun getIndex(index: Int): Value = seq[index]

        fun assign(key: Int, value: Value): SequenceValue =
            SequenceValue(seq.subList(0, key) + value + seq.subList(key + 1, seq.size))

        override fun equals(other: Any?): Boolean = other is SequenceValue && seq == other.seq
        override fun hashCode(): Int = seq.hashCode()

        override fun toExpressionAST(): ExpressionAST = SequenceDisplayAST(seq.map { it.toExpressionAST() })
    }

    data class MapValue(val map: Map<Value, Value>) : DataStructureValue() {
        override fun contains(item: Value): BoolValue = BoolValue(map.containsKey(item))
        override fun notContains(item: Value): BoolValue = BoolValue(!map.containsKey(item))
        override fun modulus(): IntValue = IntValue(map.size.toLong())
        fun union(other: MapValue): MapValue = MapValue(map + other.map)
        fun difference(other: SetValue): MapValue = MapValue(map - other.set)

        fun get(key: Value): Value = map[key] ?: throw UnsupportedOperationException("Map didn't contain key $key")

        fun assign(key: Value, value: Value): MapValue = MapValue(map + mapOf(key to value))

        override fun equals(other: Any?): Boolean = other is MapValue && map == other.map
        override fun hashCode(): Int = map.hashCode()

        override fun toExpressionAST(): ExpressionAST {
            val assignments = map.map { (k, v) -> Pair(k.toExpressionAST(), v.toExpressionAST()) }
            return MapConstructorAST(assignments[0].first.type(), assignments[0].second.type(), assignments)
        }
    }

    data class MultisetValue(val map: Map<Value, Int>) : DataStructureValue() {
        override fun contains(item: Value): BoolValue = BoolValue(map.containsKey(item) && map[item] != 0)
        override fun notContains(item: Value): BoolValue = BoolValue(!map.containsKey(item) || map[item] == 0)
        override fun modulus(): IntValue = IntValue(map.values.sum().toLong())

        fun properSubsetOf(other: MultisetValue): BoolValue =
            BoolValue(multisetDifference(map, other.map).isEmpty() && multisetDifference(map, other.map).isNotEmpty())

        fun subsetOf(other: MultisetValue): BoolValue = BoolValue(multisetDifference(map, other.map).isEmpty())
        fun supersetOf(other: MultisetValue): BoolValue = BoolValue(multisetDifference(other.map, map).isEmpty())
        fun properSupersetOf(other: MultisetValue): BoolValue =
            BoolValue(multisetDifference(map, other.map).isNotEmpty() && multisetDifference(other.map, map).isEmpty())

        fun disjoint(other: MultisetValue): BoolValue = BoolValue(map.keys.none { it in other.map })
        fun union(other: MultisetValue): MultisetValue = MultisetValue(
            other.map.keys.fold(map.toMutableMap()) { m, k ->
                if (m.containsKey(k)) m[k] = m[k]!! + other.map[k]!! else m[k] = other.map[k]!!
                m
            },
        )

        fun difference(other: MultisetValue): MultisetValue = MultisetValue(multisetDifference(map, other.map))
        fun intersect(other: MultisetValue): MultisetValue = MultisetValue(multisetIntersect(map, other.map))

        fun get(key: Value): IntValue = if (key in map) {
            IntValue(map[key]!!.toLong())
        } else {
            throw UnsupportedOperationException("Multiset didn't contain key $key")
        }

        fun assign(key: Value, value: Int): MultisetValue = MultisetValue(map + mapOf(key to value))

        override fun equals(other: Any?): Boolean = other is MultisetValue && map == other.map
        override fun hashCode(): Int = map.hashCode()

        override fun toExpressionAST(): ExpressionAST =
            SetDisplayAST(map.map { (k, v) -> List(v) { k.toExpressionAST() } }.reduceLists(), true)
    }

    data class SetValue(val set: Set<Value>) : DataStructureValue() {
        override fun contains(item: Value): BoolValue = BoolValue(item in set)
        override fun notContains(item: Value): BoolValue = BoolValue(item !in set)
        override fun modulus(): IntValue = IntValue(set.size.toLong())
        override fun toExpressionAST(): ExpressionAST = SetDisplayAST(set.map { it.toExpressionAST() }, false)

        fun properSubsetOf(other: SetValue): BoolValue =
            BoolValue(other.set.containsAll(set) && (other.set subtract set).isNotEmpty())

        fun subsetOf(other: SetValue): BoolValue = BoolValue(other.set.containsAll(set))
        fun supersetOf(other: SetValue): BoolValue = BoolValue(set.containsAll(other.set))
        fun properSupersetOf(other: SetValue): BoolValue =
            BoolValue(set.containsAll(other.set) && (set subtract other.set).isNotEmpty())

        fun disjoint(other: SetValue): BoolValue = BoolValue(set.none { other.set.contains(it) })
        fun union(other: SetValue): SetValue = SetValue(set union other.set)
        fun difference(other: SetValue): SetValue = SetValue(set subtract other.set)
        fun intersect(other: SetValue): SetValue = SetValue(set intersect other.set)

        override fun equals(other: Any?): Boolean = (other is SetValue) && set == other.set
        override fun hashCode(): Int = set.hashCode()
    }

    data class StringValue(val value: String) : Value() {
        override fun equals(other: Any?): Boolean = other is StringValue && value == other.value
        override fun hashCode(): Int = value.hashCode()
        override fun toExpressionAST(): ExpressionAST = StringLiteralAST(value)
    }

    data class BoolValue(val value: Boolean) : Value() {
        fun not(): BoolValue = BoolValue(!value)
        fun iff(other: BoolValue): BoolValue = BoolValue(value && other.value || !value && !other.value)
        fun impl(other: BoolValue): BoolValue = BoolValue(!value || other.value)
        fun rimpl(other: BoolValue): BoolValue = BoolValue(!other.value || value)
        fun and(other: BoolValue): BoolValue = BoolValue(value && other.value)
        fun or(other: BoolValue): BoolValue = BoolValue(value || other.value)

        override fun toExpressionAST(): ExpressionAST = BooleanLiteralAST(value)

        override fun equals(other: Any?): Boolean = other is BoolValue && value == other.value
        override fun hashCode(): Int = value.hashCode()
    }

    data class IntValue(val value: Long) : Value() {
        fun negate(): IntValue = IntValue(-1 * value)
        fun plus(other: IntValue): IntValue = IntValue(value + other.value)
        fun subtract(other: IntValue): IntValue = IntValue(value - other.value)
        fun multiply(other: IntValue): IntValue = IntValue(value * other.value)
        fun divide(other: IntValue): IntValue = TODO("Euclidean division")
        fun modulo(other: IntValue): IntValue = IntValue(value % other.value)
        fun lessThan(other: IntValue): BoolValue = BoolValue(value < other.value)
        fun lessThanEquals(other: IntValue): BoolValue = BoolValue(value <= other.value)
        fun greaterThanEquals(other: IntValue): BoolValue = BoolValue(value >= other.value)
        fun greaterThan(other: IntValue): BoolValue = BoolValue(value > other.value)

        override fun equals(other: Any?): Boolean = other is IntValue && value == other.value
        override fun hashCode(): Int = value.hashCode()

        override fun toExpressionAST(): ExpressionAST = IntegerLiteralAST(value.toInt())
    }
}
