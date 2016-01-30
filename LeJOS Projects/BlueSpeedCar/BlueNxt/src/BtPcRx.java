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

public class BtPcRx {

	public static DataExchange DE;
	public static ManageBtConn BTC;
	public static ManageNxtMotors NXT_M;
	public static ManageNxtSteering NXT_S;
	public static ManageSensors NXT_NS;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DE = new DataExchange();
		BTC = new ManageBtConn(DE);
		NXT_M = new ManageNxtMotors(DE);
		NXT_S = new ManageNxtSteering(DE);
		NXT_NS = new ManageSensors( DE );
		
		BTC.setPriority(Thread.MAX_PRIORITY);
		NXT_S.setPriority(Thread.NORM_PRIORITY);
		NXT_M.setPriority(Thread.NORM_PRIORITY);
		NXT_NS.setPriority(Thread.NORM_PRIORITY);
		
		BTC.start();
		NXT_S.start();
		NXT_M.start();
		NXT_NS.start();

		LCD.clearDisplay();
		while (!Button.ESCAPE.isDown())
		{
			// let the thread run
			if (DE.getNxtCmd()== DataExchange.EXIT_CMD)
			{
				if (!(DE.isBTCdone() && DE.isNXTdone()))
				{
					// wait ...
				}
				else
					break; // exit the thread.
			}
		};

		LCD.clearDisplay();
		System.exit(0);
	}
};


