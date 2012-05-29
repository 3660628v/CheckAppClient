package com.android.task.picture;

import com.android.task.tools.*;
import com.android.task.video.VideoRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import com.android.task.R;
import com.android.task.R.id;
import com.android.task.R.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;


public class PhotoCapturer extends Activity
{
	SurfaceView sView;
	SurfaceHolder surfaceHolder;
	int screenWidth, screenHeight;
	int imgWidth, imgHeight;
	// ����ϵͳ���õ������
	Camera camera;
	//�Ƿ��������
	boolean isPreview = false;
	private final int  GET_CONTENT_URI = 123;
	
	protected void onActivityResult(int requestCode, int resultCode,  Intent intent) {
		if(requestCode==GET_CONTENT_URI)  
		{
			Uri result = intent == null || resultCode != RESULT_OK ? null  : intent.getData();  
			Log.i("aa",result.toString());
			finish();
		}
	}
    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// ����ȫ��
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.cam_pic_layout);
		
		WindowManager wm = (WindowManager) getSystemService(
			Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		// ��ȡ��Ļ�Ŀ�͸�
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();
		// ��ȡ������SurfaceView���
		sView = (SurfaceView) findViewById(R.id.pic_view);
		// ���SurfaceView��SurfaceHolder
		surfaceHolder = sView.getHolder();
		// ΪsurfaceHolder���һ���ص�������
		surfaceHolder.addCallback(new Callback()
		{
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height)
			{
			}

			public void surfaceCreated(SurfaceHolder holder)
			{
				// ������ͷ
				initCamera();
			}
			
			public void surfaceDestroyed(SurfaceHolder holder)
			{
				Log.i("aa","surface destroyed");

				// ���camera��Ϊnull ,�ͷ�����ͷ
				if (camera != null)
				{
					if (isPreview)
					{
						camera.stopPreview();
						isPreview = false;
					}
					camera.release();
					camera = null;
				}
			}		
		});
		// ���ø�SurfaceView�Լ���ά������    
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		// set button event
		Button recBtn = (Button) findViewById(R.id.pic_rec_btn);
		recBtn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				if(isPreview){
					try {
						// take picture and save
						camera.takePicture(null, null, myPicture);
					} catch (Exception e) {
						// TODO: handle exception
						Log.e("aa", "Error take picture: " + e.getMessage());
					}
				}
			}
		});
		
		Button exitBtn = (Button) findViewById(R.id.pic_exit_btn);
		exitBtn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
					UploadMessage.set_upload_message(null);
					PhotoCapturer.this.finish();
					return;
				}
		});
		
	}

    private void initCamera()
	{
		if (!isPreview)
		{
			camera = Camera.open();
			Log.i("aa","open camera");
		}
		if (camera != null && !isPreview)
		{
			try
			{
				Camera.Parameters parameters = camera.getParameters();
				// ����Ԥ����Ƭ�Ĵ�С
				parameters.setPreviewSize(screenWidth, screenHeight);
				Log.i("aa",String.valueOf(screenWidth));
				Log.i("aa",String.valueOf(screenHeight));
				camera.setDisplayOrientation(90);
				parameters.setRotation(90);


				// ÿ����ʾ4֡
				/*parameters.setPreviewFrameRate(4);
				// ����ͼƬ��ʽ
				parameters.setPictureFormat(PixelFormat.JPEG);
				// ����JPG��Ƭ������
				parameters.set("jpeg-quality", 85);
				//������Ƭ�Ĵ�С
				imgWidth = screenWidth;
				imgHeight = screenHeight;
				parameters.setPictureSize(imgWidth, imgHeight);*/
				camera.setParameters(parameters);
				//ͨ��SurfaceView��ʾȡ������
				camera.setPreviewDisplay(surfaceHolder);
				// ��ʼԤ��
				camera.startPreview();
				// set autofocus
				camera.autoFocus(null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			isPreview = true;
		}
	}

	
	PictureCallback myPicture = new PictureCallback()
	{
		public void onPictureTaken(byte[] data, Camera camera)
		{
			// first save temp picture to dir
			final File picFile = Tools.getOutputMediaFile(
					Tools.MEDIA_TYPE_IMAGE);
			if( picFile == null ){
				Log.e("aa", "Fail to generate image, check storage permission.");
				return;
			}
			
			// save to file
			try {
				FileOutputStream fos = new FileOutputStream(picFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.e("aa", "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.e("aa", "Error accessing file: " + e.getMessage());
				Toast.makeText(
						PhotoCapturer.this, 
						"�޷�����ͼ������洢����", Toast.LENGTH_SHORT).show();
			}
			
			// show layout
			final Bitmap bm = BitmapFactory.decodeByteArray(data
					, 0, data.length);	// null
			if(bm == null) {
				Log.e("aa", "bitmap is null.");
				return;
			}
			View pic_save_dialog = getLayoutInflater().inflate(
					R.layout.cam_pic_save_layout, null);
			ImageView pic_view = (ImageView)pic_save_dialog.findViewById(
					R.id.pic_save_view);
			pic_view.setImageBitmap(bm);	// error
			// show in dialog
			AlertDialog.Builder save_diaglog = new AlertDialog.Builder(
					PhotoCapturer.this);
			save_diaglog.setView(pic_save_dialog);
			save_diaglog.setPositiveButton("����", new OnClickListener()
			{
				public void onClick(DialogInterface dialog,
					int which)
				{
					InsertFileToMediaStore insert_file = new InsertFileToMediaStore(PhotoCapturer.this,picFile,"image/jpeg");
					Uri uri = insert_file.insert();
					picFile.delete();
					UploadMessage.set_upload_message(uri);
					finish();
				}
			});
			save_diaglog.setNegativeButton("ȡ��", new OnClickListener()
			{
				public void onClick(DialogInterface dialog,
					int which)
				{
					// delete saved file
					if(picFile != null) {
						picFile.delete();
					}
				}
			});
			
			save_diaglog.show();

			//�������
			camera.stopPreview();
			camera.startPreview();
			isPreview = true;
		}
	};
}


