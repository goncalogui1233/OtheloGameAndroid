package com.example.otello.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import java.io.ByteArrayOutputStream


object OtheloUtils {

    /**
     * Function that returns the rotation to be applied to the bitmap
     */
    fun rotateBitmap(imgPath: String) : Matrix {
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

    fun getStringFromBitmap(bitmap: Bitmap) : String {
        val byteArrayBitmapStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50,
                byteArrayBitmapStream)
        val b: ByteArray = byteArrayBitmapStream.toByteArray()
        return Base64.encodeToString(b, Base64.URL_SAFE)
    }

    fun getBitmapFromString(bitmap: String) : Bitmap {
        val decodedString: ByteArray = Base64.decode(bitmap, 0)
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    }

    fun getBoardDimensionByPlayerNumber(playerNumber: Int): Int {
        return if (playerNumber == 3) {
            10
        }
        else {
            8
        }
    }


}