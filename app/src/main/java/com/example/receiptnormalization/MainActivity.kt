package com.example.receiptnormalization

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiptnormalization.ui.theme.PurpleGrey80
import com.example.receiptnormalization.ui.theme.ReceiptNormalizationTheme
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            ReceiptNormalizationTheme {
                Log.e("LOG_TEST", "APP STARTED")

                ReceiptApp(Modifier)
            }
        }
    }
}

@Composable
fun ReceiptApp( modifier: Modifier = Modifier) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentScreen by remember { mutableStateOf("main") }
    when (currentScreen) {

        "main" -> MainPage(
            modifier = Modifier,
            onImageSelected = { uri ->
                selectedImageUri = uri
                currentScreen = "result"
            }
        )

        "result" -> ReceiptCorrectedPage(
            modifier = Modifier,
            imageUri = selectedImageUri,
            onBack = {
                currentScreen = "main"
            }
        )
    }

}

@Preview(showBackground = true,
    showSystemUi = true)
@Composable
fun ReceiptAppPreview() {
    ReceiptNormalizationTheme {
        ReceiptApp(modifier = Modifier)
    }
}

@Composable
fun MainPage(modifier: Modifier = Modifier,
             onImageSelected: (Uri?) -> Unit
             ){
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = colorResource(R.color.whitegrey))
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){

        Text(
            text = "Завантажте фото чека",
            fontSize = 24.sp,
            color = colorResource(R.color.greygreen)
        )
        Spacer(modifier = Modifier.height(15.dp))
        CreateSelectFromAGalleryBtn(modifier = modifier,
            onImageUri = onImageSelected)
        Spacer(modifier = Modifier.height(10.dp))
        CreateTakeAPhotoBtn(modifier = modifier,
            onImageUri = onImageSelected)
    }
}

@Composable
fun ReceiptCorrectedPage(
    modifier: Modifier = Modifier,
    imageUri: Uri?,
    onBack: () -> Unit){

    LaunchedEffect(Unit) {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Initialization failed")
        } else {
            Log.d("OpenCV", "Initialization success")
        }
    }

    /////
    var startTime by remember {
        mutableStateOf(0L)
    }
    //////

    val context = LocalContext.current
    val model = remember(context) { YoloModel(context) }

    LaunchedEffect(imageUri) {
        startTime = System.currentTimeMillis()
    }

    val bitmap by produceState<Bitmap?>(null, imageUri) {

        value = withContext(Dispatchers.IO) {

            imageUri?.let { uri ->

                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, uri)
                    )
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

            }

        }

    }

    //додано
    var processingFinished by remember {
        mutableStateOf(false)
    }


    val resultMat by produceState<Mat?>(null, bitmap) {

        processingFinished = false

        value = withContext(Dispatchers.Default) {
            try {
                bitmap?.let { model.process(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        processingFinished = true
    }

    Column(
        modifier = modifier.fillMaxSize().background(color = colorResource(R.color.whitegrey)).padding(25.dp),
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
                iconBtn = rememberVectorPainter(Icons.Rounded.ArrowBack),
                actionBtn = onBack
            )
        }
        Spacer(modifier= Modifier.padding(15.dp))

        val resultBitmap = remember(resultMat) {
            resultMat?.let { matToBitmap(it) }
        }

        ///////////
        LaunchedEffect(resultBitmap, processingFinished) {

            if (processingFinished) {

                val endTime = System.currentTimeMillis()

                Log.d(
                    "FULL_TIME",
                    "Full pipeline time = ${endTime - startTime} ms"
                )
            }
        }
        ////////////
        when {

            bitmap == null -> {

                Text("Завантаження фото...")

            }

            !processingFinished -> {

                Text("Обробка...")

            }

            resultBitmap != null -> {

                Image(
                    bitmap = resultBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned {

                        val endTime = System.currentTimeMillis()

                        Log.d(
                            "FULL_TIME",
                            "Displayed in ${endTime - startTime} ms"
                        )
                    },
                    contentScale = ContentScale.Fit
                )

            }

            else -> {

                Text("Чек на фото не знайдено.")

            }
        }

//        if (resultBitmap != null) {
//            Image(
//                bitmap = resultBitmap.asImageBitmap(),
//                contentDescription = null,
//                modifier = Modifier.fillMaxWidth(),
//                contentScale = ContentScale.Fit
//            )
//        } else {
//            Text("Обробка...")
//        }

//        imageUri?.let {
//
//            Image(
//                painter = rememberAsyncImagePainter(it),
//                contentDescription = null
//            )
//        }
    }
}

fun matToBitmap(mat: Mat): Bitmap {
    val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(mat, bmp)
    return bmp
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
            Log.i("SelectPhotoBtn", "Photo selection started")
            pickMedia.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
            Log.i("SelectPhotoBtn", "Photo selected")
        }
    )
}



@Composable
fun CreateTakeAPhotoBtn(
    modifier: Modifier= Modifier,
    onImageUri: (Uri?)->Unit
    ){
    val context = LocalContext.current

    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {
                onImageUri(imageUri)
            }
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val cameraGranted =
                permissions[android.Manifest.permission.CAMERA] == true

            val storageGranted =
                permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == true

            if (cameraGranted && storageGranted) {

                val uri = createImageUri(context)

                if (uri != null) {

                    imageUri = uri

                    cameraLauncher.launch(uri)

                }
            }
        }
    CreateBtn(
        modifier = modifier,
        nameBtn = "Зробити фото",
        descriptionBtn = "Take a photo",
        iconBtn = painterResource(R.drawable.take_a_photo),
        actionBtn = {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    )
}

fun createImageUri(context: Context): Uri? {

    val contentValues = ContentValues().apply {

        put(
            MediaStore.Images.Media.DISPLAY_NAME,
            "receipt_${System.currentTimeMillis()}.jpg"
        )

        put(
            MediaStore.Images.Media.MIME_TYPE,
            "image/jpeg"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/Receipts"
            )

        }
    }

    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
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
            actionBtn()
        },
        colors = ButtonDefaults.buttonColors(colorResource(R.color.lightgreygreen))
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