package com.celloud.Utils

import org.apache.spark.Partitioner
import org.apache.spark.util.Utils

/**
  * Created by think on 2017/3/31.
  */
 class customPartitions(numParts:Int) extends  Partitioner{
  override def numPartitions:Int = numParts
  override def getPartition(key:Any):Int = key match {
    case null => 0
    case _ => key.toString.toInt
  }
}
