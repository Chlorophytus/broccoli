// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class ShaderMemory(width: Int) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val enable = Input(Bool())
    val write = Input(Bool())
    val setAddress = Input(Bool())
    val writeAddress = Input(UInt(width.W))
    
    val ready = Output(Bool())
    
    val debugCurrentAddress = Output(UInt(width.W))
  })
  
  withReset(~io.aresetn) {
    val ram = SyncReadMem((1 << width), UInt(8.W))
    val programCounter = UInt(width.W)
    val loadWordsNext = Reg(UInt(3.W))
    val state = Reg(UInt(5.W))
    
    // ========================================================================
    //  State Machine
    // ========================================================================
    when(~io.aresetn | state(4)) {
      state := 1.U(5.W)
    }.otherwise {
      state := state << 1.U(5.W)
    }
    when(~io.aresetn) {
      enable := false.B
    }.elsewhen(state(0)) {
      enable := io.enable
    }
    when(~io.aresetn) {
      write := false.B
    }.elsewhen(state(0)) {
      write := io.write
    }
  }
}
