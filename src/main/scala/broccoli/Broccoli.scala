// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** root project to generate Verilog from
  */
class Broccoli extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())

    val outRed = Output(UInt(4.W))
    val outGrn = Output(UInt(4.W))
    val outBlu = Output(UInt(4.W))
    val outVSync = Output(Bool())
    val outHSync = Output(Bool())

    val vBlank = Output(Bool())
    val hBlank = Output(Bool())
  })

  withReset(~io.aresetn) {
    val mVGA = Module(new VGAIntervalDriver)

    mVGA.clock := clock

    mVGA.io.aresetn := io.aresetn
    mVGA.io.enable := io.enable

    io.outRed := mVGA.io.x(3, 0)
    io.outGrn := mVGA.io.y(3, 0)
    io.outBlu := "b1111".U(4.W)

    io.outVSync := mVGA.io.vSync
    io.outHSync := mVGA.io.hSync

    io.vBlank := mVGA.io.vblankn
    io.hBlank := mVGA.io.hblankn
  }
}
