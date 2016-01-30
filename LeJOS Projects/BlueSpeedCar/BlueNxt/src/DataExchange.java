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

public class DataExchange {

	/* 
	 * Thread Status Handling
	 */	
	private boolean _isBTCdone = false;
	private boolean _isNXTdone = false;
	private boolean _isUIdone  = false;
	private boolean _isBtConn  = false;
	
	public boolean isBTCdone() {
		return _isBTCdone;
	}

	public void setBTCdone(boolean value) {
		_isBTCdone = value;
	}

	public boolean isNXTdone() {
		return _isNXTdone;
	}

	public void setNXTdone(boolean value) {
		synchronized(this)
		{ _isNXTdone = value; }
	}

	public boolean isUIdone() {
		return _isUIdone;
	}

	public void setUIdone(boolean value) {
		_isUIdone = value;
	}

	public boolean isBtConn() {
		return _isBtConn;
	}

	public void setBtConn(boolean value) {
		_isBtConn = value;
	}

	
	/*
	 *  BT protocol handling
	 */	
	// Opcodes read/write
	private int nxtDyntag, nxtCmd, nxtPos, nxtSpeed, nxtOpcode;	

	// Dyntag
	public static final int MLTP = 256;
	public static final int MIN_DYN = 0;
	public static final int MAX_DYN = 255;

	// Special Commands
	public static final int MIN_CMD 	= MAX_DYN+1;
	public static final int MAX_CMD 	= MIN_DYN*MLTP-1;
	public static final int NULL_CMD 	= 0;
	public static final int STOP_CMD 	= 1;
	public static final int CAL_CMD 	= 3;
	public static final int START_CMD 	= 5;
	public static final int SPDLEV_CMD = 7;
	public static final int FRWD_CMD	= 11;
	public static final int BKWD_CMD 	= 13;
	public static final int LEFT_CMD 	= 15;
	public static final int RIGHT_CMD 	= 17;
	public static final int RELEASE_CMD = 21;
	public static final int OTHER3_CMD = 23;
	public static final int OTHER4_CMD = 25;	
	public static final int EXIT_CMD   = 99;

	// Turning Angle (Position)
	public static final int MIN_POS_CMD = MAX_CMD + 1;
	public static final int MAX_POS_CMD = (MIN_POS_CMD)*(MLTP)-1;
	public static final int MAX_TURN_ANGLE = 60;	// deg
	public static final int ANGULAR_RES = 2;		// deg
	public static final double ANGULAR_SHAFT = 1.5;	

	// Front Wheel Steering
	private int curSteerPos;

	public int getCurSteerPos() {
		return curSteerPos;
	}

	public void setCurSteerPos(int value) {
		curSteerPos = value;
	}	

	// Back Wheel Speed
	public static final int MIN_SPD_CMD = MIN_POS_CMD + 1;
	public static final int MAX_SPD_CMD = (MIN_SPD_CMD)*(MLTP)-1;
	public static final int MIN_FRM_RADIUS = 50; 
	public static final int MAX_FRM_RADIUS = 90+150; 
		
	// Used on NXT: Multipliers of incoming speed command
	public static final int MIN_SPEED_LEVEL = 2;
	public static final int MAX_SPEED_LEVEL = 7;
	private int speedLevel = MIN_SPEED_LEVEL+2;
	
	public int getSpeedLevel() {
		return speedLevel;
	}

	public void incSpeedLevel() {
		speedLevel += 1;
		if (speedLevel > MAX_SPEED_LEVEL)
			speedLevel = MIN_SPEED_LEVEL;
	}
	
	// Object Creator 
	public DataExchange() {
		nxtDyntag = MIN_DYN;		// DynTag
		nxtCmd    = NULL_CMD;		// Special Command
		nxtPos	  = MIN_POS_CMD;	// Turn Angle command
		nxtSpeed  = MIN_SPD_CMD;	// Wheel Speed command
		encodeNxtOpcode();			// Final Opcode
	};

	public int getNxtDyn() {
		return nxtDyntag;
	}

	private void setNxtDyn(int value) {
		nxtDyntag = value;
	}

	public void incNxtDyn() {		
		nxtDyntag += 1;

		if (nxtDyntag > MAX_DYN)
			nxtDyntag = MIN_DYN;

		encodeNxtOpcode();	// Opcode
	}

	public int getNxtOpcode() {
		return nxtOpcode;
	}

	public void encodeNxtOpcode() {
		nxtOpcode = nxtDyntag + nxtCmd*MLTP + nxtPos*MLTP*MLTP + nxtSpeed*MLTP*MLTP*MLTP;
	}	

	public int getNxtCmd() {
		return nxtCmd;
	}

	public void setNxtCmd(int value) {
		nxtCmd = value;
		encodeNxtOpcode();	// Opcode
	}

	public int getNxtPos() {
		return nxtPos;
	}

	public void setNxtPos(int value) {
		nxtPos = value;
		encodeNxtOpcode();	// Opcode
	}

	public int getNxtSpeed() {
		return nxtSpeed;
	}

	public void setNxtSpeed(int value) {
		nxtSpeed = value;
		encodeNxtOpcode();	// Opcode
	}

	// PC-Transmitter: Transmit Special Commands
	public void encodeSpecialCommands(int cmdCode /*cmd Code*/) 
	{			
		synchronized(this)
		{
			// increment Dyntag
			incNxtDyn();
			
			// set Code to transmit
			setNxtCmd( cmdCode );
		}
	}
	
	
	// NXT-Receiver: Decode Turn Angle and Speed
	public void decodeTurnAngleAndSpeed(int value) 
	{	
		synchronized(this)
		{
			//nxtDyntag + nxtCmd*MLTP + nxtPos*MLTP*MLTP + nxtSpeed*MLTP*MLTP*MLTP;
			int opcode=value, e_speed=-1, e_pos=-1, e_cmd=-1, e_dyn=-1, divider=MLTP*MLTP*MLTP;

			// extrapolate NXT wheel speed
			e_speed = (opcode - (opcode % divider))/divider;
			opcode -= e_speed*divider; 
			divider = MLTP*MLTP;

			// extrapolate NXT turn-angle (with offset)
			e_pos = (opcode - (opcode % divider))/divider;
			opcode -= e_pos*divider;
			e_pos -= MAX_TURN_ANGLE;	// remove offset
			divider = MLTP;

			// extrapolate NXT command
			e_cmd = (opcode - (opcode % divider))/divider;
			opcode -= e_cmd*divider;

			// extrapolate Dyntag
			e_dyn = opcode;

	    	setNxtSpeed(e_speed); 
			setNxtPos(e_pos);
			setNxtCmd(e_cmd);
			setNxtDyn(e_dyn);
		}
	}	
	
	
	/**
	 * Obstacle Detection
	 * */
	public static final int MIN_DIST_2_OBJ =  20;
	
	private int nxtDetectedDistance;
	
	void setDistance( int value ) { nxtDetectedDistance = value; };
	
	int getDistance() { return nxtDetectedDistance; };
};