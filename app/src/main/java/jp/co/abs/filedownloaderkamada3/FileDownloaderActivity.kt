package jp.co.abs.filedownloaderkamada3

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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


//パス文字列
private val downloadPath = Environment.getExternalStorageDirectory().path + "/Kamada_Picture"
private var fileName = ""
private var notifySuccess = "ダウンロードが完了しました"
private var notifyFailure = "画像取得に失敗しました"
var selfPathFile : String = ""

class FileDownloaderActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileDownloaderScreen(applicationContext)
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun PermissionDialog(
    downloadPathFile:File,
    openAlertDialog: MutableState<Boolean>
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
                                downloadPathFile.mkdir()
                                Log.d("Log", "new_File:$downloadPathFile")
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
                            //exitTheApplication.value = true
                        }
                    ) { Text(text = "しない") }
                }
            }
        }
    }
}

@SuppressLint("SimpleDateFormat", "CoroutineCreationDuringComposition")
fun downloadImage(urlEntered:String, showProgressBer: MutableState<Boolean>, context: Context) {
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
            val file = File(downloadPath, fileName)
            selfPathFile = file.toString()
            Log.d("selfPathFile", selfPathFile)
            // ファイルに書き込み
            FileOutputStream(file).use { stream ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            // 処理が終わったら、メインスレッドに切り替える。
            withContext(Dispatchers.Main) {
                // プログレスバーを非表示
                showProgressBer.value = false
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
    val downloadPathFile = File(downloadPath)
    val openAlertDialog = remember { mutableStateOf(true) }
    val showProgressBer = remember { mutableStateOf(false) }
    // 親コンポーネントにフォーカスを移動させるのに使う
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    var url by remember { mutableStateOf("") }
    var imageBitmap = remember {
        try {
            val file = File(selfPathFile)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            // エラー処理
            e.printStackTrace()
            null
        }
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
        selfPathFile = uri.toString()
    }
//    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
//        if (uri == null) return@rememberLauncherForActivityResult
//        imageUri = uri
//        selfPathFile = uri.toString()
//    }
//    val exitTheApplication = remember { mutableStateOf(false) }
//    val context = LocalContext.current
//    val navController = rememberNavController()

    when {
        openAlertDialog.value ->
            PermissionDialog(downloadPathFile,openAlertDialog)
    }

//    if(exitTheApplication.value){
//        if (!navController.popBackStack()) {
//            // 戻る画面がない場合は、明示的にアクティビティを終了
//            (context as? Activity)?.finish()
//        }
//    }

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
                    Toast.makeText(applicationContext, "画像を取得しました", Toast.LENGTH_SHORT).show()
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
                    onClick = { downloadImage(urlEntered = url, showProgressBer = showProgressBer, context = applicationContext) },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "ダウンロード開始")
                }
            }
            Box(modifier = Modifier.weight(10f)){
                if (showProgressBer.value){
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }else {
                    if (imageBitmap != null) {
                        Image(bitmap = imageBitmap!!, contentDescription = "Internal Storage Image")
                    }else{
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "画像image")
                        }
                    }
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selfPathFile = ""
                        imageBitmap = null },
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
                        launcher.launch("image/*")
                        Toast.makeText(applicationContext, "画像を取得しました", Toast.LENGTH_SHORT).show()
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(text = "ダウンロードした画像")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    //PermissionDialog()
    //FileDownloaderScreen()
}