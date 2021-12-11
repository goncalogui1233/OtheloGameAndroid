package com.example.otello.photo

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.otello.ProfileActivity
import com.example.otello.R
import kotlinx.android.synthetic.main.photo_dialog.*
import kotlinx.android.synthetic.main.photo_dialog.view.*
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.random.nextInt

class PhotoDialog2 : DialogFragment() {

    private lateinit var cameraExecutor: ExecutorService
    private var directory : String = ""
    private var imageCapture : ImageCapture? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.photo_dialog, container, false)

        directory = requireActivity().applicationContext.filesDir.toString()

        askPermissions()

        view.btnTakePhoto.setOnClickListener {
            takePhoto()
            view.btnTakePhoto.isEnabled = false
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        return view
    }

    private val cameraRequestCode = 1290

    private fun askPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), cameraRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            cameraRequestCode -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(requireContext(), "Unable to access camera", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val file = File(directory + "/" + Random.nextInt() + ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        PhotoPreview.newInstance(file.absolutePath).show(requireActivity().supportFragmentManager, "KKK")
                        dismiss()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(requireContext(), "Photo Not Saved", Toast.LENGTH_SHORT).show()
                    }

                })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            }catch (e : Exception) {
                Log.e("TAG", "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}