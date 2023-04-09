package fuzzd.generator.ast

import fuzzd.generator.ast.ExpressionAST.IdentifierAST
import fuzzd.generator.ast.Type.BoolType
import fuzzd.generator.ast.Type.CharType
import fuzzd.generator.ast.Type.ClassType
import fuzzd.generator.ast.Type.ConstructorType.ArrayType
import fuzzd.generator.ast.Type.IntType
import fuzzd.generator.ast.Type.MapType
import fuzzd.generator.ast.Type.MethodReturnType
import fuzzd.generator.ast.Type.MultisetType
import fuzzd.generator.ast.Type.RealType
import fuzzd.generator.ast.Type.SetType
import fuzzd.generator.ast.Type.StringType
import fuzzd.generator.ast.error.InvalidFormatException
import fuzzd.generator.ast.error.InvalidInputException
import fuzzd.generator.ast.operators.BinaryOperator
import fuzzd.generator.ast.operators.UnaryOperator
import fuzzd.utils.escape

fun checkParams(expected: List<IdentifierAST>, actual: List<ExpressionAST>, context: String) {
    if (expected.size != actual.size) {
        throw InvalidInputException("Number of parameters for context {$context} doesn't match. Expected ${expected.size}, got ${actual.size}")
    }

    (expected.indices).forEach { i ->
        val expectedType = expected[i].type()
        val actualType = actual[i].type()

        if (actualType != expectedType) {
            throw InvalidInputException("Parameter type mismatch for parameter $i in context {$context}. Expected $expectedType, got $actualType")
        }
    }
}

sealed class ExpressionAST : ASTElement {

    abstract fun type(): Type

    class ExpressionListAST(val exprs: List<ExpressionAST>) : ExpressionAST() {
        override fun type(): Type = MethodReturnType(exprs.map { it.type() })

        override fun toString(): String = exprs.joinToString(", ")
    }

    class ClassInstantiationAST(val clazz: ClassAST, val params: List<ExpressionAST>) :
        ExpressionAST() {
        init {
            val expectedParams = clazz.constructorFields.toList()
            checkParams(expectedParams, params, "constructor call for ${clazz.name}")
        }

        override fun type(): Type = ClassType(clazz)

        override fun toString(): String = "new ${clazz.name}(${params.joinToString(", ")})"
    }

    class NonVoidMethodCallAST(val method: MethodSignatureAST, val params: List<ExpressionAST>) :
        ExpressionAST() {
        init {
            val methodParams = method.params
            checkParams(methodParams, params, "method call to ${method.name}")
        }

        override fun type(): Type = MethodReturnType(method.returns.map { it.type() })

        override fun toString(): String = "${method.name}(${params.joinToString(", ")})"
    }

    class FunctionMethodCallAST(
        val function: FunctionMethodSignatureAST,
        val params: List<ExpressionAST>,
    ) :
        ExpressionAST() {
        init {
            val functionParams = function.params
            checkParams(functionParams, params, "function method call to ${function.name}")
        }

        override fun type(): Type = function.returnType

        override fun toString(): String = "${function.name}(${params.joinToString(", ")})"
    }

    class TernaryExpressionAST(
        val condition: ExpressionAST,
        val ifBranch: ExpressionAST,
        val elseBranch: ExpressionAST,
    ) : ExpressionAST() {
        init {
            if (condition.type() != BoolType) {
                throw InvalidInputException("Invalid input type for ternary expression condition. Got ${condition.type()}")
            }

            if (ifBranch.type() != elseBranch.type()) {
                throw InvalidInputException("Ternary expression branches have different types. If branch: ${ifBranch.type()}. Else branch: ${elseBranch.type()}")
            }
        }

        override fun type(): Type = ifBranch.type()

        override fun toString(): String = "if ($condition) then $ifBranch else $elseBranch"
    }

    class UnaryExpressionAST(val expr: ExpressionAST, val operator: UnaryOperator) : ExpressionAST() {
        init {
            if (!operator.supportsInput(expr.type())) {
                throw InvalidInputException("Operator $operator does not support input type ${expr.type()}")
            }
        }

