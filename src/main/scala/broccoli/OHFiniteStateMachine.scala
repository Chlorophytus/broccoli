// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** One Hot Finite State Machine
  *
  * @param stateCount number of states to use
  */
class OHFiniteStateMachine(stateCount: Int) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val output = Output(UInt(stateCount.W))
  })

  withReset(~io.aresetn) {
    val state = RegInit(1.U(stateCount.W))

    when(~io.aresetn) {
      state := 1.U(stateCount.W)
    }.elsewhen(io.enable) {
      state := state.rotateLeft(1)
    }

    io.output := state
  }
}
