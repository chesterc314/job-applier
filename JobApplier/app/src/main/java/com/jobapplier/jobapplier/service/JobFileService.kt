package com.jobapplier.jobapplier.service

import com.jobapplier.jobapplier.model.FileNames
import java.io.File
import java.lang.StringBuilder

object JobFileService {
    fun writeToErrorLogFile(jobResult: Failure, destinationFilePath: String) {
        val builder = StringBuilder()
        jobResult.errorMessages.forEach {
            builder.appendln("${it.errorMessage},${it.additionalInfo}")
        }
        FileService.writeFile(builder.toString(), FileNames.JOB_ERROR_LOG, destinationFilePath, true)
    }

    fun writeToJobEntriesFile(jobResult: JobResult, destinationFilePath: String) {
        if(jobResult.jobEntries.isEmpty()){
            return
        }
        val builder = StringBuilder()
        jobResult.jobEntries.forEach {
            builder.appendln("${it.username},${it.jobLink},${it.location},${it.cvPath},${it.toEmailAddress},${it.jobLink}")
        }
        FileService.writeFile(builder.toString(), FileNames.JOB_ENTRIES, destinationFilePath)
    }

    fun readFromJobEntriesFile(destinationFilePath: String): List<JobEntry> {
        val filePath = "$destinationFilePath/${FileNames.JOB_ENTRIES}"
        if (File(filePath).exists()) {
            val lines = FileService.readFile(filePath)
            return lines.map { it ->
                val parts = it.split(",")
                JobEntry(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4],
                        parts[5]
                )
            }
        } else {
            return listOf()
        }
    }

}