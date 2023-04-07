package fuzzd.generator

import fuzzd.generator.ast.ClassAST
import fuzzd.generator.ast.DafnyAST
import fuzzd.generator.ast.ExpressionAST
import fuzzd.generator.ast.ExpressionAST.ArrayIndexAST
import fuzzd.generator.ast.ExpressionAST.ArrayInitAST
import fuzzd.generator.ast.ExpressionAST.BinaryExpressionAST
import fuzzd.generator.ast.ExpressionAST.BooleanLiteralAST
import fuzzd.generator.ast.ExpressionAST.CharacterLiteralAST
import fuzzd.generator.ast.ExpressionAST.ClassInstanceAST
import fuzzd.generator.ast.ExpressionAST.ClassInstantiationAST
import fuzzd.generator.ast.ExpressionAST.FunctionMethodCallAST
import fuzzd.generator.ast.ExpressionAST.IdentifierAST
import fuzzd.generator.ast.ExpressionAST.IntegerLiteralAST
import fuzzd.generator.ast.ExpressionAST.LiteralAST
import fuzzd.generator.ast.ExpressionAST.MapConstructorAST
import fuzzd.generator.ast.ExpressionAST.MapIndexAST
import fuzzd.generator.ast.ExpressionAST.MapIndexAssignAST
import fuzzd.generator.ast.ExpressionAST.NonVoidMethodCallAST
import fuzzd.generator.ast.ExpressionAST.RealLiteralAST
import fuzzd.generator.ast.ExpressionAST.SetDisplayAST
import fuzzd.generator.ast.ExpressionAST.StringLiteralAST
import fuzzd.generator.ast.ExpressionAST.TernaryExpressionAST
import fuzzd.generator.ast.ExpressionAST.UnaryExpressionAST
import fuzzd.generator.ast.FunctionMethodAST
import fuzzd.generator.ast.FunctionMethodSignatureAST
import fuzzd.generator.ast.MainFunctionAST
import fuzzd.generator.ast.MethodAST
import fuzzd.generator.ast.MethodSignatureAST
import fuzzd.generator.ast.SequenceAST
import fuzzd.generator.ast.StatementAST
import fuzzd.generator.ast.StatementAST.AssignmentAST
import fuzzd.generator.ast.StatementAST.BreakAST
import fuzzd.generator.ast.StatementAST.CounterLimitedWhileLoopAST
import fuzzd.generator.ast.StatementAST.DeclarationAST
import fuzzd.generator.ast.StatementAST.IfStatementAST
import fuzzd.generator.ast.StatementAST.MultiDeclarationAST
import fuzzd.generator.ast.StatementAST.PrintAST
import fuzzd.generator.ast.StatementAST.TypedDeclarationAST
import fuzzd.generator.ast.StatementAST.VoidMethodCallAST
import fuzzd.generator.ast.TopLevelAST
import fuzzd.generator.ast.TraitAST
import fuzzd.generator.ast.Type
import fuzzd.generator.ast.Type.BoolType
import fuzzd.generator.ast.Type.CharType
import fuzzd.generator.ast.Type.ConstructorType
import fuzzd.generator.ast.Type.ConstructorType.ArrayType
import fuzzd.generator.ast.Type.IntType
import fuzzd.generator.ast.Type.LiteralType
import fuzzd.generator.ast.Type.MapType
import fuzzd.generator.ast.Type.RealType
import fuzzd.generator.ast.Type.SetType
import fuzzd.generator.ast.error.IdentifierOnDemandException
import fuzzd.generator.ast.error.MethodOnDemandException
import fuzzd.generator.ast.identifier_generator.NameGenerator.ClassNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.ControlFlowGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.FieldNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.FunctionMethodNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.MethodNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.ParameterNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.ReturnsNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.TraitNameGenerator
import fuzzd.generator.ast.operators.BinaryOperator.AdditionOperator
import fuzzd.generator.ast.operators.BinaryOperator.GreaterThanEqualOperator
import fuzzd.generator.context.GenerationContext
import fuzzd.generator.selection.AssignType
import fuzzd.generator.selection.AssignType.ARRAY_INDEX
import fuzzd.generator.selection.ExpressionType
import fuzzd.generator.selection.ExpressionType.BINARY
import fuzzd.generator.selection.ExpressionType.CONSTRUCTOR
import fuzzd.generator.selection.ExpressionType.FUNCTION_METHOD_CALL
import fuzzd.generator.selection.ExpressionType.IDENTIFIER
import fuzzd.generator.selection.ExpressionType.LITERAL
import fuzzd.generator.selection.ExpressionType.MAP_INDEX
import fuzzd.generator.selection.ExpressionType.MAP_INDEX_ASSIGN
import fuzzd.generator.selection.ExpressionType.TERNARY
import fuzzd.generator.selection.ExpressionType.UNARY
import fuzzd.generator.selection.SelectionManager
import fuzzd.generator.selection.StatementType
import fuzzd.generator.selection.StatementType.ASSIGN
import fuzzd.generator.selection.StatementType.CLASS_INSTANTIATION
import fuzzd.generator.selection.StatementType.DECLARATION
import fuzzd.generator.selection.StatementType.IF
import fuzzd.generator.selection.StatementType.MAP_ASSIGN
import fuzzd.generator.selection.StatementType.METHOD_CALL
import fuzzd.generator.selection.StatementType.PRINT
import fuzzd.generator.selection.StatementType.WHILE
import fuzzd.generator.symbol_table.DependencyTable
import fuzzd.generator.symbol_table.FunctionSymbolTable
import fuzzd.generator.symbol_table.SymbolTable
import fuzzd.utils.foldPair
import fuzzd.utils.reduceLists
import fuzzd.utils.unionAll

