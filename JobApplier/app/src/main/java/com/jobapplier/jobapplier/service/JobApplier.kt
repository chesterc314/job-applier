package com.jobapplier.jobapplier.service

import com.jobapplier.jobapplier.model.ApplicationModel
import com.jobapplier.jobapplier.model.MessageTemplate

object JobApplier {
    fun applyForJob(values: Map<String, String>): JobResult {
        val application = ApplicationModel(
                values.getOrDefault("jobTitle", "").toLowerCase(),
                values.getOrDefault("location", "").toLowerCase(),
                values.getOrDefault("firstName", ""),
                values.getOrDefault("lastName", ""),
                values.getOrDefault("email", ""),
                values.getOrDefault("cell", ""),
                values.getOrDefault("cvFilePath", ""),
                values.getOrDefault("password", ""),
                MessageTemplate.JOB_MESSAGE_TEMPLATE_SUBJECT,
                MessageTemplate.JOB_MESSAGE_TEMPLATE
        )
        val destinationFilePath: String = values.getOrDefault("filePath", "")
        val oldJobEntries: List<JobEntry> = JobFileService.readFromJobEntriesFile(destinationFilePath).distinctBy { it.jobLink }
        return JobService.apply(application, oldJobEntries, {
            JobFileService.writeToErrorLogFile(it, destinationFilePath)
        }, {
            JobFileService.writeToJobEntriesFile(it, destinationFilePath)
        })
    }
}