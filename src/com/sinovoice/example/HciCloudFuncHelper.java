package com.sinovoice.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.api.tts.HciCloudTts;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.tts.ITtsSynthCallback;
import com.sinovoice.hcicloudsdk.common.tts.TtsConfig;
import com.sinovoice.hcicloudsdk.common.tts.TtsInitParam;
import com.sinovoice.hcicloudsdk.common.tts.TtsSynthResult;

public class HciCloudFuncHelper extends HciCloudHelper{
    private static final String TAG = HciCloudFuncHelper.class.getSimpleName();

    /**
     * 文件输出流，用来将合成的数据写到文件中。
     */
    private static FileOutputStream mFos;

    /**
     * 引擎合成过程中,每合成一段文字都会调用该回调方法通知外部并传回音频数据 音频数据保存在对象
     * TtsSynthResult中,通过该对象的getVoieceData()方法可以获取 合成是设定音频格式的音频数据
     */
    private static ITtsSynthCallback mTtsSynthCallback = new ITtsSynthCallback() {
        @Override
        public boolean onSynthFinish(int errorCode, TtsSynthResult result) {
            // errorCode 为当前合成操作返回的错误码,如果返回值为HciErrorCode.HCI_ERR_NONE则表示合成成功
            if (errorCode != HciErrorCode.HCI_ERR_NONE) {
                Log.e(TAG, "synth error, code = " + errorCode);
                return false;
            }
            
            if (mFos == null) {
                initFileOutputStream();
            }

            // 将本次合成的数据写入文件
            // 每次合成的数据，可能不是需要合成文本的全部，需要多次写入
            if(result != null && result.getVoiceData() != null){
                int length = result.getVoiceData().length;
                if (length > 0) {
                    try {
                        mFos.write(result.getVoiceData(), 0, length);
                        mFos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            //如果没有更多的数据，就对保存到音频的数据flush。
            if (!result.isHasMoreData()) {
                flushOutputStream();
            }
            
            //mark 标记语言的回调结果
            if(result.getTtsSynthMark().size() > 0)
            {
            	for(int i=0;i<result.getTtsSynthMark().size();++i)
            	{
            		ShowMessage(result.getTtsSynthMark().get(i).toStirng());
            	}
            }

            // 返回true表示处理结果成功,通知引擎可以继续合成并返回下一次的合成结果; 如果不希望引擎继续合成, 则返回false
            // 该方法在引擎中是同步的,即引擎会持续阻塞一直到该方法执行结束
            return true;
        }
    };

    /**
     * 语音合成的功能函数
     * @param capkey	使用的能力信息，本地为tts.local.synth，云端为对应的发音人
     * @param synthConfig	合成的参数的配置串类
     * @param synthText	合成的文本信息
     */
    public static void Synth(String capkey, TtsConfig synthConfig, String synthText) {
    	ShowMessage("....Synth start...");

        //启动TTS会话
        TtsConfig sessionConfig = new TtsConfig();
        sessionConfig.addParam(TtsConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
        Session session = new Session();
        int errCode = HciCloudTts.hciTtsSessionStart(sessionConfig.getStringConfig(), session);
        if (HciErrorCode.HCI_ERR_NONE != errCode) {
			ShowMessage("hciTtsSessionStart error:" + HciCloudSys.hciGetErrorInfo(errCode));
			return;
		}
		ShowMessage("hciTtsSessionStart Success");

        // 灵云语音合成的接口函数，合成的音频文件通过mTtsSynthCallback回调保存
        errCode = HciCloudTts.hciTtsSynth(session, synthText, synthConfig.getStringConfig(),
                mTtsSynthCallback);
        if (errCode == HciErrorCode.HCI_ERR_NONE) {
        	ShowMessage("hciTtsSynth Success");
        }else{
        	ShowMessage("hciTtsSynth error:" + HciCloudSys.hciGetErrorInfo(errCode));
        }
        
        HciCloudTts.hciTtsSessionStop(session);
        ShowMessage("hciTtsSessionStop");
    }


    /**
     * TTS合成动作完毕，将合成的数据输出到文件中
     */
    private static void flushOutputStream() {
        try {
            mFos.close();
            mFos = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 初始化保存音频文件的流信息
     */
    private static void initFileOutputStream() {
        try {
        	//保存音频文件的路径，pcm是音频的原始格式，一般的播放器无法播放，可以使用AudioTrack类进行播放
            String filePath = Environment.getExternalStorageDirectory() + "/synth.pcm";
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            } else {
                file.createNewFile();
            }
            mFos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 语音合成演示的功能函数
     * @param context	context上下文
     * @param capkey	使用的capkey，本地为tts.local.synth，云端为对应的发音人，从assets目录的下的AccountInfo.txt中读取。
     * @param view	view对象
     */
	public static void Func(Context context,String capkey,TextView view) {
		
		setTextView(view);
		setContext(context);
        //TTS初始化
        TtsInitParam ttsInitParam = new TtsInitParam();
        // 获取App应用中的lib的路径,放置能力所需资源文件。如果使用/data/data/packagename/lib目录,需要添加android_so的标记
        //使用本地的capkey需要配置dataPath，云端的capkey可以不用配置。
        String dataPath = context.getFilesDir().getAbsolutePath().replace("files", "lib");
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_DATA_PATH, dataPath);
        //加载语音库以so的方式，需要把对应的音库资源拷贝到libs/armeabi目录下，并修改名字为libxxx.so的方式。
        //还可以按照none的方式加载，此时不需要对音库修改名称，直接拷贝到dataPath目录下即可，最好设置dataPath为sd卡目录。比如
        //String dataPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "sinovoice";
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_FILE_FLAG, TtsInitParam.VALUE_OF_PARAM_FILE_FLAG_ANDROID_SO);
        
        // 用户可以根据自己可用的能力进行设置,本地能力为tts.local.synth 
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_INIT_CAP_KEYS, capkey);
        Log.i(TAG, "hciTtsInit config :" + ttsInitParam.getStringConfig());
        // 调用初始化方法
        int errCode = HciCloudTts.hciTtsInit(ttsInitParam.getStringConfig());
        if (errCode != HciErrorCode.HCI_ERR_NONE) {
            ShowMessage("hciTtsInit error:" + HciCloudSys.hciGetErrorInfo(errCode));
            return;
        } else {
        	ShowMessage("hciTtsInit Success");
        }
        
        byte[] synthData = getAssetFileData("tts.txt");
		String synthText = null;
		try {
			synthText = new String(synthData, "utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//语音合成的配置串参数，可以通过addParam参数添加，列出一些常见的参数配置
        TtsConfig synthConfig = new TtsConfig();
        //合成的音频格式，默认为pcm16k16bit
        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_AUDIO_FORMAT, "pcm16k16bit");
        //合成的语速设置，默认为5
        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_SPEED, "5");
        //合成的基频设置，默认为5
        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_PITCH, "5");
        //合成的音量设置，默认为5
        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_VOLUME, "5");
        //数字阅读方式，有电报读法和数字读法两种
        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_DIGIT_MODE, "auto_number");
        //英文阅读方式 ，有按照单词和字母两种方式，默认为自动判断
        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_ENG_MODE, "auto");
        //标点符号读法，读符号和不读符号两种，默认为不读
        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_PUNC_MODE, "off");
        //标记处理方式 ,仅本地能力支持，默认为none，本地支持s3ml标记
//        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_TAG_MODE, "none");
        //朗读风格, clear: 清晰		vivid: 生动 		normal: 抑扬顿挫  		plain: 平稳庄重 
//        synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_VOICE_STYLE, "clear");
        //只有云端的capkey支持此方法，云端合成后，把音频文件发送到本地客户端时可以进行压缩以减少流量消耗，默认为none，不压缩
        //speex压缩，压缩的比较多，对应的libs/armeabi目录下的libjtspeex.so库，设置为此方式时需要添加对应的so库。
        //opus压缩，对应的libs/armeabi目录下的libjtopus.so库，设置为此方式时需要添加对应的so库。
//        synthConfig.addParam(TtsConfig.EncodeConfig.PARAM_KEY_ENCODE, "speex");
        
        if (capkey.indexOf("tts.cloud.synth") != -1)
        {
        	//property 属于 私有云 云端能力 必填参数，使用公有云的请忽略此参数
    		//none: 所有标记将会被视为文本读出，缺省值
        	synthConfig.addParam(TtsConfig.PrivateCloudConfig.PARAM_KEY_PROPERTY, "cn_xiaokun_common");
            synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_TAG_MODE, "none");
        }
        Synth(capkey, synthConfig, synthText);
        //TTS反初始化
		HciCloudTts.hciTtsRelease();
	}

}
