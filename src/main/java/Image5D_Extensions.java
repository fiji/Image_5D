//
// Image5D_Extensions.java
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
import i5d.gui.ChannelControl;
import ij.IJ;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.macro.ExtensionDescriptor;
import ij.plugin.PlugIn;

/**
 * Macro extensions for Image5D.
 *
 * @author Shannon Stewman
 */
public class Image5D_Extensions implements PlugIn, MacroExtension {

  @Override
  public void run(String arg) {
    if (!IJ.macroRunning()) {
      IJ.error("Cannot install extensions from outside a macro!");
      return;
    }
    
    Functions.registerExtensions(this);
  }
  
  private ExtensionDescriptor[] extensions = {
      ExtensionDescriptor.newDescriptor("getChannel", this, ARG_OUTPUT+ARG_NUMBER),
      ExtensionDescriptor.newDescriptor("getFrame", this, ARG_OUTPUT+ARG_NUMBER),
      ExtensionDescriptor.newDescriptor("setChannel", this, ARG_NUMBER ),
      ExtensionDescriptor.newDescriptor("setFrame", this, ARG_NUMBER ),
      ExtensionDescriptor.newDescriptor("getDisplayMode", this),
      ExtensionDescriptor.newDescriptor("setDisplayMode", this, ARG_STRING),
  };

  @Override
  public ExtensionDescriptor[] getExtensionFunctions() {
    return extensions;
  }

  @Override
  public String handleExtension(String name, Object[] args) {
    if (!(IJ.getImage() instanceof Image5D)) {
      IJ.error("Current image is not an Image5D");
    }
    
    Image5D im5d = (Image5D) IJ.getImage();
    
    if (name.equals("getChannel")) {
      int[] pos = im5d.getCurrentPosition();
      ((Double[]) args[0])[0] = new Double(pos[im5d.getColorDimension()]+1);
    } else if (name.equals("setChannel")) {
      int ch = ( (Double) args[0] ).intValue();
      im5d.setChannel(ch);
    } else if (name.equals("getFrame")) {
      int[] pos = im5d.getCurrentPosition();
      ((Double[]) args[0])[0] = new Double(pos[4]+1);
    } else if (name.equals("setFrame")) {
      int fr = ( (Double) args[0] ).intValue();
      im5d.setFrame(fr);
    } else if (name.equals("getDisplayMode")) {
      int mode = im5d.getDisplayMode();
      switch (mode) {
      case ChannelControl.ONE_CHANNEL_COLOR:
        return "color";
      case ChannelControl.ONE_CHANNEL_GRAY:
        return "gray";
      case ChannelControl.OVERLAY:
        return "overlay";
      default:
        return "unknown";  
      }
    } else if (name.equals("setDisplayMode")) {
      String arg = (String)args[0];
      if (arg.equals("color")) {
        im5d.setDisplayMode(ChannelControl.ONE_CHANNEL_COLOR);
      } else if (arg.equals("gray")) {
        im5d.setDisplayMode(ChannelControl. ONE_CHANNEL_GRAY);
      } else if (arg.equals("overlay")) {
        im5d.setDisplayMode(ChannelControl.OVERLAY);
      } else {
        IJ.error("Illegal display mode value: "+arg);
        return null;
      }
    }
    
    return null;
  }

}
