package fuzzd.validator

import fuzzd.validator.executor.ExecutionResult
import fuzzd.validator.executor.execution_handler.CsExecutionHandler
import fuzzd.validator.executor.execution_handler.GoExecutionHandler
import fuzzd.validator.executor.execution_handler.JavaExecutionHandler
import fuzzd.validator.executor.execution_handler.JsExecutionHandler
import fuzzd.validator.executor.execution_handler.PyExecutionHandler
import fuzzd.validator.executor.execution_handler.RustExecutionHandler
import fuzzd.validator.executor.execution_handler.VerificationHandler
import fuzzd.utils.compareVersions
import java.io.File

class OutputValidator {
    fun validateFile(
        fileDir: File,
        mainFileName: String,
        targetOutput: String?,
        verifier: Boolean,
        language: String,
    ): ValidationResult {
        var dafnyVersionProcess = Runtime.getRuntime().exec("dafny /version")
        var dafnyVersionOutput = dafnyVersionProcess.inputStream.bufferedReader().readText()
        dafnyVersionOutput = dafnyVersionOutput.replace(Regex("(?i)dafny "), "")
        val fileDirPath = fileDir.path
        val executionHandlersMap = mapOf(
                "rs" to RustExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                "cs" to CsExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                "js" to JsExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                "py" to PyExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                "java" to JavaExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                "go" to GoExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput)
            )

        var executionHandlers = listOf(
                RustExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                CsExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                JsExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                PyExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                JavaExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput),
                GoExecutionHandler(fileDirPath, mainFileName, dafnyVersion=dafnyVersionOutput)
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
