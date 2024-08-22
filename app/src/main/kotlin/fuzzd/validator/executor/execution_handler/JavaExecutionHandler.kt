package fuzzd.validator.executor.execution_handler

class JavaExecutionHandler(fileDir: String, fileName: String, older: Int) : AbstractExecutionHandler(fileDir, fileName, older=older) {
    override fun getCompileTarget(): String = "java"

    override fun getExecuteCommand(fileDir: String, fileName: String): String =
        "java -jar $fileDir/$fileName.jar"
}
