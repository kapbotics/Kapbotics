/* 
Copyright (c) 2013-2016, KapBotics
All rights reserved.

This file is part of BlueKap program. BtConnSend is the thread providing
Bluetooth communication with the NXT.

    BlueKap is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    BlueKap is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details, visit here: 
    <http://www.gnu.org/licenses/>.
*/

import lejos.nxt.LCD;
import lejos.pc.comm.NXTConnector;
import lejos.pc.comm.NXTCommLogListener;
import java.io.DataOutputStream;
import java.io.IOException;


public class BtConnSend extends Thread 
{
	private DataExchange DEObj;
	private NXTConnector conn;
	private DataOutputStream dos; 
	private int oldDyn, txOC;
	
	public BtConnSend( DataExchange DE ) 
	{
		DEObj = DE;
		DEObj.setBtConn(false);
		oldDyn = 0;
		txOC  = 0;
	};


	public void run() 
	{	
	    synchronized(this) { DEObj.setBTCdone(false); }
	 
		// instantiate BT connection
		conn = new NXTConnector();
		
		conn.addLogListener(
				new NXTCommLogListener()
				{
					public void logEvent(String message) {
						System.out.println("BTS:: Log.listener: "+message);
					}

					public void logEvent(Throwable throwable) {
						System.out.println("BTS:: Log.listener - stack trace: ");
						throwable.printStackTrace();
					}	
				} 
				);

		// Connect to any NXT over BT
		boolean connected = conn.connectTo("btspp://NXT"); 

		if (!connected) {
			System.err.println("BTS:: Failed to connect to any NXT");
			System.exit(1);
		} else {
			synchronized(this) { DEObj.setBtConn(true);}
		}

		// Open BT Data Output Stream
		try {
			dos = new DataOutputStream(conn.getOutputStream());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("BTS::EX-1:: IO error trying to buffer your CMD!");
		}

		// start infinite loop
		oldDyn = DEObj.getNxtDyn();
		
		while (true)
		{			
			// let the thread run: send opcode via BT
			if ((DEObj.getNxtCmd() != DataExchange.NULL_CMD) && (DEObj.getNxtDyn() != oldDyn))
			{	
				try {
					txOC = DEObj.getNxtOpcode();					
					dos.writeInt(txOC);					
					if (conn != null) {	dos.flush(); }				
					oldDyn = DEObj.getNxtDyn();				
					
					System.out.println("BT-Send:: Dyntag: " + DEObj.getNxtDyn() + " with OPCODE: " + DEObj.getNxtOpcode());
				
				} 
				catch (IOException ioe) {
					System.err.println("BTS::EX-2:: IO Exception writing bytes:");
					ioe.printStackTrace();			
					break;
				}
			}
			
			if (DEObj.getNxtCmd() == DataExchange.EXIT_CMD)
			{	break;	}
			
			try {
				// wait for data to drain
				BtConnSend.sleep(100);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LCD.drawString("EX-3:",0,5);
			} 
			
		};

		// Exit
		try {
			dos.close();
			conn.close();
			synchronized(this) { 
				DEObj.setBtConn(false); 
				DEObj.setBTCdone(true); 
				}
			System.exit(0);
		} 
		
		catch (IOException ioe) {
			System.err.println("BTS:: IOException closing connection:");
			ioe.printStackTrace();			
			System.exit(1);
		};		
	}
}