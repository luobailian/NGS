package com.celloud

import com.celloud.Core.callSnpPipe

/**
  * Created by luo on 2017/3/22.
  */
object callSnpPipeMain {
  def main(args: Array[String]) {
    assert(args.length >= 4, "是否本地, partition数; 输入文件1;  输出文件")
    //是否本地, partition数; 线程数; snap路径; 数据库; 输入文件1; 输入文件2, 输出文件
    val isLocal = args(0).toBoolean
    val partitionNum = args(1).toInt  //partition数量
    val samtoolsPath = args(2).replace("__"," ")
    val inputpath = args(3)
    val outputPath = args(4)
    callSnpPipe.LoadCallSnpPipe(isLocal,partitionNum,inputpath,samtoolsPath,outputPath)
  }
}
