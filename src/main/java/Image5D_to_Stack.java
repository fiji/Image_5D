//
// Image5D_to_Stack.java
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
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class Image5D_to_Stack implements PlugIn {

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
        
        ImageStack currentImageStack = currentImage.getImageStack();
        
        // Copy references to pixel arrays to new image. Don't just copy the reference to the stack,
        // because the stack is disassembled when the currentImage is flushed.
        ImagePlus newImage = new ImagePlus(currentImage.getTitle(), currentImageStack.getProcessor(1));
        ImageStack newStack = newImage.getStack();
        newStack.setSliceLabel(currentImageStack.getSliceLabel(1), 1);       
        for (int i=2; i<=currentImage.getImageStackSize(); i++) {
            newStack.addSlice(currentImageStack.getSliceLabel(i), currentImageStack.getPixels(i));
        }
        newImage.setStack(null, newStack);
        
        newImage.setDimensions(currentImage.getNChannels(), currentImage.getNSlices(), currentImage.getNFrames());
        newImage.setCalibration(currentImage.getCalibration().copy());
        
        newImage.getProcessor().resetMinAndMax();
        newImage.show(); 
        
        currentImage.getWindow().close();

        if(newImage.getWindow() != null)
            WindowManager.setCurrentWindow(newImage.getWindow());
    }

}
