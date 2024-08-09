package fuzzd.validator

import fuzzd.validator.executor.ExecutionResult
import fuzzd.validator.executor.execution_handler.CsExecutionHandler
import fuzzd.validator.executor.execution_handler.GoExecutionHandler
import fuzzd.validator.executor.execution_handler.JavaExecutionHandler
import fuzzd.validator.executor.execution_handler.JsExecutionHandler
import fuzzd.validator.executor.execution_handler.PyExecutionHandler
import fuzzd.validator.executor.execution_handler.RustExecutionHandler
import fuzzd.validator.executor.execution_handler.VerificationHandler
import java.io.File

class OutputValidator {
    fun validateFile(
        fileDir: File,
        mainFileName: String,
        targetOutput: String?,
        verifier: Boolean,
        language: String,
    ): ValidationResult {
        val dafnyVersionProcess = Runtime.getRuntime().exec("dafny --version")
        val dafnyVersionOutput = dafnyVersionProcess.inputStream.bufferedReader().readText()
        val older: Boolean = if (dafnyVersionOutput.compareTo("4.5.0") <= 0){
            true
        } else{
            false
        }
        val fileDirPath = fileDir.path
        val executionHandlersMap = mapOf(
            "cs" to CsExecutionHandler(fileDirPath, mainFileName, older=older),
            "js" to JsExecutionHandler(fileDirPath, mainFileName, older=older),
            "py" to PyExecutionHandler(fileDirPath, mainFileName, older=older),
            "java" to JavaExecutionHandler(fileDirPath, mainFileName, older=older),
            "go" to GoExecutionHandler(fileDirPath, mainFileName, older=older),
            "rs" to RustExecutionHandler(fileDirPath, mainFileName, older=older)
        )
        var executionHandlers = listOf(
                CsExecutionHandler(fileDirPath, mainFileName, older=older),
                JsExecutionHandler(fileDirPath, mainFileName, older=older),
                PyExecutionHandler(fileDirPath, mainFileName, older=older),
                JavaExecutionHandler(fileDirPath, mainFileName, older=older),
                GoExecutionHandler(fileDirPath, mainFileName, older=older),
                RustExecutionHandler(fileDirPath, mainFileName, older=older)
            )
        if (language != "dafny" && language != "miscompilation" && language != ""){
            executionHandlers = listOf(executionHandlersMap[language]!!)
        }
        

        return if (verifier) {
            val verificationHandler = VerificationHandler(fileDirPath, mainFileName)
            execute(executionHandlers + verificationHandler)

            ValidationResult(executionHandlers, verificationHandler, targetOutput)
        } else {
            execute(executionHandlers)

            ValidationResult(executionHandlers, null, targetOutput)
        }
    }

    fun verifyFiles(fileDir: File, fileNames: List<String>): List<ExecutionResult> {
        val verificationHandlers = fileNames.map { VerificationHandler(fileDir.path, it) }
        execute(verificationHandlers)

        return verificationHandlers.map { it.verificationResult() }
    }

    private fun execute(runnables: List<Runnable>) {
        runnables.map { Thread(it) }
            .map { t -> t.start(); t }
            .map { t -> t.join() }
    }
}
