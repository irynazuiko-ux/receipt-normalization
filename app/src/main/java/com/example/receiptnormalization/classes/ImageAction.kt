package com.example.receiptnormalization.classes

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

class ImageAction {

    @Composable
    fun SelectFromAGallery(
        imageUri: Uri?,
        onImageUri: (Uri?)->Unit
        ){
        val pickMedia = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if(uri!= null){
                Log.d("PhotoPicker", "Selected URI: $uri")
                onImageUri(uri)
            }
        }
        pickMedia.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun TakeAPhoto(

    ){

    }

    fun CorrectImage(){

    }
}