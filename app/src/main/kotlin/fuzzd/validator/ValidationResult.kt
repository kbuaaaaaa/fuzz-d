package fuzzd.validator

import fuzzd.utils.indent
import fuzzd.validator.executor.execution_handler.ExecutionHandler
import fuzzd.validator.executor.execution_handler.JavaExecutionHandler
import fuzzd.validator.executor.execution_handler.VerificationHandler

class ValidationResult(handlers: List<ExecutionHandler>,val verificationHandler: VerificationHandler?, val targetOutput: String?) {
    private val succeededCompile: List<ExecutionHandler>
    private val failedCompile: List<ExecutionHandler>
    private val succeededExecute: List<ExecutionHandler>
    private val failedExecute: List<ExecutionHandler>

    init {
        succeededCompile = handlers.filter { h ->
            val c = h.compileResult(); c.terminated && c.exitCode == 0
        }

        failedCompile = handlers.filter { h ->
            val c = h.compileResult(); !c.terminated || c.exitCode != 0
        }

        succeededExecute = succeededCompile.filter { h ->
            val e = h.executeResult(); e.terminated && e.exitCode == 0
        }

        failedExecute = succeededCompile.filter { h ->
            val e = h.executeResult(); !e.terminated || e.exitCode != 0
        }
    }

    private fun differentOutput(): Boolean =
        succeededExecute.any { h ->
            h.executeResult().stdOut != (targetOutput ?: succeededExecute[0].executeResult().stdOut)
        }

    override fun toString(): String {
        val sb = StringBuilder()

        if (verificationHandler != null) {
            sb.appendLine("--------------------------------- VERIFICATION -------------------------------")
            sb.appendLine("${verificationHandler.verificationResult()}")
        }

        sb.appendLine("--------------------------------- COMPILE FAILED -------------------------------")
        failedCompile.forEach { h -> sb.append("${h.getCompileTarget()}:\n${indent(h.compileResult())} \n") }

        sb.appendLine("--------------------------------- EXECUTE FAILED -------------------------------")
        failedExecute.forEach { h -> sb.append("${h.getCompileTarget()}:\n${indent(h.executeResult())}\n") }

        sb.appendLine("--------------------------------- EXECUTE SUCCEEDED -------------------------------")
        sb.appendLine("Target Output: $targetOutput")
        succeededExecute.forEach { h -> sb.append("${h.getCompileTarget()}:\n${indent(h.executeResult())}\n") }

        sb.appendLine("Java crash: ${failedCompile.filterIsInstance<JavaExecutionHandler>().isNotEmpty()}")
        sb.appendLine("Compiler crash: ${failedCompile.filter { it !is JavaExecutionHandler }.isNotEmpty()}")
        sb.appendLine("Execute crash: ${failedExecute.isNotEmpty()}")
        sb.appendLine("Different output: ${differentOutput()}")

        return sb.toString()
    }

    fun prettyReport(targetLanguage: String): String {
        val sb = StringBuilder()

        if (targetLanguage == "miscompilation") {
            sb.append("Behaviour:\nThere might be a miscompilation in some language backends\n")
            sb.append("Command:\n")
            succeededExecute.forEach { h -> sb.append("dafny run main.dfy -t ${h.getCompileTarget()} --no-verify --allow-warnings\n")}
            succeededExecute.forEach { h -> sb.append("${h.getCompileTarget()} output:\n${indent(h.executeResult())}\n")}
            sb.append("Different output: true\n")
        }
        else if (targetLanguage == "dafny") {
            failedCompile.firstOrNull()?.let { h ->
                sb.append("Behaviour:\nCompilation of program failed\n")
                sb.append("Command:\ndafny build main.dfy -t <any language> --no-verify --allow-warnings\n")
                sb.append("Output:\n${h.compileResult()}\n")
            }

            failedExecute.firstOrNull()?.let { h ->
                sb.append("Behaviour:\nExecution of program failed\n")
                sb.append("Command:\ndafny run main.dfy -t ${h.getCompileTarget()} --no-verify --allow-warnings\n")
                sb.append("Output:\n${indent(h.executeResult())}\n")
            }

        }
        else {
            failedCompile.filter { h -> h.getCompileTarget() == targetLanguage }.forEach { h ->
                sb.append("Behaviour:\nCompilation of program failed\n")
                sb.append("Command:\ndafny build main.dfy -t ${h.getCompileTarget()} --no-verify --allow-warnings\n")
                sb.append("Output:\n${h.compileResult()}\n")
            }

            failedExecute.filter { h -> h.getCompileTarget() == targetLanguage }.forEach { h ->
                sb.append("Behaviour:\nExecution of program failed\n")
                sb.append("Command:\ndafny run main.dfy -t ${h.getCompileTarget()} --no-verify --allow-warnings\n")
                sb.append("Output:\n${h.executeResult()}\n")
            }
        }

        return sb.toString()
    }
}
