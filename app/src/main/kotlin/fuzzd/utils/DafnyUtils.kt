package fuzzd.utils

import java.io.File
import java.lang.ProcessBuilder.Redirect

const val DAFNY_ADVANCED = "advanced"
const val DAFNY_MAIN = "main"
const val DAFNY_TYPE = "dfy"
const val DAFNY_GENERATED = "generated"

fun runCommand(command: String): Process {
    return Runtime.getRuntime().exec(command)
}

fun compareVersions(version1: String, version2: String): Int {
    val version1Parts = version1.split(".")
    val version2Parts = version2.split(".")
    if (version1Parts[0].toInt() > version2Parts[0].toInt()) {
        return 1
    } else if (version1Parts[0].toInt() == version2Parts[0].toInt()) {
        if (version1Parts[1].toInt() > version2Parts[1].toInt()) {
            return 1
        } else if (version1Parts[1].toInt() == version2Parts[1].toInt()) {
            return 0
        }
    }
    return -1
}

fun compileDafny(targetLanguage: String, fileDir: String, fileName: String, timeout: Long, dafnyVersion: String): Process {
    val processBuilder = if (compareVersions(dafnyVersion, "4.5.0") < 0){
            if (compareVersions(dafnyVersion, "3.10.0") < 0){
                ProcessBuilder(
                "timeout",
                timeout.toString(),
                "dafny",
                "/compileVerbose:0",
                "/noVerify",
                "/compile:2",
                "/spillTargetCode:1",
                "/compileTarget:$targetLanguage",
                "/functionSyntax:4",
                "$fileDir/$fileName.dfy",
                )
            } else{
                ProcessBuilder(
                "timeout",
                timeout.toString(),
                "dafny",
                "build",
                "$fileDir/$fileName.dfy",
                "-t",
                "$targetLanguage",
                "--no-verify",
                "--function-syntax:4",
                )
            }
        } else{
            ProcessBuilder(
            "timeout",
            timeout.toString(),
            "dafny",
            "build",
            "$fileDir/$fileName.dfy",
            "-t",
            "$targetLanguage",
            "--no-verify",
            "--allow-warnings",
            "--function-syntax:4",
            )
        }  
    return processBuilder.start()
}

fun verifyDafny(fileDir: String, fileName: String, timeout: Long): Process {
    val command = "timeout $timeout dafny /compile:0 $fileDir/$fileName.dfy"
    return runCommand(command)
}

fun Process.readInputStream(): String = String(inputStream.readAllBytes())

fun Process.readErrorStream(): String = String(errorStream.readAllBytes())
