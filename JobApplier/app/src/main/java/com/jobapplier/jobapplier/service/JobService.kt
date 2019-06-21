package com.jobapplier.jobapplier.service

import com.jobapplier.jobapplier.model.ApplicationModel
import javax.mail.AuthenticationFailedException

abstract class ErrorMessage(val errorMessage: String, open val additionalInfo: String)
abstract class JobResult(open val jobEntries: List<JobEntry>)

data class JobErrorMessage(val link: String, val jobEmail: String, override val additionalInfo: String) : ErrorMessage("Error sending C.V. application", additionalInfo)
data class ScraperErrorMessage(override val additionalInfo: String) : ErrorMessage("Error retrieving jobs to send C.V.", additionalInfo)
data class JobEntry(val username: String,
                    val jobTitle: String,
                    val location: String,
                    val cvPath: String,
                    val toEmailAddress: String,
                    val jobLink: String)

data class Success(override val jobEntries: List<JobEntry>) : JobResult(jobEntries)
data class Failure(val errorMessages: List<ErrorMessage>, override val jobEntries: List<JobEntry>) : JobResult(jobEntries)

object JobService {
    fun apply(application: ApplicationModel, oldJobEntries: List<JobEntry>, errorAction: (Failure) -> Unit, successAction: (JobResult) -> Unit) {
        val errors = ArrayList<ErrorMessage>()
        val jobEntries = ArrayList<JobEntry>()
        try {
            JobScraperService.getAsyncJobs(application.jobTitle, application.location) { jobs ->
                val newJobApplications = jobs.filter {
                    !oldJobEntries.any { job -> job.username == application.email && job.jobLink == it.link && job.toEmailAddress == it.email }
                }
                newJobApplications.forEach {
                    val jobEntry = JobEntry(application.email, application.jobTitle, application.location, application.cvFilePath, it.email, it.link)
                    val displayName = "${application.firstName} ${application.lastName}"
                    val subject = application.messageSubject.replace("{displayName}", displayName)
                    val fileName = "$subject.pdf"
                    val emailMessage = EmailMessage(
                            toAddress = it.email,
                            subject = subject,
                            message = application.messageTemplate
                                    .replace("{jobLink}", it.link)
                                    .replace("{email}", application.email)
                                    .replace("{cell}", application.cell)
                                    .replace("{displayName}", displayName),
                            attachment = Attachment(fileName, application.cvFilePath)
                    )
                    val email = Email(application.email, application.password, emailMessage)
                    sendEmailAsyncAndHandleErrors(email, application, errors, it, jobEntries, jobEntry)
                }
                if (errors.isNotEmpty()) {
                    val failure = Failure(errors, jobEntries)
                    successAction(failure)
                    errorAction(failure)
                } else {
                    val success = Success(jobEntries)
                    successAction(success)
                }
            }
        } catch (e: Exception) {
            errors.add(ScraperErrorMessage("Your Email: ${application.email} Job Title: ${application.jobTitle} and Location: ${application.location}"))
            val failure = Failure(errors, jobEntries)
            errorAction(failure)
        }
    }

    private fun sendEmailAsyncAndHandleErrors(email: Email,
                                              application: ApplicationModel,
                                              errors: ArrayList<ErrorMessage>,
                                              it: Job,
                                              jobEntries: ArrayList<JobEntry>,
                                              jobEntry: JobEntry) {
        EmailService.sendAsyncEmail(email) { ex ->
            when (ex) {
                is AuthenticationFailedException -> {
                    val additionalInfo =
                            "Error with email authentication Your Email: ${application.email}." +
                                    "Please check that your email and password is correct or please enable less secure applications on your Gmail account, here: https://www.google.com/settings/security/lesssecureapps"
                    errors.add(JobErrorMessage(it.link, it.email, additionalInfo))
                    jobEntries.remove(jobEntry)
                }
                is Exception -> {
                    errors.add(JobErrorMessage(it.link, it.email, "Error sending email for this job. Your Email: ${application.email} and CV path: ${application.cvFilePath}"))
                    jobEntries.remove(jobEntry)
                }
                else -> jobEntries.add(jobEntry)
            }
        }
    }
}