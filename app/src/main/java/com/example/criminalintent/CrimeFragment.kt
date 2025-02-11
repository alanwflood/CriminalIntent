package com.example.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import java.io.File
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"

private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0

private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_TIME = 0

private const val DIALOG_PHOTO = "DialogPhoto"
private const val REQUEST_PHOTO = 0

private const val REQUEST_CONTACT = 1
private const val REQUEST_CAMERA = 2
private const val REQUEST_PHONE = 3

private const val DATE_FORMAT = "EEE, MM, dd"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

    private var photoViewWidth: Int? = null
    private var photoViewHeight: Int? = null

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider.NewInstanceFactory().create(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()

        val crimeId: UUID =
            arguments?.getSerializable(ARG_CRIME_ID) as UUID
        Log.d(TAG, "args bundle crime id: $crimeId")
        // Then load crime from the database
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crime, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Assign lateinit vars to items on screen
        titleField = view.findViewById(R.id.crime_title_input)
        dateButton = view.findViewById(R.id.crime_date_button)
        timeButton = view.findViewById(R.id.crime_time_button)
        solvedCheckBox = view.findViewById(R.id.crime_solved_checkbox)
        reportButton = view.findViewById(R.id.crime_report_button)
        suspectButton = view.findViewById(R.id.crime_suspect_button)
        callButton = view.findViewById(R.id.crime_call_button)
        photoButton = view.findViewById(R.id.crime_camera_button)
        photoView = view.findViewById(R.id.crime_image)

        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner, Observer { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "com.example.criminalintent.fileprovider",
                    photoFile
                )
                updateUI()
            }
        })

    }

    override fun onStart() {
        super.onStart()
        applyAllEventsToViews()
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = DateFormat.format("EEE, d MMM yyyy", crime.date)
        timeButton.text = DateFormat.format("HH:mm", crime.date)
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            callButton.visibility = View.VISIBLE
            suspectButton.text = crime.suspect
        } else {
            callButton.visibility = View.GONE
        }

        updatePhotoView()
    }

    private fun updatePhotoView(width: Int? = photoViewWidth, height: Int? = photoViewHeight) {
        if (::photoFile.isInitialized) {
            if (photoFile.exists()) {
                if (width != null && height != null) {
                    val bitmap =
                        getScaledBitmap(photoFile.path, width, height)
                    photoView.setImageBitmap(bitmap)
                } else {
                    val bitmap =
                        getScaledBitmap(photoFile.path, requireActivity())
                    photoView.setImageBitmap(bitmap)
                }
            } else {
                photoView.setImageDrawable(null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                setSuspectFromContact(data)
            }
            requestCode == REQUEST_CAMERA -> {
                updatePhotoView()
            }
        }
    }

    private fun setSuspectFromContact(data: Intent) {
        val contactUri: Uri = data.data ?: return
        // Fields to query for values
        val queryFields = arrayOf(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts._ID
        )
        // Perform your query - the contactUri is like a "where" clause here
        val cursor =
            requireActivity().contentResolver.query(
                contactUri,
                queryFields,
                null,
                null,
                null
            )

        cursor?.use {
            if (it.count == 0) {
                return
            }
            it.moveToFirst()
            val id = it.getString(1)
            val suspect = it.getString(0)
            crime.suspect = suspect
            crimeDetailViewModel.saveCrime(crime)
            suspectButton.text = suspect
        }
    }

    private fun applyAllEventsToViews() {
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked

            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date, false).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        timeButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date, true).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_TIME)
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            // Setup intent to select a contact
            val pickContactIntent =
                Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI
                )

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            // Check app to handle contact intent
            val packageManager: PackageManager =
                requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(
                    pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

            // No app exists to handle intent
            if (resolvedActivity == null) {
                // Disable the button
                this.isEnabled = false
            }
        }

        photoView.apply {
            // Opens Crime Photo Dialog on Click if capable
            setOnClickListener {
                // If there is a photo file
                if (photoFile.exists()) {
                    // Open the crime photo dialog
                    CrimeDialogFragment.newInstance(photoFile.path).apply {
                        setTargetFragment(this@CrimeFragment, REQUEST_PHOTO)
                        show(this@CrimeFragment.requireFragmentManager(), DIALOG_PHOTO)
                    }
                    // Else if photo button is enabled
                } else if (photoButton.isEnabled) {
                    // Do whatever photo button does (Opens the Camera)
                    photoButton.performClick()
                }
            }

            val observer = viewTreeObserver
            if (observer.isAlive) {
                observer.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        observer.removeOnGlobalLayoutListener(this)
                        updatePhotoView()
                    }
                })
            }
        }


        callButton.apply {
            // TODO
        }

        photoButton.apply {
            // Create image capture intent
            val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            // Get default apps
            val packageManager: PackageManager = requireActivity().packageManager
            // Is there any camera apps available for the capture intent?
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(
                captureImageIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            // If no camera app exists to handle intent
            if (resolvedActivity == null) {
                // Disable the button
                isEnabled = false
            }

            setOnClickListener {
                // Assign photoUri to intent
                captureImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                // Get available camera activities to populate list
                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(
                        captureImageIntent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )

                // For each activity in the possible camera activities ->
                for (activity in cameraActivities) {
                    // -> Grant permission to access the photoUri
                    requireActivity().grantUriPermission(
                        activity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                // Run the activity
                startActivityForResult(captureImageIntent, REQUEST_CAMERA)
            }
        }

    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }


    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}