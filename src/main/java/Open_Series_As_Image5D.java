//
// Open_Series_As_Image5D.java
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

import ij.*;
import ij.plugin.*;

/*
 * Created on 29.05.2005
 */

/** Opens a series of images or image stacks and converts it to an Image5D. 
 * Calls first the HyperVolumeOpener plugin, then the Stack_to_Image5D plugin.
 * 
 * @author Joachim Walter
 */
public class Open_Series_As_Image5D implements PlugIn {

	public void run(String arg) {
	    if (IJ.versionLessThan("1.34p")) return;
        
        ImagePlus imp1 = WindowManager.getCurrentImage();
        int id=0;
        if (imp1!=null)
            id = imp1.getID();
	    
        Hypervolume_Opener h = new Hypervolume_Opener();
	    h.run("");
	    
        // If no new image opened, return.
        ImagePlus imp2 = WindowManager.getCurrentImage();
        if (imp2==null || imp2.getID()==id)
            return;
        
	    Stack_to_Image5D s = new Stack_to_Image5D();
	    s.run("");
	}
}
