package fuzzd.validator.executor.execution_handler

class PyExecutionHandler(fileDir: String, fileName: String, older:Boolean) : AbstractExecutionHandler(fileDir, fileName, older=older) {
    override fun getCompileTarget(): String = "py"

    override fun getExecuteCommand(fileDir: String, fileName: String): String =
        "python3 $fileDir/$fileName-py/__main__.py"
}
