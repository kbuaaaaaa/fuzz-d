package fuzzd.validator.executor.execution_handler

class GoExecutionHandler(fileDir: String, fileName: String, dafnyVersion: String) : AbstractExecutionHandler(fileDir, fileName, dafnyVersion=dafnyVersion) {
    override fun getCompileTarget(): String = "go"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = "$fileDir/$fileName"
}
