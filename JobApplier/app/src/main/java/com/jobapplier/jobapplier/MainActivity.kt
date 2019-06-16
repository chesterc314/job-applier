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
import com.jobapplier.jobapplier.service.JobApplier
import com.jobapplier.jobapplier.service.JobResult
import com.robertlevonyan.views.customfloatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*


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
    private val values = HashMap<String, String>()
    private lateinit var privateSharedPrefs: SharedPreferences
    private lateinit var adView: AdView
    private lateinit var rewardedVideoAd: RewardedVideoAd
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        adView = findViewById(R.id.adView)
        floatingActionButton = findViewById(R.id.floatingActionButton)
        progressBar = ProgressBar(this)
        progressBar.setBackgroundColor(Color.GREEN)
        progressBar.visibility = View.INVISIBLE
        progressBar.x = floatingActionButton.x
        progressBar.y = floatingActionButton.y
        progressBar.z = floatingActionButton.z
        val locationAdapter = ArrayAdapter.createFromResource(this,
                R.array.location_array,
                android.R.layout.simple_spinner_item)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = locationAdapter
        locationSpinner.onItemSelectedListener = this
        btnFileBrowser.setOnClickListener(this)
        MobileAds.initialize(this, getString(R.string.admob_jobapplier_app_id))
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        rewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this)
        rewardedVideoAd.rewardedVideoAdListener = this
        loadRewardedVideoAd()
    }

    fun applyNow(v: View) {
        if (v.isEnabled) {
            setValuesFromView()
            val valuesNotFilledIn = values.filter { (_, value) -> value.isEmpty() || value.isBlank() }
                    .keys.fold("") { acc, value ->
                "$acc$value\n"
            }
            if (valuesNotFilledIn.isNotEmpty()) {
                Snackbar.make(adView, "Please complete the following:\n$valuesNotFilledIn", Snackbar.LENGTH_INDEFINITE)
                        .setActionTextColor(Color.RED)
                        .setAction("View details") { viewPopup("Please enter the following:\n$valuesNotFilledIn") }
                        .show()
                return
            }
            askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_PERMISSION_JOB) {
                askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_PERMISSION) {
                    askToViewAnAd()
                }
            }
        }

    }

    private fun askToViewAnAd() {
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
            putInt(JOB_APPLIER_LOCATION_POS_ID, position)
            apply()
        }
    }

    private fun readJobDataFromSharedPreferences() {
        privateSharedPrefs.all.forEach { (key, value) ->
            when (value) {
                is String -> {
                    this.values[key] = value
                }
                is Int -> {
                    if (key == JOB_APPLIER_LOCATION_POS_ID) {
                        this.position = value
                    }
                }
            }
        }
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
            Snackbar.make(adView, "Number of jobs applied for ${successResult.jobEntries.size}", Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(Color.GREEN)
                    .setAction("View details") { viewPopup(builder.toString()) }
                    .show()
        }, { failureResult ->
            updateViewElementsStatus(isEnabled = true)
            buildSuccessfulJobTitles(failureResult, builder)
            builder.appendln("Failed job applications:")
            failureResult.errorMessages.forEach {
                builder.appendln("${it.errorMessage},${it.additionalInfo}")
            }
            Snackbar.make(adView, "Errors occurred for specific job applications", Snackbar.LENGTH_INDEFINITE)
                    .setActionTextColor(Color.RED)
                    .setAction("View details") { viewPopup(builder.toString()) }
                    .show()

        })
    }

    private fun buildSuccessfulJobTitles(result: JobResult, builder: StringBuilder) {
        builder.appendln("Successfully sent job applications:")
        result.jobEntries.forEach {
            builder.appendln("Job: ${it.jobLink}")
        }
    }

    private fun viewPopup(text: String) {
        val linearLayout = LinearLayout(this)
        val scrollView = ScrollView(this)
        val alertDialogBuilder = AlertDialog.Builder(this)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.isVerticalScrollBarEnabled = true
        val textView = TextView(this)
        textView.text = text
        linearLayout.addView(textView)
        scrollView.addView(linearLayout)
        alertDialogBuilder.setView(scrollView)
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
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
            this.progressBar.visibility = View.INVISIBLE
        } else {
            this.floatingActionButton.visibility = View.INVISIBLE
            this.progressBar.visibility = View.VISIBLE
        }
    }

    private fun showRewardedAd() {
        if (rewardedVideoAd.isLoaded) {
            rewardedVideoAd.show()
        }else{
            loadRewardedVideoAd()
        }
    }

    private fun loadRewardedVideoAd() {
        rewardedVideoAd.loadAd(getString(R.string.admob_jobapplier_reward_ad_unit_id), AdRequest.Builder().build())
    }

    override fun onStart() {
        readJobDataFromSharedPreferences()
        updateViewWithData()
        super.onStart()
    }

    override fun onResume() {
        readJobDataFromSharedPreferences()
        updateViewWithData()
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
                    askToViewAnAd()
                }
                REQUEST_READ_PERMISSION_BROWSER -> browserDocumentFiles()
                REQUEST_WRITE_PERMISSION -> askToViewAnAd()
            }
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rate -> {
                rateApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                AlertDialog.Builder(this)
                        .setMessage("Request permission to access resource")
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode!!)
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }.create()
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
        const val JOB_APPLIER_LOCATION_POS_ID = "com.jobapplier.jobapplier.LOCATION_POS_ID"
    }
}
