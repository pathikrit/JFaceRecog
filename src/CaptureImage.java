import com.googlecode.javacv.OpenCVFrameGrabber;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class CaptureImage {
  private static void captureFrame() {
  // 0-default camera, 1 - next...so on
  final OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
  try {
    grabber.start();
    IplImage img = grabber.grab();
    if (img != null) {
    cvSaveImage("capture.jpg", img);
    }
  } catch (Exception e) {
    e.printStackTrace();
  }
  }
  public static void main(String[] args) {
  captureFrame();
  }
}
