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

class ValidatorRunner(private val dir: File, private val logger: fuzzd.logging.Logger, private val interpret: Boolean, private val language: String) {
    private val validator = OutputValidator()
    private val interpreterRunner = InterpreterRunner(dir, logger)

    fun run(file: File, verify: Boolean) {
        if (interpret) {
            logger.log { "Lexing & Parsing ${file.name}" }
            val input = file.inputStream()
            val cs = CharStreams.fromStream(input)
            val tokens = org.antlr.v4.runtime.CommonTokenStream(dafnyLexer(cs))
            val ast = DafnyVisitor().visitProgram(dafnyParser(tokens).program())

            return run(ast, verify)
        } else {
            return {
                val validationResult = validator.validateFile(dir, "main", null, verify)
                if (language == ""){
                    logger.log { validationResult }
                }
                else{
                    logger.log { validationResult.prettyReport(language) }
                }
            }()
        }
    }

    fun run(ast: DafnyAST, verify: Boolean) {
        val output: Pair<String, List<StatementAST>> = runCatching {
            interpreterRunner.run(ast, false, verify)
        }.onFailure {
            it.printStackTrace()
            throw it
        }.getOrThrow()
        val validationResult = validator.validateFile(dir, "main", output.first, verify)
        if (language == ""){
            logger.log { validationResult }
        }
        else{
            logger.log { validationResult.prettyReport(language) }
        }
    }
}
