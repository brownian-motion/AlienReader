package com.brownian.alienreader.node
//
//import android.speech.tts.TextToSpeech
//import android.speech.tts.UtteranceProgressListener
//import com.brownian.alienreader.message.Receiver
//import com.brownian.alienreader.message.Sender
//import com.brownian.alienreader.message.tts.ReadCommand
//import com.brownian.alienreader.message.tts.TtsInitializedMessage
//import com.brownian.alienreader.message.tts.TtsProgressMessage
//import com.brownian.alienreader.message.tts.TtsShutdownCommand
//
//interface TtsControllerNode {
//    val progressSender: Sender<TtsProgressMessage>
//    val initSender: Sender<TtsInitializedMessage>
//    val readCommandListener: Receiver<ReadCommand>
//    val shutdownCommandListener: Receiver<TtsShutdownCommand>
//}
//
//class TtsControllerNodeImpl(private val tts: TextToSpeech) : TtsControllerNode, AutoCloseable {
//    init {
//        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//            override fun onDone(utteranceId: String) {
//                progressSender.send(TtsProgressMessage.TtsDoneMessage(utteranceId))
//            }
//
//            @Deprecated("Deprecated as of API 21")
//            override fun onError(utteranceId: String) {
//            }
//
//            override fun onError(utteranceId: String, errorCode: Int) {
//                progressSender.send(TtsProgressMessage.TtsErrorMessage(utteranceId, errorCode))
//            }
//
//            override fun onStart(utteranceId: String) {
//                progressSender.send(TtsProgressMessage.TtsStartedMessage(utteranceId))
//            }
//
//            override fun onStop(utteranceId: String, interrupted: Boolean) {
//                progressSender.send(TtsProgressMessage.TtsStoppedMessage(utteranceId, interrupted))
//            }
//        })
//    }
//
//    override lateinit var progressSender: Sender<TtsProgressMessage>
//    override lateinit var initSender: Sender<TtsInitializedMessage>
//
//    override val readCommandListener = Receiver<ReadCommand> { message ->
//        tts.speak(message.content.body, TextToSpeech.QUEUE_FLUSH, null, message.content.id.fullname)
//    }
//
//    override val shutdownCommandListener = Receiver<TtsShutdownCommand> { message ->
//        tts.shutdown()
//    }
//
//    override fun close() = tts.shutdown()
//}