package com.sf.lib_scannerbluetooth.headset

import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import com.sf.lib_scannerbluetooth.pool.BluetoothExecutorTools
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ThreadPoolExecutor

/**
 * @Author: Jinhuan.Li
 * @Date: 2019/8/6
 * @Des: //录音器实现
 */
class Recorder(context: Context) {
    companion object {
        val TAG: String = "Recorder"
    }

    /**
     * 录音数据回调接口
     */
    var mListener: RecoderListener? = null

    interface RecoderListener {
        fun onData(data: ByteArray)
    }

    /**
     * 音频manager
     */
    private lateinit var mAudioManager: AudioManager
    /**
     * 录音线程
     */
//    private var mRecordingThread: RecordThread? = null
    private var mRecordingThread: RecordThread1? = null


    init {
        mAudioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }

    fun startRecord(listener: RecoderListener) {
        mListener = listener
        /**
         * 当前平台是否支持使用SCO的关闭调用用例.通话模式。语音从蓝牙进。
         */
        if (mAudioManager.isBluetoothScoAvailableOffCall) {

            if (mAudioManager.isBluetoothScoOn) {
                mAudioManager.stopBluetoothSco()
                Log.e("BTRecordImpl", "1mAudioManager.stopBluetoothSco()")
            }
            Log.e("BTRecordImpl", "1  开启 startBluetoothSco")
            mAudioManager.startBluetoothSco()
            var timeout = 100
            /**
             * 再次打开，如果sco录音还是没有开启，倒计时开始又重新开启sco
             */
            while (!mAudioManager.isBluetoothScoOn && timeout-- > 0) {
                Thread.sleep(10)
                if (timeout == 50) {
                    Log.e("BTRecordImpl", "2 重新开启 startBluetoothSco")
                    mAudioManager.startBluetoothSco()
                }
                Log.e("BTRecordImpl", "change BluetoothScoOn" + mAudioManager.isBluetoothScoOn + ":" + timeout)
            }
            /**
             * 先中断之前的录音
             */
            if (mRecordingThread != null) {
                mRecordingThread!!.pause()
//                mRecordingThread!!.interrupt()
            }
            /**
             * 重新开启录音线程
             */
//            mRecordingThread = RecordThread()
//            mRecordingThread!!.start()
            //fixme  优化线程
            if (mRecordingThread == null) {
                mRecordingThread = RecordThread1()
            }
            if (BluetoothExecutorTools.sExecutorService == null) {
                Log.e(TAG, "线程池为空，启动播放失败")
                return
            }
            BluetoothExecutorTools.sExecutorService.execute(mRecordingThread);
        }

    }

    private val SAMPLE_RATE_HZ = 16000

