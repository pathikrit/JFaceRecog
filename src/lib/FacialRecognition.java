package lib;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.base.Throwables;
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
    public String name;        // name of person at box - null if unidentified
    public double confidence;  // confidence that face at box is name - NaN if name is null

    static PotentialFace newUnidentifiedFace(Rectangle box) {
      return new PotentialFace(box, null, Double.NaN);
    }

    PotentialFace(Rectangle box, String name, double confidence) {
      this.box = box;
      this.name = name;
      this.confidence = confidence;
    }

    @Override
    public String toString() {
      return String.format("%s found at (%d,%d) with confidence = %s", name, box.x, box.y, confidence);
    }
  }

  /**
   * Does facial recognition (or detection only if db is null or empty)
   */
  public static synchronized List<PotentialFace> run(BufferedImage image, FaceDb db) {
    final List<PotentialFace> faces = detectFaces(image);

    if (db != null && db.size() > 0) {
      if (!trainingCache.containsKey(db)) {
        System.out.println("Training ...");
        trainingCache.put(db, new Training(db));
      }
      final Training training = trainingCache.get(db);

      for(PotentialFace face : faces) {
        training.identify(image, face);
      }
    }

    return faces;
  }

  private static Map<FaceDb, Training> trainingCache = Maps.newConcurrentMap();

  static void invalidateTrainingCache(FaceDb db) {
    trainingCache.remove(db);
  }

  private FacialRecognition() {
    // no one can construct me - I only have one public static method - run
  }

  private static class Training {
    // We can try out different algorithms here: http://docs.opencv.org/trunk/modules/contrib/doc/facerec/facerec_api.html
    private static final Double THRESHHOLD = 80d;
    private static final FaceRecognizerPtr ALGO_FACTORY =
        com.googlecode.javacv.cpp.opencv_contrib.createLBPHFaceRecognizer(1, 8, 8, 8, THRESHHOLD);
        //com.googlecode.javacv.cpp.opencv_contrib.createFisherFaceRecognizer(0, THRESHHOLD);
        //com.googlecode.javacv.cpp.opencv_contrib.createEigenFaceRecognizer(0, THRESHHOLD);
    private static final Pair<Integer, Integer> scale = Pair.of(100, 100);

    private final Map<Integer, String> names = Maps.newHashMap();
    private final FaceRecognizer algorithm;

    /**
     * Creating new trainings are VERY expensive and should be always cached
     * http://stackoverflow.com/questions/11913980/
     */
    Training(FaceDb db) {
      final int numberOfImages = db.size();
      final MatVector images = new MatVector(numberOfImages);
      final CvMat labels = cvCreateMat(1, numberOfImages, CV_32SC1);

      int imgCount = 0, personCount = 0;
      for(String name : db.names()) {
        for (BufferedImage image : db.get(name)) {
          images.put(imgCount, toTinyGray(image, scale));
          labels.put(imgCount, personCount);
          imgCount++;
        }
        names.put(personCount++, name);
      }

      this.algorithm = ALGO_FACTORY.get();
      algorithm.train(images, labels);
    }

    /**
     * Identify the face in bounding box r in image
     */
    void identify(BufferedImage image, PotentialFace face) {
      final Rectangle r = face.box;
      final BufferedImage candidate = image.getSubimage(r.x, r.y, r.width, r.height);
      final IplImage iplImage = toTinyGray(candidate, scale);
      final int[] prediction = new int[1];
      final double[] confidence = new double[1];
      algorithm.predict(iplImage, prediction, confidence);
      face.name = names.get(prediction[0]);
      face.confidence = 100*(THRESHHOLD - confidence[0])/THRESHHOLD;
    }
  }

  private static final CvMemStorage storage = CvMemStorage.create();
  private static final int F = 4; // scaling factor
  private static final CvHaarClassifierCascade classifier;
  static {
    final File classifierFile;
    try {
      classifierFile = Loader.extractResource("haarcascade_frontalface_alt.xml", null, "classifier", ".xml");
      classifier = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * This does facial detection and NOT facial recognition
   */
  private static synchronized List<PotentialFace> detectFaces(BufferedImage image) {
    cvClearMemStorage(storage);
    final IplImage iplImage = toTinyGray(image, null);
    final CvSeq cvSeq = cvHaarDetectObjects(iplImage, classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
    final int N = cvSeq.total();
    final List<PotentialFace> ret = Lists.newArrayListWithCapacity(N);
    for (int i = 0; i < N; i++) {
      final CvRect r = new CvRect(cvGetSeqElem(cvSeq, i));
      final Rectangle box = new Rectangle(r.x() * F, r.y() * F, r.width() * F, r.height() * F);
      ret.add(PotentialFace.newUnidentifiedFace(box));
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
}