        override fun type(): Type = expr.type()

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append(operator)
            val wrapExpr = expr is BinaryExpressionAST || expr is TernaryExpressionAST
            sb.append(if (wrapExpr) "($expr)" else "$expr")
            return sb.toString()
        }
    }

    class ModulusExpressionAST(val expr: ExpressionAST) : ExpressionAST() {
        init {
            val type = expr.type()
            if (type !is MapType && type !is SetType) {
                throw InvalidInputException("Invalid expression type for modulus. Got $type, expected map or set")
            }
        }

        override fun type(): Type = IntType

        override fun toString(): String = "|$expr|"
    }

    class BinaryExpressionAST(
        val expr1: ExpressionAST,
        val operator: BinaryOperator,
        val expr2: ExpressionAST,
    ) : ExpressionAST() {
        private val type1: Type = expr1.type()
        private val type2: Type = expr2.type()

        init {
            if (!operator.supportsInput(type1, type2)) {
                println("=================")
                println(this.operator::class)
                throw InvalidInputException("Operator $operator does not support input types ($type1, $type2)")
            }
        }

        override fun type(): Type = operator.outputType(type1, type2)

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append(if (shouldWrap(expr1)) "($expr1)" else "$expr1")
            sb.append(" $operator ")
            sb.append(if (shouldWrap(expr2)) "($expr2)" else "$expr2")

            return sb.toString()
        }

        private fun shouldWrap(expr: ExpressionAST) = expr is TernaryExpressionAST || expr is BinaryExpressionAST
    }

    open class IdentifierAST(
        val name: String,
        private val type: Type,
        val mutable: Boolean = true,
        private var initialised: Boolean = false,
    ) : ExpressionAST() {
        override fun type(): Type = type

        override fun toString(): String = name

        fun initialise() {
            initialised = true
        }

        fun initialised(): Boolean = initialised

        override fun equals(other: Any?): Boolean =
            other is IdentifierAST && other.name == this.name && other.type == this.type && other.mutable == this.mutable

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + mutable.hashCode()
            return result
        }
    }

    class ClassInstanceAST(
        val clazz: ClassAST,
        name: String,
    ) : IdentifierAST(name, ClassType(clazz)) {
        val fields = clazz.fields.map { ClassInstanceFieldAST(this, it) }
        val functionMethods = clazz.functionMethods.map { ClassInstanceFunctionMethodSignatureAST(this, it.signature) }
        val methods = clazz.methods.map { ClassInstanceMethodSignatureAST(this, it.signature) }
    }

    class ClassInstanceFieldAST(
        val classInstance: IdentifierAST,
        val classField: IdentifierAST,
    ) : IdentifierAST("${classInstance.name}.$classField", classField.type(), initialised = true)

    class ArrayIndexAST(
        val array: IdentifierAST,
        val index: ExpressionAST,
    ) : IdentifierAST(
        array.name,
        (array.type() as ArrayType).internalType,
    ) {
        init {
            if (array.type() !is ArrayType) {
                throw InvalidInputException("Creating array index with identifier of type ${array.type()}")
            }

            if (index.type() != IntType) {
                throw InvalidInputException("Creating array index with index of type ${index.type()}")
            }
        }

        override fun toString(): String {
            return "$array[$index]"
        }
    }

    class MapConstructorAST(
        val keyType: Type,
        val valueType: Type,
        val assignments: List<Pair<ExpressionAST, ExpressionAST>> = emptyList(),
    ) : ExpressionAST() {
        init {
            assignments.indices.forEach { i ->
                val pair = assignments[i]

                if (pair.first.type() != keyType) {
                    throw InvalidInputException("Invalid key type for index $i of map constructor. Expected $keyType, got ${pair.first.type()}")
                }

                if (pair.second.type() != valueType) {
                    throw InvalidInputException("Invalid value type for index $i of map constructor. Expected $valueType, got ${pair.second.type()}")
                }
            }
        }

        override fun type(): Type = MapType(keyType, valueType)

        override fun toString() = "map[${assignments.joinToString(", ") { "${it.first} := ${it.second}" }}]"
    }

    class IndexAST(
        val ident: IdentifierAST,
        val key: ExpressionAST,
    ) : IdentifierAST(
        ident.name,
        if (ident.type() is MapType) (ident.type() as MapType).valueType else (ident.type() as MultisetType).innerType,
    ) {
        init {
            when (val identType = ident.type()) {
                is MapType -> {
                    val expectedKeyType = identType.keyType

                    if (key.type() != expectedKeyType) {
                        throw InvalidInputException("Invalid key type for map IndexAST. Expected $expectedKeyType, got ${key.type()}")
                    }
                }

                is MultisetType -> {
                    val expectedKeyType = identType.innerType
                    if (key.type() != expectedKeyType) {
                        throw InvalidInputException("Invalid key type for multiset IndexAST. Expected $expectedKeyType, got ${key.type()}")
                    }
                }

                else -> throw InvalidInputException("Expected map/multiset type for IndexAST identifier. Got ${ident.type()}")
            }
        }

        override fun toString(): String = "$ident[$key]"
    }

    class IndexAssignAST(
        val ident: IdentifierAST,
        val key: ExpressionAST,
        val value: ExpressionAST,
    ) : IdentifierAST(ident.name, ident.type()) {
        init {
            when (val identType = ident.type()) {
                is MapType -> {
                    if (key.type() != identType.keyType) {
                        throw InvalidInputException("Invalid key type for multiset IndexAssignAST. Expected ${identType.keyType}, got ${key.type()}")
                    }

                    if (value.type() != identType.valueType) {
                        throw InvalidInputException("Invalid value type for multiset IndexAssignAST. Expected ${identType.valueType}, got ${value.type()}")
                    }
                }

                is MultisetType -> {
                    if (key.type() != identType.innerType) {
                        throw InvalidInputException("Invalid key type for multiset IndexAssignAST. Expected ${identType.innerType}, got ${value.type()}")
                    }

                    if (value.type() != IntType) {
                        throw InvalidInputException("Invalid value type for multiset IndexAssignAST. Expected int, got ${value.type()}")
                    }
                }

                else -> throw InvalidInputException("Invalid identifier type for IndexAssignAST. Expected map or multiset, got $identType")
            }
        }

        override fun toString(): String = "$ident[$key := $value]"
    }

    class SetDisplayAST(val innerType: Type, val exprs: List<ExpressionAST>, val isMultiset: Boolean) : ExpressionAST() {
        init {
            exprs.indices.forEach { i ->
                if (exprs[i].type() != innerType) {
                    throw InvalidInputException("Invalid expression type at elem $i for set display. Expected $innerType, got ${exprs[i].type()}")
                }
            }
        }

        override fun type(): Type = if (isMultiset) MultisetType(innerType) else SetType(innerType)

        override fun toString(): String = "${if (isMultiset) "multiset" else ""}{${exprs.joinToString(", ")}}"
    }

    class ArrayLengthAST(val array: IdentifierAST) : ExpressionAST() {
        init {
            if (array.type() !is ArrayType) {
                throw InvalidInputException("Creating array index with identifier of type ${array.type()}")
            }
        }

        override fun type(): Type = IntType

        override fun toString(): String = "${array.name}.Length"
    }

    class ArrayInitAST(val length: Int, private val type: ArrayType) : ExpressionAST() {
        override fun type(): Type = type

        override fun toString(): String = "new ${type.internalType}[$length]"
    }

    abstract class LiteralAST(private val value: String, private val type: Type) : ExpressionAST() {
        override fun toString(): String = value

        override fun type(): Type = type

        override fun equals(other: Any?): Boolean = other is LiteralAST && other.value == value
    }

    class BooleanLiteralAST(private val value: Boolean) : LiteralAST(value.toString(), BoolType) {
        override fun equals(other: Any?): Boolean = other is BooleanLiteralAST && other.value == value
        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * grammar from documentation:
     * digits = digit {['_'] digit}
     * hexdigits = "0x" hexdigit {['_'] hexdigit}
     */
    class IntegerLiteralAST(private val value: String, private val hexFormat: Boolean = false) :
        LiteralAST(value, IntType) {
        constructor(value: Int) : this(value.toString())

        init {
            if (!value.matches(Regex("(-)?[0-9]+"))) {
                throw InvalidFormatException("Value passed (= $value) did not match supported integer format")
            }
        }

        override fun toString(): String = if (hexFormat) {
            val sb = StringBuilder()
            val negative = value[0] == '-'
            if (negative) sb.append("-")
            sb.append("0x")

            if (negative) {
                sb.append(Integer.toHexString(value.substring(1).toInt()))
            } else {
                sb.append(Integer.toHexString(value.toInt()))
            }

            sb.toString()
        } else {
            value
        }
    }

    /**
     * grammar from documentation:
     * decimaldigits = digit {['_'] digit} '.' digit {['_'] digit}
     */
    class RealLiteralAST(value: String) : LiteralAST(value, RealType) {
        constructor(value: Float) : this(value.toString())

        init {
            if (!value.matches(Regex("(-)?[0-9]+.[0-9]+"))) {
                println(value)
                throw InvalidFormatException("Value passed (= $value) did not match supported float format")
            }
        }
    }

    class CharacterLiteralAST(char: Char) : LiteralAST("'${char.escape()}'", CharType)

    class StringLiteralAST(value: String) : LiteralAST("\"$value\"", StringType)
}
