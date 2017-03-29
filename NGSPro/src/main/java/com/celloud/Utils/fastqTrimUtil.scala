package com.celloud.Utils

import org.apache.hadoop.io.Text
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer

object fastqTrimUtil {
  def readlines(lines: String,accu:ArrayBuffer[String],linecounts:Int): (Int,String) = {
    var linecount =linecounts
    var resline = ""
    if (lines == "+") {
      if (accu.length > 2) {
        accu.remove(0, accu.length - 2)
      }
      if (accu.length == 2) {
        linecount = 3
      }
    } else {
      linecount += 1
      accu.append(lines.toString())
      if (linecount % 4 == 0) {
        //质量值
        resline = accu(0).toString + Constant.linefeed  + accu(1).toString + Constant.linefeed + "+"+ Constant.linefeed  + accu(2).toString
        accu.clear() //清除

      }
    }
    (linecount,resline)
  }
  def readlinesByKey(lines: String,accu:ArrayBuffer[String],linecounts:Int): (Int,String,String) = {
    var linecount =linecounts
    var reshead,resline = ""
    if (lines == "+") {
      if (accu.length > 2) {
        accu.remove(0, accu.length - 2)
      }
      if (accu.length == 2) {
        linecount = 3
      }
    } else {
      linecount += 1
      accu.append(lines.toString())
      if (linecount % 4 == 0) {
        val heads = accu(0).toString
        val bases = accu(1).toCharArray //碱基
        val scoreLineArrayPair = accu(2).toCharArray //质量
        accu.clear() //清除
        if(heads.contains(Constant.underline)){
          reshead=heads.split(Constant.underline)(0)//下横线
        } else if(heads.contains(Constant.slash)){
          reshead=heads.split(Constant.slash)(0) //斜线
        }else{
          reshead=heads.split(Constant.spaceStr)(0) //空格
        }
        resline =heads + Constant.linefeed  + bases + Constant.linefeed + "+"+ Constant.linefeed  + scoreLineArrayPair
      }
    }
    (linecount,reshead,resline)
  }

  //加trims计算的读取
  def readAndTrimsCalculation(lines: String,accu:ArrayBuffer[String],linecounts:Int,standCoreChar: Int,
  core_standard: Int, len_standard: Int): (Int,String,String) = {
    var linecount =linecounts
    var reshead,resline = ""
    if (lines == "+") {
      if (accu.length > 2) {
        accu.remove(0, accu.length - 2)
      }
      if (accu.length == 2) {
        linecount = 3
      }
    } else {
      linecount += 1
      accu.append(lines.toString())
      if (linecount % 4 == 0) {
        val heads = accu(0).toString
        val bases = accu(1).toCharArray //碱基
        val scoreLineArrayPair = accu(2).toCharArray //质量
        accu.clear() //清除
        var lastCharPair = "";
        var secondCharPair = ""
        var flagPair = true
        var counterPair1 = 0
        while (flagPair && counterPair1 < scoreLineArrayPair.length) {
          if (scoreLineArrayPair(counterPair1) - standCoreChar < core_standard) {
            //如果当前质量值-标准值<20,即满足截取条件
            if (counterPair1 != 0 && counterPair1 > len_standard) {
              //如果质量值长度已经满足30个的条件
              lastCharPair = scoreLineArrayPair.subSequence(0, counterPair1).toString //质量截取
              secondCharPair = bases.subSequence(0, counterPair1).toString //碱基截取
            }
            flagPair = false
          } else if (counterPair1 == scoreLineArrayPair.length - 1 && counterPair1 > len_standard) {
            //全部满足的情况,全部截取
            lastCharPair = scoreLineArrayPair.subSequence(0, counterPair1 + 1).toString
            secondCharPair = bases.subSequence(0, counterPair1 + 1).toString
            flagPair = false
          } else {
            counterPair1 += 1
          }
        }
        //质量值
        resline = heads + Constant.linefeed + secondCharPair + Constant.linefeed + "+" + Constant.linefeed + lastCharPair
        if(heads.contains(Constant.underline)){
          reshead=heads.split(Constant.underline)(0)//下横线
        } else if(heads.contains(Constant.slash)){
          reshead=heads.split(Constant.slash)(0) //斜线
        }else{
          reshead=heads.split(Constant.spaceStr)(0) //空格
        }
      }
    }
    (linecount,reshead,resline)
  }
//计算行操作
def readCompte(x:String, standCoreChar: Int,
               core_standard: Int, len_standard: Int): String = {
  val arraysplit = x.toString().split(Constant.linefeed)
  val bases = arraysplit(1).toCharArray //碱基
  val scoreLineArrayPair = arraysplit(3).toCharArray //质量
  var lastCharPair =  "";
  var secondCharPair = ""
  var flagPair = true
  var counterPair1 = 0
  while (flagPair && counterPair1 < scoreLineArrayPair.length) {
    if (scoreLineArrayPair(counterPair1) - standCoreChar < core_standard) {
      //如果当前质量值-标准值<20,即满足截取条件
      if (counterPair1 != 0 && counterPair1 > len_standard) {
        //如果质量值长度已经满足30个的条件
        lastCharPair = scoreLineArrayPair.subSequence(0, counterPair1).toString //质量截取
        secondCharPair = bases.subSequence(0, counterPair1).toString //碱基截取
      }
      flagPair = false
    } else if (counterPair1 == scoreLineArrayPair.length - 1 && counterPair1 > len_standard) {
      //全部满足的情况,全部截取
      lastCharPair = scoreLineArrayPair.subSequence(0, counterPair1 + 1).toString
      secondCharPair = bases.subSequence(0, counterPair1 + 1).toString
      flagPair = false
    } else {
      counterPair1 += 1
    }
  }
  if (lastCharPair.equals("")) "!!"
  else arraysplit(0) + Constant.linefeed + secondCharPair + Constant.linefeed + "+" + Constant.linefeed + lastCharPair
}

