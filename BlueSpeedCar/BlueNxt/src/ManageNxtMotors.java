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

public class ManageNxtMotors extends Thread {

	private DataExchange DEObj;

	private static RegulatedMotor leftMotor;  
	private static RegulatedMotor rightMotor;  

	private static int oldDyn;
	private static int newDyn;
	private static int newCmd;	
	private static int curSteerPos, newSpeed, dSpeedL, dSpeedR; 
	private static boolean isNxtStop, isLRtoggle;
	private static int detectedDistance;
	
	public ManageNxtMotors( DataExchange DE ) {
		DEObj = DE;

		oldDyn = 0;	
		newCmd = DataExchange.NULL_CMD;
		
		curSteerPos = 0; newSpeed = dSpeedL = dSpeedR = 0;
		isNxtStop = isLRtoggle = false;

		leftMotor = Motor.C;
		leftMotor.setSpeed(0);
		leftMotor.stop();
		rightMotor = Motor.A;
		rightMotor.setSpeed(0);
		rightMotor.stop();		
		detectedDistance = 255;
	};

	
	public void run() {

		//double speedFct= 1.0;	/* used for wheel speed limitation */
		double speedFct= 1.5;	/* modified for kap.jeep.2 */	
		double diffFct = 1.0;	/* used for differential wheel speed regulation */
		
		DEObj.setNXTdone(false);

		// infinite task
		while (true)
		{
			// retrieve decoded opcode info:
			synchronized(this) 
			{ 
				newDyn = DEObj.getNxtDyn();
				newCmd = DEObj.getNxtCmd();
			}
			
			// adjust wheel speed based on sonar-detected distance
			synchronized(this)
			{
				detectedDistance = DEObj.getDistance();
								
				// speedFct = 1.0; // default for kap.jeep.1
				speedFct = 1.5; // modified for kap.jeep.2
				
				if (detectedDistance <= 2*DataExchange.MIN_DIST_2_OBJ)
					speedFct = 0.2*(1+ Math.floor(detectedDistance/DataExchange.MIN_DIST_2_OBJ));
			}
			
			// calculate adjusted-differential left and right motor speed
			synchronized(this) 
			{ 
				curSteerPos = DEObj.getCurSteerPos();
				
				newSpeed = (int)( DEObj.getNxtSpeed() * 	/* user command: normalized speed */
						          DEObj.getSpeedLevel() *	/* speed factor applied */ 
						          speedFct  /* reduction factor from ultra.sound sensor */
						          );
				
				dSpeedL = dSpeedR = newSpeed;			
				diffFct = (double)Math.abs(curSteerPos) / (double)DataExchange.MAX_TURN_ANGLE;
				
				if (curSteerPos >= 0)
				{
					dSpeedL = newSpeed;
					dSpeedR = (int)(newSpeed/2 * (2-diffFct));
				} 
				else
				{
					dSpeedR = newSpeed;
					dSpeedL = (int) (newSpeed/2 * (2-diffFct));
				}
			}
			
			// apply left and right motor adjusted-differential speed
			synchronized(this) 
			{ 
				// motor speed is not set synchronously. To compensate that, change of motor speed 
				// toggles among left-first and right-first.
				isLRtoggle = !isLRtoggle;

				if (isLRtoggle)
				{	
					leftMotor.setSpeed(dSpeedL);
					rightMotor.setSpeed(dSpeedR);
				} else {
					rightMotor.setSpeed(dSpeedR);
					leftMotor.setSpeed(dSpeedL);
				}
			};

			// executing user command
			if ((newCmd != DataExchange.EXIT_CMD) && (newCmd != DataExchange.NULL_CMD))
			{
				if (oldDyn != newDyn)
				{
					oldDyn = newDyn;

					switch (newCmd)
					{
					case DataExchange.STOP_CMD:
						synchronized(this) 
						{ 
							isNxtStop = true;
							leftMotor.flt();			
							rightMotor.flt();						
							leftMotor.setSpeed(0);
							rightMotor.setSpeed(0);
						}
						LCD.clear(6);
						LCD.drawString("CMD:: STOP",0,6);
						break;
					case DataExchange.CAL_CMD:
						synchronized(this) 
						{ 
							leftMotor.flt();
							rightMotor.flt();
							leftMotor.setSpeed(0);
							rightMotor.setSpeed(0);
						}
						break;
					case DataExchange.START_CMD:
						synchronized(this) 
						{ 
							isNxtStop = false;
							LCD.clear(6);
							LCD.drawString("CMD:: START",0,6);
						}
						break;
					case DataExchange.SPDLEV_CMD:
						synchronized(this) 
						{ 	
							DEObj.incSpeedLevel();
							LCD.clear(6);
							LCD.drawString("CMD:: SPDLEV[",0,6);
							LCD.drawInt(DEObj.getSpeedLevel(),1,13,6);
							LCD.drawString("]",14,6);
						}
						break;
						
					case DataExchange.FRWD_CMD:
						// Mounting of the wheel motors requires the KapBot Jeep "forward" 
						// to be matched with the BlueNxt motors' backward, and viceversa.
						if (!isNxtStop)
						{
							synchronized(this) 
							{ 							
								if (isLRtoggle)
								{	
									leftMotor.backward();			
									rightMotor.backward();	
								} else {
									rightMotor.backward();	
									leftMotor.backward();			
								}
							}
							LCD.clear(6);
							LCD.drawString("CMD:: FORWARD",0,6);
						}
						break;
					case DataExchange.BKWD_CMD:
						if (!isNxtStop)
						{
							synchronized(this) 
							{ 
								if (isLRtoggle)
								{	
									leftMotor.forward();			
									rightMotor.forward();	
								} else {
									rightMotor.forward();	
									leftMotor.forward();			
								}
							}
							LCD.clear(6);
							LCD.drawString("CMD:: BACKWARD",0,6);
						}
						break;
					default:
						LCD.clear(6);
						LCD.drawString("CMD:: INVALID",0,6);
						break;
					}; // switch case
				} // if (old != new)

				// refresh data on LCD
				LCD.clear(7);
				LCD.drawString("USD:", 0, 7);
				LCD.drawInt(detectedDistance, 6, 5, 7);
				LCD.refresh();

				try {
					// thread going to sleep
					ManageNxtMotors.sleep(50);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
					LCD.drawString("EX-5:",0,5);
					LCD.refresh();	
				} 
			} // if (new != EXIT_CMD)
			else
			{
				if (newCmd == DataExchange.EXIT_CMD)
				{
					leftMotor.stop();	/* stop motors */
					rightMotor.stop();
					break;				/* exit the while loop */
				}
			}
		} // while loop

		DEObj.setNXTdone(true);
	}
};
