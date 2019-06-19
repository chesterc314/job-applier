package com.jobapplier.jobapplier.service

import android.os.AsyncTask
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
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
data class Email(val username: String, val password: String, val emailMessage: EmailMessage)

object EmailService {
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"

    private class EmailAsyncTask(val postAction: (Exception?) -> Unit) : AsyncTask<Email, Void, Exception?>() {
        override fun doInBackground(vararg params: Email?): Exception? {
            try {
                sendEmail(params[0]!!)
            }catch (auth: AuthenticationFailedException) {
                return auth
            } catch (e: Exception) {
                return e
            }
            return null
        }

        override fun onPostExecute(result: Exception?) {
            postAction(result)
        }
    }

    fun sendAsyncEmail(email: Email, callBack: (Exception?) -> Unit) {
        EmailAsyncTask(callBack).execute(email)
    }

    private fun sendEmail(email: Email) {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = SMTP_HOST
        props["mail.smtp.port"] = SMTP_PORT
        val session: Session = Session.getInstance(props, PasswordAuthenticator(email.username, email.password))
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(email.username))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.emailMessage.toAddress))
        message.subject = email.emailMessage.subject
        message.setText(email.emailMessage.message)
        email.emailMessage.attachment?.let { it ->
            val source = FileDataSource(it.filePath)
            val messageBodyPart = MimeBodyPart()
            val multipart = MimeMultipart()
            messageBodyPart.dataHandler = DataHandler(source)
            messageBodyPart.fileName = it.fileName
            multipart.addBodyPart(messageBodyPart)
            val textPart = MimeBodyPart()
            textPart.setText(email.emailMessage.message)
            multipart.addBodyPart(textPart)
            message.setContent(multipart)
        }
        Transport.send(message)
    }
}

