package com.jobapplier.jobapplier

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.evernote.android.job.JobManager
import com.jobapplier.jobapplier.worker.JobApplierJob
import com.jobapplier.jobapplier.worker.JobApplierJobCreator


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, View.OnClickListener {

    private lateinit var jobTitle: AutoCompleteTextView
    private lateinit var locationSpinner: Spinner
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var cell: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var txtCVPath: TextView
    private lateinit var btnFileBrowser: Button
    private lateinit var jobSwitch: Switch
    private var jobId: Int? = null
    private var isOn: Boolean = false
    private var location: String = ""
    private var position: Int = 0
    private val values = HashMap<String, String>()
    private lateinit var privateSharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        privateSharedPrefs = getSharedPreferences(JOB_APPLIER_SHARED_KEY, Context.MODE_PRIVATE)
        JobManager.create(application).addJobCreator(JobApplierJobCreator())
        jobTitle = findViewById(R.id.jobTitle)
        locationSpinner = findViewById(R.id.locationSpinner)
        firstName = findViewById(R.id.firstName)
        lastName = findViewById(R.id.lastName)
        cell = findViewById(R.id.cell)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        txtCVPath = findViewById(R.id.txtCVPath)
        btnFileBrowser = findViewById(R.id.btnFileBrowser)
        jobSwitch = findViewById(R.id.jobSwitch)

        val locationAdapter = ArrayAdapter.createFromResource(this,
                R.array.location_array,
                android.R.layout.simple_spinner_item)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = locationAdapter
        locationSpinner.onItemSelectedListener = this
        btnFileBrowser.setOnClickListener(this)
        jobSwitch.setOnCheckedChangeListener { _, isOn ->
            if (isOn) {
                requestFilePermissionToStartAJob()
            } else {
                jobId?.let { it -> cancelJob(it) }
            }
        }
    }

    override fun onStart() {
        readJobDataFromSharedPreferences()
        updateViewWithData()
        updateViewElementsStatus(!isOn)
        super.onStart()
    }

    override fun onResume() {
        readJobDataFromSharedPreferences()
        updateViewWithData()
        updateViewElementsStatus(!isOn)
        super.onResume()
    }

    override fun onPause() {
        saveJobDataToSharedPreferences()
        super.onPause()
    }

    override fun onStop() {
        saveJobDataToSharedPreferences()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_READ_AND_WRITE_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "ERROR: File permissions not granted", Toast.LENGTH_LONG).show()
            } else {
                startJob()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/pdf"
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                val uri = resultData.data
                uri?.let { it ->
                    txtCVPath.text = it.toString()
                }
            }
        }
    }

    private fun saveJobDataToSharedPreferences() {
        with(privateSharedPrefs.edit()) {
            values.forEach { it ->
                putString(it.key, it.value)
            }
            jobId?.let { it -> putInt(JOB_APPLIER_JOB_ID, it) }
            putBoolean(JOB_APPLIER_IS_ON, isOn)
            putInt(JOB_APPLIER_LOCATION_POS_ID, position)
            apply()
        }
    }

    private fun readJobDataFromSharedPreferences() {
        privateSharedPrefs.all.forEach { key, value ->
            when (value) {
                is String -> {
                    this.values[key] = value
                }
                is Int -> {
                    if (key == JOB_APPLIER_JOB_ID) {
                        this.jobId = value
                    } else if (key == JOB_APPLIER_LOCATION_POS_ID) {
                        this.position = value
                    }
                }
                is Boolean -> {
                    this.isOn = value
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
        this.txtCVPath.text = values["cvFilePath"]
        this.password.setText(values["password"])
    }

    private fun cancelJob(jobId: Int) {
        JobManager.instance().cancel(jobId)
        isOn = false
        updateViewElementsStatus(!isOn)
        saveJobDataToSharedPreferences()
    }

    private fun startJob() {
        values["jobTitle"] = this.jobTitle.text.toString()
        values["location"] = location
        values["firstName"] = this.firstName.text.toString()
        values["lastName"] = this.lastName.text.toString()
        values["email"] = this.email.text.toString()
        values["cell"] = this.cell.text.toString()
        values["cvFilePath"] = this.txtCVPath.text.toString()
        values["password"] = this.password.text.toString()

        val valuesNotFilledIn = values.filter { (_, value) -> value.isEmpty() || value.isBlank() }
                .keys.fold("") { acc, value ->
            "$acc$value\n"
        }
        if (valuesNotFilledIn.isNotEmpty()) {
            Toast.makeText(this, "Please complete the following:\n$valuesNotFilledIn", Toast.LENGTH_LONG).show()
            return
        }
        values["filePath"] = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
        isOn = true
        jobId = JobApplierJob.scheduleJob(values)
        updateViewElementsStatus(!isOn)
        saveJobDataToSharedPreferences()
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
    }

    private fun requestFilePermissionToStartAJob() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            AlertDialog.Builder(this)
                    .setMessage("Request permission to read and write to file")
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_AND_WRITE_PERMISSION)
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }.create()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_AND_WRITE_PERMISSION)
        }
    }

    companion object {
        const val REQUEST_READ_AND_WRITE_PERMISSION = 79
        const val READ_REQUEST_CODE = 65
        const val JOB_APPLIER_SHARED_KEY = "com.jobapplier.jobapplier.JOB_APPLIER_SHARED_KEY"
        const val JOB_APPLIER_IS_ON = "com.jobapplier.jobapplier.IS_ON"
        const val JOB_APPLIER_JOB_ID = "com.jobapplier.jobapplier.JOB_ID"
        const val JOB_APPLIER_LOCATION_POS_ID = "com.jobapplier.jobapplier.LOCATION_POS_ID"
    }
}
