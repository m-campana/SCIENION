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
	// Window label/plugin description
	public static final String menuName = "IRIS: Spot Check";
	public static final String tooltipDescription = "Image acquisition interface";
	public static final String versionNumber = "0.1";

	// Provides access to the Micro-Manager Java API (for app_ control and high-
	// level functions).
	private ScriptInterface app_;
	// Provides access to the Micro-Manager Core API (for direct hardware
	// control)
	private CMMCore core_;

	// Initializes app_ objects for usable within this package, class, and
	// subclasses
	protected JFrame frame; // API:
							// https://docs.oracle.com/javase/7/docs/api/javax/swing/JFrame.html
	protected Container panel; // API:
								// https://docs.oracle.com/javase/7/docs/api/java/awt/Container.html
	protected JButton runB, setB, runN, setN;
	protected JTextField nFilenameField, nXField, nYField, nAreaXField,
			nAreaYField, nChipNumField, nExpTimeField, nNumRepsField,
			nNumFramesField, nFOVDilationField;
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

		// nNumRepsField, nNumFramesField
		nNumRepsField = new JTextField("2", 3);
		nNumFramesField = new JTextField("10", 3);
		// labels
		JLabel nNumRepsLabel = new JLabel("# of Repeats (>2): ");
		JLabel nNumFramesLabel = new JLabel("  # of Frames (>2): ");
		// put them in their own pane side-by-side
		JPanel nNumParamsPanel = new JPanel();
		nNumParamsPanel.add(nNumRepsLabel);
		nNumParamsPanel.add(nNumRepsField);
		nNumParamsPanel.add(nNumFramesLabel);
		nNumParamsPanel.add(nNumFramesField);
		nNumParamsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nNumParamsPanel);

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
		JLabel nFOVDilationLabel = new JLabel("Dilation of Image (%):");
		JPanel nFOVDilationPanel = new JPanel();
		nFOVDilationPanel.add(nFOVDilationLabel);
		nFOVDilationPanel.add(nFOVDilationField);
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

		JPanel nCheckboxPanel = new JPanel();
		// make and add field for Blue only checkbox
		nBOnlyField = new JCheckBox("Blue Only");
		// nFocusField.setMnemonic(KeyEvent.VK_C); I think this allows us to
		// toggle the check box with ctrl+c
		nBOnlyField.setSelected(false);
		nCheckboxPanel.add(nBOnlyField);
		nCheckboxPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		// add field for focus checkbox
		nFocusField = new JCheckBox("Find focus");
		// nFocusField.setMnemonic(KeyEvent.VK_C); I think this allows us to
		// toggle the check box with ctrl+c
		nFocusField.setSelected(true);
		nCheckboxPanel.add(nFocusField);
		nCheckboxPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(nCheckboxPanel);

		// make and add field for path
		nFilenameField = new JTextField("e.g.: C:\\IRIS_Data\\", 50);
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
		runB.setToolTipText("Initiates scan using input parameters, then saves image to the designated folder");

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
		runN.setToolTipText("Initiates acquisition of a set of Mirror images and accompanied save file in the designated folder");

		// make a panel for the mirror button and add button to the app_ and set
		// position
		JPanel mirrorPanel = new JPanel();
		runPanel.add(runN);
		runPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(mirrorPanel);

		// trying to have a running report output
		updatePanel = new JPanel();
		updatePanel.add(new JLabel(" "));
		updatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(updatePanel);

		// frame.setResizable(false);
		frame.pack();
		frame.setVisible(true);
		frame.setLocationByPlatform(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// Point2D curXY = new Point2D.Double();

		if ("runP".equals(e.getActionCommand())) {
			try {
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

				double numX = Double.parseDouble(nXField.getText());
				double numAreaX = Double.parseDouble(nAreaXField.getText());
				double numY = Double.parseDouble(nYField.getText());
				double numAreaY = Double.parseDouble(nAreaYField.getText());
				double exposureTime = Double.parseDouble(nExpTimeField
						.getText());
				int numRepeats = Integer.parseInt(nNumRepsField.getText());
				int numFrames = Integer.parseInt(nNumFramesField.getText());
				double FOVdilation = Double.parseDouble(nFOVDilationField
						.getText()) * 0.01; // convert to ratio from percentage
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
				// in the field(s)

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

				String today_date = new SimpleDateFormat("yyyy-MM-dd")
						.format(new Date());
				String fileName = today_date + "_chip" + chipNumber
						+ "_tile-position-list.txt";
				File file = new File(dir, fileName);
				FileWriter fileWriter = new FileWriter(file);

				String message = moveStage(numX, numY, numAreaX, numAreaY,
						exposureTime, userSavePath, chipNumber, numRepeats,
						numFrames, focusOnOff, blueOnlyOnOff, FOVdilation);

				fileWriter.write(message);
				fileWriter.flush();
				fileWriter.close();

			} catch (IOException errorMessage) {
				errorMessage.printStackTrace();
			}

		} else if ("runM".equals(e.getActionCommand())) {

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

			acquireMirror(userSavePath, exposureTime, numRepeats, numFrames);
		} else {
			try {
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
	public String moveStage(double startXmm, double startYmm,
			double fieldSizemmX, double fieldSizemmY, double exposureTime,
			String rootDirName, String chipNumber, int numRepeats,
			int numFrames, boolean level, boolean blueOnly, double FOVdilation) {

		double now = System.currentTimeMillis();
		DecimalFormat FMT2 = new DecimalFormat("#00");

		double width = core_.getImageWidth();
		double height = core_.getImageHeight();
		double bitDepth = core_.getImageBitDepth();

		double stepSizeXUm = core_.getImageWidth() * core_.getPixelSizeUm()
				* FOVdilation;
		double stepSizeYUm = core_.getImageHeight() * core_.getPixelSizeUm()
				* FOVdilation;

		String str1 = "height: " + height + " width: " + width + " bitDepth: "
				+ bitDepth + " step size X: " + stepSizeXUm + " step size Y: "
				+ stepSizeYUm;

		// TODO: Find more general way to do this
		StrVector allConfigs = core_.getAvailableConfigGroups();
		String state1 = "";
		try {
			state1 = core_.getCurrentConfig(allConfigs.get(1));
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String mirrorPath = rootDirName + "\\mirror" + state1 + ".tif";

		// home XY Stage
		try {
			core_.stopSequenceAcquisition();
			core_.clearCircularBuffer();

			// core_.home("xAxis"); doesn't work, needs added functionality
			// core_.home("yAxis");

		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String str3 = "";

		double relativeX = 0.0;
		double colStartZ = 0.0;
		double colEndZ = 0.0;
		double relativeY = 0.0;
		double zHere = 0.0;
		// define test points in um
		ArrayList<Double> xPos = new ArrayList<Double>();
		ArrayList<Double> yPos = new ArrayList<Double>();
		ArrayList<Double> zPos = new ArrayList<Double>();

		double nPosX = Math.ceil(fieldSizemmX * 1000 / stepSizeXUm);
		double nPosY = Math.ceil(fieldSizemmY * 1000 / stepSizeYUm);

		// report starting position
		double zStartPos = 0.0;
		double xStartPosUm = 0.0;
		double yStartPosUm = 0.0;
		double xCurrPosUm = 0.0;
		double yCurrPosUm = 0.0;
		try {
			xStartPosUm = core_.getPosition("xAxis");
			yStartPosUm = core_.getPosition("yAxis");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		String str2 = "\n\n\nInitial position (x,y) [um]: ( " + xStartPosUm
				+ ", " + yStartPosUm + " )" + "fieldSizemmX " + fieldSizemmX
				+ "fieldSizemmY " + fieldSizemmY + "nPosX " + nPosX + "nPosY"
				+ nPosY;

		double corners[] = { 0, 0, 0, 0 };
		if (level == true) {
			corners = fourCorners(xStartPosUm, yStartPosUm, startXmm, startYmm,
					fieldSizemmX, fieldSizemmY, nPosX, nPosY);
		}
		double topLeftZ = corners[0];
		double topRightZ = corners[1];
		double bottomLeftZ = corners[2];
		double bottomRightZ = corners[3];

		for (int xi = 0; xi < nPosX; xi++) {
			// colStartZ = interpolate between z(0,0) and z(x_max, 0)
			// colEndZ = interpolate between z(0,y_max) and z(x_max, y_max)
			relativeX = (xi * stepSizeXUm) / (fieldSizemmX * 1000);
			colStartZ = topLeftZ + relativeX * (topRightZ - topLeftZ);
			colEndZ = bottomLeftZ + relativeX * (bottomRightZ - bottomLeftZ);
			for (int yi = 0; yi < nPosY; yi++) {
				relativeY = (yi * stepSizeYUm) / (fieldSizemmY * 1000);
				zHere = colStartZ + relativeY * (colEndZ - colStartZ);
				// zHere = interpolate between colStartZ and colEndZ
				xPos.add(xStartPosUm + startXmm * 1000 + stepSizeXUm * xi);
				yPos.add(yStartPosUm - startYmm * 1000 - stepSizeYUm * yi);
				zPos.add(zStartPos + zHere);
			}
		}

		for (int i = 0; i < xPos.size(); i++) {
			double start = System.currentTimeMillis();
			try {
				// move x-axis
				core_.setFocusDevice("xAxis");
				core_.setPosition(xPos.get(i));
				core_.waitForDevice("xAxis");
				// move y-axis
				core_.setFocusDevice("yAxis");
				core_.setPosition(yPos.get(i));
				core_.waitForDevice("yAxis");
				// focus on z-axis (should always end program focused on zAxis
				core_.setFocusDevice("zAxis");
				if (level == true) {
					core_.setPosition(zPos.get(i));
					core_.waitForDevice("zAxis");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			double end = System.currentTimeMillis();

			// In order to be compatible with Fiji Stitcher
			//
			// |(1,1) (1,2) ... (1,M)|;
			// |(2,1) (2,2)......... |;
			// |... ................ |;
			// |(N,1) ..........(N,M)|;

			double myXcoord = Math.ceil((i + 1) / nPosY); // first position is
															// (1,1)
			double myYcoord = (i % nPosY) + 1;

			str3 = str3 + "\n\n\nAimed for (" + myXcoord + ", " + myYcoord
					+ ") at (" + xPos.get(i) + "," + yPos.get(i) + ")" + " in "
					+ (end - start) + " ms";

			try {
				xCurrPosUm = core_.getPosition("xAxis");
				yCurrPosUm = core_.getPosition("yAxis");
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			str3 = str3 + "\n\n\nReached point (" + myXcoord + ", " + myYcoord
					+ ") at (" + xCurrPosUm + "," + yCurrPosUm + ")";

			String saveName = rootDirName + "\\chip" + chipNumber + "_X"
					+ FMT2.format(myXcoord) + "_Y" + FMT2.format(myYcoord)
					+ ".tif";

			ImagePlus colorIms = fourColor(rootDirName, (int) width,
					(int) height, exposureTime, numRepeats, numFrames, blueOnly);

			colorIms.show();

			File dir = new File(mirrorPath);
			if (!dir.isFile()) {
				displayUpdate("No mirror file!");

				// flip and save the image
				IJ.run(colorIms, "Flip Horizontally", "stack");
				IJ.save(colorIms, rootDirName + "\\chip" + chipNumber + "_X"
						+ FMT2.format(myXcoord) + "_Y" + FMT2.format(myYcoord)
						+ "_nonNorm.tif");

			} else {
				ImagePlus mir = IJ.openImage(mirrorPath);
				if (blueOnly) {
					SubstackMaker se = new SubstackMaker();
					se.makeSubstack(mir, "1");
					// TODO: make sure this accesses mirror file correctly
				}
				ImageCalculator ic = new ImageCalculator(); // http://rsb.info.nih.gov/ij/developer/source/ij/plugin/ImageCalculator.java.html
				ImagePlus normedStack = ic.run("Divide create 32-bit stack",
						colorIms, mir);
				IJ.run(normedStack, "Multiply...", "value=30000 stack");
				ImageConverter converter = new ImageConverter(normedStack);
				converter.setDoScaling(false);
				converter.convertToGray16();

				// TODO: figure out the more elegant process later?
				// ImageProcessor ip = ImageUtils
				// .makeProcessor(core_, normedStack);
				// ip.flipHorizontal();
				IJ.run(normedStack, "Flip Horizontally", "stack");

				IJ.save(normedStack, saveName);
			}

			colorIms.changes = false;
			colorIms.close();

		}
		try {
			// move x-axis
			core_.setFocusDevice("xAxis");
			core_.setPosition(xStartPosUm);
			core_.waitForDevice("xAxis");
			// move y-axis
			core_.setFocusDevice("yAxis");
			core_.setPosition(yStartPosUm);
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

		double itTook = System.currentTimeMillis() - now;
		String str4 = "Acq. took: " + itTook + " ms; Images are stored at "
				+ rootDirName;
		displayUpdate(str4);

		return str1 + str2 + str3 + "\n\n\n\n" + str4;
	}

	public ImagePlus fourColor(String rootDirName, int width, int height,
			double exposureTime, int numRepeats, int numFrames, boolean blueOnly) {

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

		if (blueOnly) {
			nColors = 1;
		}

		ImageStack colorStack = new ImageStack(width, height, nColors);
		String miniStack = "";

		for (int c = 0; c < nColors; c++) {
			try {
				core_.setConfig("LEDs", "_off");
				app_.sleep(10);
				core_.setConfig("LEDs", channels[c]);
			} catch (MMScriptException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			ImageStack metaStack = new ImageStack((int) width, (int) height,
					numRepeats);
			for (int k = 0; k < numRepeats; k++) {
				// Create a mini-stack
				miniStack = app_.getUniqueAcquisitionName("raw");
				try {
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

	public double[] fourCorners(double xStartPosUm, double yStartPosUm,
			double startXmm, double startYmm, double fieldSizemmX,
			double fieldSizemmY, double nPosX, double nPosY) {
		// TODO: when only taking one image, only autofocus one time
		Autofocus af = app_.getAutofocus();

		double topLeftZ = 0.0;
		double topRightZ = 0.0;
		double bottomLeftZ = 0.0;
		double bottomRightZ = 0.0;
		if (nPosX == 1 && nPosY == 1) {
			try {
				displayUpdate("Focusing in the four corners:");
				core_.setConfig("LEDs", "_off");
				app_.sleep(10);
				core_.setConfig("LEDs", "Blue");

				displayUpdate("top-left");
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + 1000.0 * startXmm);
				core_.waitForDevice("xAxis");
				core_.setFocusDevice("yAxis");
				core_.setPosition(yStartPosUm - 1000.0 * startYmm);
				core_.waitForDevice("yAxis");
				core_.setFocusDevice("zAxis");
				af.fullFocus();
				core_.waitForDevice("zAxis");
				topLeftZ = core_.getPosition("zAxis");
			} catch (MMScriptException e) {
				e.printStackTrace();
			} catch (MMException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {

			try {
				displayUpdate("Focusing in the four corners:");
				core_.setConfig("LEDs", "_off");
				app_.sleep(10);
				core_.setConfig("LEDs", "Blue");

				// TODO: Make this not hard coded
				int stageSpeed = 1000; // 1 mm/s -> 1000 ms/mm

				displayUpdate("top-left");
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + 1000.0 * startXmm);
				app_.sleep((int) startXmm * stageSpeed);
				core_.waitForDevice("xAxis");
				core_.setFocusDevice("yAxis");
				core_.setPosition(yStartPosUm - 1000.0 * startYmm);
				app_.sleep((int) startYmm * stageSpeed);
				core_.waitForDevice("yAxis");
				core_.setFocusDevice("zAxis");
				af.fullFocus();
				core_.waitForDevice("zAxis");
				topLeftZ = core_.getPosition("zAxis");

				displayUpdate("top-right");
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + 1000.0
						* (startXmm + fieldSizemmX));
				app_.sleep((int) (startXmm + fieldSizemmX) * stageSpeed);
				core_.waitForDevice("xAxis");
				core_.setFocusDevice("yAxis");
				core_.setPosition(yStartPosUm - 1000.0 * startYmm);
				app_.sleep((int) startYmm * stageSpeed);
				core_.waitForDevice("yAxis");
				core_.setFocusDevice("zAxis");
				af.fullFocus();
				core_.waitForDevice("zAxis");
				topRightZ = core_.getPosition("zAxis");

				displayUpdate("bottom-right");
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + 1000.0
						* (startXmm + fieldSizemmX));
				app_.sleep((int) (startXmm + fieldSizemmX) * stageSpeed);
				core_.setFocusDevice("yAxis");
				core_.setPosition(yStartPosUm - 1000.0
						* (startYmm + fieldSizemmY));
				app_.sleep((int) (startYmm + fieldSizemmY) * stageSpeed);
				core_.setFocusDevice("zAxis");
				af.fullFocus();
				core_.waitForDevice("zAxis");
				bottomRightZ = core_.getPosition("zAxis");

				displayUpdate("bottom-left");
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + 1000.0 * startXmm);
				app_.sleep((int) startXmm * stageSpeed);
				core_.waitForDevice("xAxis");
				core_.setFocusDevice("yAxis");
				core_.setPosition(yStartPosUm - 1000.0
						* (startYmm + fieldSizemmY));
				app_.sleep((int) (startYmm + fieldSizemmY) * stageSpeed);
				core_.waitForDevice("yAxis");
				core_.setFocusDevice("zAxis");
				af.fullFocus();
				core_.waitForDevice("zAxis");
				bottomLeftZ = core_.getPosition("zAxis");

				// back to the start
				displayUpdate("top-left");
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + 1000.0 * startXmm);
				core_.waitForDevice("xAxis");
				core_.setFocusDevice("yAxis");
				core_.setPosition(yStartPosUm - 1000.0 * startYmm);
				app_.sleep((int) (startYmm + fieldSizemmY) * stageSpeed);
				core_.waitForDevice("yAxis");
				core_.setFocusDevice("zAxis");
			} catch (MMScriptException e) {
				e.printStackTrace();
			} catch (MMException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * double topLeftZ = corners[0]; double topRightZ = corners[1]; double
		 * bottomLeftZ = corners[2]; double bottomRightZ = corners[3];
		 */
		double[] corners = { topLeftZ, topRightZ, bottomLeftZ, bottomRightZ };
		return corners;
	}

	@SuppressWarnings("static-access")
	public void acquireMirror(String rootDirName, double exposureTime,
			int numRepeats, int numFrames) {
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

		// make the Mirror folder
		new File(rootDirName + "\\Mirror\\").mkdir();
		File mir = new File(rootDirName + "\\Mirror\\");
		String today_date = new SimpleDateFormat("yyyy-MM-dd")
				.format(new Date());
		String saveName = "";

		for (int xi = 0; xi < nXSteps; xi++) {
			// move x-axis
			try {
				core_.setFocusDevice("xAxis");
				core_.setPosition(xStartPosUm + xi * stepSize);
				core_.waitForDevice("xAxis");
			} catch (Exception e) {
				e.printStackTrace();
			}
			for (int yi = 0; yi < nYSteps; yi++) {
				// move y-axis
				try {
					core_.setFocusDevice("yAxis");
					core_.setPosition(yStartPosUm - yi * stepSize);
					core_.waitForDevice("yAxis");
				} catch (Exception e) {
					e.printStackTrace();
				}

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
		// IJ.run("Image Sequence...", "open=[" + rootDirName +
		// "\\Mirror\\] sort");
		FolderOpener mirFolder = new FolderOpener(); // https://imagej.nih.gov/ij/developer/api/ij/plugin/FolderOpener.html
		ImagePlus stackFormatFiles = mirFolder.open(rootDirName + "\\Mirror\\");

		// IJ.run("Stack to Hyperstack...",
		// "order=xyczt(default) channels=4 slices=" + nXSteps * nYSteps
		// + " frames=1 display=Grayscale");
		HyperStackConverter hyperConverter = new HyperStackConverter();
		ImagePlus hypFormatFiles = hyperConverter
				.toHyperStack(stackFormatFiles, 4, nXSteps * nYSteps, 1,
						"xyczt", "grayscale");
		hypFormatFiles.show();

		IJ.run("Z Project...", "projection=Median");
		core_.sleep(10);
		hypFormatFiles.close();

		// TODO: Find more general way to do this
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
	}

	public void displayUpdate(String update) {
		updatePanel.removeAll();
		JLabel newUpdate = new JLabel(update);
		updatePanel.add(newUpdate);
		updatePanel.revalidate();
		updatePanel.repaint();
	}

	public static boolean deleteDirectory(File directory) {
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

}
