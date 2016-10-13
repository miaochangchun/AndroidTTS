package com.sinovoice.example;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.widget.TextView;

public class HciCloudHelper {
    private static TextView mView = null;
    private static Context mContext = null;
    

    protected static void setContext(Context context){
    	mContext = context;
    }

	
	/**
	 * 从assets目录下的tts.txt中读取文本数据
	 * 
	 * @param fileName
	 * @return
	 */
	protected static byte[] getAssetFileData(String fileName) {
		InputStream in = null;
		int size = 0;
		try {
			in = mContext.getResources().getAssets().open(fileName);
			size = in.available();
			byte[] data = new byte[size];
			in.read(data, 0, size);

			return data;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.print(e.getMessage());
			return null;
		}
	}
	
	
	protected static void setTextView(TextView view){
    	mView = view;
    }
	
	/**
	 * 显示消息
	 * 
	 * @param message
	 * @return
	 */
	protected static void ShowMessage(String message){
		 mView.append(message + "\n");
	}
	
}
