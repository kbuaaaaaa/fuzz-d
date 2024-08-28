package fuzzd.validator.executor.execution_handler

class JsExecutionHandler(fileDir: String, fileName: String, dafnyVersion: String) : AbstractExecutionHandler(fileDir, fileName, dafnyVersion=dafnyVersion) {
    override fun getCompileTarget(): String = "js"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = "node $fileDir/$fileName.js"
}
