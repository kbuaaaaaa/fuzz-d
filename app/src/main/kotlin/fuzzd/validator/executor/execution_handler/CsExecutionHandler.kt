package fuzzd.validator.executor.execution_handler

class CsExecutionHandler(
    fileDir: String,
    fileName: String,
    compileTimeout: Long = TIMEOUT_SECONDS,
    executeTimeout: Long = TIMEOUT_SECONDS,
    older: Boolean,
) : AbstractExecutionHandler(fileDir, fileName, compileTimeout, executeTimeout, older=older) {
    override fun getCompileTarget(): String = "cs"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = "dotnet $fileDir/$fileName.dll"
}
