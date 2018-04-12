#include <opencv2\opencv.hpp>
#include <opencv2\highgui\highgui.hpp>
#include <opencv2\imgproc\imgproc.hpp>
#include <opencv2\objdetect.hpp>

#include <iostream>
#include <vector>

#define FACE_CASCADE "D:\Software\opencv\sources\data\haarcascades\haarcascade_frontalface_alt2.xml"
#define MASK_1 "1.JPG"
#define MASK_2 "5.JPG"
#define MASK_3 "11.JPG"

using namespace std;

cv::Mat mask;
cv::Mat frame;
char input = 0;
double minFaceSize = 20.0;
double maxFaceSize = 200.0;
cv::Mat findFace(cv::Mat src);
cv::Mat filterMe(cv::Mat src, cv::Point& centre, cv::Size& face_size);

int main(int argc, char** argv) {
	cv::VideoCapture cap;
	if (argc == 1)
		cap.open(0);
	else
		cap.open(argv[1]);

	if (!cap.isOpened()) {
		std::cerr << "Couldn't open capture." << std::endl;
		exit(1);
	}

	mask = cv::imread(MASK_1);
	cv::namedWindow("FilterMe", CV_WINDOW_AUTOSIZE);

	for (;;) {
		cap >> frame;
		frame = findFace(frame);
		cv::imshow("FilterMe", frame);
		cv::waitKey(15);
		/*input = (char)cv::waitKey(33);
		if (input == 49)
			mask = cv::imread(MASK_1);
		else if (input == 50)
			mask = cv::imread(MASK_2);
		else if (input == 51)
			mask = cv::imread(MASK_3);
		else if (input == 52)
			mask = cv::imread(MASK_4);
		else
			break;*/
	}

	return 0;
}

cv::Mat findFace(cv::Mat src) {
	// Load face cascade
	cv::CascadeClassifier faceRec;
	faceRec.load("haarcascade_frontalface_alt2.xml");

	// Find faces
	std::vector<cv::Rect> faces;
	faceRec.detectMultiScale(
		src,
		faces,
		1.2,
		2,
		CV_HAAR_SCALE_IMAGE,
		cv::Size(minFaceSize, minFaceSize),
		cv::Size(maxFaceSize, maxFaceSize)
	);

	// draw circles on the detected faces
	for (int i = 0; i < 1/*faces.size()*/; i++) {
		// track only first face it finds
		minFaceSize = faces[0].width * 0.7;
		maxFaceSize = faces[0].height * 1.5;
		cv::Point centre(faces[i].x + faces[i].width * 0.5,
			faces[i].y + faces[i].height * 0.55);
		src = filterMe(src, centre, cv::Size(faces[i].width, faces[i].height));
	}
	return src;
}

cv::Mat filterMe(cv::Mat src, cv::Point& centre, cv::Size& faceSize) {
	cv::Mat mask1, src1;
	cv::resize(mask, mask1, faceSize);

	// ROI selection
	cv::Rect roi(centre.x - faceSize.width / 2, centre.y - faceSize.width / 2, faceSize.width, faceSize.width);
	src(roi).copyTo(src1);

	// make white region of mask transparent
	cv::Mat mask2, m1, m2;
	cv::cvtColor(mask1, mask2, CV_BGR2GRAY);
	cv::threshold(mask2, mask2, 230, 255, CV_THRESH_BINARY_INV);

	std::vector<cv::Mat> maskChannels(3), resultMask(3);
	cv::split(mask1, maskChannels);
	cv::bitwise_and(maskChannels[0], mask2, resultMask[0]);
	cv::bitwise_and(maskChannels[1], mask2, resultMask[1]);
	cv::bitwise_and(maskChannels[2], mask2, resultMask[2]);
	cv::merge(resultMask, m1); //cv::imshow("m1", m1)

	mask2 = 255 - mask2;
	std::vector<cv::Mat> srcChannels(3);
	cv::split(src1, srcChannels);
	cv::bitwise_and(srcChannels[0], mask2, resultMask[0]);
	cv::bitwise_and(srcChannels[1], mask2, resultMask[1]);
	cv::bitwise_and(srcChannels[2], mask2, resultMask[2]);
	cv::merge(resultMask, m2); //cv::imshow("m2", m2);

	cv::addWeighted(m1, 1, m2, 1, 0, m2);

	m2.copyTo(src(roi));

	return src;
}