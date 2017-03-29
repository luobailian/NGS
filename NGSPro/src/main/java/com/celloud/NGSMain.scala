package com.celloud

import com.celloud.Core.NGSCore
/**
  * Created by luo on 2017/3/9.
  */
object NGSMain {
  def main(args: Array[String]) {
    val isLocal = args(0).toBoolean //是否本地
    val partitionNum = args(1).toInt
    val core_standard = args(2).toInt
    val len_standard = args(3).toInt - 1
    val threadsNumber = args(4).toInt  //线程数
    val snapPath = args(5).replace("__"," ")   //可以用|线隔开
    val databasepath = args(6)
     val inputpath = args(7) //文件1
    val inputpath2 = args(8) //文件
    val outputQC =args(9)
    val samtoolsPath=args(10).replace("__"," ")
    val outputpath =args(11)
    assert(args.length >= 11, "isLocal; partitionNum;core_standard; len_standard; threadsNumber; " +
      "snapPath; databasepath;" +
      "inputpath1; inputpath2; outputQC; samtoolsPath; outputpath")
    NGSCore.NGSCalculation(isLocal,partitionNum,core_standard, len_standard, threadsNumber,
      snapPath,databasepath,inputpath, inputpath2, outputQC,samtoolsPath,outputpath)
  }
}
