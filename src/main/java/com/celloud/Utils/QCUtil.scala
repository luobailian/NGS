package com.celloud.Utils
import java.text.DecimalFormat


import org.apache.spark.util.LongAccumulator

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
/**
  * Created by luo on 2017/3/6.
  */
object QCUtil {
  /*def getLineResultStringAC(resultMap : mutable.HashMap[Int, Int],valueSum: LongAccumulator ,listSize: LongAccumulator): (String,Double) = {
    //valueSum质量总值,
    var  currentSumPercent, currentSum: BigDecimal = 0
    var tag10, tag25, tag50, tag75, tag90: Double = 0.00
    //resultMap最终key为质量值;value为该质量值出现次数
    //var valueSum,listSize = 0.00
    val df = new DecimalFormat("#.00")
    resultMap.toList.sorted.reverse.foreach {
      case (key: Int, value: Int) =>
        listSize.add(value)
        valueSum.add(value * key)
    }

    val tagAvg =  df.format((valueSum.value.toDouble / listSize.value.toDouble).toDouble)
    //返回10,25,50,75,90,值的质量值
    resultMap.toList.sorted.reverse.foreach {
      case (key: Int, value: Int) =>
        //currentSum总次数返回第三个, resultMap=List((32,4), (27,1)) 返回值=32   4   4  5 返回值=27   1   5  5
        //key质量  value次数  currentSum总次数   listSize列数
        currentSum = currentSum + value
        currentSumPercent = currentSum / listSize
        if (currentSumPercent >= 0.5) {
          //50~100
          if (currentSumPercent >= 0.75) {
            //75~100
            if (currentSumPercent >= 0.9) {
              //90~100
              if (tag10 == 0) {
                tag10 = key
              }
              if (tag25 == 0) {
                tag25 = key
              }
              if (tag50 == 0) {
                tag50 = key
              }
              if (tag75 == 0) {
                tag75 = key
              }
              if (tag90 == 0) {
                tag90 = key
              }
            } else {
              //75~90
              if (tag10 == 0) {
                tag10 = key
              }
              if (tag25 == 0) {
                tag25 = key
              }
              if (tag50 == 0) {
                tag50 = key
              }
              if (tag75 == 0) {
                tag75 = key
              }
            }
          } else {
            //50~75
            if (tag10 == 0) {
              tag10 = key
            }
            if (tag25 == 0) {
              tag25 = key
            }
            if (tag50 == 0) {
              tag50 = key
            }
          }
        } else {
          //位点位置落在50以下
          if (currentSumPercent >= 0.25) {
            //25~50
            if (tag10 == 0) {
              tag10 = key
            }
            if (tag25 == 0) {
              tag25 = key
            }
          } else {
            //0~25
            if (currentSumPercent >= 0.1) {
              //10~25
              if (tag10 == 0) {
                tag10 = key
              }
            }
          }
        }
    }
    ((df.format(tag10) + Constant.spaceStr + df.format(tag25) + Constant.spaceStr + df.format(tag50)
      + Constant.spaceStr+ df.format(tag75)
      + Constant.spaceStr + df.format(tag90) + Constant.spaceStr + tagAvg),tagAvg.toDouble)
  }*/
  /**
    * 占比计算
    */
  def getLineResultString(resultMap : mutable.HashMap[Int, Int]): (String,Double) = {
    //valueSum质量总值,
    var  currentSumPercent, currentSum: BigDecimal = 0
    var tag10, tag25, tag50, tag75, tag90: Double = 0.00
    //resultMap最终key为质量值;value为该质量值出现次数
    var valueSum,listSize = 0.00
    val df = new DecimalFormat("#.00")
    resultMap.toList.sorted.reverse.foreach {
      case (key: Int, value: Int) =>
        listSize += value
        valueSum += value * key
    }
    val tagAvg =  df.format((valueSum / listSize).toDouble)
    //返回10,25,50,75,90,值的质量值
    resultMap.toList.sorted.reverse.foreach {
      case (key: Int, value: Int) =>
        //currentSum总次数返回第三个, resultMap=List((32,4), (27,1)) 返回值=32   4   4  5 返回值=27   1   5  5
        //key质量  value次数  currentSum总次数   listSize列数
        currentSum = currentSum + value
        currentSumPercent = currentSum / listSize
        if (currentSumPercent >= 0.5) {
          //50~100
          if (currentSumPercent >= 0.75) {
            //75~100
            if (currentSumPercent >= 0.9) {
              //90~100
              if (tag10 == 0) {
                tag10 = key
              }
              if (tag25 == 0) {
                tag25 = key
              }
              if (tag50 == 0) {
                tag50 = key
              }
              if (tag75 == 0) {
                tag75 = key
              }
              if (tag90 == 0) {
                tag90 = key
              }
            } else {
              //75~90
              if (tag10 == 0) {
                tag10 = key
              }
              if (tag25 == 0) {
                tag25 = key
              }
              if (tag50 == 0) {
                tag50 = key
              }
              if (tag75 == 0) {
                tag75 = key
              }
            }
          } else {
            //50~75
            if (tag10 == 0) {
              tag10 = key
            }
            if (tag25 == 0) {
              tag25 = key
            }
            if (tag50 == 0) {
              tag50 = key
            }
          }
        } else {
          //位点位置落在50以下
          if (currentSumPercent >= 0.25) {
            //25~50
            if (tag10 == 0) {
              tag10 = key
            }
            if (tag25 == 0) {
              tag25 = key
            }
          } else {
            //0~25
            if (currentSumPercent >= 0.1) {
              //10~25
              if (tag10 == 0) {
                tag10 = key
              }
            }
          }
        }
    }
    ((df.format(tag10) + Constant.spaceStr + df.format(tag25) + Constant.spaceStr + df.format(tag50)
      + Constant.spaceStr+ df.format(tag75)
      + Constant.spaceStr + df.format(tag90) + Constant.spaceStr + tagAvg),tagAvg.toDouble)
  }
}
