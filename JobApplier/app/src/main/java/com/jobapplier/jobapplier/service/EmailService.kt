package com.jobapplier.jobapplier.service

import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

data class PasswordAuthenticator(private val username: String, private val password: String) : javax.mail.Authenticator() {
    override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(username, password)
    }
}

data class Attachment(val fileName: String, val filePath: String)
data class EmailMessage(val toAddress: String, val subject: String, val message: String, val attachment: Attachment? = null)

object EmailService {
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"

    fun sendEmail(username: String, password: String, emailMessage: EmailMessage) {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = SMTP_HOST
        props["mail.smtp.port"] = SMTP_PORT
        val session: Session = Session.getInstance(props, PasswordAuthenticator(username, password))
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(username))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailMessage.toAddress))
        message.subject = emailMessage.subject
        message.setText(emailMessage.message)
        emailMessage.attachment?.let { it ->
            val source = FileDataSource(it.filePath)
            val messageBodyPart = MimeBodyPart()
            val multipart = MimeMultipart()
            messageBodyPart.dataHandler = DataHandler(source)
            messageBodyPart.fileName = it.fileName
            multipart.addBodyPart(messageBodyPart)
            val textPart = MimeBodyPart()
            textPart.setText(emailMessage.message)
            multipart.addBodyPart(textPart)
            message.setContent(multipart)
        }
        Transport.send(message)
    }
}

