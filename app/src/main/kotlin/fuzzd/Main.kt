package fuzzd

import fuzzd.logging.Logger
import kotlinx.cli.*
import java.io.File
import java.util.UUID
import kotlin.random.Random

@OptIn(ExperimentalCli::class)
class Fuzz : Subcommand("fuzz", "Generate programs to test Dafny") {
    private val seed by option(ArgType.String, "seed", "s", "Generation Seed")
    private val verifier by option(ArgType.Boolean, "verifier", "v", "Generate annotated programs for testing the Dafny verifier")
    private val advanced by option(
        ArgType.Boolean,
        "advanced",
        "a",
        "Use advanced reconditioning to reduce use of safety wrappers",
    )
    private val instrument by option(
        ArgType.Boolean,
        "instrument",
        "i",
        "Instrument control flow with print statements for debugging program paths",
    )
    private val swarm by option(ArgType.Boolean, "swarm", "sw", "Run with swarm testing enabled")
    private val noRun by option(
        ArgType.Boolean,
        "noRun",
        "n",
        "Generate a program without running differential testing on it",
    )

    private val outputFile by option(ArgType.String, "output", "o", "Directory for output")

    override fun execute() {
        val fileDir = if (outputFile != null) {
            val file = File(outputFile!!)
            file.mkdir()
            file
        } else {
            val path = "output"
            File(path).mkdir()
            val dir = UUID.randomUUID().toString()
            File("$path/$dir")
        }

        val logger = Logger(fileDir)
        val generationSeed = seed?.toLong() ?: Random.Default.nextLong()

        try {
            FuzzRunner(fileDir, logger).run(
                generationSeed,
                advanced == true,
                instrument == true,
                noRun != true,
                swarm == true,
                verifier == true,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(1)
        } finally {
            logger.close()
        }
    }
}

@OptIn(ExperimentalCli::class)
class Recondition : Subcommand("recondition", "Recondition a reduced test case") {
    private val file by argument(ArgType.String, "file", "path to .dfy file to recondition")
    private val advanced by option(
        ArgType.Boolean,
        "advanced",
        "a",
        "Use advanced reconditioning to reduce use of safety wrappers",
    )

    override fun execute() {
        val file = File(file)
        val logger = Logger(file.absoluteFile.parentFile, fileName = "recondition.log")
        try {
            ReconditionRunner(file.absoluteFile.parentFile, logger).run(file, advanced == true, false)
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(1)
        } finally {
            logger.close()
        }
    }
}

@OptIn(ExperimentalCli::class)
class Interpret : Subcommand("interpret", "Interpret a valid .dfy file") {
    private val file by argument(ArgType.String, "file", "path to .dfy file to interpret")

    override fun execute() {
        val file = File(file)
        val logger = Logger(file.absoluteFile.parentFile, fileName = "interpret.log")
        try {
            InterpreterRunner(file.absoluteFile.parentFile, logger).run(file, false)
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(1)
        } finally {
            logger.close()
        }
    }
}

@OptIn(ExperimentalCli::class)
class Validate : Subcommand("validate", "Interpret and validate a .dfy file") {
    private val file by argument(ArgType.String, "file", "path to .dfy file to validate")
    private val interpret by option(ArgType.Boolean, "interpret", "i", "Interpret the file before validating")
    private val language by option(ArgType.String, "language", "l", "Language to validate against").default("")
    

    override fun execute() {
        val file = File(file)
        val logger = Logger(file.absoluteFile.parentFile, fileName = "fuzz-d.log")
        
        try {
            ValidatorRunner(file.absoluteFile.parentFile, file.name.substringBeforeLast(".dfy"),logger, interpret == true, language).run(file, false)
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(1)
        } finally {
            logger.close()
        }
    }
}

@OptIn(ExperimentalCli::class)
class VerifierFuzz : Subcommand("verifuzz", "Run fuzzing over the Dafny verifier") {
    private val seed by option(ArgType.String, "seed", "s", "Generation Seed")

    private val noRun by option(
        ArgType.Boolean,
        "noRun",
        "n",
        "Generate a program without running differential testing on it",
    )

    private val outputFile by option(ArgType.String, "output", "o", "Directory for output")

    override fun execute() {
        val fileDir = if (outputFile != null) {
            val file = File(outputFile!!)
            file.mkdir()
            file
        } else {
            val path = "output"
            File(path).mkdir()
            val dir = UUID.randomUUID().toString()
            File("$path/$dir")
        }

        val logger = Logger(fileDir)
        val generationSeed = seed?.toLong() ?: Random.Default.nextLong()

        try {
            VerifierFuzzRunner(fileDir, logger).run(generationSeed, noRun != true)
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(1)
        } finally {
            logger.close()
        }
    }
}

@OptIn(ExperimentalCli::class)
fun createArgParser(): ArgParser {
    val parser = ArgParser("fuzzd")
    val fuzz = Fuzz()
    val recondition = Recondition()
    val interpret = Interpret()
    val verifuzz = VerifierFuzz()
    val validate = Validate()
    parser.subcommands(fuzz, recondition, interpret, verifuzz, validate)

    return parser
}

fun main(args: Array<String>) {
    createArgParser().parse(args)
}
