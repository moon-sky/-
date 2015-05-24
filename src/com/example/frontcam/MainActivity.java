package com.example.frontcam;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.faceplusplus.api.FaceDetecter;
import com.faceplusplus.api.FaceDetecter.Face;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

public class MainActivity extends ActionBarActivity {
	ImageView iv;
	// to call our own custom cam
	private final static int CAMERA_PIC_REQUEST1 = 0;
	Context con;
	private HandlerThread detectThread;
	private Handler detectHandler;
	private FaceDetecter detecter;
	private HttpRequests request;
	TextView tv_info;
	final int msg_female = 1;
	final int msg_male = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initFaceplus();
		iv = (ImageView) findViewById(R.id.imageView1);
		tv_info = (TextView) findViewById(R.id.tv_info);

		con = this;
	}

	public void initFaceplus() {
		detectThread = new HandlerThread("detect");
		detectThread.start();
		detectHandler = new Handler(detectThread.getLooper());
		detecter = new FaceDetecter();
		detecter.init(this, "7d9041a0728203dee01d6fd2ec23ea25");

		// FIXME 替换成申请的key
		request = new HttpRequests("7d9041a0728203dee01d6fd2ec23ea25",
				"RkivqrEb1DlHDEeyRYI5XN5HdiBzCLrF ");
	}

	public static Bitmap getScaledBitmap(String fileName, int dstWidth) {
		BitmapFactory.Options localOptions = new BitmapFactory.Options();
		localOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(fileName, localOptions);
		int originWidth = localOptions.outWidth;
		int originHeight = localOptions.outHeight;

		localOptions.inSampleSize = originWidth > originHeight ? originWidth
				/ dstWidth : originHeight / dstWidth;
		localOptions.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(fileName, localOptions);
	}

	/**
	 * 检测人脸
	 * 
	 * @param curBitmap
	 */
	private void detectFace(final Bitmap curBitmap) {
		detectHandler.post(new Runnable() {

			@Override
			public void run() {

				Face[] faceinfo = detecter.findFaces(curBitmap);// 进行人脸检测
				if (faceinfo == null) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(MainActivity.this, "未发现人脸信息",
									Toast.LENGTH_LONG).show();
						}
					});
					return;
				}

				// 在线api交互
				try {
					JSONObject obj = request.offlineDetect(
							detecter.getImageByteArray(),
							detecter.getResultJsonString(),
							new PostParameters());
					JSONArray face=obj.getJSONArray("face");
							for(int i=0;i<face.length();i++){
								if(i>0)
									return;
								JSONObject attributes = face.getJSONObject(i).getJSONObject("attribute");
								JSONObject gender = attributes.getJSONObject("gender");
								if (gender.getString("value").equals("male")) {
									handler.obtainMessage(msg_male).sendToTarget();
								} else {
									handler.obtainMessage(msg_female).sendToTarget();
								}	
							}
	
				} catch (FaceppParseException | JSONException e) {
					e.getStackTrace();
				}

			}
		});
	}

	Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case msg_male:
				tv_info.setText("大帅哥一个");
				break;
			case msg_female:
				tv_info.setText("大美女");
				break;

			default:
				break;
			}
		};
	};

	@Override
	protected void onDestroy() {
		detecter.release(this);// 释放引擎
		super.onDestroy();
	}

	public void onClick(View view) {
		if (getFrontCameraId() == -1) {
			Toast.makeText(getApplicationContext(),
					"Front Camera Not Detected", Toast.LENGTH_SHORT).show();
		} else {
			Intent cameraIntent = new Intent();
			cameraIntent.setClass(this, CameraActivity.class);
			startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST1);

			// startActivity(new
			// Intent(MainActivity.this,CameraActivity.class));
		}
	}

	@SuppressWarnings("deprecation")
	int getFrontCameraId() {
		CameraInfo ci = new CameraInfo();
		for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
			Camera.getCameraInfo(i, ci);
			if (ci.facing == CameraInfo.CAMERA_FACING_FRONT)
				return i;
		}
		return -1; // No front-facing camera found
	}

	Bitmap bitmapFrontCam;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAMERA_PIC_REQUEST1) {
			if (resultCode == RESULT_OK) {
				try {
					bitmapFrontCam = (Bitmap) data
							.getParcelableExtra("BitmapImage");

				} catch (Exception e) {
				}
				detectFace(bitmapFrontCam);
				iv.setImageBitmap(bitmapFrontCam);
			}

		} else if (resultCode == RESULT_CANCELED) {
			Toast.makeText(this, "Picture was not taken", Toast.LENGTH_SHORT)
					.show();
		}
	}

}
