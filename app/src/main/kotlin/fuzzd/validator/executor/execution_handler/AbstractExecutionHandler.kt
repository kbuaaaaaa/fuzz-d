package fuzzd.validator.executor.execution_handler

import fuzzd.utils.compileDafny
import fuzzd.utils.compareVersions
import fuzzd.utils.readErrorStream
import fuzzd.utils.readInputStream
import fuzzd.utils.runCommand
import fuzzd.validator.executor.ExecutionResult
import java.util.concurrent.TimeUnit

abstract class AbstractExecutionHandler(
    val fileDir: String,
    val fileName: String,
    val compileTimeout: Long = TIMEOUT_SECONDS,
    val executeTimeout: Long = TIMEOUT_SECONDS,
    val dafnyVersion: String,
) : ExecutionHandler {
    private var compileResult: ExecutionResult = ExecutionResult()
    private var executionResult: ExecutionResult = ExecutionResult()

    protected abstract fun getExecuteCommand(fileDir: String, fileName: String): String

    override fun compile(): ExecutionResult {
        val process = compileDafny(getCompileTarget(), fileDir, fileName, compileTimeout, dafnyVersion)
        try {
            val termination = process.waitFor(compileTimeout, TimeUnit.SECONDS)

            return ExecutionResult(
                termination,
                if (termination) process.exitValue() else TIMEOUT_RETURN_CODE,
                process.readInputStream(),
                process.readErrorStream(),
            )
        } catch (e: InterruptedException) {
            println("Compilation interrupted")
            Thread.currentThread().interrupt() // Preserve interrupt status
        } finally {
            process?.let {
                if (it.isAlive) {
                    println("Terminating process")
                    it.destroy()
                }
            }
        }

        return ExecutionResult(false, TIMEOUT_RETURN_CODE, "", "")
    }

    override fun compileResult(): ExecutionResult = compileResult

    override fun execute(): ExecutionResult {
        val process = runCommand("timeout $executeTimeout ${getExecuteCommand(fileDir, fileName)}")
        try{
            val termination = process.waitFor(executeTimeout, TimeUnit.SECONDS)

            return ExecutionResult(
                termination,
                if (termination) process.exitValue() else TIMEOUT_RETURN_CODE,
                process.readInputStream(),
                process.readErrorStream(),
            )
        } catch (e: InterruptedException) {
            println("Execution interrupted")
            Thread.currentThread().interrupt() // Preserve interrupt status
        } finally {
            process?.let {
                if (it.isAlive) {
                    println("Terminating process")
                    it.destroy()
                }
            }
        }

        return ExecutionResult(false, TIMEOUT_RETURN_CODE, "", "")
    }

    override fun executeResult(): ExecutionResult = executionResult

    override fun run() {
        
        compileResult = compile()

        if (compileResult.exitCode == 0) {
            executionResult = execute()
        }
    }

    companion object {
        const val TIMEOUT_SECONDS = 300L
        const val TIMEOUT_RETURN_CODE = 2
    }
}
