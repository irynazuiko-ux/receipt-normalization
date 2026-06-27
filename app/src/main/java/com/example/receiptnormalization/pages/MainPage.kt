package com.example.receiptnormalization.pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiptnormalization.R
import com.example.receiptnormalization.classes.ImageAction
import com.example.receiptnormalization.ui.theme.PurpleGrey80

class MainPage {


    @Preview(showBackground = true,
        showSystemUi = true)
    @Composable
    fun CreateMainPage(modifier: Modifier = Modifier){
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = PurpleGrey80)
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){

            Text(
                text = "Завантажте фото чека",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(15.dp))
            CreateSelectFromAGalleryBtn(modifier = modifier,
                onImageUri = { uri ->
                    selectedImageUri = uri
                })
            Spacer(modifier = Modifier.height(10.dp))
            CreateTakeAPhotoBtn(modifier = modifier)
        }
    }

    @Composable
    fun CreateSelectFromAGalleryBtn(
        modifier: Modifier= Modifier,
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
        CreateBtn(
            modifier = modifier,
            nameBtn = "Обрати фото з галереї",
            descriptionBtn = "Select Photo From A Gallery",
            iconBtn = painterResource(R.drawable.choose_from_a_gallery),
            actionBtn = {
                pickMedia.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }
        )
    }

    @Composable
    fun CreateTakeAPhotoBtn(modifier: Modifier= Modifier){

        CreateBtn(
            modifier = modifier,
            nameBtn = "Зробити фото",
            descriptionBtn = "Take a photo",
            iconBtn = painterResource(R.drawable.take_a_photo),
            actionBtn = {}
        )
    }

    @Composable
    fun CreateBtn(
        modifier: Modifier = Modifier,
        nameBtn: String,
        descriptionBtn: String,
        iconBtn: Painter,
        actionBtn: ()->Unit
    ){
        Button(
            onClick = {
                actionBtn
            },
            colors = ButtonDefaults.buttonColors(colorResource(R.color.purple_200))
        ) {
            Row(modifier = modifier
            ) {
                Image(
                    painter = iconBtn,
                    contentDescription = descriptionBtn,
                    modifier = Modifier.padding(8.dp)

                )
                Text(
                    text = nameBtn,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}