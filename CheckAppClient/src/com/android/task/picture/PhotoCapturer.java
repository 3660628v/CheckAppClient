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
	private final String TAG = PhotoCapturer.class.getName();
	SurfaceView sView;
	SurfaceHolder surfaceHolder;
	int imgWidth, imgHeight;
	// ����ϵͳ���õ������
	Camera camera;
	//�Ƿ��������
	boolean isPreview = false;
	
	
	
    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// ����ȫ��
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.cam_pic_layout);
		
		sView = (SurfaceView) findViewById(R.id.pic_view);
		surfaceHolder = sView.getHolder();
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
				Log.i(TAG,"surface destroyed");

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
						Log.e(TAG, "Error take picture: " + e.getMessage());
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
			Log.d(TAG,"open camera");
		}
		if (camera != null && !isPreview)
		{
			try
			{
				CameraSetting.setCameraParameter(camera, surfaceHolder);
				camera.startPreview();
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
			final File picFile = Tools.getOutputMediaFile(Tools.MEDIA_TYPE_IMAGE);
			if( picFile == null ){
				Log.e(TAG, "Fail to generate image, check storage permission.");
				return;
			}
			
			// save to file
			try {
				FileOutputStream fos = new FileOutputStream(picFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, "Error accessing file: " + e.getMessage());
				Toast.makeText(PhotoCapturer.this, "�޷�����ͼ������洢����", Toast.LENGTH_SHORT).show();
			}
			
			final Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);	
			if(bm == null) {
				Log.e(TAG, "bitmap is null.");
				return;
			}
			
			PhotoSaveDialog photo_save_dialog = new PhotoSaveDialog(PhotoCapturer.this,bm,picFile);
			photo_save_dialog.getPhotoSaveDialog().show();
			
			//�������
			camera.stopPreview();
			camera.startPreview();
			isPreview = true;
		}
	};
}


