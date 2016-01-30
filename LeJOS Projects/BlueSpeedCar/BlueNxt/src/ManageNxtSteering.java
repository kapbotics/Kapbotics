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

import lejos.nxt.*;
import lejos.robotics.RegulatedMotor;

public class ManageNxtSteering extends Thread {

	private DataExchange DEObj;

	private static RegulatedMotor centreMotor; 

	private static int oldDyn;
	private static int newDyn;
	private static int newCmd;
	private static int curPos, newPos;
	private static boolean isNxtStop, isNxtCalib;
	private static final int MIN_STEER_SPD = 800;

	
	public ManageNxtSteering( DataExchange DE ) {
		// initialize variables
		DEObj = DE;

		centreMotor = Motor.B;
		centreMotor.resetTachoCount();
		curPos = centreMotor.getTachoCount();
		centreMotor.setSpeed(MIN_STEER_SPD);
		//centreMotor.setAcceleration(10000);
		centreMotor.flt();

		oldDyn = 0;	
		newCmd = DataExchange.NULL_CMD;
		isNxtStop = isNxtCalib = false;
	};

	
	public int getDeltaPos(int newP, int curP)
	{
		//int result = (int)((newP-curP)/DataExchange.ANGULAR_SHAFT);
		int result = (int)((newP-curP)); /* modified for kap.jeep.2 */	
		return result;
	}
	
	
	public void run() {

		// infinite task
		while (true)
		{
			// retrieve decoded opcode info
			synchronized(this) 
			{ 
				newDyn = DEObj.getNxtDyn();
				newCmd = DEObj.getNxtCmd(); 
				newPos = DEObj.getNxtPos();
				curPos = centreMotor.getTachoCount();
				DEObj.setCurSteerPos(curPos);
			};			

			// steer to newPos, if conditions apply
			if ((newCmd != DataExchange.EXIT_CMD) && (newCmd != DataExchange.NULL_CMD))
			{
				if (oldDyn != newDyn)
				{
					oldDyn = newDyn;

					switch (newCmd)
					{
					case DataExchange.STOP_CMD:
						isNxtStop = true;
						centreMotor.stop();
						break;
					case DataExchange.CAL_CMD:
						centreMotor.rotate(getDeltaPos(newPos,curPos));
						LCD.clear(6);
						LCD.drawString("CMD:: CALIBRATE",0,6);
						break;
					case DataExchange.START_CMD:
						isNxtStop = false;
						if (isNxtCalib)
						{
							// calibration is done, the current TachoCount 
							// position becomes the reference, i.e. "0 deg".
							isNxtCalib = false;
							centreMotor.resetTachoCount();
							curPos = centreMotor.getTachoCount();
						}
						break;
					case DataExchange.RELEASE_CMD:
						// when user releases the mouse drag, the vehicle attempts to recover a straight drive.
						// It simulates what happens naturally in a real car when the driver gets his hands off 
						// the steering upon exiting from a curve.
						centreMotor.rotateTo(0);					
						break;
					case DataExchange.FRWD_CMD:
					case DataExchange.BKWD_CMD:
						if (!isNxtStop)
						{	centreMotor.rotate(getDeltaPos(newPos,curPos));	}
						break;
					default:
						break;
					}; // switch case
				} // if (old != new)
				else
				{	centreMotor.flt(); }

				try {
					// thread going to sleep
					ManageNxtSteering.sleep(50);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
					LCD.drawString("EX-6:",0,5);
					LCD.refresh();	
				} 

			} // if (new != EXIT_CMD)
			else
			{
				if (newCmd == DataExchange.EXIT_CMD)
				{
					centreMotor.stop();	/* stop motor */
					break;				/* exit while loop */
				}
			}
		} // while loop

		DEObj.setNXTdone(true);
	}
};
