package fuzzd.generator.selection.probability_manager

class VerifierProbabilityManager(val probabilityManager: ProbabilityManager) : ProbabilityManager {
    override fun classType(): Double = 0.0

    override fun traitType(): Double = 0.0

    override fun datatype(): Double = probabilityManager.datatype()

    override fun arrayType(): Double = 0.0

    override fun datatstructureType(): Double = probabilityManager.datatstructureType()

    override fun literalType(): Double = probabilityManager.literalType()

    override fun setType(): Double = probabilityManager.setType()

    override fun multisetType(): Double = probabilityManager.multisetType()

    override fun mapType(): Double = probabilityManager.multisetType()

    override fun sequenceType(): Double = probabilityManager.sequenceType()

    override fun stringType(): Double = probabilityManager.stringType()

    override fun intType(): Double = probabilityManager.intType()

    override fun boolType(): Double = probabilityManager.boolType()

    override fun charType(): Double = probabilityManager.charType()

    override fun ifStatement(): Double = probabilityManager.ifStatement()

    override fun matchStatement(): Double = probabilityManager.matchStatement()

    override fun forallStatement(): Double = 0.0

    override fun forLoopStatement(): Double = probabilityManager.forLoopStatement()

    override fun whileStatement(): Double = probabilityManager.whileStatement() / 3

    override fun methodCall(): Double = probabilityManager.methodCall()

    override fun mapAssign(): Double = probabilityManager.mapAssign()

    override fun assignStatement(): Double = probabilityManager.assignStatement()

    override fun classInstantiation(): Double = 0.0

    override fun assignIdentifier(): Double = probabilityManager.assignIdentifier()

    override fun assignArrayIndex(): Double = 0.0

    override fun binaryExpression(): Double = probabilityManager.binaryExpression()

    override fun unaryExpression(): Double = probabilityManager.unaryExpression()

    override fun modulusExpression(): Double = probabilityManager.modulusExpression()

    override fun multisetConversion(): Double = probabilityManager.multisetConversion()

    override fun functionCall(): Double = probabilityManager.functionCall()

    override fun ternary(): Double = probabilityManager.ternary()

    override fun matchExpression(): Double = probabilityManager.matchExpression()

    override fun assignExpression(): Double = probabilityManager.assignExpression()

    override fun indexExpression(): Double = probabilityManager.indexExpression()

    override fun identifier(): Double = probabilityManager.identifier()

    override fun literal(): Double = probabilityManager.literal()

    override fun constructor(): Double = probabilityManager.constructor()

    override fun comprehension(): Double = probabilityManager.comprehension()

    override fun comprehensionConditionIntRange(): Double = probabilityManager.comprehensionConditionIntRange()

    override fun arrayIndexType(): Double = 0.0

    override fun mapIndexType(): Double = probabilityManager.mapIndexType()

    override fun multisetIndexType(): Double = probabilityManager.multisetIndexType()

    override fun sequenceIndexType(): Double = probabilityManager.sequenceIndexType()

    override fun stringIndexType(): Double = probabilityManager.stringIndexType()

    override fun datatypeIndexType(): Double = 0.0

    override fun arrayInitDefault(): Double = 0.0

    override fun arrayInitComprehension(): Double = 0.0

    override fun arrayInitValues(): Double = 0.0

    override fun methodStatements(): Int = probabilityManager.methodStatements()

    override fun ifBranchStatements(): Int = probabilityManager.ifBranchStatements()

    override fun forLoopBodyStatements(): Int = probabilityManager.forLoopBodyStatements()

    override fun whileBodyStatements(): Int = probabilityManager.whileBodyStatements()

    override fun mainFunctionStatements(): Int = probabilityManager.mainFunctionStatements()

    override fun matchStatements(): Int = probabilityManager.matchStatements()

    override fun comprehensionIdentifiers(): Int = probabilityManager.comprehensionIdentifiers()

    override fun numberOfTraits(): Int = 0
}