/* 
Copyright (c) 2013-2016, KapBotics
All rights reserved.

This file is part of BlueKap program. TKAPPFrame is the main class that starts the main
UI and the communication thread with the NXT via Bluetooth.

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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;
import java.awt.event.WindowAdapter;
import javax.swing.SwingConstants;
import java.awt.Toolkit;


public class TKAPPFrame extends JFrame implements MouseMotionListener,MouseListener {

	/**
	 * The BlueKap GUI is created. Etc ...
	 */
	private static final long serialVersionUID = 1L;

	/* PDU class */
	public static DataExchange DE;		

	/* PC BT-Send thread */
	public static BtConnSend PC_BTS;	

	/* BlueKap GUI */
	private static TKAPPFrame frame;	
	private static JLabel lbBkg, lbKAPP;
	private static int xFrm, yFrm, wFrm, hFrm;
	private static int xBrd, yBrd;
	private static int xRef, yRef, xLab, yLab, wLab, hLab;

	/* BlueKap control variables  */
	private static boolean cmdStartStop, cmdCalibrate, isNormalDrive;

	/* BlueKap main application */
	public static void main(String[] args) throws InterruptedException {

		DE = new DataExchange();
		PC_BTS = new BtConnSend(DE);			
		frame  = null; 
		lbBkg  = lbKAPP = null;	
		isNormalDrive = true;
		cmdStartStop = cmdCalibrate = false;

		// start BT-Send thread
		PC_BTS.setPriority(Thread.MAX_PRIORITY);
		PC_BTS.start();

		while (!DE.isBtConn())
		{
			// wait for BT connection to establish
			// with the NXT brick (running BT_PC_rx)
		}

		// launch GUI:
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame = new TKAPPFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				};					
			}
		});
	}

	/**
	 * KAPPFrame class constructor
	 */
	public TKAPPFrame() 
	{
		// instantiate the Frame
		setResizable(false);
		setIconImage(Toolkit.getDefaultToolkit().getImage("images/kAPPgo.png"));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("Blue Kapp");		
		setBackground(Color.WHITE);
		xFrm=500; yFrm=100; wFrm=250+122; hFrm=250+145;
		xBrd = yBrd = 5;		
		setBounds(xFrm, yFrm, wFrm, hFrm);

		// create content panel
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new LineBorder(Color.GRAY, 5));
		contentPanel.setLayout(null);
		contentPanel.setBackground(Color.GRAY);

		// create background Label: load the default compass map
		lbBkg = new JLabel("");
		lbBkg.setToolTipText("");
		lbBkg.setBackground(Color.WHITE);
		lbBkg.setIcon(new ImageIcon("images/kAPPframe.png"));
		int x_o=8, wx_o=205+150;
		int y_o=7,  hy_o=205+150;
		lbBkg.setBounds(x_o,y_o,wx_o,hy_o);

		// create foreground Label
		xRef = 93+63; 
		yRef = 100+75;
		lbKAPP = new JLabel("");
		lbKAPP.setHorizontalAlignment(SwingConstants.CENTER);
		lbKAPP.setBackground(Color.WHITE);
		lbKAPP.setIcon(new ImageIcon("images/kAPPicon.png"));	
		lbKAPP.setToolTipText("");
		wLab = lbKAPP.getIcon().getIconWidth();
		hLab = lbKAPP.getIcon().getIconHeight();
		xLab = xRef; 
		yLab = yRef; 
		lbKAPP.setBounds(xRef,yRef,58,25); // manual setting
		//System.out.println("xLab: " + xLab + "; yLab: " + yLab + "; wLab: " + wLab + "; hLab: " + hLab);

		// add MouseMotionListener to label
		lbKAPP.addMouseMotionListener(this);
		lbKAPP.addMouseListener(this);		

		// add layout and label to content panel
		contentPanel.setOpaque(true);
		contentPanel.add(lbKAPP);
		contentPanel.add(lbBkg);
		getContentPane().add(contentPanel);

		// add window event "onClose"
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				synchronized(this) 
				{
					try 
					{
						// On Window Close: send the EXIT command to the NXT brick
						DE.incNxtDyn();
						DE.setNxtCmd(DataExchange.EXIT_CMD);
					} 
					catch (Exception err) 
					{
						System.err.println("GUI::EX-2:: IO error trying to buffer your CMD!");
						err.printStackTrace();
					}		
				}		
			}
		});		
	};


	/**
	 * KAPPFrame GUI event handling
	 */
	@Override
	public void mouseDragged(MouseEvent e) 
	{
		// calculate new icon position
		xLab += (int)(e.getX() - wLab/2);
		yLab += (int)(e.getY() - hLab/2);		

		// icon should not fall off from or partially hidden within the frame
		if (xLab < 4*xBrd)
			xLab = 4*xBrd;
		if (xLab+wLab > wFrm-6*xBrd)
			xLab = wFrm-6*xBrd-wLab;
		if (yLab < 2*yBrd)
			yLab = 2*yBrd;
		if (yLab+hLab > hFrm-8*yBrd)
			yLab = hFrm-8*yBrd-hLab;

		// calculate OpCode command based on new icon position
		int xCmd = xLab-xRef;
		int yCmd = yRef-yLab;

		// encode new OpCode to be sent over BT to the NXT application
		try {
			// if not in "STOP" or if stopped due to "CALIBRATION", send the OpCode
			if (!cmdStartStop || cmdCalibrate)
				DE.encodeTurnAngleAndSpeed(xCmd, yCmd, cmdCalibrate, isNormalDrive);
		} 
		catch (Exception err) 
		{
			System.err.println("GUI::EX-1:: IO error trying to buffer your CMD!");
			err.printStackTrace();
		}

		// redraw kAPP icon in new position
		lbKAPP.setBounds(xLab,yLab,wLab,hLab);
		lbKAPP.repaint(xLab, yLab, wLab, hLab);	
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent arg0) 
	{
		// LEFT BUTTON
		// toggle between normal and reverse drive mode 
		if (arg0.getButton() == MouseEvent.BUTTON1 && arg0.getClickCount()==2)
		{
			// reverse drive direction selected
			isNormalDrive = !isNormalDrive;

			// apply reverse compass map if reverse drive is selected 
			if (isNormalDrive)
				lbBkg.setIcon(new ImageIcon("images/kAPPframe.png"));	
			else
				lbBkg.setIcon(new ImageIcon("images/kAPPframeRev.png"));

			// recalculate KAPP icon position within the toggled compass map 
			xLab += (int)(arg0.getX() - wLab/2); 	/* current */
			yLab += (int)(arg0.getY() - hLab/2);		
			int xCmd = -(xLab-xRef);
			int yCmd = -(yRef-yLab);			
			xLab = xCmd + xRef;						/* new */
			yLab = yRef - yCmd;				

			// redraw kAPP icon
			lbKAPP.setBounds(xLab,yLab,wLab,hLab);
			lbKAPP.repaint(xLab, yLab, wLab, hLab);			

			// drag the mouse pointer on top of the KAPP icon
			xFrm = frame.getX();
			yFrm = frame.getY();
			int newMouseX = xLab+xBrd+(int)(wLab/2)+xFrm;
			int newMouseY = yLab+4*yBrd+(int)(hLab/2)+yFrm;
			try {
				Robot robot = new Robot();
				robot.mouseMove(newMouseX, newMouseY);	            
			} catch (AWTException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		// WHEEL BUTTON
		// increase/decrease wheel speed (in calibration only)
		if (arg0.getButton() == MouseEvent.BUTTON2 && cmdCalibrate) 
		{
			DE.encodeSpecialCommands(DataExchange.SPDLEV_CMD);
			DE.incSpeedLevel();
			lbKAPP.setToolTipText("SPEED LEVEL: " + DE.getSpeedLevel());
		}
		
		// RIGHT BUTTON
		// send START/STOP special commands 
		if (arg0.getButton() == MouseEvent.BUTTON3) 
		{
			if (arg0.getClickCount()>=2)
			{
				// toggle STOP/START command:
				// true => STOP_CMD; false => START_CMD
				cmdStartStop = !cmdStartStop;	

				if (cmdStartStop)
				{
					DE.encodeSpecialCommands(DataExchange.STOP_CMD);
					lbKAPP.setToolTipText("*STOP*");
					frame.setIconImage(Toolkit.getDefaultToolkit().getImage("images/kAPPstop.png"));

				}
				else
				{
					DE.encodeSpecialCommands(DataExchange.START_CMD);
					cmdCalibrate = false;
					lbKAPP.setToolTipText("*GO*");
					frame.setIconImage(Toolkit.getDefaultToolkit().getImage("images/kAPPgo.png"));
				}
			}
			
			// send CALIBRATE special command
			if (cmdStartStop && (arg0.getClickCount()==1))
			{
				// if the Stop command is issued, perform command Motors calibration
				cmdCalibrate = true;
				DE.encodeSpecialCommands(DataExchange.CAL_CMD);
				lbKAPP.setToolTipText("*CALIBRATION*");
				frame.setIconImage(Toolkit.getDefaultToolkit().getImage("images/kAPPcal.png"));
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void mouseReleased(MouseEvent arg0) 
	{
		if (!cmdStartStop)
			synchronized(this)
			{
				DE.incNxtDyn();
				DE.setNxtCmd(DataExchange.RELEASE_CMD); 
			}
	}
}
