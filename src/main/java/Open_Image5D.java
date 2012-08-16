//
// Open_Image5D.java
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

/* J. Walter 2005-09-24 
 * Opens a TIFF file as Image5D.
 * Severely copied from ij.io.Opener. Opener is not used or inherited, because 
 * detailed changes to the dialog and behaviour are planned. */

import i5d.Image5D;
import i5d.cal.ChannelCalibration;
import i5d.cal.ChannelDisplayProperties;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.TiffDecoder;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Open_Image5D implements PlugIn {

	// 0x004c5554 (LUT) for channel lookup tables
	// Structure: int type (1 for 768 byte RGB LUT)
	// int channel number (channel, whose LUT this entry is, starting from 1)
	// 768 bytes: LUT for R, G, B in ascending order
	public static final int tagLUT = 0x004c5554;
	public static final int tagCB = 0x00432642; // "C&B" for contrast and
																							// brightness settings
	public static final int tagTHR = 0x00544852;
	public static final int tagGRA = 0x00475241;
	public static final int tagOVL = 0x004f564c;
	public static final int tagLBL = 0x004c424c;
	public static final int tagCAL = 0x0043414c;

	@Override
	public void run(final String arg) {
		IJ.register(Open_Image5D.class);

		if (IJ.versionLessThan("1.35c")) return;

		// Get filename and path
		final OpenDialog od = new OpenDialog("Open Image5D", "");
		final String directory = od.getDirectory();
		final String name = od.getFileName();
		if (name != null) {

			// TODO: analyze name for -c01z001t001 pattern.
			// Load images in loop according to this pattern.

			// Copied from Opener.openTIFF()
			final TiffDecoder td = new TiffDecoder(directory, name);
			if (IJ.debugMode) td.enableDebugging();
			FileInfo[] info = null;
			try {
				info = td.getTiffInfo();
			}
			catch (final IOException e) {
				String msg = e.getMessage();
				if (msg == null || msg.equals("")) msg = "" + e;
				IJ.error("TiffDecoder", msg);
				return;
			}
			if (info == null) return;

			switch (info[0].fileType) {
				case FileInfo.GRAY8:
				case FileInfo.COLOR8:
				case FileInfo.BITMAP:
				case FileInfo.GRAY16_SIGNED:
				case FileInfo.GRAY16_UNSIGNED:
				case FileInfo.GRAY32_INT:
				case FileInfo.GRAY32_UNSIGNED:
				case FileInfo.GRAY32_FLOAT:
					break;
				default: // Unhandled cases, e.g. RGB images
					IJ.error("Unsupported image type.");
					return;
			}

			// Copied from Opener.openTIFF2()
			ImagePlus imp = null;
			if (info.length > 1) { // try to open as stack
				imp = (new Opener()).openTiffStack(info);
			}
			else { // Single image or ImageJ-stack
				final FileOpener fo = new FileOpener(info[0]);
				imp = fo.open(false);
			}

			if (imp != null) {
				// get dimensions:
				String title = imp.getTitle();
				final int nChannels = imp.getNChannels();
				final int nSlices = imp.getNSlices();
				final int nFrames = imp.getNFrames();

				// Create Image5D with the loaded image data.
				final Image5D i5d =
					new Image5D(title, imp.getImageStack(), nChannels, nSlices, nFrames);

				// Copy over the calibration (pixel width, height, depth, frame
				// interval).
				i5d.setCalibration(imp.getCalibration().copy());
				final boolean[] hasLUT = new boolean[nChannels];

				// Read MetaData, if image is an Image5D
				final String description = info[0].description;
				if (description != null && description.length() >= 7 &&
					description.startsWith("ImageJ"))
				{

					final Properties props = new Properties();
					final InputStream is =
						new ByteArrayInputStream(description.getBytes());
					try {
						props.load(is);
						is.close();
					}
					catch (final IOException e) {
						IJ.error("Exception reading properties: " + e.getMessage());
					}

					if (props.getProperty("Image5D", null) != null) {
						final int[] metaDataTypes = info[0].metaDataTypes;
						final byte[][] metaData = info[0].metaData;

						final ChannelCalibration[] chCalibration =
							new ChannelCalibration[nChannels];
						final ChannelDisplayProperties[] chDispProps =
							new ChannelDisplayProperties[nChannels];
						for (int c = 1; c <= nChannels; c++) {
							chCalibration[c - 1] = new ChannelCalibration();
							chDispProps[c - 1] = new ChannelDisplayProperties();
						}

						if (metaDataTypes != null) {
							for (int n = 0; n < metaDataTypes.length; ++n) {
								// Copy Scaling
								// Copy Calibrations
								// Fill ChannelDisplayProperties
								try {
									final int tag = metaDataTypes[n];
									int entryType;
									final ByteArrayInputStream bs =
										new ByteArrayInputStream(metaData[n]);
									final DataInputStream ds = new DataInputStream(bs);
									switch (tag) {
										case tagLUT:
											entryType = ds.readInt();
											switch (entryType) {
												case 1:
													final int channel = ds.readInt();
													if (channel < 1 || channel > nChannels) break;
													final byte[] rLut = new byte[256];
													final byte[] gLut = new byte[256];
													final byte[] bLut = new byte[256];
													ds.read(rLut);
													ds.read(gLut);
													ds.read(bLut);
													chDispProps[channel - 1]
														.setColorModel(new IndexColorModel(8, 256, rLut,
															gLut, bLut));
													hasLUT[channel - 1] = true;
													break;
											}
											break;
										case tagCB:
											entryType = ds.readInt();
											switch (entryType) {
												case 1:
													final int channel = ds.readInt();
													if (channel < 1 || channel > nChannels) break;
													chDispProps[channel - 1].setMinValue(ds.readDouble());
													chDispProps[channel - 1].setMaxValue(ds.readDouble());
													break;
											}
											break;
										case tagTHR:
											entryType = ds.readInt();
											switch (entryType) {
												case 1:
													final int channel = ds.readInt();
													if (channel < 1 || channel > nChannels) break;
													chDispProps[channel - 1].setMinThreshold(ds
														.readDouble());
													chDispProps[channel - 1].setMaxThreshold(ds
														.readDouble());
													chDispProps[channel - 1].setLutUpdateMode(ds
														.readInt());
													break;
											}
											break;
										case tagGRA:
											entryType = ds.readInt();
											switch (entryType) {
												case 1:
													final int channel = ds.readInt();
													if (channel < 1 || channel > nChannels) break;
													chDispProps[channel - 1].setDisplayedGray(ds
														.readBoolean());
													break;
											}
											break;
										case tagOVL:
											entryType = ds.readInt();
											switch (entryType) {
												case 1:
													final int channel = ds.readInt();
													if (channel < 1 || channel > nChannels) break;
													chDispProps[channel - 1].setDisplayedInOverlay(ds
														.readBoolean());
													break;
											}
											break;
										case tagLBL:
											entryType = ds.readInt();
											switch (entryType) {
												case 1:
													final int channel = ds.readInt();
													if (channel < 1 || channel > nChannels) break;
													final byte[] temp = new byte[metaData[n].length - 8];
													ds.read(temp);
													chCalibration[channel - 1].setLabel(new String(temp));
													break;
											}
											break;
										case tagCAL:
											entryType = ds.readInt();
											switch (entryType) {
												case 1:
													final int channel = ds.readInt();
													if (channel < 1 || channel > nChannels) break;
													final int funct = ds.readInt();
													final int num = ds.readInt();
													final double[] coeff = new double[num];
													for (int i = 0; i < num; i++) {
														coeff[i] = ds.readDouble();
													}
													final boolean zeroClip = ds.readBoolean();
													final byte[] temp =
														new byte[metaData[n].length - 4 * 4 - num * 8 - 1];
													ds.read(temp);
													chCalibration[channel - 1].setFunction(funct, coeff,
														new String(temp), zeroClip);
													break;
											}
											break;
									}
									bs.close();
								}
								catch (final IOException e) {
									IJ.log("Exception reading metadata entry: " + n + " tag: " +
										metaDataTypes[n] + "\n" + e.getMessage());
								}
							}

							// write ChannelDisplayProperties to Image5D
							for (int c = 1; c <= nChannels; c++) {
								i5d.setChannelCalibration(c, chCalibration[c - 1]);
								i5d.setChannelDisplayProperties(c, chDispProps[c - 1]);
								i5d.restoreChannelProperties(c);
							}
						}
					}
				}

				// Wenn keine LUTs vorhanden: apply default colormap
				for (int c = 0; c < nChannels; ++c) {
					if (!hasLUT[c]) {
						i5d.setChannelColorModel(c + 1, ChannelDisplayProperties
							.createModelFromColor(Color.getHSBColor(1f / nChannels * c, 1f,
								1f)));
					}
				}

				// Prune trailing .tif or .tiff from the title of TIFF images
				title = i5d.getTitle();
				final int tLength = title.length();
				if (title.substring(tLength - 4, tLength).equalsIgnoreCase(".tif")) title =
					title.substring(0, tLength - 4);
				else if (title.substring(tLength - 5, tLength)
					.equalsIgnoreCase(".tiff")) title = title.substring(0, tLength - 5);
				i5d.setTitle(title);

				i5d.setCurrentPosition(0, 0, 0, 0, 0);
				i5d.show();
			}
			else {
				// error message
			}
		}
		else {
			// error message
		}

	}

}
