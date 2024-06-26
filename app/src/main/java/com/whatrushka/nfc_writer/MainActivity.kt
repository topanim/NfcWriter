package com.whatrushka.nfc_writer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatrushka.nfc_writer.api.Api
import com.whatrushka.nfc_writer.api.Client
import com.whatrushka.nfc_writer.ui.theme.NfcWriterTheme
import kotlinx.coroutines.launch
import java.io.IOException


class MainActivity : ComponentActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private var tag: Tag? = null

    private val api = Api(Client)

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapter = NfcAdapter.getDefaultAdapter(this)

        val pm: PackageManager = packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Log.d("m", "Has NFC functionality")
        } else {
            Log.d("m", "Has No NFC functionality")
        }

        nfcAdapter = if (adapter != null) {
            adapter
        } else {
            val nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
            nfcManager.defaultAdapter
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        setContent {
            NfcWriterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    val cardHash = remember { mutableStateOf("") }
                    val login = remember { mutableStateOf("") }
                    val password = remember { mutableStateOf("") }
                    val idUser = remember { mutableStateOf("") }

                    val scope = rememberCoroutineScope()
                    val modalBottomSheetState = rememberModalBottomSheetState(
                        initialValue = ModalBottomSheetValue.Hidden,
                        skipHalfExpanded = true
                    )

                    ModalBottomSheetLayout(
                        sheetState = modalBottomSheetState,
                        sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        sheetContent = {
                            RegSheet(
                                idUser = idUser,
                                cardHash = cardHash,
                                sheetState = modalBottomSheetState
                            )
                        }
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                            ) {
                                Text(
                                    text = "Прошивка карты",
                                    style = TextStyle(
                                        fontWeight = FontWeight.W700,
                                        fontSize = 26.sp,
                                        color = Color.Black
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = cardHash.value,
                                    onValueChange = { cardHash.value = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(text = "HASH карты")
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.DarkGray,
//                                unfocusedLabelColor = Color.DarkGray,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        focusedLabelColor = Color.DarkGray,
                                        focusedTextColor = Color.DarkGray,
                                        unfocusedTextColor = Color.Gray,
                                    )
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.DarkGray
                                    ),
                                    onClick = {
                                        try {
                                            if (tag == null) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    ERROR_DETECTED,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                write(cardHash.value, tag)
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    WRITE_SUCCESS,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: IOException) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                WRITE_ERROR,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            e.printStackTrace()
                                        } catch (e: FormatException) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                WRITE_ERROR,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            e.printStackTrace()
                                        }
                                    }
                                ) {
                                    Text(
                                        modifier = Modifier.padding(8.dp),
                                        text = "Прошить карту",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp,
                                            color = Color.White

                                        )
                                    )
                                }
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.DarkGray
                                    ),
                                    onClick = { scope.launch { modalBottomSheetState.show() } }
                                ) {
                                    Text(
                                        modifier = Modifier.padding(8.dp),
                                        text = "Выпустить карту",
                                        style = TextStyle(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun RegSheet(
        cardHash: MutableState<String>,
        modifier: Modifier = Modifier,
        idUser: MutableState<String>,
        sheetState: ModalBottomSheetState
    ) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        Column(
            Modifier
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = "Выпуск карты",
                style = TextStyle(
                    fontWeight = FontWeight.W700,
                    fontSize = 26.sp,
                    color = Color.Black
                ),
                modifier = Modifier.padding(top = 36.dp)
            )

            OutlinedTextField(
                value = idUser.value,
                onValueChange = { idUser.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                placeholder = {
                    Text(text = "ID пользователя")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.DarkGray,
//                                unfocusedLabelColor = Color.DarkGray,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    focusedLabelColor = Color.DarkGray,
                    focusedTextColor = Color.DarkGray,
                    unfocusedTextColor = Color.Gray,
                )
            )

            Row(Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier.padding(top = 16.dp, bottom = 36.dp),
                    onClick = {
                        if (idUser.value.isNotBlank()) {
                            scope.launch {
                                try {
                                    val hash = api.addCard(idUser.value.toInt())
                                    cardHash.value = hash
                                    sheetState.hide()
                                } catch (ex: Exception) {
                                    Log.d("m", ex.stackTraceToString())
                                    Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Заполните поле", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray
                    ),
                ) {
                    Text(
                        text = "Выпустить",
                        style = TextStyle(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(
            this,
            pendingIntent,
            arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)),
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
    }

    private fun write(text: String, tag: Tag?) {
        val records = arrayOf(createRecord(text))
        val message = NdefMessage(records)
        // Get an instance of Ndef for the tag.
        val ndef = Ndef.get(tag)
        // Enable I/O
        ndef.connect()
        // Write the message
        ndef.writeNdefMessage(message)
        // Close the connection
        ndef.close()
    }

    private fun createRecord(text: String): NdefRecord {
        val lang = "en"
        val textBytes = text.toByteArray()
        val langBytes = lang.toByteArray(charset("US-ASCII"))
        val langLength = langBytes.size
        val textLength = textBytes.size
        val payload = ByteArray(1 + langLength + textLength)

        // set status byte (see NDEF spec for actual bits)
        payload[0] = langLength.toByte()

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength)
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }

    companion object {
        const val ERROR_DETECTED = "No NFC tag detected!"
        const val WRITE_SUCCESS = "Text written to the NFC tag successfully!"
        const val WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?"
    }
}