package com.jobapplier.jobapplier.worker

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.jobapplier.jobapplier.model.ApplicationModel
import com.jobapplier.jobapplier.model.MessageTemplate
import com.jobapplier.jobapplier.service.Failure
import com.jobapplier.jobapplier.service.JobFileService
import org.jetbrains.annotations.Nullable
import java.util.concurrent.TimeUnit

class JobApplierJobCreator : JobCreator {
    @Nullable
    override fun create(tag: String): Job? {
        return when (tag) {
            JobApplierJob.TAG -> JobApplierJob()
            else -> null
        }
    }
}

class JobApplierJob : Job() {
    override fun onRunJob(params: Job.Params): Job.Result {
        val extras = params.extras
        val application = ApplicationModel(
                extras.getString("jobTitle", "").toLowerCase(),
                extras.getString("location", "").toLowerCase(),
                extras.getString("firstName", ""),
                extras.getString("lastName", ""),
                extras.getString("email", ""),
                extras.getString("cell", ""),
                extras.getString("cvFilePath", ""),
                extras.getString("password", ""),
                MessageTemplate.JOB_MESSAGE_TEMPLATE_SUBJECT,
                MessageTemplate.JOB_MESSAGE_TEMPLATE
        )
        val destinationFilePath = extras.getString("filePath", "")
        val oldJobEntries = JobFileService.readFromJobEntriesFile(destinationFilePath)
        val jobResult = com.jobapplier.jobapplier.service.JobService.apply(application, oldJobEntries)
        when (jobResult) {
            is Failure -> {
                JobFileService.writeToErrorLogFile(jobResult, destinationFilePath)
                JobFileService.writeToJobEntriesFile(jobResult, destinationFilePath)
            }
            else -> {
                JobFileService.writeToJobEntriesFile(jobResult, destinationFilePath)
            }
        }

        return Job.Result.SUCCESS
    }

    companion object {
        const val TAG = "job_applier"
        fun scheduleJob(values: Map<String, String>): Int? {
            val extras = PersistableBundleCompat()
            values.forEach { it ->
                extras.putString(it.key, it.value)
            }

            val jobRequests = JobManager.instance().getAllJobRequestsForTag(JobApplierJob.TAG)
            return if (!jobRequests.isEmpty()) {
                null
            } else {
                JobRequest.Builder(JobApplierJob.TAG)
                        .setPeriodic(TimeUnit.MINUTES.toMillis(15)) //TODO: Set back to 24 hours
                        .setUpdateCurrent(true)
                        .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                        .setRequirementsEnforced(true)
                        .setExtras(extras)
                        .build()
                        .schedule()
            }
        }
    }
}