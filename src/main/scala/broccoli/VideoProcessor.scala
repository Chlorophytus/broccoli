package broccoli

import chisel3._
import chisel3.util._

/** Handles framebuffer and DVI output duties.
  *
  * NOTE: This module should be clocked at 25.175MHz, 0° phase
  */
class VideoProcessor extends Module {
  final val LANE_BLU = 0
  final val LANE_GRN = 1
  final val LANE_RED = 2

  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val clockD0 = Input(Clock()) // lane clock, 125.875MHz, 0° phase
    val clockD90 = Input(Clock()) // DDR clock, 125.875MHz, -90° phase

    val tmdsOBlu = Output(Bool())
    val tmdsOGrn = Output(Bool())
    val tmdsORed = Output(Bool())
  })
  withReset(!io.aresetn) {
    val mDriver = Module(new VideoIntervalDriver(new VideoIntervalData {
      val width = 640
      val height = 480
      val vFrontPorch = 10
      val hFrontPorch = 16
      val vBlank = 2
      val hBlank = 96
      val vBackPorch = 33
      val hBackPorch = 48
      val vNegateSync = true
      val hNegateSync = true
    }))
    val mLanes = for (i <- (0 to 2)) yield {
      Module(new TMDSLane)
    }

    mDriver.clock := clock
    mDriver.io.aresetn := io.aresetn
    mDriver.io.enable := io.enable

    for ((lane, n) <- mLanes.zipWithIndex) {
      lane.io.clockP := clock
      lane.clock := io.clockD0
      lane.io.clockD90 := io.clockD90

      lane.io.aresetn := io.aresetn
      lane.io.enable := io.enable
      lane.io.tmdsIDataEnable := mDriver.io.hBlankN && mDriver.io.vBlankN
      lane.io.tmdsIData := Cat(mDriver.io.x(4, 0) ^ mDriver.io.y(4, 0), 0.U(4.W))

      if (n == LANE_BLU) {
        lane.io.tmdsIControl := Cat(mDriver.io.vSync, mDriver.io.hSync)
      } else {
        lane.io.tmdsIControl := 0.U(2.W)
      }
    }

    io.tmdsOBlu := mLanes(LANE_BLU).io.tmdsOData
    io.tmdsOGrn := mLanes(LANE_GRN).io.tmdsOData
    io.tmdsORed := mLanes(LANE_RED).io.tmdsOData
  }
}
