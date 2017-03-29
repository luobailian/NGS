package com.celloud.algorithms

import org.apache.spark.SparkContext

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
/**
  * Created by luo on 2017/2/17.
  */
class HMM extends Serializable{
  //马尔可夫模型前向计算    ref,read,quality数组,返回BAQ值
  def beforeCompute(refcontent:Array[Char], readcontent:Array[Char], qualityArray:Array[Char], qvalue:Int, midse:Array[Array[Double]]): (ArrayBuffer[Double],Double) ={
    val beforeBAQ = new ArrayBuffer[Double]
    var returnendvalue =0.00

    val refArrayLength=refcontent.length+1   //ref长度
    val readArrayLength=readcontent.length+1   //read长度

    //初始化马尔可夫模型矩阵
    val matrix_M,matrix_I,matrix_D = Array.ofDim[Double](readArrayLength,refArrayLength)
    for(i <- 1 to refArrayLength-1){
      val  ref= refcontent(i-1).toString()  //ref碱基
      val eki = ATCGEki("M",(qualityArray(0)-qvalue).toDouble,ref, readcontent(0).toString())
      matrix_M(1)(i)=eki*midse(3)(0) //前向值
      matrix_I(1)(i)=0.25* midse(3)(1)
    }
    beforeBAQ +=  (matrix_M(1)(1))
    //计算BAQ
    var i = 2 ;var status="M"
    while(i < readArrayLength){
      var k = 1
      var read=""; var quality=0
      if(readcontent(i-1).equals("_")) {//read为_,当前ref与read下一个比
        status = "D"
        read = readcontent(i-2).toString() //read碱基
        quality = qualityArray(i-2) - qvalue //read质量
      } else {
        read = readcontent(i-1).toString() //read碱基
        quality = qualityArray(i-1) - qvalue //read质量
      }
      if(refcontent(i-1).equals("_")){
        status="I"
      }
      while(k < refArrayLength){
        var ref =""
        if(refcontent(k-1).equals("_")){
          ref= refcontent(k-2).toString()  //ref为_,ref减1与read比较
        }else{
          ref= refcontent(k-1).toString()  //ref碱基
        }
        val eki = ATCGEki(status,quality.toDouble,ref, read)//前向eki
        //计算前向M
        matrix_M(i)(k)= eki*((matrix_M(i-1)(k-1)* midse(0)(0))+ (matrix_I(i-1)(k-1)* midse(1)(0))+ (matrix_D(i-1)(k-1)*midse(2)(0)))
        //计算前向I
        matrix_I(i)(k)=((matrix_M(i-1)(k)* midse(0)(1))+ (matrix_I(i-1)(k)* midse(1)(1)))*0.25
        //计算前向D
        matrix_D(i)(k)=matrix_M(i)(k-1)* midse(0)(2)+ matrix_D(i)(k-1)* midse(2)(2)
        k += 1
      }
      if(status=="M"){
        beforeBAQ += matrix_M(i)(i)
      }
      //最后一行计算总和
      if(i == readArrayLength-1){
        //计算前向end的和
        var computcount =0
        while(computcount<refArrayLength){
          val endbasem = matrix_M(i)(computcount)//前向最后一项的值
          val endbasei = matrix_I(i)(computcount)//前向最后一项的值
          val backic =endbasem*midse(0)(4)+ endbasei*midse(1)(4)
          returnendvalue += backic  //返回总和
          computcount += 1
        }
      }
      i+=1
    }
    (beforeBAQ,returnendvalue)
  }
  //马尔可夫模型后向计算
  def backCompute(refcontent:Array[Char], readcontent:Array[Char], qualityArray:Array[Char], qvalue:Int, midse:Array[Array[Double]]): ArrayBuffer[Double] ={
    val backBAQ = new ArrayBuffer[Double]
    val refArrayLength=refcontent.length+1   //ref长度
    val readArrayLength=readcontent.length+1   //read长度

    //初始化马尔可夫模型矩阵
    val matrix_M,matrix_I,matrix_D = Array.ofDim[Double](readArrayLength,refArrayLength)
    for(i <- 1 to refArrayLength-1){
      matrix_M(readArrayLength-1)(refArrayLength-i)= midse(0)(4)   //blmk=a04
      matrix_I(readArrayLength-1)(refArrayLength-i)= midse(1)(4)  //blik=a14
    }
    backBAQ +=  (matrix_M(readArrayLength-1)(readArrayLength-1))
    //计算后向
    var j = readArrayLength-2; var status="M"
    while (j > -1){
      var k =refArrayLength-2
      var read=""; var quality=0
      if(readcontent(j).equals("_")) {//read为_,当前ref与read下一个比
        status = "D"
        read = readcontent(j+1).toString() //read碱基
        quality = qualityArray(j+1) - qvalue //read质量
      } else {
        read = readcontent(j).toString() //read碱基
        quality = qualityArray(j).toInt-qvalue  //质量
      }
      if(refcontent(j).equals("_")){
        status="I"
      }
      while(k > -1){
        //后向eki
        var ref =""
        if(refcontent(k).equals("_")){
          ref= refcontent(k+1).toString()  //ref为_,ref减1与read比较
        }else{
          ref= refcontent(k).toString()  //ref碱基
        }
        val eki = ATCGEki(status,quality.toDouble,ref, read)

        //后向M
        val fmc = (eki *matrix_M(j+1)(k+1)*midse(0)(0))+ (matrix_I(j+1)(k)*midse(0)(1)*0.25)+ (matrix_D(j)(k+1)*midse(0)(2))
        matrix_M(j)(k)= fmc

        //后向I
        val fic =(eki*matrix_M(j+1)(k+1)*midse(1)(0))+ (matrix_I(j+1)(k)* midse(1)(1)*0.25)
        matrix_I(j)(k)=fic
        //后向D
        val dic =(eki*matrix_M(j+1)(k+1)* midse(2)(0))+ (matrix_D(j)(k+1)* midse(2)(2))
        matrix_D(j)(k)=dic

        k += -1
      }
      if(status=="M"){
        println(j)
        backBAQ += matrix_M(j)(j)
      }
      j += -1
    }
    backBAQ
  }
  //ATCG碱基模型MID Q质量值
  def ATCGEki(Status: String,Q: Double,ref: String, reads : String) : Double = {
    var eki =0.00
    val p = scala.math.pow(10.0, -Q/10.0)
    if(ref.trim()==reads.trim()){
      //1-错误率
      eki  = 1-p
    }else{
      eki =p/3
    }
    eki
  }
  //MIDSE矩阵  l样本长度,L标准长度
  def midseMatrix(l: Double, refl: Double):  Array[Array[Double]] = {
    val midse = Array.ofDim[Double](5,5)
    val am =0.001
    val bm=0.1

    val t = 2.00*l
    val r = 1.00/t   //r=1/2l
    //第一行
    midse(0)(0)= (1.00- 2.00*am).toDouble*(1-r).toDouble  // (1-2a)*(1-r)
    midse(0)(1)=am*(1.00-r)     //  a(1-r)
    midse(0)(2)=am*(1.00-r)    //  a(1-r)
    midse(0)(4)=r      //r

    midse(1)(0) =  (1.00-bm)*(1.00-r)  //(1-b)(1-r)
    midse(1)(1) = bm*(1.00-r).toDouble    //b(1-r)
    midse(1)(4)=r              //r

    midse(2)(0) = 1.00-bm.toDouble   //1-b
    midse(2)(2) =  bm.toDouble                   //b
    //S转M
    midse(3)(0)= (1.00-am)/refl   //(1-a)/L
    midse(3)(1)= am/refl  //a/L
    midse
  }
}
object HMM {




}
