package com.jobapplier.jobapplier

import com.jobapplier.jobapplier.model.ApplicationModel
import com.jobapplier.jobapplier.model.MessageTemplate
import com.jobapplier.jobapplier.service.Failure
import com.jobapplier.jobapplier.service.JobService
import com.jobapplier.jobapplier.service.Success
import org.junit.Assert
import org.junit.Test

class JobServiceUnitTest {
    @Test
    fun apply_executeSuccessfully(){
        val application = ApplicationModel(
                "Cashier",
                "Johannesburg",
                "John",
                "Smith",
                "",
                "0611231233",
                "",
                "",
                MessageTemplate.JOB_MESSAGE_TEMPLATE_SUBJECT,
                MessageTemplate.JOB_MESSAGE_TEMPLATE)
        val jobResult = JobService.apply(application, ArrayList())
        Assert.assertTrue( jobResult is Success)
    }
}