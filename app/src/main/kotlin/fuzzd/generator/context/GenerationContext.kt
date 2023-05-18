package fuzzd.generator.context

import fuzzd.generator.ast.ClassAST
import fuzzd.generator.ast.MethodSignatureAST
import fuzzd.generator.ast.identifier_generator.NameGenerator.IdentifierNameGenerator
import fuzzd.generator.ast.identifier_generator.NameGenerator.LoopCounterGenerator
import fuzzd.generator.symbol_table.FunctionSymbolTable
import fuzzd.generator.symbol_table.GlobalSymbolTable
import fuzzd.generator.symbol_table.SymbolTable

data class GenerationContext(
    val globalSymbolTable: GlobalSymbolTable,
    val functionSymbolTable: FunctionSymbolTable,
    val statementDepth: Int = 1,
    val expressionDepth: Int = 1,
    val symbolTable: SymbolTable = SymbolTable(),
    val identifierNameGenerator: IdentifierNameGenerator = IdentifierNameGenerator(),
    val loopCounterGenerator: LoopCounterGenerator = LoopCounterGenerator(),
    val methodContext: MethodSignatureAST? = null, // track if we are inside a method
    val onDemandIdentifiers: Boolean = true,
    val functionCalls: Boolean = true,
    val effectfulStatements: Boolean = true,
) {
    private var globalState: ClassAST? = null

    fun globalState(): ClassAST = globalState!!

    fun setGlobalState(globalState: ClassAST?): GenerationContext {
        this.globalState = globalState
        return this
    }

    fun increaseExpressionDepth(): GenerationContext =
        GenerationContext(
            globalSymbolTable,
            functionSymbolTable,
            statementDepth,
            expressionDepth + 1,
            symbolTable,
            identifierNameGenerator,
            loopCounterGenerator,
            methodContext,
            onDemandIdentifiers,
            functionCalls,
            effectfulStatements,
        ).setGlobalState(globalState)

    fun increaseExpressionDepthWithSymbolTable(): GenerationContext =
        GenerationContext(
            globalSymbolTable,
            functionSymbolTable,
            statementDepth,
            expressionDepth + 1,
            SymbolTable(symbolTable),
            identifierNameGenerator,
            loopCounterGenerator,
            methodContext,
            onDemandIdentifiers,
            functionCalls,
            effectfulStatements,
        ).setGlobalState(globalState)

    fun increaseStatementDepth(): GenerationContext =
        GenerationContext(
            globalSymbolTable,
            functionSymbolTable,
            statementDepth + 1,
            expressionDepth,
            SymbolTable(symbolTable),
            identifierNameGenerator,
            loopCounterGenerator,
            methodContext,
            onDemandIdentifiers,
            functionCalls,
            effectfulStatements,
        ).setGlobalState(globalState)

    fun disableOnDemand(): GenerationContext =
        GenerationContext(
            globalSymbolTable,
            functionSymbolTable,
            statementDepth,
            expressionDepth,
            symbolTable,
            identifierNameGenerator,
            loopCounterGenerator,
            methodContext,
            false,
            functionCalls,
            effectfulStatements,
        ).setGlobalState(globalState)

    fun disableFunctionCalls(): GenerationContext =
        GenerationContext(
            globalSymbolTable,
            functionSymbolTable,
            statementDepth,
            expressionDepth,
            symbolTable,
            identifierNameGenerator,
            loopCounterGenerator,
            methodContext,
            onDemandIdentifiers,
            false,
            effectfulStatements,
        ).setGlobalState(globalState)

    fun disableEffectfulStatements(): GenerationContext =
        GenerationContext(
            globalSymbolTable,
            functionSymbolTable,
            statementDepth,
            expressionDepth,
            symbolTable,
            identifierNameGenerator,
            loopCounterGenerator,
            methodContext,
            onDemandIdentifiers,
            functionCalls,
            false,
        ).setGlobalState(globalState)

    fun withSymbolTable(symbolTable: SymbolTable): GenerationContext =
        GenerationContext(
            globalSymbolTable,
            functionSymbolTable,
            statementDepth,
            expressionDepth,
            symbolTable,
            identifierNameGenerator,
            loopCounterGenerator,
            methodContext,
            onDemandIdentifiers,
            effectfulStatements,
        ).setGlobalState(globalState)
}
