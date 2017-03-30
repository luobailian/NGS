package com.celloud

import com.celloud.Core.TrimNewApi

/**
  * Created by luo on 2017/2/17.
  */
object FastQCTrimsApi {
  def main(args: Array[String]) {
    val isLocal = args(0).toBoolean

    assert(args.length>=7,"输入文件1，输入文件2，当前质量值-标准值<20，质量值长度截取条件，输出文件1，输出文件2,isLocal")
    //参数   输入文件1，输入文件2，当前质量值-标准值<20，质量值长度截取条件，输出文件1，输出文件2
    val inputpath =args(1)  //文件
    val inputpath2 =args(2) //文件
    val core_standard = args(3).toInt
    val len_standard = args(4).toInt-1
    val outputPath =args(5)
    val outputPath2 =args(6)

    TrimNewApi.LoadQCTrims(inputpath,inputpath2,core_standard,len_standard,outputPath,outputPath2,isLocal)
  }
}

