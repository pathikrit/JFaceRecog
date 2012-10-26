package lib;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizer;
import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizerPtr;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.MatVector;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.googlecode.javacv.cpp.opencv_contrib.createLBPHFaceRecognizer;
import static com.googlecode.javacv.cpp.opencv_core.CV_32SC1;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateMat;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvLoad;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_INTER_AREA;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

public class FacialRecognition {

  /**
   * Result returned by FacialRecognition
   */
  public static class PotentialFace {
    public final Rectangle box;
    public final String name;
    public final double confidence;  // confidence that face at r is name

    protected PotentialFace(Rectangle box, String name, double confidence) {
      this.box = box;
      this.name = name;
      this.confidence = confidence;
    }

    @Override
    public String toString() {
      return String.format("%s found at (%d,%d) with confidence = %s", name, box.x, box.y, confidence);
    }
  }

  private static Map<FaceDb, FaceRecognizer> algorithmCache = Maps.newConcurrentMap();

  /**
   * This is the only public method of this class
   * If db is not null it tries to identify faces given db
   * Else if db is null, it simply does facial detection and not recognition
   */
  public static synchronized List<PotentialFace> run(BufferedImage image, FaceDb db) {
    final FaceRecognizer algorithm;
    final Map<Integer, String> names;
    if (canDoFacialRecognition(db)) {
      algorithm = algorithmPtr.get();
      names = train(algorithm, db); // TODO!!! This is expensive and thus should be cached on disk using algorithm.save
    } else {
      algorithm = null;
      names = null;
    }

    final List<PotentialFace> faces = Lists.newArrayList();
    for(Rectangle r : detectFaces(image)) {
      final PotentialFace face;
      if (algorithm == null) {
        face = new PotentialFace(r, null, Double.NaN);
      } else {
        final BufferedImage candidate = image.getSubimage(r.x, r.y, r.width, r.height);
        final IplImage iplImage = toTinyGray(candidate, scale);
        final int[] prediction = new int[1];
        final double[] confidence = new double[1];
        algorithm.predict(iplImage, prediction, confidence);
        confidence[0] = 100*(THRESHHOLD - confidence[0])/THRESHHOLD;
        face = new PotentialFace(r, names.get(prediction[0]), confidence[0]);
      }
      faces.add(face);
    }
    return faces;
  }

  /**
   * code from here: http://stackoverflow.com/questions/11913980/
   */
  private static Map<Integer, String> train(FaceRecognizer algorithm, FaceDb db) {
    final int numberOfImages = db.size();
    final MatVector images = new MatVector(numberOfImages);
    final CvMat labels = cvCreateMat(1, numberOfImages, CV_32SC1);
    final Map<Integer, String> names = Maps.newHashMap();

    int imgCount = 0, personCount = 0;
    for(String name : db.names()) {
      for (BufferedImage image : db.get(name)) {
        images.put(imgCount, toTinyGray(image, scale));
        labels.put(imgCount, personCount);
        imgCount++;
      }
      names.put(personCount++, name);
    }

    algorithm.train(images, labels);
    return names;
  }

  // We can try out different algorithms here: http://docs.opencv.org/trunk/modules/contrib/doc/facerec/facerec_api.html
  private static final Double THRESHHOLD = 100d;
  private static final FaceRecognizerPtr algorithmPtr =
      createLBPHFaceRecognizer(1, 8, 8, 8, THRESHHOLD);
      //createFisherFaceRecognizer(0, THRESHHOLD).get(),
      //createEigenFaceRecognizer(0, THRESHHOLD).get()

  private static final CvHaarClassifierCascade classifier;
  static {
    final File classifierFile;
    try {
      classifierFile = Loader.extractResource("haarcascade_frontalface_alt.xml", null, "classifier", ".xml");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Loader.load(opencv_objdetect.class); // Preload the opencv_objdetect module to work around a known bug.
    classifier = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
  }

  private static final CvMemStorage storage = CvMemStorage.create();
  private static final int F = 4; // scaling factor
  private static final Pair<Integer, Integer> scale = Pair.of(100, 100);

  /**
   * This does facial detection and NOT facial recognition
   */
  private static synchronized List<Rectangle> detectFaces(BufferedImage image) {
    cvClearMemStorage(storage);
    final CvSeq cvSeq = cvHaarDetectObjects(toTinyGray(image, null), classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
    final int N = cvSeq.total();
    final List<Rectangle> ret = Lists.newArrayListWithCapacity(N);
    for (int i = 0; i < N; i++) {
      CvRect r = new CvRect(cvGetSeqElem(cvSeq, i));
      ret.add(new Rectangle(r.x()* F, r.y()* F, r.width()* F, r.height()* F));
    }
    return ret;
  }

  /**
   * Images should be grayscaled and scaled-down for faster calculations
   */
  private static IplImage toTinyGray(BufferedImage image, Pair<Integer, Integer> scale) {
    final IplImage iplImage = IplImage.createFrom(image);
    if (scale == null) {
      scale = Pair.of(iplImage.width()/F, iplImage.height()/F);
    }
    final IplImage gray = IplImage.create(iplImage.width(), iplImage.height(), IPL_DEPTH_8U, 1);
    final IplImage tiny = IplImage.create(scale.getLeft(), scale.getRight(), IPL_DEPTH_8U, 1);
    cvCvtColor(iplImage, gray, CV_BGR2GRAY);   //todo: do tiny before gray
    cvResize(gray, tiny, CV_INTER_AREA);
    return tiny;
  }

  private static boolean canDoFacialRecognition(FaceDb db) {
    return db != null && db.size() > 0;
  }
}
