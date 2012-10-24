package com.addepar.fun;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class SayCheese {

  public static void main(String[] args) throws FrameGrabber.Exception {
    final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
    grabber.start();
    IplImage img = grabber.grab();
    if (img != null) {
      cvSaveImage("capture.jpg", img);
    }
  }
}
