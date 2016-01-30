//Copyright (c) 2013-2016, KapBotics
//All rights reserved.
//
//This file is part of BlueNxt program.
//
//    BlueNXT is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    BlueNXT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details <http://www.gnu.org/licenses/>.

import java.io.DataInputStream;
// import java.io.InputStream;		/* android only */
import java.io.IOException;

//import java.io.DataOutputStream; 	/* new */
//import java.io.OutputStream;		/* new */

import lejos.nxt.LCD;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

public class ManageBtConn extends Thread {

	private DataExchange DEObj;
	private int rxOC;
	private BTConnection btc;
	// private InputStream is; 		/* if interfacing with Android device */
	private DataInputStream dis;

	//private OutputStream os;		/* TODO: output stream */
	//private DataOutputStream dos;	
	//private int txDyn;
	
	private String connected = "MBTC:: Connected";
	private String waiting   = "MBTC:: Waiting...";
	private String closing   = "MBTC:: Closing...";
	private String disopen   = "MBTC:: DIS open";
	//private String dosopen   = "MBTC:: DOS open";	/* TODO: output stream */

	public ManageBtConn( DataExchange DE ) {
		DEObj = DE;
		btc = null;
		rxOC = 0;
		// txDyn = 0; /* TODO: output stream */
	};

	public void run() {

		LCD.drawString(waiting,0,0);
		LCD.refresh();

		synchronized(this) { DEObj.setBTCdone(false); }
		
		btc = Bluetooth.waitForConnection(0, NXTConnection.LCP);

		if (btc != null)
		{
			LCD.clear();
			LCD.drawString(connected,0,0);
			LCD.refresh();	

			
			// if interfacing with Android device //
			// btc.setIOMode(NXTConnection.RAW);
						
			try {
				dis = btc.openDataInputStream(); // for PC only
				
				// if interfacing with Android device
				//is  = btc.openInputStream();
				//dis = new DataInputStream(is);
				
				LCD.drawString(disopen,0,0);					
				LCD.refresh();	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LCD.drawString("EX-1:",0,5);
				LCD.refresh();	
			};

			/* TODO: output stream
			 * try { //dos = btc.openDataOutputStream(); // for PC only os
			 * = btc.openOutputStream(); dos = new DataOutputStream(os);
			 * 
			 * LCD.drawString(dosopen,0,1); LCD.refresh(); } catch (Exception e)
			 * { // TODO Auto-generated catch block e.printStackTrace();
			 * LCD.drawString("EX-5:",0,5); LCD.refresh(); };
			 */
				
			// infinite task
			while( DEObj.getNxtCmd() != DataExchange.EXIT_CMD )
			{
				/* TODO: output stream
				 * try { synchronized(this) { txDyn++; txDyn = (txDyn >
				 * DataExchange.MAX_DYN) ? 0 : txDyn; dos.writeInt(txDyn); } }
				 * catch (Exception e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); LCD.drawString("EX-6:",0,5); }
				 */

				try { /* receive opcodes */
					synchronized(this)
					{
						// get new data from BT
						rxOC = dis.readInt();
						DEObj.decodeTurnAngleAndSpeed(rxOC);

						// Show Received data
						LCD.drawString("DYN: ", 0, 2);
						LCD.drawInt(DEObj.getNxtDyn(),4,6,2);
						LCD.drawString("CMD: ", 0, 3);
						LCD.drawInt(DEObj.getNxtCmd(),4,6,3);
						LCD.drawString("POS: ", 0, 4);
						LCD.drawInt(DEObj.getNxtPos(),4,6,4);
						LCD.drawString("VEL: ", 0, 5);
						LCD.drawInt(DEObj.getNxtSpeed(),4,6,5);
						LCD.refresh();
					}					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LCD.drawString("EX-2:",0,5);
					LCD.refresh();	
				}

				try {
					// wait for data to drain
					ManageBtConn.sleep(100);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LCD.drawString("EX-3:",0,5);
				} 

			} // end while loop


			LCD.drawString(closing,0,0);					
			try {
				dis.close();
				//dos.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LCD.drawString("EX-4:",0,5);
			} 

			while (!DEObj.isNXTdone())
			{
				// wait the motor thread to finish.
			}
			
			btc.close();
			synchronized(this) { DEObj.setBTCdone(true); }
		}
	};
}
