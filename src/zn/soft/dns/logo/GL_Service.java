/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package zn.soft.dns.logo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import zn.soft.dns.logo.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * Wallpaper entry point.
 */
public final class GL_Service extends WallpaperService {
    private final Handler mHandler = new Handler();
	private Engine mEngine;

	@Override
	public final Engine onCreateEngine() {
		mEngine = new WallpaperEngine();
		return mEngine;
	}

	/**
	 * Private wallpaper engine implementation.
	 */
	private final class WallpaperEngine extends Engine implements SensorEventListener {
	    SensorManager mSensorManager;
	    Sensor mAccelerometerSensor;
        Sensor mMagneticFieldSensor;
        
        
        private SensorManager msensorManager; //�������� �������� �������
        
        private float[] rotationMatrix;     //������� ��������
        private float[] accelData;           //������ � �������������
        private float[] magnetData;       //������ ������������� �������
        private float[] OrientationData;
        private boolean mTimer;

		private final Runnable mDrawAll = new Runnable() {
            public void run() {
            	while(true)
            	onRedrawAll();
            }
        };
		// GLSurfaceView implementation.
		private ZNSurfaceView mGLSurfaceView;

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {

			// Uncomment for debugging.
			// android.os.Debug.waitForDebugger();

			super.onCreate(surfaceHolder);
			
			mTimer = true;
			msensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	        
		    rotationMatrix = new float[16];
		    accelData = new float[3];
		    magnetData = new float[3];
		    OrientationData = new float[3];
//			mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
//	        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//	        if(sensors.size() > 0)
//	        {
//	            for (Sensor sensor : sensors) {
//	                switch(sensor.getType())
//	                {
//	                case Sensor.TYPE_ACCELEROMETER:
//	                    if(mAccelerometerSensor == null) mAccelerometerSensor = sensor;
//	                    break;
//	                default:
//	                    break;
//	                }
//	        }}
			
			

			mGLSurfaceView = new ZNSurfaceView();
			setTouchEventsEnabled(true);
			
		}

		
		   private void loadNewSensorData(SensorEvent event) {
		        final int type = event.sensor.getType(); //���������� ��� �������
		        if (type == Sensor.TYPE_ACCELEROMETER) { //���� ������������
		                accelData = event.values.clone();
		        }
		       
		        if (type == Sensor.TYPE_MAGNETIC_FIELD) { //���� ������������ ������
		                magnetData = event.values.clone();
		        }
		    }		
		
