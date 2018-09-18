package com.jobapplier.jobapplier.model

data class ApplicationModel(val jobTitle: String,
                            val location: String,
                            val firstName: String,
                            val lastName: String,
                            val email: String,
                            val cell: String,
                            val cvFilePath: String,
                            val password: String,
                            val messageSubject: String,
                            val messageTemplate: String)