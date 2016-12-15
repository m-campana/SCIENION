/**
 * Written by Bella Campana (start date: 2016-09-12).
 * 
 * Modified from micromanager HelloWorld and 
 * ArrayScan plugin written by Derin Sevenler (Unlu lab, BU, 2015)
 * 
 * This will start the process of interfacing with previously written code to run 
 * machines and stuff, and will therefore be hard to bug test.  Wish me luck!
 * 
 * To create a new one of these, follow instructions at:
 * https://micro-manager.org/wiki/Writing_plugins_for_Micro-Manager
 */

package org.micromanager.IRISSpotCheckPlugin;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import mmcorej.CMMCore;
import mmcorej.StrVector;
import mmcorej.TaggedImage;

import org.micromanager.api.Autofocus;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.MMException;
import org.micromanager.utils.MMScriptException;

import java.io.BufferedWriter;
// File I/O
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// fourColor 
import ij.*;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import ij.plugin.SubstackMaker;
import ij.process.ImageConverter;
import ij.plugin.FolderOpener;

import java.util.ArrayList;
import java.util.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class IRISSpotCheck implements MMPlugin, ActionListener {
	// Window label/plugin description in Micro-manager, when you select
	// "Plugins"
	public static final String menuName = "IRIS: Spot Check";
	public static final String tooltipDescription = "Image acquisition interface";
	public static final String versionNumber = "0.1";

	// Provides access to the Micro-Manager gui API (for displaying our plugin)
	private ScriptInterface app_;
	// Provides access to the Micro-Manager Core API (for direct hardware
	// control)
	private CMMCore core_;

	// Initializes app_ objects for use within this package, class, and
	// subclasses

	// API: https://docs.oracle.com/javase/7/docs/api/javax/swing/JFrame.html
	protected JFrame frame;
	// API: https://docs.oracle.com/javase/7/docs/api/java/awt/Container.html
	protected Container panel;
	// You get the idea. Google "JButton API" to get more info on these
	protected JButton runB, setB, runN, setN;
	protected JTextField nFilenameField, nXField, nYField, nAreaXField,
			nAreaYField, nChipNumField, nExpTimeField, nNumRepsField,
			nNumFramesField, nFOVDilationField, nXYStageSpeedField;
	protected JCheckBox nFocusField, nBOnlyField;

	protected JPanel updatePanel;

	// required meta functions
	@Override
	public void setApp(ScriptInterface app) {
		app_ = app;
		core_ = app.getMMCore();
	}

	@Override
	public void dispose() {
		// We do nothing here as the only object we create, our dialog, should
		// be dismissed by the user.
	}

	@Override
	public String getInfo() {
		return tooltipDescription;
	}

	@Override
	public String getDescription() {
		return tooltipDescription;
	}

	// In Derin's code, this returns the variable versionNumber
	// For increased generality, this is a recommended change
	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getCopyright() {
		return "Boston University, 2016";
	}

	// This is where the magic happens
	@Override
	public void show() {
		// Create the panel for content layout, using BoxLayout default
		// BoxLayout API:
		// https://docs.oracle.com/javase/7/docs/api/javax/swing/BoxLayout.html

		// This is the text that appears in the top of the plugin window
		frame = new JFrame("IRIS: Spot Check");
		panel = frame.getContentPane();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		// add content

		// Imaging parameters
		// make and add tip label
		JPanel imagingParamsTip = new JPanel();
		imagingParamsTip.add(new JLabel("Enter imaging parameters:"));
		imagingParamsTip.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(imagingParamsTip);

		// make and add field for exposure time
		nExpTimeField = new JTextField("65", 4);
		JLabel nExpTimeLabel = new JLabel("Exposure time (ms):");
		JPanel nExpTimePanel = new JPanel();
		nExpTimePanel.add(nExpTimeLabel);
		nExpTimePanel.add(nExpTimeField);
		nExpTimePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nExpTimePanel);

		// Make and add field for the # of frames to be averaged. Later there is
		// a check to ensure there are 2 or more frames and repeats (otherwise
		// the code does not work)
		nNumRepsField = new JTextField("2", 3);
		nNumFramesField = new JTextField("10", 3);
		// labels
		JLabel nNumRepsLabel = new JLabel("# of Repeats (>2)*: ");
		JLabel nNumFramesLabel = new JLabel("  # of Frames (>2): ");
		// put them in their own pane side-by-side
		JPanel nNumParamsPanel = new JPanel();
		nNumParamsPanel.add(nNumRepsLabel);
		nNumParamsPanel.add(nNumRepsField);
		nNumParamsPanel.add(nNumFramesLabel);
		nNumParamsPanel.add(nNumFramesField);
		nNumParamsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nNumParamsPanel);

		// Note about repeats
		// make and add tip label
		JPanel imagingParamsTip1 = new JPanel();
		imagingParamsTip1
				.add(new JLabel(
						"*Total number of averages = numFrames*numRepeats. Usually, numRepeats=2."));
		imagingParamsTip1.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(imagingParamsTip1);

		// make and add tip label
		JPanel startPosPanel = new JPanel();
		startPosPanel.add(new JLabel("Enter starting positions (mm):"));
		startPosPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(startPosPanel);

		// nXField, nYField, nAreaXField, nAreaYField
		nXField = new JTextField("0.0", 3);
		nYField = new JTextField("0.0", 3);
		// labels
		JLabel nXLabel = new JLabel("X: ");
		JLabel nYLabel = new JLabel("     Y: ");
		// put them in their own pane side-by-side
		JPanel nXYPanel = new JPanel();
		nXYPanel.add(nXLabel);
		nXYPanel.add(nXField);
		nXYPanel.add(nYLabel);
		nXYPanel.add(nYField);
		nXYPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nXYPanel);

		// make and add tip label
		JPanel scanAreaPanel = new JPanel();
		scanAreaPanel.add(new JLabel("Enter scan area  (mm):"));
		scanAreaPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(scanAreaPanel);

		// nXField, nYField, nAreaXField, nAreaYField
		nAreaXField = new JTextField("2.48", 3);
		nAreaYField = new JTextField("1.99", 3);
		// labels
		JLabel nAreaXLabel = new JLabel("dX: ");
		JLabel nAreaYLabel = new JLabel("     dY: ");
		// put them in their own pane side-by-side
		JPanel nAreaXYPanel = new JPanel();
		nAreaXYPanel.add(nAreaXLabel);
		nAreaXYPanel.add(nAreaXField);
		nAreaXYPanel.add(nAreaYLabel);
		nAreaXYPanel.add(nAreaYField);
		nAreaXYPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nAreaXYPanel);

		nFOVDilationField = new JTextField("100.00", 6);
		nXYStageSpeedField = new JTextField("1.0", 4);
		JLabel nFOVDilationLabel = new JLabel("Dilation of Step Size (%):");
		JLabel nMotorSpeedLabel = new JLabel("X, Y Motor Speed (mm/s):");
		JPanel nFOVDilationPanel = new JPanel();
		nFOVDilationPanel.add(nFOVDilationLabel);
		nFOVDilationPanel.add(nFOVDilationField);
		nFOVDilationPanel.add(nXYStageSpeedField);
		nFOVDilationPanel.add(nMotorSpeedLabel);
		nFOVDilationPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nFOVDilationPanel);

		// make and add field for chip number
		nChipNumField = new JTextField("1", 2);
		JLabel nChipNumLabel = new JLabel("Chip number:");
		JPanel nChipNumPanel = new JPanel();
		nChipNumPanel.add(nChipNumLabel);
		nChipNumPanel.add(nChipNumField);
		nChipNumPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nChipNumPanel);

		// make and add field for Blue only checkbox
		JPanel nCheckboxPanel = new JPanel();
		nBOnlyField = new JCheckBox("Blue Only");
		// nFocusField.setMnemonic(KeyEvent.VK_C);
		// The above allows us to toggle the check box with ctrl+c
		// Not necessary right now.
		nBOnlyField.setSelected(false);
		nCheckboxPanel.add(nBOnlyField);
		nCheckboxPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		// add field for focus checkbox
		nFocusField = new JCheckBox("Find focus");
		nFocusField.setSelected(true);
		nCheckboxPanel.add(nFocusField);
		nCheckboxPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nCheckboxPanel);

		// make and add field for path
		nFilenameField = new JTextField("e.g.: C:\\IRIS_Data\\", 55);
		JLabel nFileLabel = new JLabel("Save path:");
		JPanel nFilenamePanel = new JPanel();
		nFilenamePanel.add(nFileLabel);
		nFilenamePanel.add(nFilenameField);
		nFilenamePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nFilenamePanel);

		// make the 'run' button
		runB = new JButton("Run");
		runB.setActionCommand("runP");
		runB.addActionListener(this);
		runB.setToolTipText("Performs scan using input parameters, and saves image(s) to the designated folder");

		// make a panel for the run button and add button to the app_ and set
		// position
		JPanel runPanel = new JPanel();
		runPanel.add(runB);
		runPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(runPanel);

		// make the 'Acquire Mirror' button
		runN = new JButton("Acquire Mirror");
		runN.setActionCommand("runM");
		runN.addActionListener(this);
		runN.setToolTipText("Acquires a Mirror image and saves image to the designated folder");

		// make a panel for the mirror button and add button to the app_ and set
		// position
		JPanel mirrorPanel = new JPanel();
		runPanel.add(runN);
		runPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(mirrorPanel);

		// Reports on a successful scan, can and should be improved to display
		// all updates given by the program as it is running.
		updatePanel = new JPanel();
		updatePanel.add(new JLabel(" "));
		updatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(updatePanel);

		frame.pack();
		frame.setVisible(true);
		frame.setLocationByPlatform(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Point2D curXY = new Point2D.Double();

		if ("runP".equals(e.getActionCommand())) {

			// Clears all open windows and processes running in ImageJ
			try {
				app_.clearMessageWindow();
				core_.stopSequenceAcquisition();
				core_.clearCircularBuffer();
				app_.enableLiveMode(false);
				app_.closeAllAcquisitions();
			} catch (MMScriptException e1) {
				e1.printStackTrace();
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			// Retrieves all user unput data from the plugin window
			double numX = Double.parseDouble(nXField.getText());
			double numAreaX = Double.parseDouble(nAreaXField.getText());
			double numY = Double.parseDouble(nYField.getText());
			double numAreaY = Double.parseDouble(nAreaYField.getText());
			double exposureTime = Double.parseDouble(nExpTimeField.getText());
			int numRepeats = Integer.parseInt(nNumRepsField.getText());
			int numFrames = Integer.parseInt(nNumFramesField.getText());
			// The user enters this value in percent and we want the ratio
			double FOVdilation = Double
					.parseDouble(nFOVDilationField.getText()) * 0.01;
			// This stage speed is dividing distance in microns, and we want
			// output in milliseconds for our sleep timers, so the user input of
			// mm/s is perfect
			double stageSpeed = Double
					.parseDouble(nXYStageSpeedField.getText());
			// As stated previously, there must be at least 2 repeats and 2
			// frames for our for loops to work.
			if (numFrames < 2) {
				displayUpdate("There must be 2 or more frames.  Setting # of frames = 2.");
				numFrames = 2;
			}
			if (numRepeats < 2) {
				displayUpdate("There must be 2 or more repeats.  Setting # of repeats = 2.");
				numRepeats = 2;
			}
			String chipNumber = nChipNumField.getText();
			boolean focusOnOff = nFocusField.isSelected();
			boolean blueOnlyOnOff = nBOnlyField.isSelected();
			// TODO: add checks to make sure valid information is entered
			// in all field(s)

			// This identifies whether the user-entered directory is valid. If
			// it is not, it saves all data to the default directory
			// C:\IRIS_Data\. If C:\IRIS_Data\ does not exist, it creates this
			// folder.
			String defaultSavePath = "C:\\IRIS_Data\\";
			File dirDefault = new File(defaultSavePath);
			String userSavePath = nFilenameField.getText();
			File dir = new File(userSavePath);
			if (!dir.isDirectory()) {
				dir = dirDefault;
				userSavePath = defaultSavePath;
				if (!dirDefault.isDirectory()) {
					new File(defaultSavePath).mkdir();
				}
			}

			// Save format is compatible with FIJI Stitching-> Gridd/Collection
			// stitching when using "filename defined position"
			String today_date = new SimpleDateFormat("yyyy-MM-dd")
					.format(new Date());
			String fileName = today_date + "_chip" + chipNumber
					+ "_tile-position-list.txt";
			File file = new File(dir, fileName);

			// Runs the code that moves stages and acquires and processes images
			moveStage(numX, numY, numAreaX, numAreaY, exposureTime,
					userSavePath, chipNumber, numRepeats, numFrames,
					focusOnOff, blueOnlyOnOff, FOVdilation, stageSpeed, file);

		} else if ("runM".equals(e.getActionCommand())) {

			// See the above block for all comments related to this section

			// Closes any process left open
			try {
				app_.clearMessageWindow();
				core_.stopSequenceAcquisition();
				core_.clearCircularBuffer();
				app_.enableLiveMode(false);
				app_.closeAllAcquisitions();
			} catch (MMScriptException e1) {
				e1.printStackTrace();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			// Default Mirror settings
			double exposureTime = Double.parseDouble(nExpTimeField.getText());
			int numRepeats = Integer.parseInt(nNumRepsField.getText());
			int numFrames = Integer.parseInt(nNumFramesField.getText());
			double stageSpeed = Double
					.parseDouble(nXYStageSpeedField.getText());
			if (numFrames < 2) {
				displayUpdate("There must be 2 or more frames.  Setting # of frames = 2.");
				numFrames = 2;
			}
			if (numRepeats < 2) {
				displayUpdate("There must be 2 or more repeats.  Setting # of repeats = 2.");
				numRepeats = 2;
			}

			String defaultSavePath = "C:\\IRIS_Data\\";
			File dirDefault = new File(defaultSavePath);
			String userSavePath = nFilenameField.getText();
			File dir = new File(userSavePath);
			if (!dir.isDirectory()) {
				dir = dirDefault;
				userSavePath = defaultSavePath;
				if (!dirDefault.isDirectory()) {
					new File(defaultSavePath).mkdir();
				}
			}

			// Moves stage, acquires images and processes them to save as one
			// mirror file
			acquireMirror(userSavePath, exposureTime, numRepeats, numFrames,
					stageSpeed);
		} else {
			try {
				// Something has gone horribly wrong
				String defaultSavePath = "C:\\IRIS_Data\\";
				File dir = new File(defaultSavePath);

				if (!dir.isDirectory()) {
					new File(defaultSavePath).mkdir();
				}

				FileWriter fileWriter = new FileWriter(dir);
				String today_date = new SimpleDateFormat("yyyy-MM-dd")
						.format(new Date());
				double now = System.currentTimeMillis();
				fileWriter.write("You are ");
				fileWriter.write("a failure on " + today_date + " at " + now);
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException errorMessage) {
				errorMessage.printStackTrace();
			}
		}

	}

	@SuppressWarnings("static-access")
	public void moveStage(double startXmm, double startYmm,
			double userInputFieldSizemmX, double userInputFieldSizemmY,
			double exposureTime, String rootDirName, String chipNumber,
			int numRepeats, int numFrames, boolean level, boolean blueOnly,
			double FOVdilation, double stageSpeed, File file) {

		// Records the time at start of scan for later reporting
		double now = System.currentTimeMillis();

		// Number format for saving data to the .txt file
		DecimalFormat FMT1 = new DecimalFormat("#0.0000");
		// Number format for nice save names
		DecimalFormat FMT2 = new DecimalFormat("#00");

		// Returns the image FOV in pixels
		double width = core_.getImageWidth();
		double height = core_.getImageHeight();

		// Returns the bit depth, if camera is set up correctly it will be
		// 16-bit
		double bitDepth = core_.getImageBitDepth();

		// Calculates the step size of our stage in microns as it scans the
		// whole region of the chip. This includes the FOV dilation in case the
		// user wants to stitch images together leaving out the edges because of
		// illumination inconsistencies, clipping, etc.
		// This FOV dilation parameter is echoed in the FIJI Stitching plugin
		// Grid/Collection stitching as % overlap.
		double stepSizeXUm = core_.getImageWidth() * core_.getPixelSizeUm()
				* FOVdilation;
		double stepSizeYUm = core_.getImageHeight() * core_.getPixelSizeUm()
				* FOVdilation;

		// For saving the time to take each image to a text file
		double timeNow_ms = System.currentTimeMillis();

		// We initialize a file writer that will be opened and closed so that we
		// always have our data, even if the program crashes.
		BufferedWriter bw = null;
		FileWriter fileWriter = null;
		FileWriter fileWriter1 = null;
		try {
			// Opens fileWriter and the "true" argument allows appending
			fileWriter = new FileWriter(file, true);
			fileWriter.write("\n" + rootDirName + "chip" + chipNumber
					+ "\ntotal numFrames = " + numFrames * numRepeats
					+ "\npixHeight	pixWidth	bitDepth	stepSizeX	stepSizeY	\n"
					+ height + "	" + width + "	" + bitDepth + "	"
					+ FMT1.format(stepSizeXUm / 1000) + "	"
					+ FMT1.format(stepSizeYUm / 1000));

			// TODO: Find more general way to do this:
			// Here we look at the state of our second configuration group
			// (which should ALWAYS be for the objective state). We take this
			// string, append it to our mirror.tif file name and use this to
			// access the correct mirror file.
			StrVector allConfigs = core_.getAvailableConfigGroups();
			String state1 = "";
			try {
				state1 = core_.getCurrentConfig(allConfigs.get(1));
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			String mirrorPath = rootDirName + "\\mirror" + state1 + ".tif";

			// Initializes variables for z-focus calculations
			double relativeX = 0.0;
			double colStartZ = 0.0;
			double colEndZ = 0.0;
			double relativeY = 0.0;
			double zHere = 0.0;

			// Arrays to store test points in um
			ArrayList<Double> xPos = new ArrayList<Double>();
			ArrayList<Double> yPos = new ArrayList<Double>();
			ArrayList<Double> zPos = new ArrayList<Double>();

			// Calculates the number of tiles needed to cover the input area
			double nPosX = Math
					.ceil(userInputFieldSizemmX * 1000 / stepSizeXUm);
			double nPosY = Math
					.ceil(userInputFieldSizemmY * 1000 / stepSizeYUm);

			// Calculates the field size of the image for use in the focusing
			// algorithm. this means it needs the furthest distance between
			// upper left corners in the image space, not the entire width of
			// the image. Otherwise the focusing algorithm will think your image
			// is one tile longer than it actually is and the slope of the tilt
			// will not be correct. Think about it like technically at each
			// corner you are finding optimal focus in the upper left hand
			// corner.
			double fieldSizeUmX = (nPosX - 1) * stepSizeXUm;
			double fieldSizeUmY = (nPosY - 1) * stepSizeYUm;

			// report starting position
			double xStartPosUm = 0.0;
			double yStartPosUm = 0.0;
			double zStartPosUm = 0.0;
			try {
				xStartPosUm = core_.getPosition("xAxis");
				yStartPosUm = core_.getPosition("yAxis");
				zStartPosUm = core_.getPosition("zAxis");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			// for later calculation of sleep times when moving the stage long
			// distances
			double xCurrPosUm = xStartPosUm;
			double yCurrPosUm = yStartPosUm;

			// Gives us the user inputs from our plugin so they are all stored
			// in the .txt file
			fileWriter
					.append("\ninitialPositionX	initialPositionY	initialPositionZ	fieldSizemmX	fieldSizemmX	nPosX	nPosY	\n"
							+ FMT1.format(xStartPosUm)
							+ "	"
							+ FMT1.format(yStartPosUm)
							+ "	"
							+ FMT1.format(zStartPosUm)
							+ "	"
							+ FMT1.format((stepSizeXUm + fieldSizeUmX) / 1000)
							+ "	"
							+ FMT1.format((stepSizeYUm + fieldSizeUmY) / 1000)
							+ "	"
							+ FMT2.format(nPosX)
							+ "	"
							+ FMT2.format(nPosY) + "	");

			// Allocates an array for the z-position of optimal focus in each
			// corner
			ArrayList<Double> corners = new ArrayList<Double>();
			// If there is only one image to be taken, it only focuses once and
			// then acquires the image
			if (level && nPosY == 1 && nPosY == 1) {
				level = false;
				singleFocus(xStartPosUm, yStartPosUm, startXmm, startYmm,
						stageSpeed);
				try {
					zPos.add(core_.getPosition("zAxis"));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			// If multiple images are to be taken, this function finds the
			// optimal focus and each corner and returns them
			if (level) {
				corners = fourCorners(xStartPosUm, yStartPosUm, startXmm,
						startYmm, fieldSizeUmX, fieldSizeUmY, nPosX, nPosY,
						stageSpeed, file);
			} else {
				corners.add(0.0);
				corners.add(0.0);
				corners.add(0.0);
				corners.add(0.0);
			}
			double topLeftZ = corners.get(0);
			double topRightZ = corners.get(1);
			double bottomRightZ = corners.get(2);
			double bottomLeftZ = corners.get(3);

			// Writes the calculated positions to .txt file
			fileWriter.append("Corner z-positions\nTL	TR	BR	BL	\n"
					+ FMT1.format(corners.get(0)) + "	"
					+ FMT1.format(corners.get(1)) + "	"
					+ FMT1.format(corners.get(2)) + "	"
					+ FMT1.format(corners.get(3)) + "\n\n\nX	Y	Z	xi	yi	\n");
			int mmm = 0;
			// This section calculates the optimal z-position for every image in
			// the sequence using the corner z-values and assuming the tilt is
			// perfectly inear (as it should be for a Si wafer. It calculates
			// the correct z-position in the horizontal direction on top and
			// bottom and then the correct position in the vertical direction
			// using the top and bottom points.
			for (int xi = 0; xi < nPosX; xi++) {
				// colStartZ = interpolate between z(0,0) and z(x_max, 0)
				// colEndZ = interpolate between z(0,y_max) and z(x_max, y_max)
				relativeX = (xi * stepSizeXUm) / (fieldSizeUmX);
				colStartZ = topLeftZ + relativeX * (topRightZ - topLeftZ);
				colEndZ = bottomLeftZ + relativeX
						* (bottomRightZ - bottomLeftZ);
				for (int yi = 0; yi < nPosY; yi++) {
					relativeY = (yi * stepSizeYUm) / (fieldSizeUmY);
					zHere = colStartZ + relativeY * (colEndZ - colStartZ);
					// zHere = interpolate between colStartZ and colEndZ
					xPos.add(xStartPosUm + startXmm * 1000 + stepSizeXUm * xi);
					yPos.add(yStartPosUm - startYmm * 1000 - stepSizeYUm * yi);
					zPos.add(zHere);
					// Writes the positions the motors were asked to go to, not
					// necessarily the place they are in.
					fileWriter.append(FMT1.format(xPos.get(mmm)) + "	"
							+ FMT1.format(yPos.get(mmm)) + "	"
							+ FMT1.format(zPos.get(mmm)) + "	"
							+ FMT2.format(xi + 1) + "	" + FMT2.format(yi + 1)
							+ "	\n");
					mmm++;
				}
			}
			// Start data column of motor-reported positions and close this file
			// writer so the .txt fiel contains the heading
			fileWriter
					.append("\n\n\nPositions reported by motors:\nX	Y	TimeTaken_ms	\n");
			fileWriter.flush();
			fileWriter.close();

			// Moves the motors to the the positions stored in our arrays. Sleep
			// timers are included so the program doesn't attempt to run while
			// the motors are moving. This assumes the motors are moving at a
			// constant velocity.
			for (int i = 0; i < xPos.size(); i++) {
				try {
					// move x-axis
					core_.setFocusDevice("xAxis");
					core_.setPosition(xPos.get(i));
					app_.sleep((long) Math.abs((xPos.get(i) - xCurrPosUm)
							/ stageSpeed) + 1000);
					core_.waitForDevice("xAxis");
					// move y-axis
					core_.setFocusDevice("yAxis");
					core_.setPosition(yPos.get(i));
					app_.sleep((long) Math.abs((yPos.get(i) - yCurrPosUm)
							/ stageSpeed) + 1000);
					core_.waitForDevice("yAxis");
					// focus on z-axis (should always end program focused on
					// zAxis
					core_.setFocusDevice("zAxis");
					if (level == true) {
						core_.setPosition(zPos.get(i));
						app_.sleep(1000);
						core_.waitForDevice("zAxis");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				// In order to be compatible with Fiji Stitcher
				//
				// |(1,1) (1,2) ... (1,M)|;
				// |(2,1) (2,2)......... |;
				// |... ................ |;
				// |(N,1) ..........(N,M)|;

				// We find the indices at this location
				double myXcoord = Math.ceil((i + 1) / nPosY); // first position
																// is
																// (1,1)
				double myYcoord = (i % nPosY) + 1;

				try {
					xCurrPosUm = core_.getPosition("xAxis");
					yCurrPosUm = core_.getPosition("yAxis");
				} catch (Exception e1) {
					e1.printStackTrace();
				}

				// opens a buffered file writer that will report this info in
				// the txt file and the user will have access to the data even
				// if the program crashes.
				try {
					double current = System.currentTimeMillis() - now;
					fileWriter1 = new FileWriter(file, true);
					bw = new BufferedWriter(fileWriter1);
					bw.write(FMT1.format(xCurrPosUm) + "	"
							+ FMT1.format(yCurrPosUm) + "	"
							+ FMT1.format(current) + "	\n");

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					// Closes the buffered file writer and file writer safely
					try {

						if (bw != null)
							bw.close();

						if (fileWriter1 != null)
							fileWriter1.close();

					} catch (IOException ex) {

						ex.printStackTrace();

					}
				}

				String saveName = rootDirName + "\\chip" + chipNumber + "_X"
						+ FMT2.format(myXcoord) + "_Y" + FMT2.format(myYcoord)
						+ ".tif";

				// Acquires and averages our images
				ImagePlus colorIms = fourColor(rootDirName, (int) width,
						(int) height, exposureTime, numRepeats, numFrames,
						blueOnly);

				colorIms.show();

				// Finds mirror file associated with selected objective,
				// otherwise saves an unnormalized image
				File dir = new File(mirrorPath);
				if (!dir.isFile()) {
					displayUpdate("No mirror file!");

					// flip and save the image
					IJ.run(colorIms, "Flip Horizontally", "stack");
					IJ.save(colorIms,
							rootDirName + "\\chip" + chipNumber + "_X"
									+ FMT2.format(myXcoord) + "_Y"
									+ FMT2.format(myYcoord) + "_nonNorm.tif");

				} else {
					ImagePlus mir = IJ.openImage(mirrorPath);
					if (blueOnly) {
						SubstackMaker se = new SubstackMaker();
						se.makeSubstack(mir, "1");
					}
					ImageCalculator ic = new ImageCalculator(); // http://rsb.info.nih.gov/ij/developer/source/ij/plugin/ImageCalculator.java.html
					ImagePlus normedStack = ic.run(
							"Divide create 32-bit stack", colorIms, mir);
					IJ.run(normedStack, "Multiply...", "value=30000 stack");
					ImageConverter converter = new ImageConverter(normedStack);
					converter.setDoScaling(false);
					// TODO: Why does Derin convert to 16-bit here?
					converter.convertToGray16();

					// Flips the image so it appears as it would if you were
					// looking at the chip, not a reflected image.
					IJ.run(normedStack, "Flip Horizontally", "stack");
					timeNow_ms = System.currentTimeMillis() - timeNow_ms;
					IJ.save(normedStack, saveName);

				}

				// Prevents ImageJ from asking if you want to save the stack
				// before closing and therefore crashing the program
				colorIms.changes = false;
				colorIms.close();

			}
			try {
				// Move back to the starting position!
				// move x-axis
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm);
				app_.sleep((long) Math.abs((xStartPosUm - xCurrPosUm)
						/ stageSpeed));
				core_.waitForDevice("xAxis");
				// move y-axis
				core_.setFocusDevice("yAxis");
				core_.setPosition(yStartPosUm);
				app_.sleep((long) Math.abs((yStartPosUm - yCurrPosUm)
						/ stageSpeed));
				core_.waitForDevice("yAxis");

				core_.setFocusDevice("zAxis");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				// Set LEDs to Blue, so the user can easily view their spots.
				core_.setConfig("LEDs", "_off");
				app_.sleep(10);
				core_.setConfig("LEDs", "Blue");
			} catch (MMScriptException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// reports successful imaging, save location, and image time.
			double itTook = System.currentTimeMillis() - now;
			String str4 = "\nAcq. took: " + FMT1.format(itTook)
					+ " ms; Images are stored at " + rootDirName;
			displayUpdate(str4);

		} catch (IOException e3) {
			e3.printStackTrace();
		}
	}

	public ImagePlus fourColor(String rootDirName, int width, int height,
			double exposureTime, int numRepeats, int numFrames, boolean blueOnly) {
		// Acquires averaged images for functions moveStage and acquireMirror

		try {
			core_.setExposure(exposureTime);
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		String[] channels = new String[4];

		channels[0] = "Blue";
		channels[1] = "Green";
		channels[2] = "Orange";
		channels[3] = "Red";

		int nColors = channels.length;

		// Takes images only at the blue wavelength
		if (blueOnly) {
			nColors = 1;
		}

		// Initializes the stack to hold RGBY images
		ImageStack colorStack = new ImageStack(width, height, nColors);
		// Images are first placed in a miniStack and then averaged in order to
		// conserve memory
		String miniStack = "";

		for (int c = 0; c < nColors; c++) {
			try {
				// Turns on appropriate LED
				core_.setConfig("LEDs", "_off");
				app_.sleep(10);
				core_.setConfig("LEDs", channels[c]);
			} catch (MMScriptException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			// Sptorage for images for memory conservation, as above
			ImageStack metaStack = new ImageStack((int) width, (int) height,
					numRepeats);
			for (int k = 0; k < numRepeats; k++) {
				// Create a mini-stack
				miniStack = app_.getUniqueAcquisitionName("raw");
				try {
					// Acquires images
					app_.openAcquisition(miniStack, rootDirName, numFrames, 1,
							1, 1, true, false);
					core_.startSequenceAcquisition(numFrames, 200, true); // numImages,
																			// intervalMs,
																			// stopOnOverflow
				} catch (MMScriptException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				int frame = 0;
				// Collects images until it has the correct amount
				while (frame < numFrames) {
					if (core_.getRemainingImageCount() > 0) {
						try {
							TaggedImage img = core_.popNextTaggedImage();
							app_.addImageToAcquisition(miniStack, frame, 0, 0,
									0, img);
							frame = frame + 1;

						} catch (MMScriptException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						core_.sleep(3);
					}
				}
				try {
					// Cleans everything up
					core_.stopSequenceAcquisition();
					core_.clearCircularBuffer();

				} catch (Exception e) {
					e.printStackTrace();
				}

				// Average the mini-stack...
				IJ.run("Z Project...", "start=0 stop=" + numFrames
						+ " projection=[Average Intensity]");
				ImagePlus miniAvg = IJ.getImage(); // this is an ImagePlus
													// object

				// ... and add it to the meta-stack
				metaStack.setPixels(miniAvg.getProcessor().getPixels(), k + 1);

				// close the mini-stack and mini-average
				try {
					app_.promptToSaveAcquisition(miniStack, false);
					// changed from (b/c deprecated):
					// app_.getAcquisition(miniStack).promptToSave(false);
					app_.closeAcquisitionWindow(miniStack);
				} catch (MMScriptException e) {
					e.printStackTrace();
				}
				miniAvg.close();
			}

			// average the meta-stack.
			ImagePlus metaSlices = new ImagePlus("repeatImages", metaStack);
			metaSlices.show();
			IJ.run("Z Project...", "start=0 stop=" + numRepeats
					+ " projection=[Average Intensity]");
			ImagePlus totalAvg = IJ.getImage();
			metaSlices.close();
			totalAvg.show();
			colorStack.setPixels(totalAvg.getProcessor().getPixels(), c + 1);
			totalAvg.close();
		}
		ImagePlus colorIms = new ImagePlus("colorImages", colorStack);

		return colorIms;

	}

	public ArrayList<Double> fourCorners(double xStartPosUm,
			double yStartPosUm, double startXmm, double startYmm,
			double fieldSizeUmX, double fieldSizeUmY, double nPosX,
			double nPosY, double stageSpeed, File file) {
		// Focuses in the top left corner of the corners of our AOI, and returns
		// those values to be part of the focused z-position calculation

		// Intiializes the autofocus in the GUI. It is apparently difficult to
		// select Autofocus params from the plugin, plus the user should play
		// with this themselves, so right now this autofocus algorithm will do
		// whatever the setting you select in the normal autofocus window.
		Autofocus af = app_.getAutofocus();

		ArrayList<Double> corners = new ArrayList<Double>();

		double topLeftZ = 0.0;
		double topRightZ = 0.0;
		double bottomLeftZ = 0.0;
		double bottomRightZ = 0.0;

		try {
			displayUpdate("Focusing in the four corners:");
			core_.setConfig("LEDs", "_off");
			app_.sleep(10);
			core_.setConfig("LEDs", "Blue");

			core_.setFocusDevice("xAxis");
			core_.setPosition(xStartPosUm + 1000.0 * startXmm);
			app_.sleep((int) (10 + Math.abs(1000.0 * startXmm / stageSpeed)));
			core_.waitForDevice("xAxis");
			core_.setFocusDevice("yAxis");
			core_.setPosition(yStartPosUm - 1000.0 * startYmm);
			app_.sleep((int) (1 + Math.abs(1000.0 * startYmm / stageSpeed)));
			core_.waitForDevice("yAxis");
			core_.setFocusDevice("zAxis");
			af.fullFocus();
			app_.sleep((int) (1));
			core_.waitForDevice("zAxis");
			topLeftZ = core_.getPosition("zAxis");
			corners.add(topLeftZ);
			displayUpdate("top-left z = " + topLeftZ);

			core_.setFocusDevice("xAxis");
			core_.setPosition(xStartPosUm + 1000.0 * (startXmm) + fieldSizeUmX);
			app_.sleep((int) (1 + Math.abs((startXmm * 1000 + fieldSizeUmX)
					/ stageSpeed)));
			core_.waitForDevice("xAxis");
			core_.setFocusDevice("zAxis");
			af.fullFocus();
			app_.sleep((int) (1));
			core_.waitForDevice("zAxis");
			topRightZ = core_.getPosition("zAxis");
			corners.add(topRightZ);
			displayUpdate("top-right z = " + topRightZ);

			core_.setFocusDevice("yAxis");
			core_.setPosition(yStartPosUm - 1000.0 * (startYmm) - fieldSizeUmY);
			app_.sleep((int) (1 + Math.abs((startYmm * 1000 - fieldSizeUmY)
					/ stageSpeed)));
			core_.setFocusDevice("zAxis");
			af.fullFocus();
			app_.sleep((int) (1));
			core_.waitForDevice("zAxis");
			bottomRightZ = core_.getPosition("zAxis");
			corners.add(bottomRightZ);
			displayUpdate("bottom-right z = " + bottomRightZ);

			core_.setFocusDevice("xAxis");
			core_.setPosition(xStartPosUm + 1000.0 * startXmm);
			app_.sleep((int) (1 + Math.abs((1000.0 * startXmm + fieldSizeUmX)
					/ stageSpeed)));
			core_.waitForDevice("xAxis");
			core_.setFocusDevice("zAxis");
			af.fullFocus();
			app_.sleep((int) (1));
			core_.waitForDevice("zAxis");
			bottomLeftZ = core_.getPosition("zAxis");
			corners.add(bottomLeftZ);
			displayUpdate("bottom-left z = " + bottomLeftZ);

			core_.setFocusDevice("yAxis");
			core_.setPosition(yStartPosUm - 1000.0 * startYmm);
			app_.sleep((int) (1 + Math.abs((startYmm * 1000 - fieldSizeUmY)
					/ stageSpeed)));
			core_.waitForDevice("yAxis");
			core_.setFocusDevice("zAxis");
			displayUpdate("back to top left ");
		} catch (MMScriptException e) {
			e.printStackTrace();
		} catch (MMException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return corners;
	}

	@SuppressWarnings("static-access")
	public void acquireMirror(String rootDirName, double exposureTime,
			int numRepeats, int numFrames, double stageSpeed) {
		// Moves the stage 500 microns in several directions, takes six images
		// this way and then takes the median to exclude any outliers from
		// shmutz
		double width = core_.getImageWidth();
		double height = core_.getImageHeight();
		int nXSteps = 3;
		int nYSteps = 2;
		double stepSize = 500.0;

		// report starting position
		double xStartPosUm = 0.0;
		double yStartPosUm = 0.0;
		try {
			xStartPosUm = core_.getPosition("xAxis");
			yStartPosUm = core_.getPosition("yAxis");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// make the Mirror folder to hold images at each location. Later we will
		// delete this folder and save only a single image.
		new File(rootDirName + "\\Mirror\\").mkdir();
		File mir = new File(rootDirName + "\\Mirror\\");
		String today_date = new SimpleDateFormat("yyyy-MM-dd")
				.format(new Date());
		String saveName = "";

		// Moves the stage to 6 different locations for imaging.
		for (int xi = 0; xi < nXSteps; xi++) {
			// move x-axis
			try {
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + xi * stepSize);
				app_.sleep((long) (xi * stepSize / stageSpeed));
				core_.waitForDevice("xAxis");
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (int yi = 0; yi < nYSteps; yi++) {
				// move y-axis
				try {
					core_.setFocusDevice("yAxis");
					core_.setPosition(yStartPosUm - yi * stepSize);
					app_.sleep((long) (yi * stepSize / stageSpeed));
					core_.waitForDevice("yAxis");
				} catch (Exception e) {
					e.printStackTrace();
				}

				// takes an averaged image
				ImagePlus mirrorStack = fourColor(rootDirName, (int) width,
						(int) height, exposureTime, numRepeats, numFrames,
						false);
				mirrorStack.show();

				// save the image
				saveName = rootDirName + "\\Mirror\\" + today_date + " mirror"
						+ "_" + xi + "_" + yi + ".tif";
				IJ.save(mirrorStack, saveName);
				mirrorStack.close();

			}

		}

		// Find median
		// Open image Sequence
		FolderOpener mirFolder = new FolderOpener(); // https://imagej.nih.gov/ij/developer/api/ij/plugin/FolderOpener.html
		ImagePlus stackFormatFiles = mirFolder.open(rootDirName + "\\Mirror\\");

		// Stack to Hyperstack...
		HyperStackConverter hyperConverter = new HyperStackConverter();
		ImagePlus hypFormatFiles = hyperConverter
				.toHyperStack(stackFormatFiles, 4, nXSteps * nYSteps, 1,
						"xyczt", "grayscale");
		hypFormatFiles.show();

		// find median
		IJ.run("Z Project...", "projection=Median");
		core_.sleep(10);
		hypFormatFiles.close();

		// TODO: Find more general way to do this:
		// Here we look at the state of our second configuration group
		// (which should ALWAYS be for the objective state). We take this
		// string, append it to our mirror.tif file name and use this to
		// access the correct mirror file.
		StrVector allConfigs = core_.getAvailableConfigGroups();
		String state1 = "";
		try {
			state1 = core_.getCurrentConfig(allConfigs.get(1));
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String mirrorPath = rootDirName + "\\mirror" + state1 + ".tif";

		ImagePlus totalMir = IJ.getImage();
		totalMir.show();
		IJ.save(totalMir, mirrorPath);
		totalMir.close();

		boolean exitSuccess = deleteDirectory(mir);
		if (!exitSuccess) {
			displayUpdate("An error occurred, please delete Mirror folder by hand.");
		}
		IJ.run("Close All");
		core_.sleep(10);

		// put stages back to origin
		try {
			// move x-axis
			core_.setFocusDevice("xAxis");
			core_.setPosition(xStartPosUm);
			app_.sleep((long) Math.abs((xStartPosUm - nXSteps * stepSize)
					/ stageSpeed));
			core_.waitForDevice("xAxis");
			// move y-axis
			core_.setFocusDevice("yAxis");
			core_.setPosition(yStartPosUm);
			app_.sleep((long) Math.abs((yStartPosUm - nYSteps * stepSize)
					/ stageSpeed));
			core_.waitForDevice("yAxis");

			core_.setFocusDevice("zAxis");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			core_.setConfig("LEDs", "_off");
			app_.sleep(10);
			core_.setConfig("LEDs", "Blue");
		} catch (MMScriptException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void displayUpdate(String update) {
		// This function was intended to provide a running account of what the
		// plugin is doing, but I never bothered to make it work right. This is
		// barely important, so now it just functions as a report when the
		// imaging is over.
		updatePanel.removeAll();
		JLabel newUpdate = new JLabel(update);
		updatePanel.add(newUpdate);
		updatePanel.revalidate();
		updatePanel.repaint();
	}

	public static boolean deleteDirectory(File directory) {
		// Safely deletes a directory, for use with the Mirror folder
		if (directory.exists()) {
			File[] files = directory.listFiles();
			if (null != files) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						deleteDirectory(files[i]);
					} else {
						files[i].delete();
					}
				}
			}
		}
		return (directory.delete());
	}

	public void singleFocus(double xStartPosUm, double yStartPosUm,
			double startXmm, double startYmm, double stageSpeed) {
		// if you only have one FOV and don't need to focus in 4 corners.
		Autofocus af = app_.getAutofocus();

		try {
			displayUpdate("Focusing for single FOV:");
			core_.setConfig("LEDs", "_off");
			app_.sleep(10);
			core_.setConfig("LEDs", "Blue");

			core_.setFocusDevice("xAxis");
			core_.setPosition(xStartPosUm + 1000.0 * startXmm);
			app_.sleep((int) (1 + Math.abs(1000.0 * startXmm / stageSpeed)));
			core_.waitForDevice("xAxis");
			core_.setFocusDevice("yAxis");
			core_.setPosition(yStartPosUm - 1000.0 * startYmm);
			app_.sleep((int) (1 + Math.abs(1000.0 * startYmm / stageSpeed)));

			core_.waitForDevice("yAxis");
			core_.setFocusDevice("zAxis");
			af.fullFocus();
			core_.waitForDevice("zAxis");

		} catch (MMScriptException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
