package com.sinovoice.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
/**
 * 从assets目录下的AccountInfo.txt读取配置信息
 * @author sinovoice
 *
 */
public class AccountInfo {

    private static AccountInfo mInstance;

    private Map<String, String> mAccountMap;

    private AccountInfo() {
        mAccountMap = new HashMap<String, String>();
    }

    public static AccountInfo getInstance() {
        if (mInstance == null) {
            mInstance = new AccountInfo();
        }
        return mInstance;
    }

    /**
     * 获取capkey信息，本地为tts.local.synth，云端为对应的发音人，需要从灵云开发者社区勾选对应的能力才可以使用，否则会返回错误12
     * @return	返回capkey的字符串
     */
    public String getCapKey(){
        return mAccountMap.get("capKey");
    }
    /**
     * 获取developerKey信息，为开发者信息，需要从开发者社区新建应用，在应用详情中查看。
     * @return	返回developerKey的字符串
     */
    public String getDeveloperKey(){
        return mAccountMap.get("developerKey");
    }
    /**
     * 获取appKey信息，为应用信息，需要从开发者社区新建应用，在应用详情中查看。
     * @return	返回appKey的字符串
     */
    public String getAppKey(){
        return mAccountMap.get("appKey");
    }
    /**
     * 获取使用的Url信息，需要从开发者社区新建应用，在应用详情中查看。测试应用的url地址为http://test.api.hcicloud.com:8888
     * 如果申请商用，需要向灵云平台进行申请，申请成功后此url地址会有变动，请开发者注意。
     * @return	返回Url的字符串
     */
    public String getCloudUrl(){
        return mAccountMap.get("cloudUrl");
    }
    
    /**
     * 加载assets目录下的AccountInfo.txt文件
     * @param context	上下文
     * @return	成功返回true，失败返回false
     */
    public boolean loadAccountInfo(Context context) {
        boolean isSuccess = true;
        try {
            InputStream in = null;
            in = context.getResources().getAssets().open("AccountInfo.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(in,
                    "utf-8");
            BufferedReader br = new BufferedReader(inputStreamReader);
            String temp = null;
            String[] sInfo = new String[2];
            temp = br.readLine();
            while (temp != null) {
                if (!temp.startsWith("#") && !temp.equalsIgnoreCase("")) {
                    sInfo = temp.split("=");
                    if (sInfo.length == 2){
                        if(sInfo[1] == null || sInfo[1].length() <= 0){
                            isSuccess = false;
                            Log.e("AccountInfo", sInfo[0] + "is null");
                            break;
                        }
                        mAccountMap.put(sInfo[0], sInfo[1]);
                    }
                }
                temp = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            isSuccess = false;
        }
        
        return isSuccess;
    }
    

}
