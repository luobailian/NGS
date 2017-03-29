package com.celloud

import com.celloud.Core.FastQCTrimsJoin

/**
  * Created by luo on 2017/3/15.
  */
object FastQCTrimsJoinMain {
  def main(args: Array[String]) {

    val isLocal = args(0).toBoolean //是否本地
    val QCandTrim = args(1).toInt //0表示全部,1表示QC,2表示Trims
    if (QCandTrim.equals(1)) {
      assert(args.length >= 5, "isLocal,0表示全部,1表示QC,2表示Trims;输入文件;输出文件;分区数量")
      val inputpath = args(2) //文件
      val outputQC = args(3)
      val partitionNum = args(4).toInt
      FastQCTrimsJoin.LoadTrims(inputpath, "", 0, 0, "", "", outputQC, isLocal, QCandTrim,partitionNum)
    } else if (QCandTrim.equals(2)) {
      assert(args.length >= 9, "isLocal,0全部,1表QC,2表Trims;输入文件1;输入文件2;当前质量值-标准值<20;" +
        "质量值长度截取条件;输出文件1;输出文件2;分区数量")
      val inputpath = args(2) //文件
      val inputpath2 = args(3) //文件
      val core_standard = args(4).toInt
      val len_standard = args(5).toInt - 1
      val outputPath = args(6)
      val outputPath2 = args(7)
      val partitionNum = args(8).toInt
      FastQCTrimsJoin.LoadTrims(inputpath, inputpath2, core_standard, len_standard, outputPath, outputPath2, "", isLocal,
        QCandTrim,partitionNum)
    } else {
      assert(args.length >= 10, "isLocal,0表示全部,1表示QC,2表示Trims;输入文件1;输入文件2;当前质量值-标准值<20;" +
        "质量值长度截取条件;输出文件1;输出文件2,输出文件QC,分区数量")
      val inputpath = args(2) //文件
      val inputpath2 = args(3) //文件
      val core_standard = args(4).toInt
      val len_standard = args(5).toInt - 1
      val outputPath = args(6)
      val outputPath2 = args(7)
      val outputQC = args(8)
      val partitionNum = args(9).toInt
      FastQCTrimsJoin.LoadTrims(inputpath, inputpath2, core_standard, len_standard, outputPath, outputPath2, outputQC,
        isLocal, QCandTrim,partitionNum)
    }

  }
}
