package com.jobapplier.jobapplier.model

object MessageTemplate{
    const val JOB_MESSAGE_TEMPLATE =
            "To whom it may concern,\n\n" +
            "Please find attached my C.V.\n\n" +
            "Email address found at {jobLink}\n\n" +
            "Contact me at Email: {email} Cell: {cell}\n\n" +
            "Thank you.\n" +
            "Kind regards,\n{displayName}"
    const val JOB_MESSAGE_TEMPLATE_SUBJECT = "C.V. of {displayName}"
}