		@Override
		public final void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mDrawAll);
			mGLSurfaceView.onDestroy();
			mGLSurfaceView = null;
			msensorManager.unregisterListener(this);
		}

		@Override
		public final void onTouchEvent(MotionEvent me) {
//			mGLSurfaceView.onTouch(me);
			onRedrawAll();
		}

		public final void onRedrawAll() {
			mGLSurfaceView.onRedraw();
			//mHandler.removeCallbacks(mDrawAll);
            if (mTimer) {
               // mHandler.postDelayed(mDrawAll, 1000 / 25);
            }
//

		}
		
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        	msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI );
        	msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI );
        
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            msensorManager.unregisterListener(this);
            mHandler.removeCallbacks(mDrawAll);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {

        }
		
		@Override
		public final void onVisibilityChanged(boolean visible) {
			super.onVisibilityChanged(visible);
			if (visible) {
		    	msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI );
		    	msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI );
				mGLSurfaceView.onResume();
				mGLSurfaceView.requestRender();
				onRedrawAll();
			} else {
				mGLSurfaceView.onPause();
				msensorManager.unregisterListener(this);
				mHandler.removeCallbacks(mDrawAll);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSensorChanged(SensorEvent arg0) {
			// TODO Auto-generated method stub
			loadNewSensorData(arg0); // �������� ������ � �������
	        SensorManager.getRotationMatrix(rotationMatrix, null, accelData, magnetData); //�������� ������� ��������
	        SensorManager.getOrientation(rotationMatrix, OrientationData); //�������� ������ ���������� ���������� � ������������
	        try{
	        mGLSurfaceView.onRePosition(OrientationData[0],OrientationData[2],OrientationData[1]);
	        }catch(Exception e){
	        	
	        	
	        }

		}

	}

	/**
	 * Lazy as I am, I din't bother using GLWallpaperService (found on GitHub)
	 * project for wrapping OpenGL functionality into my wallpaper service.
	 * Instead am using GLSurfaceView and trick it into hooking into Engine
	 * provided SurfaceHolder instead of SurfaceView provided one GLSurfaceView
	 * extends. For saving some bytes Renderer is implemented here too.
	 */
	private final class ZNSurfaceView extends GLSurfaceView implements
			GLSurfaceView.Renderer, Runnable {
		private float x,y,z;
		// Screen aspect ratio.
		private final float mAspectRatio[] = new float[2];
		// Screen vertices filling whole view.
		private ByteBuffer mScreenVertices;
		// Boolean value for indicating if shader compiler is supported.
		private final boolean mShaderCompilerSupported[] = new boolean[1];
		// Our one and only shader program id.
		private int mShaderProgram = -1;
		private SurfaceHolder mSurfaceHolder;
		// Boolean for indicating whether touch events are being followed.
		private boolean mTouchFollow = false;
		// Two {x, y } tuples for indicating touch start and current position.
		private final float mTouchPositions[] = new float[4];
		// Last touch event time.
		private long mTouchTime;
		// Screen width and height.
		private int mWidth, mHeight;

		/**
		 * Default constructor.
		 */
		private ZNSurfaceView() {
			super(GL_Service.this);

			setEGLContextClientVersion(2);
			setRenderer(this);
			setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			//onPause();

			final byte SCREEN_COORDS[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
			mScreenVertices = ByteBuffer.allocateDirect(2 * 4);
			mScreenVertices.put(SCREEN_COORDS).position(0);
		}

		@Override
		public final SurfaceHolder getHolder() {
			if (mSurfaceHolder == null) {
				mSurfaceHolder = mEngine.getSurfaceHolder();
			}
			return mSurfaceHolder;
		}

		/**
		 * Private shader program loader method.
		 * 
		 * @param vs
		 *            Vertex shader source.
		 * @param fs
		 *            Fragment shader source.
		 * @return Shader program id.
		 */
		private final int loadProgram(String vs, String fs) throws Exception {
			int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
			int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
			int program = GLES20.glCreateProgram();
			if (program != 0) {
				GLES20.glAttachShader(program, vertexShader);
				GLES20.glAttachShader(program, fragmentShader);
				GLES20.glLinkProgram(program);
				int[] linkStatus = new int[1];
				GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS,
						linkStatus, 0);
				if (linkStatus[0] != GLES20.GL_TRUE) {
					String error = GLES20.glGetProgramInfoLog(program);
					GLES20.glDeleteProgram(program);
					throw new Exception(error);
				}
			}
			return program;
		}

		/**
		 * Private method for loading raw String resources.
		 * 
		 * @param resourceId
		 *            Raw resource id.
		 * @return Resource as a String.
		 * @throws Exception
		 */
		private final String loadRawResource(int resourceId) throws Exception {
			InputStream is = getResources().openRawResource(resourceId);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) != -1) {
				baos.write(buf, 0, len);
			}
			return baos.toString();
		}

		/**
		 * Private shader loader method.
		 * 
		 * @param shaderType
		 *            Vertex or fragment shader.
		 * @param source
		 *            Shader source code.
		 * @return Loaded shader id.
		 */
		private final int loadShader(int shaderType, String source)
				throws Exception {
			int shader = GLES20.glCreateShader(shaderType);
			if (shader != 0) {
				GLES20.glShaderSource(shader, source);
				GLES20.glCompileShader(shader);
				int[] compiled = new int[1];
				GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS,
						compiled, 0);
				if (compiled[0] == 0) {
					String error = GLES20.glGetShaderInfoLog(shader);
					GLES20.glDeleteShader(shader);
					throw new Exception(error);
				}
			}
			return shader;
		}

		/**
		 * Should be called once underlying Engine is destroyed. Calling
		 * onDetachedFromWindow() will stop rendering thread which is lost
		 * otherwise.
		 */
		public final void onDestroy() {
			super.onDetachedFromWindow();
		}

		@Override
		public final void onDrawFrame(GL10 unused) {

			// Clear screen buffer.
			//GLES20.glClearColor(0, 0, 0, 1);
			//GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

			// If shader compiler is not supported return immediately.
			//if (mShaderCompilerSupported[0] == false)				return;
			

			// Disable unneeded rendering flags.
			GLES20.glDisable(GLES20.GL_CULL_FACE);
			GLES20.glDisable(GLES20.GL_BLEND);
			GLES20.glDisable(GLES20.GL_DEPTH_TEST);

			GLES20.glUseProgram(mShaderProgram);
			int uAspectRatio = GLES20.glGetUniformLocation(mShaderProgram,
					"uAspectRatio");
			int uTouchPos = GLES20.glGetUniformLocation(mShaderProgram,
					"uTouchPos");
			int uGyro = GLES20.glGetUniformLocation(mShaderProgram,
					"uGyro");
			int vTime = GLES20.glGetUniformLocation(mShaderProgram,
					"Time");
			int aPosition = GLES20.glGetAttribLocation(mShaderProgram,
					"aPosition");
	        int aTime = GLES20.glGetAttribLocation(mShaderProgram, "aTime");
			//int uSampler1Location = GLES20.glGetUniformLocation(mShaderProgram, "tex0");

	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
	        long time = SystemClock.uptimeMillis() % 259000L;
	        float ff[] = new float[1];
	        ff[0] = (float)time/50.0f;
	        x = (float)time/200.0f;
	        GLES20.glVertexAttrib1f(aTime, (float)time );
			GLES20.glUniform1fv(vTime , 1, ff, 0);
			GLES20.glUniform2fv(uAspectRatio, 1, mAspectRatio, 0);
			GLES20.glUniform2fv(uTouchPos, 2, mTouchPositions, 0);
			GLES20.glUniform3f(uGyro, x, y, z);//glUniform2fv(uTouchPos, 2, mTouchPositions, 0);

			GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false,
					2, mScreenVertices);
			GLES20.glEnableVertexAttribArray(aPosition);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}

		@Override
		public final void onSurfaceChanged(GL10 unused, int width, int height) {
			mWidth = width;
			mHeight = height;

			GLES20.glViewport(0, 0, mWidth, mHeight);
			mAspectRatio[0] = (1.1f * mWidth) / Math.min(mWidth, mHeight);
			mAspectRatio[1] = (1.1f * mHeight) / Math.min(mWidth, mHeight);
		}
	    
		 private int mTextureID;
		    private int maTimeHandle;

		    
		private void AssignBitmap(int id,int Filter) {

				InputStream is = getResources().openRawResource(id);
				Bitmap bitmap;
				try {
					bitmap = BitmapFactory.decodeStream(is);
				} finally {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
				bitmap.recycle();
				GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MIN_FILTER, Filter);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MAG_FILTER, Filter);
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
						GLES20.GL_CLAMP_TO_EDGE);
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
						GLES20.GL_CLAMP_TO_EDGE);
			}		    
		    
	    ///
	    // Create a 2D texture image 
	    //
	    private int createTexture2D( )
	    {
	        // Texture object handle
	        int[] textureId = new int[1];
	        int    width = 256,
	               height = 256;
//	        ByteBuffer pixels;
	        mTextureID = textureId[0];
	        //pixels = genCheckImage( width, height, 64 );
	        
	        // Generate a texture object
	        GLES20.glGenTextures ( 1, textureId, 0 );

	        // Bind the texture object
	        GLES20.glBindTexture ( GLES20.GL_TEXTURE_2D, mTextureID );
			AssignBitmap(R.raw.dns,GLES20.GL_LINEAR);
	        return textureId[0];
	    }
	    ///
	    //  Generate an RGB8 checkerboard image
	    //
	    private ByteBuffer genCheckImage( int width, int height, int checkSize )
	    {
	        int x,xMagnetic,yMagnetic,
	            y,i;
	        int Balls = 17;
	        double Ambient = 900;
	        int[] xCenters = new int[100];
	        int[] yCenters = new int[100];
	        int[] zCenters = new int[100];
	        float[] pCenters = new float[100];
	        for(i=0;i<Balls;i++){
	        	xCenters[i]= (int) Math.round(Math.random()*width);
	        	yCenters[i]= (int) Math.round(Math.random()*height);
	        	zCenters[i]= (int) Math.round(Math.random()*255);
	        	pCenters[i]= (float) (1.0+Math.random()*3.0);
	        	
	        }
	        byte[] pixels = new byte[width * height * 3];
	        double q = 0,z;
	        for ( y = 0; y < height; y++ )
	            for ( x = 0; x < width; x++ )
	            {
	            	
	                byte rColor = 0;
	                byte bColor = 0;
	                byte gColor = 0;
	                z = 0.0;
	                for(i=0;i<Balls;i++){
	                	xMagnetic = xCenters[i] - x;
	                	yMagnetic = yCenters[i] - y;
	                	//q = Math.pow(Math.pow(xMagnetic, pCenters[i])+Math.pow(yMagnetic, pCenters[i]),1/pCenters[i]);
	     //           	q = Math.sqrt((double)(xMagnetic *xMagnetic+yMagnetic*yMagnetic));
	                	q = Math.sqrt((double)(xMagnetic *xMagnetic+yMagnetic*yMagnetic+zCenters[i]));
	                	if(q != 0.0 )z =z + 1.0/q; else z = z+1;
	                }
	                int r =(int) Math.round(Ambient * z);
	                if(r>250)r=250;
	                rColor = (byte) (r);
	                bColor =  rColor;
	                gColor =  rColor;

	                
//	                if ( ( x / checkSize ) % 2 == 0 )
//	                {
//	                    bColor = (byte)(rColor * ( 1 - ( ( y / checkSize ) % 2 ) ));
//	                }
//	                else
//	                {
//	                    bColor = (byte)(127 * ( ( y / checkSize ) % 2 ));
//	                }

	                
	                
	                
	                pixels[(y * height + x) * 3] = rColor;
	                pixels[(y * height + x) * 3 + 1] = gColor;//(byte) (bColor * rColor * y * x);
	                pixels[(y * height + x) * 3 + 2] = bColor; 
	            } 

//	        for ( y = 0; y < height; y++ )
//	            for ( x = 0; x < width; x++ )
//	            { 
	        
	        ByteBuffer result = ByteBuffer.allocateDirect(width*height*3);
	        result.put(pixels).position(0);
	        return result;
	    }
	    

	    
	    private void setRenderTexture(int frameBuf, int texture,int x,int y)
	    {
	       // if (frameBuf == 0)
	          //  GLES20.glViewport(0, 0, scrWidth, scrHeight);
	        //else
	        GLES20.glViewport(0, 0, x, y);
	        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuf);
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
	    } 
		@Override
		public final void onSurfaceCreated(GL10 unused, EGLConfig config) {
			// Check if shader compiler is supported.
			GLES20.glGetBooleanv(GLES20.GL_SHADER_COMPILER,
					mShaderCompilerSupported, 0);

			// If not, show user an error message and return immediately.
			if (mShaderCompilerSupported[0] == false) {
				new Handler(Looper.getMainLooper()).post(this);
				return;
			}

			// Shader compiler supported, try to load shader.
			try {
				String vs = loadRawResource(R.raw.gl_vs);
				String fs = loadRawResource(R.raw.gl_fs);
				mShaderProgram = loadProgram(vs, fs);
			} catch (Exception ex) {
				mShaderCompilerSupported[0] = false;
				ex.printStackTrace();
			}
			createTexture2D( );
		}

		
		public final void onRedraw() {
			requestRender();
		}		

		public final void onRePosition(float u,float v,float h) {
			x = u;
			y = v;
			z = h;
			
			requestRender();
		}		

		
		
		public final void onTouch(MotionEvent me) {
			mTouchTime = SystemClock.uptimeMillis();
			switch (me.getAction()) {
			// On touch down set following flag and initialize touch position
			// start and current values.
			case MotionEvent.ACTION_DOWN:
				mTouchFollow = true;
				mTouchPositions[0] = (((2f * me.getX()) / mWidth) - 1f)
						* mAspectRatio[0];
				mTouchPositions[1] = (1f - ((2f * me.getY()) / mHeight))
						* mAspectRatio[1];
				// Flow through..

				// On touch move update current position only.
			case MotionEvent.ACTION_MOVE:
				mTouchPositions[2] = (((2f * me.getX()) / mWidth) - 1f)
						* mAspectRatio[0];
				mTouchPositions[3] = (1f - ((2f * me.getY()) / mHeight))
						* mAspectRatio[1];
//				requestRender();
				break;
			// On touch up mark touch follow flag as false.
			case MotionEvent.ACTION_UP:
				mTouchFollow = false;
//				requestRender();
				break;
			}
			
			requestRender();
		}

		@Override
		public final void run() {
			while(true){
			requestRender();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
			//Toast.makeText(GL_Service.this, R.string.error_shader_compiler,Toast.LENGTH_LONG).show();
		}

	}

}