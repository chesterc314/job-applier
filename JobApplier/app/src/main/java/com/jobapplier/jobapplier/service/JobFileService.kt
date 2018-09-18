package com.jobapplier.jobapplier.service

import com.jobapplier.jobapplier.model.FileNames
import java.io.File

object JobFileService {
    fun writeToErrorLogFile(jobResult: Failure, destinationFilePath: String) {
        val messages = jobResult.errorMessages
                .map { "${it.errorMessage},${it.additionalInfo}\n" }
                .fold("") { acc, it -> "$acc$it" }
        FileService.writeFile(messages, FileNames.JOB_ERROR_LOG, destinationFilePath, true)
    }

    fun writeToJobEntriesFile(jobResult: JobResult, destinationFilePath: String) {
        val jobEntriesContent = jobResult.jobEntries
                .map { "${it.username},${it.jobLink},${it.location},${it.cvPath},${it.toEmailAddress},${it.jobLink}\n" }
                .fold("") { acc, it -> "$acc$it" }
        FileService.writeFile(jobEntriesContent, FileNames.JOB_ENTRIES, destinationFilePath)
    }

    fun readFromJobEntriesFile(destinationFilePath: String): List<JobEntry>{
        val filePath = "$destinationFilePath${FileNames.JOB_ENTRIES}"
        if(File(filePath).exists()) {
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
        }else{
            return listOf()
        }
    }

}