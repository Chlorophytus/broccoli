// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS (Transition Minimized Differential Signal) lane
  *
  * useful for DVI video signalling
  */
class TMDSLane extends Module {
  final val cntWidth = 12
  final val popcntWidth = 3
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val wen = Input(Bool())
    val strobe = Input(Bool())

    val de = Input(Bool())
    val d = Input(UInt(8.W))
    val c = Input(UInt(2.W))

    val qq = Output(UInt(2.W))

    val debugCnt = Output(SInt(cntWidth.W))
    val debugQWhich = Output(Bool())
    val debugQOut = Output(UInt(10.W))
  })
  withReset(~io.aresetn) {
    // Hold registers at State0
    val holdDE = false.B
    val holdD = 0.U(8.W)
    val holdC = 0.U(2.W)

    // val mXOR = Module(new TMDSLaneXOR(false))
    // val mXNOR = Module(new TMDSLaneXOR(true))
  }
}
