package com.android.task.main.function;


import com.android.task.tools.EquipmentId;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.webkit.WebView;


public class SysSetting {
	private final String TAG 					= SysSetting.class.getName();
	private final String TITLE					= "ϵͳ����";
	private final CharSequence[] FUNCTION_ITEMS = {"�Ʒ�����", "�豸����", "ȡ��"};


	private Activity mA;
	private WebView mWebView;
	private AlertDialog mSettingDialog;
	private UrlConfigure mUrlConf;
	private EquipmentId mEquipmentId;



	

	public SysSetting(Activity a,WebView w)
	{
		this.mA 			= a;
		this.mWebView		= w;
		this.mUrlConf		= new UrlConfigure(a,this.mWebView);
		this.mEquipmentId	= new EquipmentId(a);
		this.init();
	}
	
	public AlertDialog getSettingDialog() {
		return mSettingDialog;
	}
	
	private void init()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this.mA);
		builder.setTitle(TITLE);
		builder.setItems(FUNCTION_ITEMS, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				switch (item) {
				case 0:
					Log.d(TAG,"�Ʒ�����");
					SysSetting.this.mUrlConf.getUrlDialog().show();
					break;
				case 1:
					Log.d(TAG,"�豸����");
					Log.d(TAG,mEquipmentId.getId());
					break;
				case 2:
					Log.d(TAG,"ȡ��");
				default:
					break;
				}
			};
		});
		mSettingDialog = builder.create();
	}
}