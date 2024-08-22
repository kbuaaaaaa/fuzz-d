package fuzzd.validator.executor.execution_handler

class GoExecutionHandler(fileDir: String, fileName: String, older: Int) : AbstractExecutionHandler(fileDir, fileName, older=older) {
    override fun getCompileTarget(): String = "go"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = "$fileDir/$fileName"
}
