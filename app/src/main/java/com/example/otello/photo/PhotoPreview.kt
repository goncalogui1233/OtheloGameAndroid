package com.example.otello.photo

import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import com.example.otello.ProfileActivity
import com.example.otello.R
import com.example.otello.utils.ConstStrings
import com.example.otello.utils.OtheloUtils
import kotlinx.android.synthetic.main.photo_preview.view.*
import java.io.File

class PhotoPreview : DialogFragment() {

    var photoPath : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photoPath = arguments?.getString("photoPath", "")!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.photo_preview, container, true)

        val bitmap = BitmapFactory.decodeFile(photoPath)
        val rotation = OtheloUtils.rotateBitmap(photoPath)
        val picture = Bitmap.createBitmap(bitmap, 0,0, bitmap.width, bitmap.height, rotation, true)
        view.previewImageView.setImageBitmap(picture)

        view.btnSavePhoto.setOnClickListener {

            val sharedPref = requireActivity().getSharedPreferences(ConstStrings.SHARED_PREFERENCES_INSTANCE, Context.MODE_PRIVATE)
            val picPath = sharedPref.getString(ConstStrings.SHARED_PREFERENCES_PHOTO, "")

            //Remove old picture from file system
            if(!picPath.isNullOrEmpty()){
                val oldPic = File(sharedPref.getString(ConstStrings.SHARED_PREFERENCES_PHOTO, "")!!)
                if(oldPic.exists()) {
                    oldPic.delete()
                }
            }

            sharedPref.edit {
                this.putString(ConstStrings.SHARED_PREFERENCES_PHOTO, photoPath)
                commit()
            }

            dismiss()

        }

        view.btnDismissPhoto.setOnClickListener {

        }



        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        if(activity is ProfileActivity)
            (activity as ProfileActivity).onDismiss(dialog)
    }

    companion object {

        fun newInstance(photoPath : String): PhotoPreview{
            val args = Bundle()
            args.putString("photoPath", photoPath)

            val fragment = PhotoPreview()
            fragment.arguments = args
            return fragment
        }

    }

}