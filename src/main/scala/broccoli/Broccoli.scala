// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** root project to generate Verilog from
  */
class Broccoli extends Module {
  final val blu = 0
  final val grn = 1
  final val red = 2
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val writeEnable = Input(Bool())
    val writeStrobe = Input(Bool())
    val registerAddress = Input(UInt(4.W))
    val registerData = Input(UInt(12.W))
    val registersReady = Output(Bool())

    val clockF = Input(Clock()) // clockP x 5 (0degs phase)
    val clockD = Input(Clock()) // clockP x 5 (-90degs phase)

    // TMDS intermediaries, need a OBUFDS-like primitive to encode them.
    val outRed = Output(Bool())
    val outGrn = Output(Bool())
    val outBlu = Output(Bool())

    val vBlank = Output(Bool())
    val hBlank = Output(Bool())
  })
  
  withReset(~io.aresetn) {
    val mLanes = Seq.fill(3)(Module(new TMDSLane()))
    val mDDRs = Seq.fill(3)(Module(new TMDSDDR()))
    val mConstraints = Module(new VGAIntervalDynamicDriver())

    mConstraints.io.aresetn := io.aresetn
    mConstraints.clock := clock
    mConstraints.io.enable := io.enable
    mConstraints.io.writeStrobe := io.writeStrobe
    mConstraints.io.writeEnable := io.writeEnable
    mConstraints.io.registerAddress := io.registerAddress
    mConstraints.io.registerData := io.registerData
    
    mLanes(blu).io.d := "hFF".U(8.W)
    mLanes(blu).io.c := Cat(mConstraints.io.vSync, mConstraints.io.hSync)

    mLanes(grn).io.d := mConstraints.io.y(7, 0)
    mLanes(grn).io.c := 0.U(2.W)
    
    mLanes(red).io.d := mConstraints.io.x(7, 0)
    mLanes(red).io.c := 0.U(2.W)

    for (i <- (0 to 2)) {
      mLanes(i).clock := io.clockF
      mDDRs(i).io.clock := io.clockD

      mDDRs(i).io.aresetn := io.aresetn
      mLanes(i).io.aresetn := io.aresetn
      mLanes(i).io.enable := io.enable

      mDDRs(i).io.d := mLanes(i).io.qq

      // blanks are active-low
      mLanes(i).io.de := mConstraints.io.vBlank & mConstraints.io.hBlank & mConstraints.io.ready
    }

    io.outBlu := mDDRs(blu).io.q
    io.outGrn := mDDRs(grn).io.q
    io.outRed := mDDRs(red).io.q
    io.registersReady := mConstraints.io.ready
    io.vBlank := mConstraints.io.vBlank
    io.hBlank := mConstraints.io.hBlank
  }
}
