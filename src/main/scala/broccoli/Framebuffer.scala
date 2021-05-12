// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class Framebuffer(width: BigInt, height: BigInt) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val data = Input(UInt(9.W))
    val enable = Input(Bool())
    val strobe = Input(Bool())

    val address = Input(UInt(12.W))
    val write = Input(Bool())
    val ready = Output(Bool())

    val result = Output(UInt(9.W))
  })

  final val STATEWIDTH = 4

  withReset(~io.aresetn) {
    val state = Reg(UInt(STATEWIDTH.W))
    // GOTCHA: 9bpp, easy, because for one thing a shader has the ability to
    // left shift each channel by 1
    val frameMemory = SyncReadMem(width * height, UInt(9.W))
    val result = Reg(UInt(9.W))
    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn) {
      state := 0.U(STATEWIDTH.W)
    }.elsewhen(io.enable & io.strobe & ~state.orR()) {
      state := 1.U(STATEWIDTH.W)
    }.elsewhen(io.enable & ~io.strobe) {
      state := state << 1.U(STATEWIDTH.W)
    }
    // ========================================================================
    //  Hold Write Flags and Data
    // ========================================================================
    when(~io.aresetn) {
      write := 0.B
    }.elsewhen(io.enable & state(0)) {
      write := io.write
    }
    when(~io.aresetn) {
      address := 0.U(12.W)
    }.elsewhen(io.enable & state(0)) {
      address := io.address
    }
    when(~io.aresetn) {
      data := 0.U(9.W)
    }.elsewhen(io.enable & state(0)) {
      data := io.data
    }
    // ========================================================================
    //  Memory Reads and Writes
    // ========================================================================
    when(~io.aresetn) {
      result := 0.U(8.W)
    }.elsewhen(io.enable & ~write) {
      result := frameMemory.read(address)
    }.elsewhen(io.enable & write & state(1)) {
      frameMemory.write(address, data)
    }

    io.ready := ~state.orR()
    io.result := result
  }
}