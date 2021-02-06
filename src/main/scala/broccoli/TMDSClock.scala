// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class TMDSClock extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val qq = Output(UInt(2.W))
  })
  withReset(~io.aresetn) {
    val qq = Reg(UInt(2.W))
    val qState = Reg(UInt(5.W)) // Enum support is broken.

    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn || qState(4)) {
      qState := 1.U(5.W)
    }.otherwise {
      qState := qState << 1.U(5.W)
    }
    // ========================================================================
    //  Q_OUT
    // ========================================================================
    when(qState(0)) { qq := "b00".U(2.W) }
    when(qState(1)) { qq := "b00".U(2.W) }
    when(qState(2)) { qq := "b01".U(2.W) }
    when(qState(3)) { qq := "b11".U(2.W) }
    when(qState(4)) { qq := "b11".U(2.W) }
    io.qq := qq
  }
}
