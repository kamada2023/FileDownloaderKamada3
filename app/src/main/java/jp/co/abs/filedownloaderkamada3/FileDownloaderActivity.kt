package jp.co.abs.filedownloaderkamada3

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date


private var fileName = ""
private var notifySuccess = "ダウンロードが完了しました"
private var notifyFailure = "画像取得に失敗しました"
private var notifyImageAcquisition = "画像を取得しました"
var imageUri: Uri = if (Build.VERSION.SDK_INT >= 29) {
    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
} else {
    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
}
var selfPathFile : String = ""
const val REQUEST_READ_MEDIA_IMAGES = 1

class FileDownloaderActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 外部ストレージの書き込み権限がアプリに対して既に付与されているかを確認
            if (checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // 書き込み権限が付与されていない場合はリクエストを行う
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_READ_MEDIA_IMAGES
                )
            } else {
                FileDownloaderScreen(applicationContext)
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun PermissionDialog(
    openAlertDialog: MutableState<Boolean>,
    exitTheApplication: MutableState<Boolean>
){
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
            ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(textAlign = TextAlign.Center, text ="ストレージへの\nアクセス許可")
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            try {
                                // 保存先のディレクトリが無ければ作成する
                                //downloadPathFile.mkdir()
                                //Log.d("Log", "new_File:$downloadPathFile")
                                //Log.d("filePath", "new_File:$filePath")
                                //テストディレクトリ
                                val collection = if (Build.VERSION.SDK_INT >= 29) {
                                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                } else {
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                }
                                Log.d("MEDIA_Uri","$collection")
                                openAlertDialog.value = false
                            } catch (error: SecurityException) {
                                // ファイルに書き込み用のパーミッションが無い場合など
                                error.printStackTrace()
                            } catch (error: IOException) {
                                // 何らかの原因で誤ってディレクトリを2回作成してしまった場合など
                                error.printStackTrace()
                            } catch (error: Exception) {
                                error.printStackTrace()
                            }
                        }
                    ) { Text(text = "許可する") }
                    TextButton(
                        onClick = {
                            openAlertDialog.value = false
                            //アクティビティ終了
                            exitTheApplication.value = true
                        }
                    ) { Text(text = "しない") }
                }
            }
        }
    }
}

