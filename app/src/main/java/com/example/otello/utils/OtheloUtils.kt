package com.example.otello.utils

import android.graphics.Matrix
import android.media.ExifInterface

object OtheloUtils {

    /**
     * Function that returns the rotation to be applied to the bitmap
     */
    fun rotateBitmap(imgPath : String) : Matrix {
        val exifInterface = ExifInterface(imgPath)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
        val m = Matrix()
        when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            else -> m.postRotate(0f)
        }

        return m
    }

}