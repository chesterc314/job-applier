# job-applier
* This is an android application intended to apply for jobs using a website scraper(http://jsoup.org/), smtp client(https://javaee.github.io/javamail/Android) and job scheduler(https://github.com/evernote/android-job)
# How it works
* You input the job title and location for the job you are looking for.
* Using this input to scrap email addresses of potential jobs from the http://www.jobmail.co.za
* Then other details that were also inputed such as your email credentials and C.V. document will be used to send out your C.V. to the email addresses scrapped.
* This will run daily.
# Installation
* Download APK (https://github.com/chesterc314/job-applier/raw/master/JobApplier/app/release/app-release.apk).
* Enable install from "Unknown sources" in settings on your Android device.
* Open application, enter all your details (your password is stored locally on your device *Note: it uses only gmail credentials for now*). (Please enable less secure applications here: https://www.google.com/settings/security/lesssecureapps)
* Switch on job applier (please be aware as soon as you switch it on, it will start applying to jobs immediately then after it will run again within 24 hours).
* It will apply for jobs daliy. 

# Note: Do not kill this application with your task manager when job applier is enabled or else it will not work.
