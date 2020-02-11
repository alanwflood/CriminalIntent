package com.example.criminalintent

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment

private const val ARG_CRIME_PHOTO_PATH = "crime_photo_path"

class CrimeDialogFragment() : DialogFragment() {
    private lateinit var photoBitmap: Bitmap
    private lateinit var photoView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoPath = arguments?.getSerializable(ARG_CRIME_PHOTO_PATH) as String
        photoBitmap = getScaledBitmap(photoPath, requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_crime_photo_dialog, container)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoView = view.findViewById(R.id.crime_dialog_image)
        photoView.setImageBitmap(photoBitmap)
    }

    companion object {
        fun newInstance(photoPath: String): CrimeDialogFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_PHOTO_PATH, photoPath)
            }
            return CrimeDialogFragment().apply {
                arguments = args
            }
        }
    }
}