    package com.example.mynewproject

    import android.app.Activity
    import android.support.v7.app.AppCompatActivity
    import android.os.Bundle
    import android.speech.RecognizerIntent
    import android.view.KeyEvent
    import android.view.View
    import kotlinx.android.synthetic.main.activity_main.*
    import android.view.LayoutInflater
    import com.google.api.gax.core.FixedCredentialsProvider
    import com.google.auth.oauth2.ServiceAccountCredentials
    import com.google.auth.oauth2.GoogleCredentials
    import com.google.cloud.dialogflow.v2.*
    import java.util.*
    import android.widget.*
    import android.widget.Toast
    import android.content.ActivityNotFoundException
    import android.content.Intent
    import android.speech.tts.TextToSpeech
    import android.text.Editable


    const val USER=0
    const val BOT=1
    const val SPEECH_INPUT=2

    class MainActivity : AppCompatActivity() {

        private val uuid = UUID.randomUUID().toString()

        private var client: SessionsClient? = null
        private var session: SessionName? = null

        private var asistan_voice:TextToSpeech?=null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            val scrollview=findViewById<ScrollView>(R.id.scroll_chat)
            scrollview.post{
                scrollview.fullScroll(ScrollView.FOCUS_DOWN)
            }

            val queryEditText = findViewById<EditText>(R.id.edittext)
            queryEditText.setOnKeyListener { view, keyCode, event ->
                if (event.action === KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            sendMessage(send)
                            true
                        }
                        else -> {
                        }
                    }
                }
                false
            }

            send.setOnClickListener(this::sendMessage)

            microphone.setOnClickListener(this::sendMicrophoneMessage)

            initAsisstant()

            initAsisstantVoice()

        }

        private fun initAsisstantVoice() {

            asistan_voice= TextToSpeech(applicationContext,object : TextToSpeech.OnInitListener {
                override fun onInit(status: Int) {
                    if (status!=TextToSpeech.ERROR){
                        asistan_voice?.language=Locale("en")
                    }
                }

            })

        }

        private fun initAsisstant() {
            try {
                val stream = resources.openRawResource(R.raw.asistan)
                val credentials = GoogleCredentials.fromStream(stream)
                val projectId = (credentials as ServiceAccountCredentials).projectId

                val settingsBuilder = SessionsSettings.newBuilder()
                val sessionsSettings =
                    settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build()
                client = SessionsClient.create(sessionsSettings)
                session = SessionName.of(projectId, uuid)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


        private fun sendMessage(view: View) {
            val msg = edittext.text.toString()
            if (msg.trim { it <= ' ' }.isEmpty()) {
                Toast.makeText(this@MainActivity, getString(R.string.enter), Toast.LENGTH_LONG).show()
            } else {
                appendText(msg, USER)
                edittext.setText("")

                // Java V2
                val queryInput =
                    QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("tr")).build()
                RequestTask(this@MainActivity, session!!, client!!, queryInput).execute()
            }
        }

        private fun sendMicrophoneMessage(view:View){
            val intent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt)
            )
            try {
                startActivityForResult(intent, SPEECH_INPUT)
            } catch (a: ActivityNotFoundException) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        private fun appendText(message: String, type: Int) {
            val layout: FrameLayout
            when (type) {
                USER -> layout = appendUserText()
                BOT -> layout = appendBotText()
                else -> layout = appendBotText()
            }
            layout.isFocusableInTouchMode = true
            linear_chat.addView(layout)
            val tv = layout.findViewById<TextView>(R.id.chatMsg)
            tv.setText(message)
            Util.hideKeyboard(this)
            layout.requestFocus()
            edittext.requestFocus() // change focus back to edit text to continue typing
            if(type!= USER) asistan_voice?.speak(message,TextToSpeech.QUEUE_FLUSH,null)
        }


        fun appendUserText(): FrameLayout {
            val inflater = LayoutInflater.from(this@MainActivity)
            return inflater.inflate(R.layout.user_message, null) as FrameLayout
        }

        fun appendBotText(): FrameLayout {
            val inflater = LayoutInflater.from(this@MainActivity)
            return inflater.inflate(R.layout.bot_message, null) as FrameLayout
        }

        fun onResult(response: DetectIntentResponse?) {
            try {
                if (response != null) {
                    var botReply:String=""
                    if(response.queryResult.fulfillmentText==" ")
                        botReply= response.queryResult.fulfillmentMessagesList[0].text.textList[0].toString()
                    else
                        botReply= response.queryResult.fulfillmentText

                    appendText(botReply, BOT)
                } else {
                    appendText(getString(R.string.repeat), BOT)
                }
            }catch (e:Exception){
                appendText(getString(R.string.repeat), BOT)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            when(requestCode){
                SPEECH_INPUT->{
                    if(resultCode== Activity.RESULT_OK
                        && data !=null){
                        val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        edittext.text=Editable.Factory.getInstance().newEditable(result[0])
                        sendMessage(send)
                    }
                }
            }
        }

        override fun onPause() {
            super.onPause()
        }

        override fun onDestroy() {
            super.onDestroy()
            if(asistan_voice !=null){
                asistan_voice?.stop()
                asistan_voice?.shutdown()
            }
        }

    }
