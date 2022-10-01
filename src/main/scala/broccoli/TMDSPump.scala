// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS output shift register, double data rate
  */
class TMDSPump extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val in = Input(UInt(10.W))
    val out = Output(Bool())
  })

  withReset(~io.aresetn) {
    val mState = Module(new OHFiniteStateMachine(5))
    val mDDR = Module(new TMDSDDR())
    val hold = RegInit(0.U(10.W))

    when(~io.aresetn) {
      hold := 0.U(10.W)
    }.elsewhen(io.enable & mState.io.output(4)) {
      hold := io.in
    }.elsewhen(io.enable) {
      hold := hold.rotateRight(2)
    }

    mState.io.aresetn := io.aresetn
    mState.clock := clock
    mState.io.enable := io.enable

    mDDR.io.aresetn := io.aresetn
    mDDR.io.clock := clock
    mDDR.io.d := hold(1, 0)
    io.out := mDDR.io.q
  }
}