    internal inner class RecordThread1 : Runnable {

        private val audioRecord: AudioRecord
        private val bufferSize: Int
        private var isRun: Boolean = false
        private var mStartTime = 0L

        init {
            /**
             * 设置音频源，蓝牙录音
             */
            var audiosource = MediaRecorder.AudioSource.VOICE_RECOGNITION
            if (Build.VERSION.SDK_INT > 19) {
                audiosource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            }
            /**
             * 录音缓冲大小
             * 1.采样率16000，蓝牙耳机采用
             * 2.音频通道 CHANNEL_IN_MONO，支持所有平台运行音频相关
             * 3.返回音频格式。暂时采用pcm 16位
             */
            this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2
            /**
             * 初始化AudioRecorder录音器
             * 1.采样率
             * 2.通道
             * 3.音频格式
             * 4.缓冲区
             */
            this.audioRecord = AudioRecord(audiosource,
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    this.bufferSize)

        }


        override fun run() {
            Thread.currentThread().name = "AIUI_RecordThread"
            this.isRun = true
            try {
                if (audioRecord.state == 1) {

                    /**
                     * 开始录音
                     */
                    this.audioRecord.startRecording()

                    mStartTime = System.currentTimeMillis()
                    /**
                     * 录音循环读取数据
                     */
                    while (this.isRun) {

                        val buffer = ByteArray(bufferSize)
                        val readBytes = audioRecord.read(buffer, 0, bufferSize)
                        if (readBytes > 0) {
//                            val valume = calculateVolume(buffer)
                            Log.d(TAG, "录音数据=" + readBytes)
                            /**
                             * 读取到音频，返回抛出去给回调
                             */
                            if (mListener != null) {
                                mListener!!.onData(buffer)
                            }
//                            Log.e("RecordingManager", "endVoiceRequest() --> " + valume)
                        }

                    }

                    try {

                        this.audioRecord.stop()
                        this.audioRecord.release()
                    } catch (audioException: Exception) {

                    }

                    Log.e("RecordingManager", "endVoiceRequest() --> ")
//                  this.audioTrack.stop()

                }
            } catch (e2: Exception) {
                Log.e("BtRecordImpl", "error: " + e2.message)
                try {
                    this.audioRecord.stop()
                    this.audioRecord.release()
                } catch (audioException: Exception) {

                }

                isRun = false

            }

        }

        fun pause() {
            this.isRun = false
            try {
                this.audioRecord.stop()
                this.audioRecord.release()
            } catch (e: Exception) {

            }
        }

//        @Synchronized override fun start() {
//            if (!isRun) {
//                super.start()
//            }
//        }

        private fun calculateVolume(buffer: ByteArray): Int {
            val audioData = ShortArray(buffer.size / 2)
            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData)
            var sum = 0.0
            // 将 buffer 内容取出，进行平方和运算
            for (i in audioData.indices) {
                sum += (audioData[i] * audioData[i]).toDouble()
            }
            // 平方和除以数据总长度，得到音量大小
            val mean = sum / audioData.size.toDouble()
            val volume = 10 * Math.log10(mean)
            return volume.toInt()
        }

    }

    /**
     * 录音线程
     */
    internal inner class RecordThread : Thread() {
        private val audioRecord: AudioRecord
        private val bufferSize: Int
        private var isRun: Boolean = false
        private var mStartTime = 0L

        init {
            /**
             * 设置音频源，蓝牙录音
             */
            var audiosource = MediaRecorder.AudioSource.VOICE_RECOGNITION
            if (Build.VERSION.SDK_INT > 19) {
                audiosource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
            }
            /**
             * 录音缓冲大小
             * 1.采样率16000，蓝牙耳机采用
             * 2.音频通道 CHANNEL_IN_MONO，支持所有平台运行音频相关
             * 3.返回音频格式。暂时采用pcm 16位
             */
            this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2
            /**
             * 初始化AudioRecorder录音器
             * 1.采样率
             * 2.通道
             * 3.音频格式
             * 4.缓冲区
             */
            this.audioRecord = AudioRecord(audiosource,
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    this.bufferSize)

        }

        /**
         * 开始录音
         */
        override fun run() {
            super.run()
            Thread.currentThread().name = "AIUI_RecordThread"
            this.isRun = true
            try {
                if (audioRecord.state == 1) {

                    /**
                     * 开始录音
                     */
                    this.audioRecord.startRecording()

                    mStartTime = System.currentTimeMillis()
                    /**
                     * 录音循环读取数据
                     */
                    while (this.isRun) {

                        val buffer = ByteArray(bufferSize)
                        val readBytes = audioRecord.read(buffer, 0, bufferSize)
                        if (readBytes > 0) {
//                            val valume = calculateVolume(buffer)
                            Log.d(TAG, "录音数据=" + readBytes)
                            /**
                             * 读取到音频，返回抛出去给回调
                             */
                            if (mListener != null) {
                                mListener!!.onData(buffer)
                            }
//                            Log.e("RecordingManager", "endVoiceRequest() --> " + valume)
                        }

                    }

                    try {

                        this.audioRecord.stop()
                        this.audioRecord.release()
                    } catch (audioException: Exception) {

                    }

                    Log.e("RecordingManager", "endVoiceRequest() --> ")
//                  this.audioTrack.stop()

                }
            } catch (e2: Exception) {
                Log.e("BtRecordImpl", "error: " + e2.message)
                try {
                    this.audioRecord.stop()
                    this.audioRecord.release()
                } catch (audioException: Exception) {

                }

                isRun = false

            }

        }

        fun pause() {
            this.isRun = false
            try {
                this.audioRecord.stop()
                this.audioRecord.release()
            } catch (e: Exception) {

            }
        }

        @Synchronized
        override fun start() {
            if (!isRun) {
                super.start()
            }
        }

        private fun calculateVolume(buffer: ByteArray): Int {
            val audioData = ShortArray(buffer.size / 2)
            ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData)
            var sum = 0.0
            // 将 buffer 内容取出，进行平方和运算
            for (i in audioData.indices) {
                sum += (audioData[i] * audioData[i]).toDouble()
            }
            // 平方和除以数据总长度，得到音量大小
            val mean = sum / audioData.size.toDouble()
            val volume = 10 * Math.log10(mean)
            return volume.toInt()
        }
    }

    /**
     * 停止录音
     */
    fun stopRecord() {
        mRecordingThread?.pause()
    }
}
