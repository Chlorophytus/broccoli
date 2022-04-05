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

    val done = Output(Bool())
  })

  withReset(~io.aresetn) {
    val mConstraints = Module(new VGAIntervalDynamicDriver())
    // temporary before we get a real RISCV microcontroller going to handle
    // the display control
    val mInit = Module(new InitializePPU())

    mInit.io.aresetn := io.aresetn
    mInit.io.enable := io.enable
    io.done := mInit.io.done
    mConstraints.io.aresetn := io.aresetn
    mConstraints.clock := clock
    mConstraints.io.enable := io.enable
    mConstraints.io.writeStrobe := mInit.io.writeStrobe
    mConstraints.io.writeEnable := mInit.io.writeEnable
    mConstraints.io.registerAddress := mInit.io.registerAddress
    mConstraints.io.registerData := mInit.io.registerData
    mInit.io.ready := mConstraints.io.ready

    io.outBlu := 15.U(4.W)
    io.outGrn := mConstraints.io.x(3, 0)
    io.outRed := mConstraints.io.y(3, 0)
    io.vBlank := mConstraints.io.vBlank
    io.hBlank := mConstraints.io.hBlank
    io.outVSync := mConstraints.io.vSync
    io.outHSync := mConstraints.io.hSync
  }
}