private fun String.isGrantedPermission(context: Context): Boolean {
    // checkSelfPermission は PERMISSION_GRANTED or PERMISSION_DENIED のどちらかを返す
    // そのため checkSelfPermission の戻り値が PERMISSION_GRANTED であれば許可済みになる。
    return context.checkSelfPermission(this) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("SimpleDateFormat", "CoroutineCreationDuringComposition")
fun downloadImage(urlEntered:String, showProgressBer: MutableState<Boolean>, context: Context, showDownloadImage: MutableState<Boolean>) {
    val stringUrl: String = urlEntered
    if (stringUrl.isEmpty()){
        Toast.makeText(context, notifyFailure, Toast.LENGTH_SHORT).show()
        return
    }

    //launchを呼び出す前にプログレスバーを表示
    showProgressBer.value = true
    CoroutineScope(Dispatchers.Default).launch(Dispatchers.IO) {
        try {
            val url = URL(stringUrl)
            val urlCon = url.openConnection() as HttpURLConnection
            // タイムアウト設定
            urlCon.readTimeout = 10000
            urlCon.connectTimeout = 20000
            // リクエストメソッド
            urlCon.requestMethod = "GET"
            // リダイレクトを自動で許可しない設定
            urlCon.instanceFollowRedirects = false
            //画像をダウンロード
            val ism = urlCon.inputStream
            val bmp = BitmapFactory.decodeStream(ism)

            val sdf = SimpleDateFormat("yyyyMMdd_HH:mm:ss")
            val current = sdf.format(Date())
            // 保存先のファイル作成
            fileName = "$current.png"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.IS_PENDING, true)
                }
                //※1　専用直下フォルダを作成したい場合
                put(
                    MediaStore.Images.ImageColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Kamada_Picture/"
                )
            }

            val contentResolver = context.contentResolver
            val contentUri = contentResolver.insert(imageUri, contentValues)
            Log.d("File","$imageUri")
            //※2 ファイルを書き込む
            contentResolver.openFileDescriptor(contentUri!!, "w", null).use {
                FileOutputStream(it!!.fileDescriptor).use { output ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                imageUri = contentUri
            }

            contentValues.clear()
            if (Build.VERSION.SDK_INT >= 29) {
                contentResolver.update(contentUri, contentValues.apply {
                    put(MediaStore.Images.Media.IS_PENDING, false)
                }, null, null)
            } else {
                contentResolver.update(contentUri, contentValues, null, null)
            }

            // 処理が終わったら、メインスレッドに切り替える。
            withContext(Dispatchers.Main) {
                // プログレスバーを非表示
                showProgressBer.value = false
                showDownloadImage.value = true
                Toast.makeText(context, notifySuccess, Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }

    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "Range")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDownloaderScreen(applicationContext:Context) {
    val openAlertDialog = remember { mutableStateOf(true) }
    val showProgressBer = remember { mutableStateOf(false) }
    // 親コンポーネントにフォーカスを移動させるのに使う
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    var url by remember { mutableStateOf("") }
    val context = LocalContext.current
    val showDownloadImage = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            showDownloadImage.value = true
        }
        if (uri != null){
            Toast.makeText(applicationContext, notifyImageAcquisition, Toast.LENGTH_SHORT).show()
        }
    }
    val exitTheApplication = remember { mutableStateOf(false) }
    val permission = Manifest.permission.READ_MEDIA_IMAGES

    when {
        openAlertDialog.value ->
            if (permission.isGrantedPermission(context)){
                //パーミッションが許可されている
            }else {
                //パーミッションが不許可である
                PermissionDialog(openAlertDialog,exitTheApplication)
            }
    }

    if(exitTheApplication.value){
        //明示的にアクティビティ終了
        (context as? Activity)?.finish()
    }
    Log.d("testPATH",MediaStore.Images.ImageColumns.RELATIVE_PATH)
    Log.d("testPATH",Environment.DIRECTORY_PICTURES)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = { Text("FileDownLoader 3.0") }
            )
        }

    ){ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .clickable(
                    interactionSource = interactionSource,
                    enabled = true,
                    indication = null,
                    onClick = { focusRequester.requestFocus() } // 押したら外す
                )
                .focusRequester(focusRequester) // フォーカス操作するやつをセット
                .focusTarget(), // フォーカス当たるように
        ) {
            Button(
                onClick = {
                    //ギャラリーに遷移するIntentの作成
                    //val intent = Intent(Intent.ACTION_PICK)
                    //intent.type = "image/*"
                    //ギャラリーへ遷移
                    launcher.launch("image/*")
                },
                modifier = Modifier.weight(0.8f),
                shape = MaterialTheme.shapes.small
                ) {
                Text(text = "GALLERYから選択")
            }
            Row{
                TextField(
                    modifier = Modifier
                        .padding(10.dp)
                        .weight(1f),
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text(text = "URLを入力してください") },
                    singleLine = false
                )
                Button(
                    modifier = Modifier.weight(0.7f),
                    onClick = {
                        downloadImage(urlEntered = url, showProgressBer = showProgressBer, context = applicationContext, showDownloadImage)
                              },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "ダウンロード開始")
                }
            }
            Box(modifier = Modifier.weight(10f)){
                if (showProgressBer.value){
                    CircularProgressIndicator(
                        modifier = Modifier.width(64.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }else {
                    if (showDownloadImage.value) {
                        val imageBitmap =
                            try {
                                val inputStream = context.contentResolver.openInputStream(imageUri)
                                Log.d("File","$imageUri")
                                val bitmap = BitmapFactory.decodeFile(inputStream.toString())
                                bitmap?.asImageBitmap()
                            } catch (e: Exception) {
                                // エラー処理
                                e.printStackTrace()
                                null
                            }
                        Image(bitmap = imageBitmap!!, contentDescription = "Internal Storage Image")
                    }else{
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ){ }
                    }
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showDownloadImage.value = false
                        url = ""
                              },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "Clear")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        //val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                        //intent.type = "image/*"
                        //intent.addCategory(Intent.CATEGORY_OPENABLE)
                        launcher.launch("image/png")

                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "ダウンロードした画像")
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun GreetingPreview() {
    //PermissionDialog()
    //FileDownloaderScreen()
}