// import fuzzd.utils.SAFE_ADDITION_REAL
// import fuzzd.utils.SAFE_DIVISION_REAL
// import fuzzd.utils.SAFE_MULTIPLY_REAL
// import fuzzd.utils.SAFE_SUBTRACT_CHAR
// import fuzzd.utils.SAFE_SUBTRACT_REAL

class Generator(
    private val selectionManager: SelectionManager,
    private val instrument: Boolean = false,
) : ASTGenerator {
    private val classNameGenerator = ClassNameGenerator()
    private val fieldNameGenerator = FieldNameGenerator()
    private val functionMethodNameGenerator = FunctionMethodNameGenerator()
    private val methodNameGenerator = MethodNameGenerator()
    private val traitNameGenerator = TraitNameGenerator()
    private val methodCallTable = DependencyTable<MethodSignatureAST>()

    private val controlFlowGenerator = ControlFlowGenerator()

    /* ==================================== TOP LEVEL ==================================== */

    override fun generate(): DafnyAST {
        val context = GenerationContext(FunctionSymbolTable())

        val globalFields = (1..selectionManager.selectNumberOfGlobalFields()).map { generateField(context) }.toSet()
        val globalStateClass = ClassAST.builder().withName(GLOBAL_STATE).withFields(globalFields).build()
        context.setGlobalState(globalStateClass)

        val mainFunction = generateMainFunction(context)
        val ast = mutableListOf<TopLevelAST>()

        val methodBodyQueue = context.functionSymbolTable.methods().toMutableList()
        val visitedMethods = mutableSetOf<MethodAST>()

        while (methodBodyQueue.isNotEmpty()) {
            val method = methodBodyQueue.removeFirst()

            if (method in visitedMethods) continue
            visitedMethods.add(method)

            val functionContext = GenerationContext(
                context.functionSymbolTable,
                methodContext = method.signature,
            ).setGlobalState(context.globalState())
            val body = generateMethodBody(functionContext, method)
            method.setBody(body)

            methodBodyQueue.addAll(context.functionSymbolTable.methods() subtract methodBodyQueue.toSet() subtract visitedMethods)
        }

        ast.add(context.globalState())
        ast.addAll(context.functionSymbolTable.functionMethods())
        ast.addAll(context.functionSymbolTable.methods())
        ast.addAll(context.functionSymbolTable.traits())
        ast.addAll(context.functionSymbolTable.classes())
        ast.add(mainFunction)

        return DafnyAST(ast)
    }

    override fun generateMainFunction(context: GenerationContext): MainFunctionAST {
        val (globalStateParams, globalStateDeps) = context.globalState().fields.map {
            if (it.type() is LiteralType) {
                generateLiteralForType(context, it.type() as LiteralType)
            } else {
                generateExpression(context.increaseExpressionDepth().disableFunctionCalls(), it.type())
            }
        }.foldPair()

        val globalStateIdentifier = ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)
        val globalStateDecl =
            DeclarationAST(globalStateIdentifier, ClassInstantiationAST(context.globalState(), globalStateParams))

        context.symbolTable.add(globalStateIdentifier)

        val body = generateSequence(context)
        val prints = generateChecksum(context)
        return MainFunctionAST(SequenceAST(globalStateDeps + globalStateDecl + body.statements + prints))
    }

    override fun generateChecksum(context: GenerationContext): List<PrintAST> {
        val fields = context.symbolTable.withType(IntType) + context.symbolTable.withType(BoolType)
        return fields.map { field -> PrintAST(field) }
    }

    override fun generateTrait(context: GenerationContext): TraitAST {
        val fields = (1..selectionManager.selectNumberOfFields()).map { generateField(context) }.toSet()

        val functionMethods =
            (1..selectionManager.selectNumberOfFunctionMethods()).map { generateFunctionMethodSignature(context) }
                .toSet()

        val methods = (1..selectionManager.selectNumberOfMethods()).map { generateMethodSignature(context) }.toSet()

        val numberOfInherits = selectionManager.selectNumberOfTraitInherits()
        val selectedTraits = selectTraits(context, numberOfInherits)

        val trait = TraitAST(traitNameGenerator.newValue(), selectedTraits, functionMethods, methods, fields)

        context.functionSymbolTable.addTrait(trait)

        return trait
    }

    private fun selectTraits(context: GenerationContext, n: Int): Set<TraitAST> {
        val traits = context.functionSymbolTable.traits().toMutableList()
        val selectedTraits = mutableSetOf<TraitAST>()

        for (i in 1..n) {
            if (traits.isEmpty()) {
                traits.add(generateTrait(context))
            }

            val selected = selectionManager.randomSelection(traits)
            traits.remove(selected)
            selectedTraits.add(selected)
        }

        return selectedTraits
    }

    override fun generateClass(context: GenerationContext): ClassAST {
        val classContext =
            GenerationContext(FunctionSymbolTable(context.functionSymbolTable.topLevel())).setGlobalState(context.globalState())

        // get traits
        val numberOfTraits = selectionManager.selectNumberOfTraits()
        val selectedTraits = selectTraits(context, numberOfTraits)

        // generate fields
        val additionalFields = (1..selectionManager.selectNumberOfFields()).map { generateField(classContext) }.toSet()
        val requiredFields = selectedTraits.map { it.fields() }.unionAll()

        classContext.symbolTable.addAll(additionalFields union requiredFields)

        // generate function methods
        val additionalFunctionMethods =
            (1..selectionManager.selectNumberOfFunctionMethods()).map { generateFunctionMethod(classContext) }.toSet()
        val requiredFunctionMethods = selectedTraits.map { it.functionMethods() }.unionAll()
            .map { signature -> generateFunctionMethod(classContext, signature) }.toSet()
        val functionMethods = requiredFunctionMethods union additionalFunctionMethods

        // generate methods
        val additionalMethods =
            (1..selectionManager.selectNumberOfMethods()).map { generateMethod(classContext) }.toSet()
        val requiredMethods =
            selectedTraits.map { it.methods() }.unionAll().map { signature -> generateMethod(signature) }.toSet()
        val methods = requiredMethods union additionalMethods
        methods.forEach { method ->
            method.setBody(
                generateMethodBody(
                    GenerationContext(
                        classContext.functionSymbolTable,
                        symbolTable = SymbolTable(classContext.symbolTable),
                        methodContext = method.signature,
                    ).setGlobalState(context.globalState()),
                    method,
                ),
            )
        }

        val clazz = ClassAST(
            classNameGenerator.newValue(),
            selectedTraits,
            functionMethods,
            methods,
            additionalFields,
            requiredFields,
        )

        // update symbol table with on-demand methods & classes generated into local global context
        context.functionSymbolTable.addFunctionMethods(classContext.functionSymbolTable.functionMethods() subtract functionMethods)
        context.functionSymbolTable.addMethods(classContext.functionSymbolTable.methods() subtract methods)
        context.functionSymbolTable.addClass(clazz)

        return clazz
    }

    override fun generateField(context: GenerationContext) =
        IdentifierAST(fieldNameGenerator.newValue(), selectionManager.selectType(context), initialised = true)

    override fun generateFunctionMethod(context: GenerationContext, targetType: Type?): FunctionMethodAST {
        val functionMethodSignature = generateFunctionMethodSignature(context, targetType)
        return generateFunctionMethod(context, functionMethodSignature)
    }

    private fun generateFunctionMethod(
        context: GenerationContext,
        signature: FunctionMethodSignatureAST,
    ): FunctionMethodAST {
        val functionContext = GenerationContext(
            context.functionSymbolTable,
            onDemandIdentifiers = false,
        ).setGlobalState(context.globalState())
        signature.params.forEach { param -> functionContext.symbolTable.add(param) }

        val (body, _) = generateExpression(functionContext, signature.returnType)
        val functionMethodAST = FunctionMethodAST(signature, body)

        context.functionSymbolTable.addFunctionMethod(functionMethodAST)

        return functionMethodAST
    }

    override fun generateFunctionMethodSignature(
        context: GenerationContext,
        targetType: Type?,
    ): FunctionMethodSignatureAST {
        val name = functionMethodNameGenerator.newValue()
        val numberOfParameters = selectionManager.selectNumberOfParameters()

        val parameterNameGenerator = ParameterNameGenerator()
        val returnType = targetType ?: generateType(context.disableOnDemand())
        val parameters = (1..numberOfParameters).map {
            IdentifierAST(
                parameterNameGenerator.newValue(),
                generateType(context.disableOnDemand()),
                mutable = false,
                initialised = true,
            )
        }

        return FunctionMethodSignatureAST(
            name,
            returnType,
            parameters + listOf(ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)),
        )
    }

    override fun generateMethod(context: GenerationContext): MethodAST {
        val method = MethodAST(generateMethodSignature(context))
        context.functionSymbolTable.addMethod(method)
        return method
    }

    private fun generateMethod(signature: MethodSignatureAST): MethodAST = MethodAST(signature)

    override fun generateMethodSignature(context: GenerationContext): MethodSignatureAST {
        val name = methodNameGenerator.newValue()
        val returnType = selectionManager.selectMethodReturnType(context)

        val returnsNameGenerator = ReturnsNameGenerator()
        val returns = returnType.map { t -> IdentifierAST(returnsNameGenerator.newValue(), t, initialised = false) }

        val numberOfParameters = selectionManager.selectNumberOfParameters()
        val parameterNameGenerator = ParameterNameGenerator()
        val parameters = (1..numberOfParameters).map {
            IdentifierAST(
                parameterNameGenerator.newValue(),
                generateType(context),
                mutable = false,
                initialised = true,
            )
        }

        return MethodSignatureAST(
            name,
            parameters + listOf(ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)),
            returns,
        )
    }

    private fun generateMethodBody(context: GenerationContext, method: MethodAST): SequenceAST {
        method.params().forEach { param -> context.symbolTable.add(param) }
        method.returns().forEach { r -> context.symbolTable.add(r) }

        val body = generateSequence(context, maxStatements = 10)

        val returnAssigns = method.returns().map { r ->
            val (expr, _) = generateExpression(context.disableOnDemand(), r.type())
            AssignmentAST(r, expr)
        }

        return body.addStatements(returnAssigns)
    }

    override fun generateSequence(context: GenerationContext, maxStatements: Int): SequenceAST {
        val n = selectionManager.selectSequenceLength(maxStatements)
        val statements = (1..n).map { generateStatement(context) }.reduceLists()
        return SequenceAST(statements)
    }

    /* ==================================== STATEMENTS ==================================== */

    override fun generateStatement(context: GenerationContext): List<StatementAST> = try {
        generateStatementFromType(selectionManager.selectStatementType(context), context)
    } catch (e: MethodOnDemandException) {
        generateStatementFromType(
            selectionManager.selectStatementType(context, methodCalls = false),
            context,
        )
    }

    private fun generateStatementFromType(
        type: StatementType,
        context: GenerationContext,
    ): List<StatementAST> = when (type) {
        ASSIGN -> generateAssignmentStatement(context)
        CLASS_INSTANTIATION -> generateClassInstantiation(context)
        DECLARATION -> generateDeclarationStatement(context)
        IF -> generateIfStatement(context)
        MAP_ASSIGN -> generateMapAssign(context)
        METHOD_CALL -> generateMethodCall(context)
        PRINT -> generatePrintStatement(context)
        WHILE -> generateWhileStatement(context)
    }

    override fun generateIfStatement(context: GenerationContext): List<StatementAST> {
        val (condition, conditionDeps) = generateExpression(context, BoolType)

        val ifPrint = PrintAST(StringLiteralAST(controlFlowGenerator.newValue()))
        val elsePrint = PrintAST(StringLiteralAST(controlFlowGenerator.newValue()))

        val ifBranch = generateSequence(context.increaseStatementDepth())
        val elseBranch = generateSequence(context.increaseStatementDepth())

        val ifStatement = IfStatementAST(
            condition,
            SequenceAST((if (instrument) listOf(ifPrint) else emptyList<StatementAST>()) + ifBranch.statements),
            SequenceAST((if (instrument) listOf(elsePrint) else emptyList<StatementAST>()) + elseBranch.statements),
        )

        return conditionDeps + ifStatement
    }

    override fun generateWhileStatement(context: GenerationContext): List<StatementAST> {
        val counterIdentifierName = context.loopCounterGenerator.newValue()
        val counterIdentifier = IdentifierAST(counterIdentifierName, IntType)
        val counterInitialisation = DeclarationAST(counterIdentifier, IntegerLiteralAST(0))

        val (condition, conditionDeps) = generateExpression(context, BoolType)

        val counterTerminationCheck = IfStatementAST(
            BinaryExpressionAST(
                counterIdentifier,
                GreaterThanEqualOperator,
                IntegerLiteralAST(
                    DAFNY_MAX_LOOP_COUNTER,
                ),
            ),
            SequenceAST(listOf(BreakAST)),
            null,
        )

        val counterUpdate = AssignmentAST(
            counterIdentifier,
            BinaryExpressionAST(counterIdentifier, AdditionOperator, IntegerLiteralAST(1)),
        )

        val whileBody = generateSequence(context.increaseStatementDepth())
        val whileLoop = CounterLimitedWhileLoopAST(
            counterInitialisation,
            counterTerminationCheck,
            counterUpdate,
            condition,
            whileBody,
        )
        return conditionDeps + whileLoop
    }

    override fun generatePrintStatement(context: GenerationContext): List<StatementAST> {
        val targetType = generateType(context.disableOnDemand())
        val (expr, exprDeps) = generateExpression(context, targetType)
        return exprDeps + PrintAST(expr)
    }

    override fun generateDeclarationStatement(context: GenerationContext): List<StatementAST> =
        generateDeclarationStatementForType(context, generateType(context), false)

    private fun generateDeclarationStatementForType(
        context: GenerationContext,
        targetType: Type,
        baseExpression: Boolean,
    ): List<StatementAST> {
        val (expr, exprDeps) = if (baseExpression) {
            generateBaseExpressionForType(context, targetType)
        } else {
            generateExpression(context, targetType)
        }

        val identifierName = context.identifierNameGenerator.newValue()
        val identifier = IdentifierAST(identifierName, targetType, initialised = true)

        context.symbolTable.add(identifier)

        return exprDeps + if (targetType is MapType || targetType is SetType) {
            TypedDeclarationAST(identifier, expr)
        } else {
            DeclarationAST(identifier, expr)
        }
    }

    override fun generateAssignmentStatement(context: GenerationContext): List<StatementAST> {
        val targetType = generateType(context, literalOnly = true)
        val (identifier, identDeps) = when (selectionManager.selectAssignType(context)) {
            AssignType.IDENTIFIER -> generateIdentifier(
                context,
                targetType,
                mutableConstraint = true,
                initialisedConstraint = false,
            )

            ARRAY_INDEX -> generateArrayIndex(context, targetType)
        }

        context.symbolTable.add(identifier)

        val (expr, exprDeps) = generateExpression(context, targetType)

        identifier.initialise()

        return identDeps + exprDeps + AssignmentAST(identifier, expr)
    }

    override fun generateClassInstantiation(context: GenerationContext): List<StatementAST> {
        // on demand create class if one doesn't exist
        if (!context.functionSymbolTable.hasClasses() || selectionManager.generateNewClass(context)) {
            generateClass(context)
        }

        val selectedClass = selectionManager.randomSelection(context.functionSymbolTable.classes().toList())
        val requiredFields = selectedClass.constructorFields

        val (params, paramDeps) = requiredFields.map { field ->
            generateExpression(context.increaseExpressionDepth(), field.type())
        }.foldPair()

        val ident = ClassInstanceAST(selectedClass, context.identifierNameGenerator.newValue())

        context.symbolTable.add(ident)

        return paramDeps + DeclarationAST(ident, ClassInstantiationAST(selectedClass, params))
    }

    @Throws(MethodOnDemandException::class)
    override fun generateMethodCall(context: GenerationContext): List<StatementAST> {
        // get callable methods
        val methods = (
            context.functionSymbolTable.methods().map { it.signature } +
                context.symbolTable.classInstances().map { it.methods }.unionAll()
            )
            .filter { method ->
                context.methodContext == null || methodCallTable.canUseDependency(
                    context.methodContext,
                    method,
                )
            }

        // no support for on demand method generation within methods
        if (methods.isEmpty() && context.methodContext != null) {
            throw MethodOnDemandException("Callable method unavailable for context ${context.methodContext.name}")
        }

        // generate new method if a callable method doesn't exist
        val method = if (methods.isEmpty() || selectionManager.generateNewMethod(context)) {
            generateMethod(context).signature
        } else {
            selectionManager.randomSelection(methods)
        }

        val (params, paramDeps) = method.params.subList(0, method.params.size - 1)
            .map { param -> generateExpression(context, param.type()) }
            .foldPair()

        // add call for method context
        if (context.methodContext != null) {
            methodCallTable.addDependency(method, context.methodContext)
        }

        val returns = method.returns
        val stateParam = ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE)

        return paramDeps + if (returns.isEmpty()) {
            // void method type
            VoidMethodCallAST(method, params + stateParam)
        } else {
            // non-void method type
            val idents = returns.map { r -> IdentifierAST(context.identifierNameGenerator.newValue(), r.type()) }
            idents.forEach { ident -> context.symbolTable.add(ident) }
            MultiDeclarationAST(idents, listOf(NonVoidMethodCallAST(method, params + stateParam)))
        }
    }

    override fun generateMapAssign(context: GenerationContext): List<StatementAST> {
        val keyType = selectionManager.selectType(context)
        val valueType = selectionManager.selectType(context)
        val mapType = MapType(keyType, valueType)

        val (map, mapDeps) = generateIdentifier(context, mapType, mutableConstraint = true, initialisedConstraint = true)
        val (key, keyDeps) = generateExpression(context.increaseExpressionDepth(), keyType)
        val (value, valueDeps) = generateExpression(context.increaseExpressionDepth(), valueType)

        return mapDeps + keyDeps + valueDeps + AssignmentAST(map, MapIndexAssignAST(map, key, value))
    }

    /* ==================================== EXPRESSIONS ==================================== */

    override fun generateExpression(
        context: GenerationContext,
        targetType: Type,
    ): Pair<ExpressionAST, List<StatementAST>> {
        return try {
            generateExpressionFromType(
                selectionManager.selectExpressionType(targetType, context),
                context,
                targetType,
            )
        } catch (e: IdentifierOnDemandException) {
            generateExpressionFromType(
                selectionManager.selectExpressionType(targetType, context, identifier = false),
                context,
                targetType,
            )
        }
    }

    private fun generateExpressionFromType(
        exprType: ExpressionType,
        context: GenerationContext,
        targetType: Type,
    ): Pair<ExpressionAST, List<StatementAST>> = when (exprType) {
        CONSTRUCTOR -> generateConstructorForType(context, targetType)
        UNARY -> generateUnaryExpression(context, targetType)
        BINARY -> generateBinaryExpression(context, targetType)
        TERNARY -> generateTernaryExpression(context, targetType)
        FUNCTION_METHOD_CALL -> generateFunctionMethodCall(context, targetType)
        IDENTIFIER -> generateIdentifier(context, targetType, initialisedConstraint = true)
        LITERAL -> generateLiteralForType(context, targetType as LiteralType)
        MAP_INDEX -> generateMapIndex(context, targetType)
        MAP_INDEX_ASSIGN -> generateMapIndexAssign(context, targetType)
    }

    override fun generateFunctionMethodCall(
        context: GenerationContext,
        targetType: Type,
    ): Pair<FunctionMethodCallAST, List<StatementAST>> {
        val callable = callableFunctionMethods(context, targetType)
        val functionMethod = if (callable.isEmpty()) {
            generateFunctionMethod(context, targetType).signature
        } else {
            selectionManager.randomSelection(callable)
        }

        val (arguments, deps) = functionMethod.params.subList(0, functionMethod.params.size - 1)
            .map { param ->
                generateExpression(context.increaseExpressionDepth(), param.type())
            }
            .foldPair()

        val fmCall = FunctionMethodCallAST(
            functionMethod,
            arguments + ClassInstanceAST(context.globalState(), PARAM_GLOBAL_STATE),
        )
        return Pair(fmCall, deps)
    }

    private fun callableFunctionMethods(
        context: GenerationContext,
        targetType: Type,
    ): List<FunctionMethodSignatureAST> =
        functionMethodsWithType(context, targetType)
            .filter { fm ->
                !fm.params
                    .map { p -> p.type() }
                    .any { t -> t is ConstructorType && !context.symbolTable.hasType(t) }
            }

    private fun functionMethodsWithType(
        context: GenerationContext,
        targetType: Type,
    ): List<FunctionMethodSignatureAST> =
        context.functionSymbolTable.withFunctionMethodType(targetType).map { it.signature } +
            context.symbolTable.classInstances().map { it.functionMethods }.unionAll()
                .filter { it.returnType == targetType }

    @Throws(IdentifierOnDemandException::class)
    override fun generateIdentifier(
        context: GenerationContext,
        targetType: Type,
        mutableConstraint: Boolean,
        initialisedConstraint: Boolean,
    ): Pair<IdentifierAST, List<StatementAST>> {
        val withType = context.symbolTable.withType(targetType).filter {
            (!mutableConstraint || it.mutable) && (!initialisedConstraint || it.initialised())
        }

        val deps = mutableListOf<StatementAST>()

        if (withType.isEmpty()) {
            if (!context.onDemandIdentifiers) throw IdentifierOnDemandException("Identifier of type $targetType not available")

            val decl = generateDeclarationStatementForType(context, targetType, true)
            deps += decl
        }

        val identifier = selectionManager.randomSelection(
            context.symbolTable.withType(targetType)
                .filter { (!mutableConstraint || it.mutable) && (!initialisedConstraint || it.initialised()) },
        )
        return Pair(identifier, deps)
    }

    override fun generateArrayIndex(
        context: GenerationContext,
        targetType: Type,
    ): Pair<ArrayIndexAST, List<StatementAST>> {
        val (identifier, deps) = generateIdentifier(context, ArrayType(targetType), initialisedConstraint = true)
        val index = IntegerLiteralAST(generateDecimalLiteralValue(false), false)

        return Pair(ArrayIndexAST(identifier, index), deps)
    }

    override fun generateSetDisplay(
        context: GenerationContext,
        targetType: Type,
    ): Pair<SetDisplayAST, List<StatementAST>> {
        val setType = targetType as SetType
        val numberOfExpressions = selectionManager.selectNumberOfConstructorFields()
        val (exprs, exprDeps) = (1..numberOfExpressions)
            .map { generateExpression(context.increaseExpressionDepth(), setType.innerType) }
            .foldPair()
        return Pair(SetDisplayAST(setType.innerType, exprs), exprDeps)
    }

    override fun generateMapConstructor(
        context: GenerationContext,
        targetType: Type,
    ): Pair<MapConstructorAST, List<StatementAST>> {
        val mapType = targetType as MapType
        val numberOfExpressions = selectionManager.selectNumberOfConstructorFields()
        val exprContext = context.increaseExpressionDepth()
        val (assigns, assignDeps) = (1..numberOfExpressions)
            .map {
                val (key, keyDeps) = generateExpression(exprContext, mapType.keyType)
                val (value, valueDeps) = generateExpression(exprContext, mapType.valueType)
                Pair(Pair(key, value), keyDeps + valueDeps)
            }
            .foldPair()

        return Pair(MapConstructorAST(mapType.keyType, mapType.valueType, assigns), assignDeps)
    }

    override fun generateMapIndexAssign(
        context: GenerationContext,
        targetType: Type,
    ): Pair<MapIndexAssignAST, List<StatementAST>> {
        val mapType = targetType as MapType
        val (map, mapDeps) = generateIdentifier(context, mapType)
        val (index, indexDeps) = generateExpression(context.increaseExpressionDepth(), targetType.keyType)
        val (newValue, newValueDeps) = generateExpression(context.increaseExpressionDepth(), targetType.valueType)

        return Pair(MapIndexAssignAST(map, index, newValue), mapDeps + indexDeps + newValueDeps)
    }

    override fun generateMapIndex(context: GenerationContext, targetType: Type): Pair<MapIndexAST, List<StatementAST>> {
        val mapType = MapType(selectionManager.selectType(context, true), targetType)
        val (map, mapDeps) = generateIdentifier(context, mapType)
        val (index, indexDeps) = generateExpression(context.increaseExpressionDepth(), mapType.keyType)

        return Pair(MapIndexAST(map, index), mapDeps + indexDeps)
    }

    override fun generateUnaryExpression(
        context: GenerationContext,
        targetType: Type,
    ): Pair<UnaryExpressionAST, List<StatementAST>> {
        val (expr, exprDeps) = generateExpression(context, targetType)
        val operator = selectionManager.selectUnaryOperator(targetType)

        return Pair(UnaryExpressionAST(expr, operator), exprDeps)
    }

    override fun generateBinaryExpression(
        context: GenerationContext,
        targetType: Type,
    ): Pair<BinaryExpressionAST, List<StatementAST>> {
        val nextDepthContext = context.increaseExpressionDepth()
        val (operator, type) = selectionManager.selectBinaryOperator(context, targetType)
        val (expr1, expr1Deps) = generateExpression(nextDepthContext, type.first)
        val (expr2, expr2Deps) = generateExpression(nextDepthContext, type.second)

        return Pair(BinaryExpressionAST(expr1, operator, expr2), expr1Deps + expr2Deps)
    }

    override fun generateTernaryExpression(
        context: GenerationContext,
        targetType: Type,
    ): Pair<TernaryExpressionAST, List<StatementAST>> {
        val (condition, conditionDeps) = generateExpression(context, BoolType)
        val (ifExpr, ifExprDeps) = generateExpression(context.increaseExpressionDepth(), targetType)
        val (elseExpr, elseExprDeps) = generateExpression(context.increaseExpressionDepth(), targetType)

        return Pair(TernaryExpressionAST(condition, ifExpr, elseExpr), conditionDeps + ifExprDeps + elseExprDeps)
    }

    override fun generateBaseExpressionForType(
        context: GenerationContext,
        targetType: Type,
    ): Pair<ExpressionAST, List<StatementAST>> = when (targetType) {
        is LiteralType -> generateLiteralForType(context, targetType)
        is ArrayType -> generateArrayInitialisation(context, targetType)
        is MapType -> generateMapConstructor(context, targetType)
        is SetType -> generateSetDisplay(context, targetType)
        // should not be possible to reach here
        else -> throw UnsupportedOperationException("Trying to generate base for non-base type $targetType")
    }

    private fun generateConstructorForType(
        context: GenerationContext,
        targetType: Type,
    ): Pair<ExpressionAST, List<StatementAST>> =
        when (targetType) {
            is ArrayType -> generateArrayInitialisation(context, targetType)
            is MapType -> generateMapConstructor(context, targetType)
            is SetType -> generateSetDisplay(context, targetType)
            else -> throw UnsupportedOperationException("Trying to generate constructor for non-constructor type")
        }

    private fun generateLiteralForType(
        context: GenerationContext,
        targetType: LiteralType,
    ): Pair<LiteralAST, List<StatementAST>> = when (targetType) {
        BoolType -> generateBooleanLiteral(context)
        CharType -> generateCharLiteral(context)
        IntType -> generateIntegerLiteral(context)
        RealType -> generateRealLiteral(context)
        // should not be possible to reach here
        else -> throw UnsupportedOperationException("Trying to generate literal for non-literal type $targetType")
    }

    override fun generateIntegerLiteral(context: GenerationContext): Pair<IntegerLiteralAST, List<StatementAST>> {
        val isHex = selectionManager.randomWeightedSelection(listOf(true to 0.7, false to 0.3))
        val value = generateDecimalLiteralValue(negative = true)
        return Pair(IntegerLiteralAST(value, isHex), emptyList())
    }

    override fun generateArrayInitialisation(
        context: GenerationContext,
        targetType: ArrayType,
    ): Pair<ArrayInitAST, List<StatementAST>> =
        Pair(ArrayInitAST(selectionManager.selectArrayLength(), targetType), emptyList())

    override fun generateBooleanLiteral(context: GenerationContext): Pair<BooleanLiteralAST, List<StatementAST>> =
        Pair(BooleanLiteralAST(selectionManager.selectBoolean()), emptyList())

    override fun generateRealLiteral(context: GenerationContext): Pair<RealLiteralAST, List<StatementAST>> {
        val beforePoint = generateDecimalLiteralValue(negative = true)
        val afterPoint = generateDecimalLiteralValue(negative = false)

        return Pair(RealLiteralAST("$beforePoint.$afterPoint"), emptyList())
    }

    override fun generateCharLiteral(context: GenerationContext): Pair<CharacterLiteralAST, List<StatementAST>> {
        val c = selectionManager.selectCharacter()
        return Pair(CharacterLiteralAST(c), emptyList())
    }

    override fun generateType(context: GenerationContext, literalOnly: Boolean): Type =
        selectionManager.selectType(context, literalOnly = literalOnly)

    private fun generateDecimalLiteralValue(negative: Boolean = true): String {
        var value = selectionManager.selectDecimalLiteral()

        if (negative && selectionManager.randomWeightedSelection(
                listOf(
                    true to 0.2,
                    false to 0.8,
                ),
            )
        ) {
            value *= -1
        }

        return value.toString()
    }

    companion object {
        private const val DAFNY_MAX_LOOP_COUNTER = 100
        private const val GLOBAL_STATE = "GlobalState"
        private const val PARAM_GLOBAL_STATE = "globalState"
    }
}
