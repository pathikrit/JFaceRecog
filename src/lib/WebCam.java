package lib;

import java.awt.image.BufferedImage;

import com.google.common.base.Throwables;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;

public class WebCam {

  private final int CAMERA_NUMBER = 0; // 0 is built-in, 1 is USB
  private final FrameGrabber grabber = initGrabber();

  public void start() {
    try {
      grabber.start();
    } catch (FrameGrabber.Exception e) {
      Throwables.propagate(e);
    }
  }

  public BufferedImage capture() {
    try {
      return grabber.grab().getBufferedImage();
    } catch (FrameGrabber.Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void stop() {
    try {
      grabber.stop();
      grabber.release();
    } catch (FrameGrabber.Exception e) {
      Throwables.propagate(e);
    }
  }

  private FrameGrabber initGrabber() {
    try {
      return FrameGrabber.createDefault(CAMERA_NUMBER);
    } catch (Exception e) {
      return new OpenCVFrameGrabber(CAMERA_NUMBER);
    }
  }
}
