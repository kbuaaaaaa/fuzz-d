package fuzzd.validator.executor.execution_handler

import fuzzd.utils.compareVersions

class PyExecutionHandler(fileDir: String, fileName: String, dafnyVersion: String) : AbstractExecutionHandler(fileDir, fileName, dafnyVersion=dafnyVersion) {
    override fun getCompileTarget(): String = "py"

    override fun getExecuteCommand(fileDir: String, fileName: String): String = if (compareVersions(dafnyVersion, "4.2.0") >= 0) {
        "python3 $fileDir/$fileName-py/__main__.py"
    } else {
        "python3 $fileDir/$fileName-py/main.py"
    }

}
