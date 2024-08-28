package fuzzd.validator.executor.execution_handler

class PyExecutionHandler(fileDir: String, fileName: String, older: Int) : AbstractExecutionHandler(fileDir, fileName, older=older) {
    override fun getCompileTarget(): String = "py"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = if (older < 2) {
            "python3 $fileDir/$fileName-py/__main__.py"
        } else {
            "python3 $fileDir/$fileName-py/$fileName.py"
        }
}
