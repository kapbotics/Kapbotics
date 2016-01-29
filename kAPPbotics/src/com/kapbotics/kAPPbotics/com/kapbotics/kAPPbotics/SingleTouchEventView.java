package com.kapbotics.kAPPbotics;


import com.kapbotics.kAPPbotics.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class SingleTouchEventView extends RelativeLayout {
	private Bitmap myIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
	private float dimIconX = myIcon.getWidth();
	private float dimIconY = myIcon.getHeight();
	private float maxX = 180; // getResources().getDimension(R.dimen.activity_background_width)-dimIconX/2;
	private float maxY = 180; // getResources().getDimension(R.dimen.activity_background_height)-dimIconY/2;
	private float ctrX = (getResources().getDimension(R.dimen.activity_background_width)-dimIconX)/2;
	private float ctrY = (getResources().getDimension(R.dimen.activity_background_height)-dimIconY)/2;	  
	private float curX, curY, cmdX, cmdY;
	private String toOutText ="";

	//private Bitmap myIcon = BitmapFactory.decodeResource(getResources(), R.drawable.kap_logo); 

	public SingleTouchEventView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		curX = ctrX;
		curY = ctrY;
	}


	@Override
	protected void onDraw(Canvas canvas) {
		// redraw the icon (@KapDrawView1)
		canvas.drawBitmap(myIcon, curX, curY, null);	    
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {

		float eventX = event.getX()-dimIconX/2;
		float eventY = event.getY()-dimIconY/2;
		boolean isOpcodeReady = false;

		// limit and save current X,Y
		eventX = (eventX<0) ? (float)0.0 : eventX;
		eventY = (eventY<0) ? (float)0.0 : eventY;

		curX= (Math.abs(eventX)>maxX ? maxX*Math.signum(eventX) : eventX);
		curY= (Math.abs(eventY)>maxY ? maxY*Math.signum(eventY) : eventY);    

		switch (event.getAction()) 
		{	    
		case MotionEvent.ACTION_DOWN:
			// pressure gesture has just begun, nothing to do
			break;		

		case MotionEvent.ACTION_MOVE:
			// pressure gesture is ongoing, do:
			// encode new OpCode to be sent over BT to the NXT application
			cmdX = curX-ctrX;
			cmdY = ctrY-curY;	
			isOpcodeReady = true;
			break;

		case MotionEvent.ACTION_UP:
			if (!MainActivity.isCmdCalibrate())
			{
				// pressure gesture has terminated, retrieve straight path
				cmdY = (float)(MainActivity.getDE().getPolRadius((int)cmdX, (int)cmdY)*Math.signum(cmdY));
				cmdY = (Math.abs(cmdY)>maxY ? maxY*Math.signum(cmdY) : cmdY);
				curY = ctrY-cmdY;
				curX = ctrX;
				cmdX = curX-ctrX;
				isOpcodeReady = true;
			}
			break;

		default:
			return false;
		}

		if (isOpcodeReady)
		{
			try {
				MainActivity.getDE().encodeTurnAngleAndSpeed((int)(cmdX),(int)(cmdY), MainActivity.isCmdCalibrate(), MainActivity.isNormalDrive());

				toOutText = "STE:: OpCode: " + MainActivity.getDE().getNxtOpcode() + "; Dyn: " + MainActivity.getDE().getNxtDyn() + "; Cmd: " 
						+ MainActivity.getDE().getNxtCmd() + "; Pos: " + MainActivity.getDE().getNxtPos() + "; Spd: " + MainActivity.getDE().getNxtSpeed();
				Log.d(MainActivity.TAG, toOutText);
			} catch (Exception err) {
				toOutText = "STE:: EX-1: opcode not encoded!";
				Log.d(MainActivity.TAG, toOutText);
			}
		};

		// Schedules a repaint.
		invalidate();
		return true;
	}

	public void mirrorCommand() 
	{
		// invert command
		cmdX = -cmdX;
		cmdY = -cmdY;				

		// send opcode
		MainActivity.getDE().encodeTurnAngleAndSpeed((int)(cmdX),(int)(cmdY), MainActivity.isCmdCalibrate(), MainActivity.isNormalDrive());

		toOutText = "STE:: OpCode: " + MainActivity.getDE().getNxtOpcode() + "; Dyn: " + MainActivity.getDE().getNxtDyn() + "; Cmd: " 
				+ MainActivity.getDE().getNxtCmd() + "; Pos: " + MainActivity.getDE().getNxtPos() + "; Spd: " + MainActivity.getDE().getNxtSpeed();
		Log.d(MainActivity.TAG, toOutText);

		// reposition icon
		curX = ctrX+cmdX; 
		curY = ctrY-cmdY;
		
		invalidate();
	}
} 
