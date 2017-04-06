package com.celloud

import com.celloud.Core.FastQCTrims
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by luo on 2017/3/3.
  */
object QCTrimsMain {
  def main(args: Array[String]) {
    val isLocal = args(0).toBoolean //是否本地
    val QCandTrim = args(1).toInt //0表示全部,1表示QC,2表示Trims
    if (QCandTrim.equals(1)) {
      if (args.length != 5) {
        System.err.println("isLocal; 0:all,1:QC,2:Trims; inputpath; outputQC; partitionNum")
        System.exit(1)
      }
      val inputpath = args(2) //文件
      val outputQC = args(3)
      val partitionNum = args(4).toInt
      FastQCTrims.LoadTrims(inputpath, "", 0, 0, "", "", outputQC, isLocal, QCandTrim,partitionNum)
    } else if (QCandTrim.equals(2)) {
      if (args.length != 9) {
        System.err.println("isLocal, 0:all,1:QC,2:Trims; inputpath1;inputpath2; Standard quality;" +
          "Intercept condition; output1; output2; partitionNum")
        System.exit(1)
      }
      val inputpath = args(2) //文件
      val inputpath2 = args(3) //文件
      val core_standard = args(4).toInt
      val len_standard = args(5).toInt - 1
      val outputPath = args(6)
      val outputPath2 = args(7)
      val partitionNum = args(8).toInt
      FastQCTrims.LoadTrims(inputpath, inputpath2, core_standard, len_standard, outputPath, outputPath2, "", isLocal,
        QCandTrim,partitionNum)
    } else {
      if (args.length != 10) {
        System.err.println("isLocal, 0:all,1:QC,2:Trims; inputpath1;inputpath2; Standard quality;" +
          "Intercept condition; output1; output2, outputQC, partitionNum")
        System.exit(1)
      }
      val inputpath = args(2) //文件
      val inputpath2 = args(3) //文件
      val core_standard = args(4).toInt
      val len_standard = args(5).toInt - 1
      val outputPath = args(6)
      val outputPath2 = args(7)
      val outputQC = args(8)
      val partitionNum = args(9).toInt
      FastQCTrims.LoadTrims(inputpath, inputpath2, core_standard, len_standard, outputPath, outputPath2, outputQC,
        isLocal, QCandTrim,partitionNum)
    }

  }
}
