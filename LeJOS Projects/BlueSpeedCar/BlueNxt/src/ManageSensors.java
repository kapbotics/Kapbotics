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

public class ManageSensors extends Thread {

	private DataExchange DEObj;
	private int newCmd;
	private UltrasonicSensor nxtSonar;

	public ManageSensors( DataExchange DE ) 
	{
		// initialize variables
		DEObj = DE;
		newCmd = DEObj.getNxtCmd(); 
		nxtSonar = new UltrasonicSensor(SensorPort.S1);
		nxtSonar.continuous();
	};
	
	public void run() 
	{
		int nxtUltraSonicDist = 0;

		// infinite task
		while ((newCmd != DataExchange.EXIT_CMD))
		{
			synchronized(this)
			{
				// retrieve distance from object
				nxtUltraSonicDist = nxtSonar.getDistance();
				DEObj.setDistance(nxtUltraSonicDist);
			};
			
			// Beep to alert!
			if (nxtUltraSonicDist <= 2*DataExchange.MIN_DIST_2_OBJ)
				Sound.beep();
						
			try {
				Thread.sleep(250);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		DEObj.setNXTdone(true);
	};
}
