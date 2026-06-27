package com.example.receiptnormalization

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class YoloModel (context: Context) {

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModel(context))
        Log.d("TFLITE", interpreter.getOutputTensor(0).shape().joinToString())
        Log.d("TFLITE", interpreter.getOutputTensor(1).shape().joinToString())
    }

    // ---------------- MODEL ----------------

    private fun loadModel(context: Context): MappedByteBuffer {
        val fd = context.assets.openFd("best-seg26n_float32.tflite")
        val inputStream = FileInputStream(fd.fileDescriptor)
        val channel = inputStream.channel

        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    fun bitmapToTensor(bitmap: Bitmap): ByteBuffer {

        val resized = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

        val buffer = ByteBuffer.allocateDirect(4 * 1 * 3 * 640 * 640)
        buffer.order(ByteOrder.nativeOrder())
        //buffer.rewind()

        val pixels = IntArray(640 * 640)
        resized.getPixels(pixels, 0, 640, 0, 0, 640, 640)

        var index = 0

        for (y in 0 until 640) {
            for (x in 0 until 640) {

                val pixel = pixels[index++]

                buffer.putFloat((pixel and 0xFF) / 255f)          // B
                buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)  // G
                buffer.putFloat(((pixel shr 16) and 0xFF) / 255f) // R
            }
        }

        buffer.rewind()

        return buffer
    }

    // ---------------- INFERENCE ----------------

    fun runModel(
        bitmap: Bitmap
    ): Pair<Array<Array<FloatArray>>, Array<Array<Array<FloatArray>>>> {

        Log.d("MODEL_DEBUG","run model start")
        val input = bitmapToTensor(bitmap)

        val detShape = interpreter.getOutputTensor(0).shape()
        val protoShape = interpreter.getOutputTensor(1).shape()

//        val outputDet =
//            Array(detShape[0]) {
//                Array(detShape[1]) {
//                    FloatArray(detShape[2])
//                }
//            }
//
//        val outputProto =
//            Array(protoShape[0]) {
//                Array(protoShape[1]) {
//                    Array(protoShape[2]) {
//                        FloatArray(protoShape[3])
//                    }
//                }
//            }

        val outputDet = Array(1) {
            Array(300) {
                FloatArray(38)
            }
        }

        val outputProto = Array(1) {
            Array(160) {
                Array(160) {
                    FloatArray(32)
                }
            }
        }

        val outputs = mapOf(
            0 to outputDet,
            1 to outputProto
        )

        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        return Pair(outputDet, outputProto)
    }

    // ---------------- DETECTION ----------------

    fun matToBitmap(mat: Mat): Bitmap {
        val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bmp)
        return bmp
    }
    fun extractBestDetection(
        detections: Array<Array<FloatArray>>
    ): FloatArray? {

        var best: FloatArray? = null
        var bestScore = 0.4f

        for (d in detections[0]) {
            val score = d[4]
            //Log.d("YOLO_DEBUG", "score = $score")
            if (score > bestScore) {
                bestScore = score
                best = d
            }
        }

        Log.d("YOLO_DEBUG", "bestScore = $bestScore")

        return best
    }

    // ---------------- MASK ----------------

    fun buildMaskFromProto(
        coeffs: FloatArray,
        proto: Array<Array<Array<FloatArray>>>
    ): Mat {

        val h = 160
        val w = 160
        val c = 32

        val mask = Mat(h, w, CvType.CV_32F)

        for (y in 0 until h) {
            for (x in 0 until w) {

                var sum = 0f

                for (i in 0 until c) {
                    val p = proto[0][y][x][i]
                    sum += coeffs[i] * p
                }

                val valSigmoid = (1f / (1f + exp(-sum)))
                mask.put(y, x, valSigmoid.toDouble())
            }
        }

        return mask
    }

    // ---------------- OPEN CV PIPELINE ----------------

    fun process(bitmap: Bitmap): Mat? {
        val startTime = System.currentTimeMillis()
        Log.d("YOLO_DEBUG", "process start")

        var width = bitmap.width/2
        var height = bitmap.height/2
        val safeBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

//        val safeBitmap = bitmap
        Log.d("YOLO_DEBUG", "bitmap config = ${safeBitmap.config}")
        val (det, proto) = runModel(safeBitmap)
        Log.d("YOLO_DEBUG", "model finished")
        val best = extractBestDetection(det) ?: return null

        if (best.size < 37) return null

        val coeffs = best.sliceArray(6 until 38) // YOLO-seg coeffs

        if (proto[0].isEmpty()) return null

        val mask = buildMaskFromProto(coeffs, proto)
        Log.d("YOLO_DEBUG", "mask ready")
        val resized = Mat()
        Imgproc.resize(mask, resized, Size(safeBitmap.width.toDouble(), safeBitmap.height.toDouble()))

        val thresh = Mat()
        val mask8 = Mat()
        resized.convertTo(mask8, CvType.CV_8U, 255.0)

        Imgproc.threshold(mask8, thresh, 127.0, 255.0, Imgproc.THRESH_BINARY)


        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        val closed = Mat()
        Imgproc.morphologyEx(thresh, closed, Imgproc.MORPH_CLOSE, kernel)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            closed,
            contours,
            Mat(),
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        if (contours.isEmpty()) return null

        //нове
        val filtered = contours.filter {
            Imgproc.contourArea(it) > 5000
        }

        if (filtered.isEmpty()) return null
        //кінець
        val biggest = filtered.maxByOrNull { Imgproc.contourArea(it) } ?: return null

        val approx = MatOfPoint2f(*biggest.toArray())
        val epsilon = 0.02 * Imgproc.arcLength(approx, true)
        val approxCurve = MatOfPoint2f()

        Imgproc.approxPolyDP(approx, approxCurve, epsilon, true)

        val points = approxCurve.toArray()
        Log.d("YOLO_DEBUG", "contour area = ${Imgproc.contourArea(biggest)}")
        val quad = if (points.size == 4) {
            points
        } else {
            Imgproc.minAreaRect(MatOfPoint2f(*biggest.toArray()))
                .let {
                    val pts = arrayOfNulls<Point>(4)
                    it.points(pts)
                    pts.requireNoNulls()
                }
        }
        val mat = Mat()
        Utils.bitmapToMat(safeBitmap, mat)

        val endTime = System.currentTimeMillis()
        Log.d(
            "YOLO_TIME",
            "Processing time = ${endTime - startTime} ms"
        )

        return warpPerspective(mat, quad)
    }

    // ---------------- SAFE ORDER ----------------

    private fun orderPoints(points: Array<Point>): Array<Point> {

        val sorted = points.sortedBy { it.x + it.y }

        val tl = sorted.first()
        val br = sorted.last()

        val rest = points.filter { it != tl && it != br }

        val tr = rest.maxByOrNull { it.x - it.y }!!
        val bl = rest.minByOrNull { it.x - it.y }!!

        return arrayOf(tl, tr, br, bl)
    }

    // ---------------- WARP ----------------

    private fun warpPerspective(original: Mat, quad: Array<Point>): Mat {

        val ordered = orderPoints(quad)

        val (tl, tr, br, bl) = ordered

        val widthTop = hypot(tr.x - tl.x, tr.y - tl.y)
        val widthBottom = hypot(br.x - bl.x, br.y - bl.y)
        val width = (widthTop + widthBottom) / 2

        val heightLeft = hypot(bl.x - tl.x, bl.y - tl.y)
        val heightRight = hypot(br.x - tr.x, br.y - tr.y)
        val height = (heightLeft + heightRight) / 2

        Log.d("YOLO_DEBUG", "warp size = $width x $height")

        val src = MatOfPoint2f(*ordered)
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(width, 0.0),
            Point(width, height),
            Point(0.0, height)
        )

        val m = Imgproc.getPerspectiveTransform(src, dst)

        val out = Mat()
        Imgproc.warpPerspective(original, out, m, Size(width, height))

        val resizedOut = Mat()

        Imgproc.resize(
            out,
            resizedOut,
            Size(width * 2.0, height * 2.0)
        )

        return resizedOut
//        return out
    }
}