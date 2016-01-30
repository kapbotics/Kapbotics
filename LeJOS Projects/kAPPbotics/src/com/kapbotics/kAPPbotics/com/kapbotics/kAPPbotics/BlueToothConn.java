package com.kapbotics.kAPPbotics;

import java.io.IOException;
//import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
//import java.io.DataInputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BlueToothConn extends Thread {

	/* Adapter, Socket, Stream */
	private boolean isBtEnabled = false;
	private BluetoothAdapter localAdapter;
	private BluetoothSocket mmSocket;
	private DataOutputStream mmOutStream;	
	//private DataInputStream mmInStream;

	/* Unique UUID for this application */
	private static final UUID KAP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  

	/* Opcode */
	private DataExchange DEObj;
	//private static final int MAX_STALE_COUNT = 50;
	
	/* Log TAG */
	private static final String TAG = "kAPPbotics";

	/* BlueToothConn constructor */
	public BlueToothConn(DataExchange DE, BluetoothDevice device)
	{		
		// Init 
		DEObj = DE;
		BluetoothSocket tmpSocket = null;
		mmOutStream = null;
		//mmInStream = null;

		// Enabling BT adapter (if not on)
		enableBT();

		// Create Bluetooth Socket
		if (isBtEnabled == true)
		{
			Log.d(TAG,"BTC:: Creating a Bluetooth Socket");

			try {
				tmpSocket = device.createRfcommSocketToServiceRecord(KAP_UUID);
				Log.d(TAG,"BTC:: Bluetooth Socket created!");
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(TAG,"BTC:: Bluetooth Socket failed!");
			}
			mmSocket = tmpSocket;

			// Try Socket connection in separate thread
			Thread connectionThread  = new Thread( new Runnable() {

				@Override
				public void run() {

					Log.d(TAG,"BTC::connectionThread:: run() entered");

					if (!DEObj.isBtConn())
					{							
						// Always cancel discovery because it will slow down a connection
						localAdapter.cancelDiscovery();

						// Make a connection to the BluetoothSocket
						try {
							// This is a blocking call and will only return on a
							// successful connection or an exception
							mmSocket.connect();			

							synchronized(this){	DEObj.setBtConn(true); };
							DEObj.incNxtDyn();		

							Log.d(TAG,"BTC::connectionThread:: mmSocket connected");
							Log.d(TAG,"BTC::connectionThread:: isBtConn = TRUE");
							Log.d(TAG,"BTC::connectionThread:: nxt Dyntag = " + DEObj.getNxtDyn());

						} catch (IOException e) {
							//connection to device failed so close the socket
							try {
								mmSocket.close();
								Log.d(TAG,"BTC::connectionThread:: cannot connect mmSocket");
								Log.d(TAG,"BTC::connectionThread:: mmSocket closed");
							} catch (IOException e2) {
								e2.printStackTrace();
								Log.d(TAG,"BTC::connectionThread:: exception thrown on mmSocket.close()!");
							}

							synchronized(this){	DEObj.setBtConn(false); };
							Log.d(TAG,"BTC::connectionThread:: isBtConn = FALSE");
						}				
					}					
				}
			});		
			connectionThread.start();

			// Initialize data streams
			OutputStream tmpOut = null;
			//InputStream tmpIn = null;

			// Get the BluetoothSocket output streams
			try {
				tmpOut = mmSocket.getOutputStream();			
				Log.d(TAG,"BTC:: mmSocket output stream retrieved");
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(TAG,"BTC:: exception thrown on mmSocket.getOutputStream();");
			}
			mmOutStream = new DataOutputStream(tmpOut);

			/*
			// Get the BluetoothSocket input streams
			try {
				tmpIn = mmSocket.getInputStream();			
				Log.d(TAG,"BTC:: mmSocket input stream retrieved");
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(TAG,"BTC:: exception thrown on mmSocket.getInputStream();");
			}
			mmInStream = new DataInputStream(tmpIn);
			*/
		}
	}

	/* Enable BT adapter */
	private void enableBT() {
		localAdapter = BluetoothAdapter.getDefaultAdapter();

		if(localAdapter.isEnabled() == false)
		{
			localAdapter.enable();
		}

		while(!(localAdapter.isEnabled()))
		{ /* wait */ };

		isBtEnabled = localAdapter.isEnabled();
		Log.d(TAG,"BTC:: BT enabled: " + isBtEnabled);		
	}

	/* BlueToothConn run() */
	public void run() {

		int oldDyn = DEObj.getNxtDyn();
		int dataIn = 0, dataInOld = 0, staleCnt = 0;
		int dataOut = 0;

		while (DEObj.getNxtCmd() != DataExchange.EXIT_OK) 
		{
			if (isBtEnabled && DEObj.isBtConn())
			{
				/*
				//read data in via socket stream
				synchronized(this)
				{
					try {
						dataIn = mmInStream.readInt();
						if (dataIn == dataInOld)
						{	
							staleCnt++;								
							if (staleCnt > MAX_STALE_COUNT)
							{
								synchronized(this){	DEObj.setBtConn(false); }
								Log.d(TAG,"BTC:: NXT is dead! Shutting down ...");	
								break;
							}
						}
						else 
						{
							staleCnt = 0;
							Log.d(TAG,"BTC:: NXT conn is alive [" + dataIn + "]");	
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}							
				}
				*/

				// if Dyntag has not changed, no UI change occurred
				if (DEObj.getNxtDyn() != oldDyn)
				{
					try {
						//send data out via socket stream
						synchronized(this)
						{
							dataOut = DEObj.getNxtOpcode();
							mmOutStream.writeInt(dataOut);
							mmOutStream.flush();		
							oldDyn = DEObj.getNxtDyn();

							// Allows the App to quit safely
							if (DEObj.getNxtCmd()==DataExchange.EXIT_CMD)
							{
								DEObj.setNxtCmd(DataExchange.EXIT_OK);
							}

							Log.d(TAG,"BTC:: opcode sent of size " + mmOutStream.size() + " bytes");	
						}

						
						goSleep(125);

					} catch (IOException e) {
						//an exception here marks connection loss
						Log.d(TAG,"BTC:: exception thrown on mmOutStream.write(buffer); ");
						//send message to UI Activity
						break;
					}
				}
			}
			else
			{	
				Log.d(TAG,"BTC:: waiting for nxt connection ...;"); 
				goSleep(1000);
			}
		}

		Log.d(TAG,"BTC:: exiting Thread: now;"); 
		System.exit(0);
	}

	public void write(int buffer) {
		try {
			//write the data to socket stream
			mmOutStream.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void cancel() {
		try {
			mmSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void goSleep(long time)
	{
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}