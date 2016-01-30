package com.kapbotics.kAPPbotics;

import com.kapbotics.kAPPbotics.R;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity {
	
	/* PDU class */
	private static DataExchange DE;		

	/* BT thread */
	private BluetoothDevice nxtDevice = null;
	private final String nxtMACaddress = "00:16:53:17:CB:D1";
	
	// this is the only OUI registered by LEGO, see http://standards.ieee.org/regauth/oui/index.shtml
    // public static final String OUI_LEGO = "00:16:53";
	private static BlueToothConn BTC;	
	
	/* Activity UI */
	private static Button btnCalibrate, btnReverseDrive, btnSpeedLevel, btnQuit;
	private static boolean cmdCalibrate, isNormalDrive;
	
	/* Log */
	public static final String TAG = "kAPPbotics";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
				
		setDE(new DataExchange());
		setNormalDrive(true); 
		setCmdCalibrate(false);		

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final SingleTouchEventView myLayout = (SingleTouchEventView) findViewById(R.id.KapDrawView1); 
		
		while (btnQuit == null)
		{
			btnCalibrate = (Button) findViewById(R.id.btnCal);
			btnReverseDrive = (Button) findViewById(R.id.btnRevD);
			btnSpeedLevel = (Button) findViewById(R.id.btnSpeed);
			btnSpeedLevel.setText("SPD:" + getDE().getSpeedLevel());
			btnQuit = (Button) findViewById(R.id.btnQuit);
		}

		nxtDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(nxtMACaddress);
		if (nxtDevice != null)
		{
			Log.d(TAG,"MA:: nxtDevice found!");
			BTC = new BlueToothConn(getDE(), nxtDevice);	
			BTC.setPriority(Thread.MAX_PRIORITY);
			BTC.start();
			Log.d(TAG,"MA:: BTC started");
		}

		btnQuit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click       		
            	if (getDE().isBtConn())
            	{
	            	// tell the NXT to close
	            	synchronized(this) 
	        		{
	        			try 
	        			{
	        				// On Window Close: send the EXIT_CMD to the NXT brick
	        				getDE().incNxtDyn();
	        				getDE().setNxtCmd(DataExchange.EXIT_CMD);
	        				Log.d(TAG,"MA:: stopping BlueNxt");
	        			} 
	        			catch (Exception err) 
	        			{
	        				Log.d(TAG,"MA:: exception thrown: can't stop BlueNxt");
	        				err.printStackTrace();
	        			}		
	        		}		
	            	
	            	// Kill the app
	            	// wait for the EXIT_OK
	            	while ( getDE().getNxtCmd() != DataExchange.EXIT_OK)
	            	{};
            	}
            	
            	// kill the App
            	try {  
            		android.os.Process.killProcess(android.os.Process.myPid()); 
            	} catch (Exception err) {
            		Log.d(TAG,"MA:: exception thrown: can't destroy kAPPbotics!");
            		err.printStackTrace();
            	}
           }
        });

		btnCalibrate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click       		
        		if (getDE().isBtConn())
            	{            		
            		// toggle calibration flag
            		synchronized(this) 
	        		{ 
            			setCmdCalibrate(!isCmdCalibrate());
            			if (isCmdCalibrate())
            			{	
             				btnCalibrate.setTextColor(getResources().getColor(R.color.amber));
            				getDE().encodeSpecialCommands(DataExchange.CAL_CMD);
             				btnCalibrate.setText(R.string.calibrate);
            			}
            			else
            			{
             				btnCalibrate.setTextColor(getResources().getColor(R.color.grey));
            				getDE().encodeSpecialCommands(DataExchange.START_CMD);
            				btnCalibrate.setText(R.string.start_go);
             			}
             		}		
            	}
            }
		});

		btnReverseDrive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	// Perform action on click       		            	
            	if (getDE().isBtConn())
            	{
	            	// toggle forward/reverse flag
        			if (myLayout != null)
        			{
         				synchronized(this) 
		        		{ 
	            			setNormalDrive(!isNormalDrive()); 
	            			
	            			if (!isNormalDrive())
	            			{
	            				btnReverseDrive.setText(R.string.reverse_drive);
	            				btnReverseDrive.setTextColor(getResources().getColor(R.color.amber));
	            			}
	            			else
	            			{
	            				btnReverseDrive.setText(R.string.normal_drive);
	            				btnReverseDrive.setTextColor(getResources().getColor(R.color.grey));            				
	            			}
	           				
	            			myLayout.mirrorCommand();
		        		}
             		}		
            	}
            }
		});
		
		btnSpeedLevel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click       		
             	if (getDE().isBtConn())
            	{
	            	// toggle calibration flag
            		synchronized(this) 
	        		{ 
               			DE.incSpeedLevel();
            			btnSpeedLevel.setText("SPD:" + getDE().getSpeedLevel());
            			DE.encodeSpecialCommands(DataExchange.SPDLEV_CMD);
              		}		
            	}
            }
		});

	}

	 /* @Override(non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
		
	public static boolean isCmdCalibrate() {
		return cmdCalibrate;
	}

	public static void setCmdCalibrate(boolean cmdCalibrate) {
		MainActivity.cmdCalibrate = cmdCalibrate;
	}

	public static boolean isNormalDrive() {
		return isNormalDrive;
	}

	public static void setNormalDrive(boolean isNormalDrive) {
		MainActivity.isNormalDrive = isNormalDrive;
	}

	public static DataExchange getDE() {
		return DE;
	}

	public static void setDE(DataExchange dE) {
		DE = dE;
	}
}
