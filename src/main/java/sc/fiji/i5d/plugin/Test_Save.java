package sc.fiji.i5d.plugin;
//
// Test_Save.java
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

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.SaveDialog;
import ij.io.TiffEncoder;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * Created on 21.08.2005
 *
 */

/**
 * For testing ImageJ extensions for saving extra Metadata
 * 
 * @author Joachim Walter
 */
public class Test_Save implements PlugIn {

	ImagePlus imp;
	FileInfo fi;
	String name, directory;

	String extraDescriptionEntries = "testentry1=dodo\ntestentry2=dudu\n";
	int[] extraMetaDataTypes = { 0x004c5554, 0x004c5554 }; // \0LUT
	byte[][] extraMetaData = { (new String("jajaja")).getBytes(),
		(new String("neinnein")).getBytes() };

	@Override
	public void run(final String args) {

		imp = WindowManager.getCurrentImage();
		fi = imp.getOriginalFileInfo();
		if (fi == null) {
			fi = imp.getFileInfo();
		}
		final String path = getPath("TIFF", ".tif");
		if (path == null) return;
		final Object info = imp.getProperty("Info");
		if (info != null && (info instanceof String)) fi.info = (String) info;
		else fi.info = "empty info";

		fi.metaDataTypes = extraMetaDataTypes;
		fi.metaData = extraMetaData;

		if (imp.getStackSize() == 1) saveAsTiff(path);
		else saveAsTiffStack(path);
	}

//
//From here on: methods copied and slightly modified from ij.io.FileSaver
//
	/** Save the image in TIFF format using the specified path. */
	public boolean saveAsTiff(final String path) {
		fi.nImages = 1;
		fi.description = getDescriptionString();
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
//	updateImp(fi, fi.TIFF);
		return true;
	}

	/** Save the stack as a multi-image TIFF using the specified path. */
	public boolean saveAsTiffStack(final String path) {
		if (fi.nImages == 1) {
			IJ.error("This is not a stack");
			return false;
		}
		if (fi.pixels == null && imp.getStack().isVirtual()) {
			IJ.error("Save As Tiff", "Virtual stacks not supported.");
			return false;
		}
		fi.description = getDescriptionString();
		fi.sliceLabels = imp.getStack().getSliceLabels();
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
//	updateImp(fi, fi.TIFF);
		return true;
	}

	void showErrorMessage(final IOException e) {
		IJ.error("An error occured writing the file.\n \n" + e);
	}

	/** Returns a string containing information about the specified image. */
	String getDescriptionString() {
		final StringBuffer sb = new StringBuffer(100);
		sb.append("ImageJ=" + ImageJ.VERSION + "\n");
		if (fi.nImages > 1) sb.append("images=" + fi.nImages + "\n");
		final int channels = imp.getNChannels();
		if (channels > 1) sb.append("channels=" + channels + "\n");
		final int slices = imp.getNSlices();
		if (slices > 1) sb.append("slices=" + slices + "\n");
		final int frames = imp.getNFrames();
		if (frames > 1) sb.append("frames=" + frames + "\n");
		if (fi.unit != null) sb.append("unit=" + fi.unit + "\n");
		if (fi.valueUnit != null) {
			sb.append("cf=" + fi.calibrationFunction + "\n");
			if (fi.coefficients != null) {
				for (int i = 0; i < fi.coefficients.length; i++)
					sb.append("c" + i + "=" + fi.coefficients[i] + "\n");
			}
			sb.append("vunit=" + fi.valueUnit + "\n");
			final Calibration cal = imp.getCalibration();
			if (cal.zeroClip()) sb.append("zeroclip=true\n");
		}

		// get stack z-spacing and fps
		if (fi.nImages > 1) {
			if (fi.pixelDepth != 0.0 && fi.pixelDepth != 1.0) sb.append("spacing=" +
				fi.pixelDepth + "\n");
			if (fi.frameInterval != 0.0) {
				final double fps = 1.0 / fi.frameInterval;
				if ((int) fps == fps) sb.append("fps=" + (int) fps + "\n");
				else sb.append("fps=" + fps + "\n");
			}
		}

		// get min and max display values
		final ImageProcessor ip = imp.getProcessor();
		final double min = ip.getMin();
		final double max = ip.getMax();
		final int type = imp.getType();
		final boolean enhancedLut =
			(type == ImagePlus.GRAY8 || type == ImagePlus.COLOR_256) &&
				(min != 0.0 || max != 255.0);
		if (enhancedLut || type == ImagePlus.GRAY16 || type == ImagePlus.GRAY32) {
			sb.append("min=" + min + "\n");
			sb.append("max=" + max + "\n");
		}

		// get non-zero origins
		final Calibration cal = imp.getCalibration();
		if (cal.xOrigin != 0.0) sb.append("xorigin=" + cal.xOrigin + "\n");
		if (cal.yOrigin != 0.0) sb.append("yorigin=" + cal.yOrigin + "\n");
		if (cal.zOrigin != 0.0) sb.append("zorigin=" + cal.zOrigin + "\n");
		if (cal.info != null && cal.info.length() <= 64 &&
			cal.info.indexOf('=') == -1 && cal.info.indexOf('\n') == -1) sb
			.append("info=" + cal.info + "\n");

		// add extra Description
		sb.append(extraDescriptionEntries); // JW
		sb.append((char) 0);
		return new String(sb);
	}

	String getPath(final String type, final String extension) {
		name = imp.getTitle();
		final SaveDialog sd = new SaveDialog("Save as " + type, name, extension);
		name = sd.getFileName();
		if (name == null) return null;
		directory = sd.getDirectory();
		imp.startTiming();
		final String path = directory + name;
		return path;
	}

}
