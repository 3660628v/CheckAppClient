package com.android.task.video;


import com.android.task.picture.PhotoCapturer;
import com.android.task.tools.*;

import java.io.File;
import java.io.IOException;

import com.android.task.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class VideoRecorder extends Activity
{

	// size
	SurfaceView sView;
	SurfaceHolder surfaceHolder;
	int screenWidth, screenHeight;
	// ����ϵͳ���õ������
	Camera camera;
	// recorder
	MediaRecorder vRecorder;
	// max video duration (milliseconds)
	int max_video_duration = 10000;	// 5 min
	// save video path
	File save_vid_file;
	//�Ƿ��������
	boolean isPreview = false;
	// if recording
	boolean isRecording = false;
	// left recording time
	int left_time = max_video_duration;
	final static int VIDEO_SHOW_REQUEST = 100;
	
	final  int VIDEO_WIDTH 	= 640;
	final  int VIDEO_HEIGHT	= 480;
	
	final  int VIDEO_MUILTI  = 30;
	
	OrientationEventListener myOrientationEventListener = null;
	
	
	
	// recording timer
	private Handler handler = new Handler();
	private Runnable task = new Runnable()
	{
		public void run()
		{
			if (isRecording)
			{
				left_time -= 1000;  
				TextView vid_timer = (TextView)findViewById(R.id.vid_info_textview);
				vid_timer.setText("ʣ��ʱ��: "+timeToString(left_time));
				
				if(left_time <= 0) {
					// ��ʾ�û�
					Toast.makeText(VideoRecorder.this, "����¼��ʱ������", Toast.LENGTH_SHORT).show();
		        	StopRecording();
				}
				else {
					handler.postDelayed(this, 1000);
				}
			 }
		}
	};
	
	 @Override  
	 protected void onActivityResult(int requestCode, int resultCode,  Intent intent) 
	 {
	  if(requestCode==VIDEO_SHOW_REQUEST && resultCode == VideoPreviewer.VIDEO_IS_OK)  
	  {  
		  InsertFileToMediaStore insert_file = new InsertFileToMediaStore(VideoRecorder.this,this.save_vid_file,"video/mp4");

		  Uri uri = insert_file.insert();
		  if (this.save_vid_file != null){
			  this.save_vid_file.delete();
			  this.save_vid_file = null;
		  }
		  UploadMessage.set_upload_message(uri);
		  finish();
	  }
	  if (requestCode==VIDEO_SHOW_REQUEST && resultCode == VideoPreviewer.VIDEO_IS_NOT_OK)
	  {
		  Toast.makeText(VideoRecorder.this, "û�õ����", Toast.LENGTH_SHORT).show();
		  //do something
	  }
	 }
	
    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cam_vid_layout);
		// ����ȫ��
		/*
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		
		WindowManager wm = (WindowManager) getSystemService(
				Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		// ��ȡ��Ļ�Ŀ�͸�
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();*/
		
		myOrientationEventListener
		   = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL){

		    @Override
		    public void onOrientationChanged(int orientation) {
		    	
		    	int cameraId = 0;
		    	
		    	android.hardware.Camera.CameraInfo info =
		                new android.hardware.Camera.CameraInfo();
		         android.hardware.Camera.getCameraInfo(cameraId, info);
		         orientation = (orientation + 45) / 90 * 90;
		         int rotation = 0;
		         if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
		             rotation = (info.orientation - orientation + 360) % 360;
		         } else {  // back-facing camera
		             rotation = (info.orientation + orientation) % 360;

		         }
		         if (VideoRecorder.this.camera != null){
//		        	 PhotoCapturer.this.camera.getParameters().setRotation(rotation);
		        	 Camera.Parameters parameters = camera.getParameters();
		        	 parameters.setRotation(rotation);
		        	 VideoRecorder.this.camera.setParameters(parameters);
//				     Toast.makeText(PhotoCapturer.this, "��ת:"+String.valueOf(rotation), Toast.LENGTH_LONG).show();

		         }
		    }};
		    
		      if (myOrientationEventListener.canDetectOrientation()){
		       Toast.makeText(this, "Can DetectOrientation", Toast.LENGTH_LONG).show();
		       myOrientationEventListener.enable();
		      }
		      else{
		       Toast.makeText(this, "Can't DetectOrientation", Toast.LENGTH_LONG).show();
		       finish();
		      }
		// ��ȡ������SurfaceView���
		sView = (SurfaceView) findViewById(R.id.vid_view);
		// ���SurfaceView��SurfaceHolder
		surfaceHolder = sView.getHolder();
		// ΪsurfaceHolder���һ���ص�������
		surfaceHolder.addCallback(new Callback()
		{
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height)
			{
				Log.i("aa", "surface changed");
				surfaceHolder = holder;
			}

			public void surfaceCreated(SurfaceHolder holder)
			{	
				Log.d("aa", "surface created");
				
				// reset time
				left_time = max_video_duration;
				// reset caption info
				TextView vid_timer = (TextView)findViewById(R.id.vid_info_textview);
				vid_timer.setText("Ԥ����");
				
				// ������ͷ
				initCamera();
			}
			
			public void surfaceDestroyed(SurfaceHolder holder)
			{
				Log.e("aa","surface destroyed.");

				// ���camera��Ϊnull, �ͷ�����ͷ
				if (camera != null)
				{
					if (isPreview)
					{
						camera.stopPreview();
						isPreview = false;
					}
					camera.release();
					Log.e("aa", "camera released in surfacedestroyed");
					camera = null;
//					Toast.makeText(VideoRecorder.this, "�õ������2", Toast.LENGTH_SHORT).show();

				}
			}
		});
		// ���ø�SurfaceView�Լ���ά������ 
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceHolder.setFixedSize(this.VIDEO_WIDTH, this.VIDEO_HEIGHT);
		// set button event
		final Button recBtn = (Button) findViewById(R.id.vid_rec_btn);
		recBtn.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
				
				if(isRecording) {
					StopRecording();
				}
				else
				{
					StartRecording();
				}
				
			}
		});
		
		Button exitBtn = (Button) findViewById(R.id.vid_exit_btn);
		exitBtn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
					ReleaseRecorder();
					if(isPreview)
					{
						camera.stopPreview();
						isPreview = false;
					}
					if(camera != null) {
						camera.release();
						camera = null;
					}
					UploadMessage.set_upload_message(null);

					VideoRecorder.this.finish();
				}
		});
		
	}
    
    /*
     * init camera:
     * open camera and start preview
     */
	private void initCamera()
	{
		if(!isPreview && camera == null)
		{
			camera = Camera.open();
			Log.e("aa","opened camera");
		}
		
		if (camera != null && !isPreview)
		{
			try
			{	
				Camera.Parameters parameters = camera.getParameters();
				camera.setDisplayOrientation(90);
				parameters.setRotation(90);
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				//parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				camera.setParameters(parameters);
				//ͨ��SurfaceView��ʾȡ������
				camera.setPreviewDisplay(surfaceHolder);
				// ��ʼԤ��
				camera.startPreview();
				camera.autoFocus(null);

				Log.i("aa", "StartPreview");
			}
			catch (Exception e)
			{
				Log.e("aa", "exception in init camera");
				Toast.makeText(VideoRecorder.this, "error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
				e.printStackTrace();
				return;
			}
			
			isPreview = true;
		}
	}
    
    /*
     * start recording
     */
    private void StartRecording() {
    	// prepare recorder
    	if( prepareRecorder() ) {
			try {
				
				TextView vid_timer = (TextView)findViewById(R.id.vid_info_textview);
				vid_timer.setText("ʣ��ʱ��: "+timeToString(left_time));
				Toast.makeText(
						VideoRecorder.this, "��ʼ¼����Ƶ", Toast.LENGTH_SHORT).show();
				vRecorder.start();
				
			} catch (Exception e) {
				Log.e("aa", "fail to record: "+e.getMessage());
				Toast.makeText(
						VideoRecorder.this, 
						e.getMessage(), Toast.LENGTH_LONG).show();
			}
			
			// change button caption
			Button recBtn = (Button) findViewById(R.id.vid_rec_btn);
			recBtn.setText("ֹͣ����");
			// change sign
			isRecording = true;
		}
		else
		{
			ReleaseRecorder();
		}
    }
    
    /*
     * stop recording
     */
    private void StopRecording() {
    	if(isRecording) {
    		// stop recording and save recorded video
			if( vRecorder != null) {
				// stop
				vRecorder.stop();
				Toast.makeText(
						VideoRecorder.this, "����¼����Ƶ", Toast.LENGTH_SHORT).show();
				// release
				ReleaseRecorder();
				// reset button caption
				Button recBtn = (Button) findViewById(R.id.vid_rec_btn);
				recBtn.setText("��ʼ����");
				// reset recording time
				left_time = max_video_duration;
				// reset sign
				isRecording = false;
				// save
				SaveRecordedVideo();
			}
    	}
    }

    
	/*
	 * create recorder instance
	 * set recorder params
	 */
	private boolean prepareRecorder()
	{	
		// prepare media recorder
		try {
			vRecorder = new MediaRecorder(); 
			// unlock camera
			camera.unlock();
			vRecorder.setCamera(camera);
			// set sources
			vRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); 
			vRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); 
			vRecorder.setProfile( CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
		    Log.d("file format",String.valueOf(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW).fileFormat));
		    Log.d("video Codec",String.valueOf(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW).videoCodec));
		    Log.d("video BitRT",String.valueOf(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW).videoBitRate));
		    Log.d("video BitRT",String.valueOf(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW).videoFrameRate));

		    

			/*
//			vRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);*/
			vRecorder.setVideoSize(this.VIDEO_WIDTH, this.VIDEO_HEIGHT);
			
			// set frame rate
			vRecorder.setVideoFrameRate(20);
			vRecorder.setVideoEncodingBitRate(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW).videoBitRate *VIDEO_MUILTI);

			
			
			// set preview output
			vRecorder.setPreviewDisplay(sView.getHolder().getSurface());
			
			// set output file path
			save_vid_file = Tools.getOutputMediaFile(Tools.MEDIA_TYPE_VIDEO);
			vRecorder.setOutputFile( save_vid_file.getAbsolutePath() );
			// prepare recorder
			vRecorder.prepare();		
			
			// start timer
			handler.postDelayed(task, 1000);
			
		} catch (IOException e) {
			Toast.makeText(VideoRecorder.this, "Aerror: "+e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.d("aa", "prepare error: " + e.getMessage());
			ReleaseRecorder();
			return false;
		}
		catch (IllegalStateException e) {
			Toast.makeText(VideoRecorder.this, "error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.d("aa", "prepare error: " + e.getMessage());
			ReleaseRecorder();
			return false;
		}
		
		return true;
	};
	
	
	private void ReleaseRecorder() {
		if(vRecorder!=null) {
			vRecorder.reset();
			vRecorder.release();
			vRecorder = null;
			if(camera!=null)
				camera.lock();
		}
	}
	
	/*
	 * preview saved video file
	 */
	private void SaveRecordedVideo() {
		
		// preview use videoview for saved file
		Intent i = new Intent(VideoRecorder.this, VideoPreviewer.class);
		i.putExtra("video_file", save_vid_file.getAbsolutePath());
		//startActivity(i);
		startActivityForResult(i, this.VIDEO_SHOW_REQUEST);

		Toast.makeText(VideoRecorder.this, "Ԥ����Ƶ", Toast.LENGTH_SHORT).show();

		camera.stopPreview();
		camera.startPreview();
		isPreview = true;
	}

	/*
	 * convert millisecond to standard time format
	 */
	private String timeToString(int left_msec) { 
		int left_sec = left_msec / 1000;	// convert millisecond to second
		if (left_sec >= 60) {  
			int min = left_sec / 60; 
			String m = min > 9 ? min + "" : "0" + min;    
			int sec = left_sec % 60;     
			String s = sec > 9 ? sec + "" : "0" + sec;       
			return m + ":" + s;      
			}
		else
		{
			return "00:" + (left_sec > 9 ? left_sec + "" : "0" + left_sec);    
		}
	} 
	 @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {   
	    		UploadMessage.set_upload_message(null);
	    	return super.onKeyDown(keyCode, event);
	}

}


