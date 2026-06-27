package com.example.receiptnormalization.pages

import android.media.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.receiptnormalization.R
import com.example.receiptnormalization.ui.theme.PurpleGrey80
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth

class ReceiptCorrectedPage {

    @Preview(
        showBackground = true,
        showSystemUi = true
    )
    @Composable
    fun CreatePage(modifier: Modifier = Modifier, receiptImage: Painter = painterResource(R.drawable.img_receipt)){
        Column(
            modifier = modifier.fillMaxSize().background(color = PurpleGrey80).padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ){
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ){
                CreateBtn(
                    modifier = modifier,
                    nameBtn = "Назад",
                    descriptionBtn ="Back",
                    iconBtn = rememberVectorPainter(Icons.Rounded.ArrowBack)
                )

            }
            Spacer(modifier= Modifier.padding(15.dp))
            Image(modifier = modifier,
                painter = receiptImage,
                contentDescription = null
            )
        }
    }

    @Composable
    fun CreateBtn(
        modifier: Modifier = Modifier,
        nameBtn: String,
        descriptionBtn: String,
        iconBtn: Painter
    ){
        Button(
            onClick = {

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
                    modifier = Modifier.padding(8.dp).align(Alignment.CenterVertically)
                )
            }
        }
    }
}