//
// Image5D_Stack_to_RGB.java
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

import i5d.Image5D;

import ij.*;
import ij.gui.ImageCanvas;
import ij.measure.*;
import ij.plugin.*;
import ij.process.*;

import java.awt.Graphics;

/** Converts the current timeframe of an Image5D to an RGB stack using the current 
 * view settings.
 * @author Joachim Walter
 */
public class Image5D_Stack_to_RGB implements PlugIn {

    @Override
    public void run(String arg) {
        ImagePlus currentImage = WindowManager.getCurrentImage();
        if (currentImage==null) {
            IJ.noImage();
            return;
        }
        if (!(currentImage instanceof Image5D)) {
            IJ.error("Image is not an Image5D.");
            return;
        }
        
        String title = currentImage.getTitle();
        int width = currentImage.getWidth();
        int height = currentImage.getHeight();
        int depth = currentImage.getNSlices();
        int currentSlice = currentImage.getCurrentSlice();
        Calibration cal = currentImage.getCalibration().copy();
        
        
        currentImage.killRoi();
        
        ImagePlus rgbImage = IJ.createImage(title+"-RGB", "RGB black", width, height, 1);
        ImageStack rgbStack = rgbImage.getStack();
        
        ImageCanvas canvas = currentImage.getCanvas();
        Graphics imageGfx = canvas.getGraphics();
        for (int i=1; i<=depth; i++) {
            currentImage.setSlice(i);

            // HACK: force immediate (synchronous) repaint of image canvas
            if (currentImage instanceof Image5D) {
              ((Image5D) currentImage).updateImageAndDraw();
            }
            currentImage.updateAndDraw();
            canvas.paint(imageGfx);

            currentImage.copy(false);
            
            ImagePlus rgbClip = ImagePlus.getClipboard();
            if (rgbClip.getType()!=ImagePlus.COLOR_RGB)
                new ImageConverter(rgbClip).convertToRGB();
            if (i>1) {
                rgbStack.addSlice(currentImage.getStack().getSliceLabel(i), 
                        rgbClip.getProcessor().getPixels());
            } else {
                rgbStack.setPixels(rgbClip.getProcessor().getPixels(), 1);
                rgbStack.setSliceLabel(currentImage.getStack().getSliceLabel(1), 1);
            }
        }
        imageGfx.dispose();

        currentImage.setSlice(currentSlice);
        rgbImage.setStack(null, rgbStack);
        rgbImage.setSlice(currentSlice);
        rgbImage.setCalibration(cal);
        
        rgbImage.killRoi();
        rgbImage.show();
    }

}
