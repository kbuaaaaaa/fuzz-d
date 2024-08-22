package fuzzd.validator.executor.execution_handler

class JsExecutionHandler(fileDir: String, fileName: String, older: Int) : AbstractExecutionHandler(fileDir, fileName, older=older) {
    override fun getCompileTarget(): String = "js"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = "node $fileDir/$fileName.js"
}
