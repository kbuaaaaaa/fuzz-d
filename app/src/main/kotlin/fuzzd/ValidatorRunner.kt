package fuzzd

import dafnyLexer
import dafnyParser
import fuzzd.generator.ast.DafnyAST
import fuzzd.generator.ast.StatementAST
import fuzzd.interpreter.Interpreter
import fuzzd.validator.OutputValidator
import fuzzd.logging.OutputWriter
import fuzzd.recondition.visitor.DafnyVisitor
import org.antlr.v4.runtime.CharStreams
import java.io.File

class ValidatorRunner(private val dir: File, private val logger: fuzzd.logging.Logger) {
    private val validator = OutputValidator()
    private val interpreterRunner = InterpreterRunner(dir, logger)

    fun run(file: File, verify: Boolean) {
        logger.log { "Lexing & Parsing ${file.name}" }
        val input = file.inputStream()
        val cs = CharStreams.fromStream(input)
        val tokens = org.antlr.v4.runtime.CommonTokenStream(dafnyLexer(cs))
        val ast = DafnyVisitor().visitProgram(dafnyParser(tokens).program())

        return run(ast, verify)
    }

    fun run(ast: DafnyAST, verify: Boolean) {
        val output: Pair<String, List<StatementAST>> = runCatching {
            interpreterRunner.run(ast, false, verify)
        }.onFailure {
            it.printStackTrace()
        }.getOrThrow()
        val validationResult = validator.validateFile(dir, "main", output.first, verify)
        logger.log { validationResult }
    }
}