package com.jobapplier.jobapplier

import com.jobapplier.jobapplier.service.Attachment
import com.jobapplier.jobapplier.service.EmailMessage
import com.jobapplier.jobapplier.service.EmailService
import org.junit.Test

class EmailServiceUnitTest {
    @Test
    fun sendEmail_executesSuccessfully() {
        val attachment = Attachment(
                "",
                "")
        val emailMessage = EmailMessage(
                "",
                "Test Email",
                "Test Message",
                attachment)
        EmailService.sendEmail(
                "",
                "",
                emailMessage)
    }
}