  def getheadspilt(file: Array[String],linefeed:String): String = {
    val heads = file(0).toString.split(linefeed)(0)
    //头分隔符
    var headspilt = ""
    if (heads.contains(Constant.underline)) headspilt = Constant.underline//下横线
    else if (heads.contains(Constant.slash)) headspilt = Constant.slash//斜线
   else   headspilt = Constant.spaceStr//空格

    headspilt
  }
  //打分
def getStandardScore(file: Array[String]): Int = {
var flag = true
var standCoreChar = 0
val lineNum = file.length
var currentPos = 1
while (standCoreChar == 0 && currentPos <= lineNum) {
  val array = file.take(currentPos).last
  val arraysplit =  array.toString.split(Constant.linefeed)
  val scoreArray = arraysplit(2).toCharArray //质量
  var counter = 0
  while (flag && counter < scoreArray.length) {
    val score =scoreArray(counter)
    if ( score  < '5') {
      standCoreChar = '!'
      flag = false
    } else if (scoreArray(counter) > 'T') {
      standCoreChar = '@'
      flag = false
    } else {
      counter = counter + 1
    }
  }
  currentPos = currentPos + 1
}
standCoreChar
}
/**
* @author yuyang
* @since 20160127
* @note get the score standard
*/
def getStandardScoreAPI(file: RDD[(Text, Text)]): Int = {

val lineNum = file.count()
var currentPos = 1
var standCoreChar = 0

while(standCoreChar == 0 && currentPos <= lineNum){
  val one = file.take(currentPos).last._2.toString.split(Constant.tab)(3)
  val scoreArray = one.toCharArray
  var flag = true
  var counter = 0

  while (flag && counter < scoreArray.length) {
    if (scoreArray(counter) < '5') {//�������ֵС��53
      standCoreChar = '!'//����ֵ��Ϊ33
      flag = false
    } else if (scoreArray(counter) > 'T') {//������84
      standCoreChar = '@'//����Ϊ64
      flag = false
    } else {
      counter = counter + 1
    }
  }
  currentPos = currentPos + 1
}
standCoreChar
}
}
