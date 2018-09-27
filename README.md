# job-applier
* This is an android application intended to apply for jobs using a website scraper(http://jsoup.org/), smtp client(https://javaee.github.io/javamail/Android) and job scheduler(https://github.com/evernote/android-job)
# How it works
* You input the job title and location for the job you are looking for.
* Using this input to scrap email addresses of potential jobs from the http://www.jobmail.co.za
* Then other details that were also inputed such as your email credentials and C.V. document will be used to send out your C.V. to the email addresses scrapped.
* This will run daily.

# Terms and Conditions
* **This application can only use your job title to send applications, it is not smart enough to look at the job description like for instance salary/wages or whether you need a car. Look at your gmail inbox daily to see whether you have an interview or a follow-up email.
* **Use this application at your OWN RISK. 
* **It is free and open source.
* **This is a BETA version so it might be buggy.

# Installation
* Download APK (https://github.com/chesterc314/job-applier/raw/master/JobApplier/app/release/app-release.apk).
* Enable install from "Unknown sources" in settings on your Android device.
* Open application, enter all your details (your password is stored locally on your device so it's secure *Note: it uses only gmail credentials for now*). 
* Ensure you enable less secure applications here: https://www.google.com/settings/security/lesssecureapps. This is to allow the application to send emails on your behalf (Do not be alarmed you can disable it when you don't need the application).
* Switch on job applier (please be aware as soon as you switch it on, it will start applying to jobs immediately then after it will run again within 24 hours).
* It will apply for jobs daliy. 
* Sent emails will appear in your Sent folder on your gmail.

# Troubleshooting
* You can check for errors in "job_applier_errors.txt" you can find it in your Documents folders on your device.
* The "job_applications_send_out.txt" file is the jobs that you already applied for, you may look through it to see what jobs the application is applying for.

# Note: Do not kill this application with your task manager when job applier is enabled or else it will not work.

# GOOD LUCK !



