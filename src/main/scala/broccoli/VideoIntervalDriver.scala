package broccoli

import chisel3._
import chisel3.util._

/** Handles video timing.
  *
  * @param data
  *   the timing information to use
  */
class VideoIntervalDriver(data: VideoIntervalData) extends Module {
  final val xBlankBegin = data.width + data.hFrontPorch - 1
  final val xBlankEnd = data.width + data.hFrontPorch + data.hBlank - 1
  final val xMax = data.width + data.hFrontPorch + data.hBlank + data.hBackPorch - 1

  final val yBlankBegin = data.height + data.vFrontPorch - 1
  final val yBlankEnd = data.height + data.vFrontPorch + data.vBlank - 1
  final val yMax =
    data.height + data.vFrontPorch + data.vBlank + data.vBackPorch - 1

  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())

    val hSync = Output(Bool())
    val vSync = Output(Bool())

    val hBlankN = Output(Bool())
    val vBlankN = Output(Bool())

    val x = Output(UInt(12.W))
    val y = Output(UInt(12.W))
  })
  withReset(!io.aresetn) {
    val rX = RegInit(0.U(12.W))
    val rY = RegInit(0.U(12.W))

    when(!io.aresetn) {
      rX := 0.U(12.W)
    }.elsewhen(io.enable && (rX < xMax.U(12.W))) {
      rX := rX + 1.U(12.W)
    }.elsewhen(io.enable) {
      rX := 0.U(12.W)
    }

    when(!io.aresetn) {
      rY := 0.U(12.W)
    }.elsewhen(io.enable && !rX.orR && (rY < yMax.U(12.W))) {
      rY := rY + 1.U(12.W)
    }.elsewhen(io.enable && !rX.orR) {
      rY := 0.U(12.W)
    }

    if (data.hNegateSync) {
      io.hSync := (rX < xBlankBegin.U(12.W)) || (rX >= xBlankEnd.U(12.W))
    } else {
      io.hSync := !((rX < xBlankBegin.U(12.W)) || (rX >= xBlankEnd.U(12.W)))
    }

    if (data.vNegateSync) {
      io.vSync := (rY < yBlankBegin.U(12.W)) || (rY >= yBlankEnd.U(12.W))
    } else {
      io.vSync := !((rY < yBlankBegin.U(12.W)) || (rY >= yBlankEnd.U(12.W)))
    }

    io.hBlankN := rX < data.width.U(12.W)
    io.vBlankN := rY < data.height.U(12.W)

    io.x := rX
    io.y := rY
  }
}
