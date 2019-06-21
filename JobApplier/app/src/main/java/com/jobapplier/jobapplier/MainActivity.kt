package com.jobapplier.jobapplier

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.reward.RewardItem
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.ads.reward.RewardedVideoAdListener
import com.jobapplier.jobapplier.service.*
import com.robertlevonyan.views.customfloatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import javax.mail.AuthenticationFailedException


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, View.OnClickListener, RewardedVideoAdListener {
    private lateinit var jobTitle: AutoCompleteTextView
    private lateinit var locationSpinner: Spinner
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var cell: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnFileBrowser: Button
    private lateinit var txtCVPath: TextView
    private var location: String = ""
    private var cvPath = ""
    private var position: Int = 0
    private var showNotice: Boolean = true
    private var emailCredentialsValid: Boolean = false
    private val values = HashMap<String, String>()
    private lateinit var privateSharedPrefs: SharedPreferences
    private lateinit var adView: AdView
    private lateinit var rewardedVideoAd: RewardedVideoAd
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adView = findViewById(R.id.adView)
        MobileAds.initialize(this, getString(R.string.admob_jobapplier_app_id))
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        rewardedVideoAd.rewardedVideoAdListener = this
        loadRewardedVideoAd()
        setSupportActionBar(toolbar)
        privateSharedPrefs = getSharedPreferences(JOB_APPLIER_SHARED_KEY, Context.MODE_PRIVATE)
        jobTitle = findViewById(R.id.jobTitle)
        locationSpinner = findViewById(R.id.locationSpinner)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        cell = findViewById(R.id.cell)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        btnFileBrowser = findViewById(R.id.btnFileBrowser)
        txtCVPath = findViewById(R.id.txtCVPath)
        floatingActionButton = findViewById(R.id.floatingActionButton)
        progressBar = findViewById(R.id.progressBar)
        btnFileBrowser.setOnClickListener(this)
        val locationAdapter = ArrayAdapter.createFromResource(this,
                R.array.location_array,
                android.R.layout.simple_spinner_item)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = locationAdapter
        locationSpinner.onItemSelectedListener = this
    }

    fun applyNow(v: View) {
        if (v.isEnabled) {
            val emailValue = values["email"]
            val passwordValue = values["password"]
            setValuesFromView()
            val valuesNotFilledIn = values.filter { (_, value) -> value.isEmpty() || value.isBlank() }
                    .keys.fold("") { acc, value ->
                "$acc$value<br />"
            }
            if (valuesNotFilledIn.isNotEmpty()) {
                Snackbar.make(adView, "Please fill in all inputs", Snackbar.LENGTH_INDEFINITE)
                        .setActionTextColor(Color.RED)
                        .setAction("View details") { viewPopup("Please enter the following:", valuesNotFilledIn) }
                        .show()
                return
            } else {
                if ((emailValue == null && passwordValue == null) ||
                        (emailValue != null && passwordValue != null && (emailValue != email.text.toString() || passwordValue != password.text.toString()) &&
                        this.emailCredentialsValid) || !this.emailCredentialsValid) {
                    validateEmailAuthentication()
                }else{
                    askForPermissionToViewAd()
                }
            }
        }
    }

    private fun validateEmailAuthentication() {
        val email = Email(
                values["email"]!!,
                values["password"]!!,
                EmailMessage(values["email"]!!,
                        "Welcome to Job Applier",
                        "Dear ${values["firstName"]} ${values["lastName"]},\n Welcome to Job Applier.\n This is a test email to valid your gmail account credentials before sending out your C.V.\n Thank you."))
        updateViewElementsStatus(isEnabled = false)
        EmailService.sendAsyncEmail(email) { ex ->
            updateViewElementsStatus(isEnabled = true)
            when (ex) {
                is AuthenticationFailedException -> {
                    val link = "https://www.google.com/settings/security/lesssecureapps"
                    val additionalInfo =
                            "Your Email: ${values["email"]}.<br /> Please check that your email and password is correct or please enable less secure applications on your gmail account, go here to enable it: <a href='$link'>$link</a>."
                    Snackbar.make(adView, "Error with email authentication", Snackbar.LENGTH_INDEFINITE)
                            .setActionTextColor(Color.RED)
                            .setAction("View details") { viewPopup("Error with email authentication", additionalInfo) }
                            .show()
                    this.emailCredentialsValid = false
                    saveJobDataToSharedPreferences()
                }
                is Exception -> {
                    val additionalInfo = "Your Email: ${values["email"]}"
                    Snackbar.make(adView, "Error sending email for this job", Snackbar.LENGTH_INDEFINITE)
                            .setActionTextColor(Color.RED)
                            .setAction("View details") { viewPopup("Error sending email for this job", additionalInfo) }
                            .show()
                    this.emailCredentialsValid = false
                    saveJobDataToSharedPreferences()
                }
                else -> {
                    this.emailCredentialsValid = true
                    saveJobDataToSharedPreferences()
                    askForPermissionToViewAd()
                }
            }
        }
    }

    private fun askForPermissionToViewAd() {
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_PERMISSION_JOB) {
            askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_PERMISSION) {
                viewAnAd()
            }
        }
    }

    private fun viewAnAd() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder
                .setCancelable(false)
                .setMessage("Watch a short video ad to send out your C.V.")
                .setPositiveButton("Continue") { dialog, _ ->
                    showRewardedAd()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel"
                ) { dialog, _ ->
                    dialog.cancel()
                }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun saveJobDataToSharedPreferences() {
        setValuesFromView()
        with(privateSharedPrefs.edit()) {
            values.forEach {
                putString(it.key, it.value)
            }
            putInt(JOB_APPLIER_LOCATION_SELECT_ID, position)
            putBoolean(JOB_APPLIER_IMPORTANT_NOTICE_KEY, showNotice)
            putBoolean(JOB_APPLIER_VALID_EMAIL_KEY, emailCredentialsValid)
            apply()
        }
    }

    private fun readJobDataFromSharedPreferences() {
        privateSharedPrefs.all.forEach { (key, value) ->
            when (value) {
                is String -> {
                    this.values[key] = value
                }
            }
        }
        val value = privateSharedPrefs.getInt(JOB_APPLIER_LOCATION_SELECT_ID, 0)
        this.position = value
        this.showNotice = privateSharedPrefs.getBoolean(JOB_APPLIER_IMPORTANT_NOTICE_KEY, true)
        this.emailCredentialsValid = privateSharedPrefs.getBoolean(JOB_APPLIER_VALID_EMAIL_KEY, false)
    }

    private fun updateViewWithData() {
        this.jobTitle.setText(values["jobTitle"])
        this.location = values["location"] ?: ""
        this.locationSpinner.setSelection(this.position)
        this.firstName.setText(values["firstName"])
        this.lastName.setText(values["lastName"])
        this.email.setText(values["email"])
        this.cell.setText(values["cell"])
        this.cvPath = values["cvFilePath"] ?: ""
        this.txtCVPath.text = this.cvPath
        this.password.setText(values["password"])
    }

    private fun applyForJobPosition() {
        values["filePath"] = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
        updateViewElementsStatus(isEnabled = false)
        val builder = StringBuilder()
        saveJobDataToSharedPreferences()
        JobApplier.applyForJob(values, { successResult ->
            updateViewElementsStatus(isEnabled = true)
            buildSuccessfulJobTitles(successResult, builder)
            if (successResult.jobEntries.isNotEmpty()) {
                Snackbar.make(adView, "Number of jobs applied for ${successResult.jobEntries.size}", Snackbar.LENGTH_INDEFINITE)
                        .setActionTextColor(Color.GREEN)
                        .setAction("View details") { viewPopup("Successfully sent job applications:", builder.toString()) }
                        .show()
            } else {
                val titleText = "No job applications to apply for"
                Snackbar.make(adView, titleText, Snackbar.LENGTH_INDEFINITE)
                        .setActionTextColor(Color.WHITE)
                        .setAction("View details") {
                            viewPopup(titleText,
                                    "This could be that you already sent out job applications for this job title: ${values["jobTitle"]} or there is just no listings for this job title")
                        }
                        .show()
            }
        }, { failureResult ->
            updateViewElementsStatus(isEnabled = true)
            buildSuccessfulJobTitles(failureResult, builder)
            failureResult.errorMessages.forEach {
                builder.append("${it.errorMessage},${it.additionalInfo} <br />")
            }
            Snackbar.make(adView, "Errors occurred for specific job applications", Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(Color.RED)
                    .setAction("View details") { viewPopup("Failed job applications:", builder.toString()) }
                    .show()

        })
    }

    private fun buildSuccessfulJobTitles(result: JobResult, builder: StringBuilder) {
        result.jobEntries.forEach {
            builder.append("Job link: <a href='${it.jobLink}'>${it.jobLink}</a> <br />")
        }
    }

    private fun viewPopup(title: String, text: String, isCancelable: Boolean = false, action: () -> Unit = {}) {
        val linearLayout = LinearLayout(this)
        val scrollView = ScrollView(this)
        val alertDialogBuilder = AlertDialog.Builder(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.isVerticalScrollBarEnabled = true
        val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.setMargins(36, 36, 36, 36)
        val textView = TextView(this)
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.layoutParams = lp
        textView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
        linearLayout.addView(textView)
        scrollView.addView(linearLayout)
        alertDialogBuilder.setView(scrollView)
        alertDialogBuilder
                .setCancelable(isCancelable)
                .setTitle(title)
                .setPositiveButton("OK") { dialog, _ ->
                    action()
                    dialog.dismiss()
                }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun setValuesFromView() {
        values["jobTitle"] = this.jobTitle.text.toString()
        values["location"] = this.location
        values["firstName"] = this.firstName.text.toString()
        values["lastName"] = this.lastName.text.toString()
        values["email"] = this.email.text.toString()
        values["cell"] = this.cell.text.toString()
        values["cvFilePath"] = this.cvPath
        values["password"] = this.password.text.toString()
    }

    private fun updateViewElementsStatus(isEnabled: Boolean) {
        this.jobTitle.isEnabled = isEnabled
        this.locationSpinner.isEnabled = isEnabled
        this.firstName.isEnabled = isEnabled
        this.lastName.isEnabled = isEnabled
        this.email.isEnabled = isEnabled
        this.cell.isEnabled = isEnabled
        this.btnFileBrowser.isEnabled = isEnabled
        this.password.isEnabled = isEnabled
        this.floatingActionButton.isEnabled = isEnabled

        if (this.floatingActionButton.isEnabled) {
            this.floatingActionButton.visibility = View.VISIBLE
            this.progressBar.visibility = View.GONE
        } else {
            this.floatingActionButton.visibility = View.GONE
            this.progressBar.visibility = View.VISIBLE
        }
    }

    private fun showRewardedAd() {
        if (rewardedVideoAd.isLoaded) {
            rewardedVideoAd.show()
        } else {
            loadRewardedVideoAd()
        }
    }

    private fun loadRewardedVideoAd() {
        rewardedVideoAd.loadAd(getString(R.string.admob_jobapplier_reward_ad_unit_id), AdRequest.Builder().build())
    }

    override fun onStart() {
        readJobDataFromSharedPreferences()
        updateViewWithData()
        if (showNotice) {
            val insecureLink = "https://www.google.com/settings/security/lesssecureapps"
            viewPopup("IMPORTANT NOTICE",
                    "Please ensure that less secure applications on your gmail account is <b>enabled</b>, if not go here: <a href='$insecureLink'>$insecureLink</a> <br /> This will allow this app to send out emails on your behave.",
                    false) {
                showNotice = false
            }
        }
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        rewardedVideoAd.resume(this)
    }

    override fun onPause() {
        saveJobDataToSharedPreferences()
        super.onPause()
        rewardedVideoAd.pause(this)
    }

    override fun onStop() {
        saveJobDataToSharedPreferences()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        rewardedVideoAd.destroy(this)
    }

    override fun onRewardedVideoAdLoaded() {
    }

    override fun onRewardedVideoAdOpened() {
    }

    override fun onRewardedVideoCompleted() {
    }

    override fun onRewarded(p0: RewardItem?) {
        loadRewardedVideoAd()
        applyForJobPosition()
    }

    override fun onRewardedVideoStarted() {
    }

    override fun onRewardedVideoAdFailedToLoad(p0: Int) {
    }

    override fun onRewardedVideoAdLeftApplication() {
    }

    override fun onRewardedVideoAdClosed() {
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                REQUEST_READ_PERMISSION_JOB -> askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_PERMISSION) {
                    viewAnAd()
                }
                REQUEST_READ_PERMISSION_BROWSER -> browserDocumentFiles()
                REQUEST_WRITE_PERMISSION -> viewAnAd()
            }
        } else {
            viewPopup("File permissions not enabled", "This app requires access to files.")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rate -> {
                if (btnFileBrowser.isEnabled) {
                    rateApp()
                    true
                } else {
                    viewPopup("Please wait...",
                            "In the process of sending out applications, once completed then you may rate this app.<br />Thank you for your patience.",
                            isCancelable = true)
                    false
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        //do nothing
    }

    private fun rateApp() {
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                    Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                    )
            )
        }

    }

    private fun askForPermission(permission: String, requestCode: Int?, action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode!!)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode!!)
            }
        } else {
            action()
        }
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        parent?.let { it ->
            this.location = it.getItemAtPosition(position).toString()
        }
        this.position = position
    }

    override fun onClick(view: View?) {
        saveJobDataToSharedPreferences()
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_PERMISSION_BROWSER) {
            browserDocumentFiles()
        }
    }

    private fun browserDocumentFiles() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val mimeTypes = arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                val path = getPath(this, resultData.data!!)
                path?.let { it ->
                    this.cvPath = it
                    saveJobDataToSharedPreferences()
                }
            }
        }
    }

    private fun getPath(context: Context, uri: Uri): String? {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    companion object {
        const val REQUEST_READ_PERMISSION_JOB = 1
        const val REQUEST_READ_PERMISSION_BROWSER = 2
        const val REQUEST_WRITE_PERMISSION = 3
        const val READ_REQUEST_CODE = 65
        const val JOB_APPLIER_SHARED_KEY = "com.jobapplier.jobapplier.JOB_APPLIER_SHARED_KEY"
        const val JOB_APPLIER_LOCATION_SELECT_ID = "com.jobapplier.jobapplier.LOCATION_SELECT_ID"
        const val JOB_APPLIER_IMPORTANT_NOTICE_KEY = "com.jobapplier.jobapplier.JOB_APPLIER_IMPORTANT_NOTICE_KEY"
        const val JOB_APPLIER_VALID_EMAIL_KEY = "com.jobapplier.jobapplier.JOB_APPLIER_VALID_EMAIL_KEY"
    }
}
