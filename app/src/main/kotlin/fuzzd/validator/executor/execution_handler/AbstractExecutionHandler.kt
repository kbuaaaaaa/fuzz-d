package fuzzd.validator.executor.execution_handler

import fuzzd.utils.compileDafny
import fuzzd.utils.readErrorStream
import fuzzd.utils.readInputStream
import fuzzd.utils.runCommand
import fuzzd.validator.executor.ExecutionResult
import java.util.concurrent.TimeUnit

abstract class AbstractExecutionHandler(val fileDir: String, val fileName: String) : ExecutionHandler {
    private var compileResult: ExecutionResult = ExecutionResult()
    private var executionResult: ExecutionResult = ExecutionResult()

    protected abstract fun getExecuteCommand(fileDir: String, fileName: String): String

    override fun compile(): ExecutionResult {
        val process = compileDafny(getCompileTarget(), fileDir, fileName)
        val termination = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        return ExecutionResult(
            termination,
            if (termination) process.exitValue() else TIMEOUT_RETURN_CODE,
            process.readInputStream(),
            process.readErrorStream()
        )
    }

    override fun compileResult(): ExecutionResult = compileResult

    override fun execute(): ExecutionResult {
        val process = runCommand(getExecuteCommand(fileDir, fileName))
        val termination = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        return ExecutionResult(
            termination,
            if (termination) process.exitValue() else TIMEOUT_RETURN_CODE,
            process.readInputStream(),
            process.readErrorStream()
        )
    }

    override fun executeResult(): ExecutionResult = executionResult

    override fun run() {
        compileResult = compile()

        if (compileResult.exitCode == 0) {
            executionResult = execute()
        }
    }

    companion object {
        private const val TIMEOUT_SECONDS = 15L
        private const val TIMEOUT_RETURN_CODE = 2
    }
}
