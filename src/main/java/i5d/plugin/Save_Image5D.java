package i5d.plugin;
//
// Save_Image5D.java
//

/*
Image5D plugins for 5-dimensional image stacks in ImageJ.

Copyright (c) 2010, Joachim Walter and ImageJDev.org.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

/* J. Walter 2005-09-25 
 * Saves an Image5D as TIFF file(s) .
 * Severely copied from ij.io.FileSaver. FileSaver is not used or inherited, because 
 * detailed changes to the dialog and behaviour are planned. */

import i5d.Image5D;
import i5d.cal.ChannelCalibration;
import i5d.cal.ChannelDisplayProperties;
import ij.IJ;
import ij.ImagePlus;
import ij.LookUpTable;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.SaveDialog;
import ij.io.TiffEncoder;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

import java.awt.image.ColorModel;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Save_Image5D implements PlugIn {

	/**
	 * Save the Image5D in TIFF format using a save file dialog. Returns false if
	 * the user selects cancel.
	 */
	@Override
	public void run(final String arg) {

		if (IJ.versionLessThan("1.35c")) return;

		final ImagePlus imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.error("No Image selected.");
			return;
		}
		else if (!(imp instanceof Image5D)) {
//            IJ.error("Image is not an Image5D.");
//            return;
		}

		String name = imp.getTitle();
		final SaveDialog sd = new SaveDialog("Save as Image5D", name, ".tif");
		name = sd.getFileName();
		if (name == null) return;
		final String directory = sd.getDirectory();
		imp.startTiming();

		saveAsImage5D(imp, name, directory);
	}

	/**
	 * Save the Image5D as a multi-image TIFF using the specified name and
	 * directory. It should be possible to call this method directly without
	 * deviation via the run() method.
	 */
	public boolean saveAsImage5D(final ImagePlus imp, final String name,
		final String directory)
	{
		if (imp == null) return false;
		final String path = directory + name;

		final FileInfo fi = imp.getFileInfo();
		final Object info = imp.getProperty("Info");
		if (info != null && (info instanceof String)) fi.info = (String) info;

//        if (fi.nImages==1)
//            {IJ.error("This is not a stack"); return false;}
		if (fi.pixels == null && imp.getStack().isVirtual()) {
			IJ.error("Save As Tiff", "Virtual stacks not supported.");
			return false;
		}

		// Get description string
		String description = (new FileSaver(imp)).getDescriptionString();

		// Reference slice labels
		fi.sliceLabels = imp.getImageStack().getSliceLabels();

		if (imp instanceof Image5D) {
			final Image5D i5d = (Image5D) imp;

			// Put a grayscale LUT into the fileinfo so that the image is also visible
			// when
			// opened by the "normal" Open command of ImageJ.
			final LookUpTable lut =
				new LookUpTable(LookUpTable.createGrayscaleColorModel(false));
			fi.lutSize = lut.getMapSize();
			fi.reds = lut.getReds();
			fi.greens = lut.getGreens();
			fi.blues = lut.getBlues();

			// Construct description string: remove trailing 0, add
			// Image5D=<version>\n, add trailing 0
			description = description.substring(0, description.length() - 1);
			description = description + "Image5D=" + Image5D.VERSION + "\n";
			description = description + "\0";
			fi.description = description;

			// Put Image5D specific metadata into ImageJ extra metadata array.
			// Entries: Color table and Min-/Max-Values for each channel.
			final int nChannelEntries = 7; // so far LUT, C&B, Threshold, displayed as
																			// gray?,
			// displayed in overlay?, Label, Calibration Function
			final int metadataSize = nChannelEntries * (i5d.getNChannels());
			final int[] metaDataTypes = new int[metadataSize];
			final byte[][] metaData = new byte[metadataSize][];

			// Make sure the channel properties are up to date.
			i5d.storeCurrentChannelProperties();

			int metadataCounter = 0;
			try {
				for (int c = 1; c <= i5d.getNChannels(); ++c) {
					final ByteArrayOutputStream bs = new ByteArrayOutputStream();
					final DataOutputStream ds = new DataOutputStream(bs);
					final ChannelCalibration chCalibration = i5d.getChannelCalibration(c);
					final ChannelDisplayProperties chDispProps =
						i5d.getChannelDisplayProperties(c);

					// LUT
					metaDataTypes[metadataCounter] = Open_Image5D.tagLUT;
					ds.writeInt(1); // Type: 1 for 768 byte RGB LUT, currently the only
													// supported one
					ds.writeInt(c);
					final ColorModel cm = chDispProps.getColorModel();
					for (int v = 0; v <= 255; ++v) {
						ds.writeByte(cm.getRed(v));
					}
					for (int v = 0; v <= 255; ++v) {
						ds.writeByte(cm.getGreen(v));
					}
					for (int v = 0; v <= 255; ++v) {
						ds.writeByte(cm.getBlue(v));
					}
					ds.flush();
					metaData[metadataCounter] = bs.toByteArray();
					metadataCounter++;
					bs.reset();

					// C&B
					metaDataTypes[metadataCounter] = Open_Image5D.tagCB;
					ds.writeInt(1); // Type: 1 for storing min- and max-value as doubles
					ds.writeInt(c);
					ds.writeDouble(chDispProps.getMinValue());
					ds.writeDouble(chDispProps.getMaxValue());
					ds.flush();
					metaData[metadataCounter] = bs.toByteArray();
					metadataCounter++;
					bs.reset();

					// Threshold
					metaDataTypes[metadataCounter] = Open_Image5D.tagTHR;
					ds.writeInt(1); // Type: 1
					ds.writeInt(c);
					ds.writeDouble(chDispProps.getMinThreshold());
					ds.writeDouble(chDispProps.getMaxThreshold());
					ds.writeInt(chDispProps.getLutUpdateMode());
					ds.flush();
					metaData[metadataCounter] = bs.toByteArray();
					metadataCounter++;
					bs.reset();

					// Displayed as grayscale?
					metaDataTypes[metadataCounter] = Open_Image5D.tagGRA;
					ds.writeInt(1); // Type: 1
					ds.writeInt(c);
					ds.writeBoolean(chDispProps.isDisplayedGray());
					ds.flush();
					metaData[metadataCounter] = bs.toByteArray();
					metadataCounter++;
					bs.reset();

					// Displayed in overlay?
					metaDataTypes[metadataCounter] = Open_Image5D.tagOVL;
					ds.writeInt(1); // Type: 1
					ds.writeInt(c);
					ds.writeBoolean(chDispProps.isDisplayedInOverlay());
					ds.flush();
					metaData[metadataCounter] = bs.toByteArray();
					metadataCounter++;
					bs.reset();

					// Channel Label
					if (chCalibration.getLabel() != null &&
						chCalibration.getLabel() != "")
					{
						metaDataTypes[metadataCounter] = Open_Image5D.tagLBL;
						ds.writeInt(1); // Type: 1
						ds.writeInt(c);
						ds.write(chCalibration.getLabel().getBytes());
						ds.flush();
						metaData[metadataCounter] = bs.toByteArray();
						metadataCounter++;
						bs.reset();
					}

					// Calibration Function
					if (chCalibration.getFunction() != Calibration.NONE) {
						metaDataTypes[metadataCounter] = Open_Image5D.tagCAL;
						ds.writeInt(1); // Type: 1
						ds.writeInt(c);
						ds.writeInt(chCalibration.getFunction());
						final boolean coefficientsNull =
							(chCalibration.getCoefficients() == null);
						if (!coefficientsNull) {
							ds.writeInt(chCalibration.getCoefficients().length);
							for (int n = 0; n < chCalibration.getCoefficients().length; n++) {
								ds.writeDouble(chCalibration.getCoefficients()[n]);
							}
						}
						else {
							ds.writeInt(0);
						}
						ds.writeBoolean(chCalibration.isZeroClip());
						ds.write(chCalibration.getValueUnit().getBytes());
						ds.flush();
						metaData[metadataCounter] = bs.toByteArray();
						metadataCounter++;
						bs.reset();
					}

					bs.close();
				}
			}
			catch (final IOException e) {}

			// Copy data to fileinfo
			fi.metaDataTypes = new int[metadataCounter];
			fi.metaData = new byte[metadataCounter][];
			for (int n = 0; n < metadataCounter; n++) {
				fi.metaDataTypes[n] = metaDataTypes[n];
				fi.metaData[n] = metaData[n];
			}

		}

		try {
			final TiffEncoder file = new TiffEncoder(fi);
			final DataOutputStream out =
				new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(path)));
			file.write(out);
			out.close();
		}
		catch (final IOException e) {
			showErrorMessage(e);
			return false;
		}
		updateImp(imp, fi, name, directory, FileInfo.TIFF);
		return true;
	}

	private void updateImp(final ImagePlus imp, final FileInfo fi,
		final String name, final String directory, final int fileFormat)
	{
		imp.changes = false;
		if (name != null) {
			fi.fileFormat = fileFormat;
			fi.fileName = name;
			fi.directory = directory;
			// TiffEncoder.IMAGE_START is not visible, just use current value (768)
			if (fileFormat == FileInfo.TIFF) fi.offset = 768;
			fi.description = null;
			imp.setTitle(name);
			imp.setFileInfo(fi);
		}
	}

	void showErrorMessage(final IOException e) {
		IJ.error("An error occured writing the file.\n \n" + e);
	}
}
