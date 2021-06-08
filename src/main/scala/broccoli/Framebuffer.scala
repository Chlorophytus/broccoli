// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Stores 9-bit pixel data in a 2D array
  *
  * @param width Width of the data store
  * @param height Height of the data store
  * @param freeRunning Sets whether the state machine is free-running
  */
class Framebuffer(width: Int, height: Int, freeRunning: Boolean)
    extends Module {
  final val STATEWIDTH = 5
  final val TOTALADDR = log2Ceil(width * height)

  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val data = Input(UInt(9.W))
    val enable = Input(Bool())
    val strobe = Input(Bool())

    val address = Input(UInt(TOTALADDR.W))
    val write = Input(Bool())
    val ready = Output(Bool())

    val result = Output(UInt(9.W))
  })

  withReset(~io.aresetn) {
    val state = Reg(UInt(STATEWIDTH.W))
    // GOTCHA: 9bpp, easy, because for one thing a shader has the ability to
    // left shift each channel by 1
    val write = Reg(Bool())
    val data = Reg(UInt(9.W))
    val address = Reg(UInt(TOTALADDR.W))
    val frameMemory = SyncReadMem(1 << TOTALADDR, UInt(9.W))
    val result = Reg(UInt(9.W))
    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    if (freeRunning) {
      // Clock is free-running. Strobe is not required.
      when(~io.aresetn) {
        state := 0.U(STATEWIDTH.W)
      }.elsewhen(io.enable & ~state.orR()) {
        state := 1.U(STATEWIDTH.W)
      }.elsewhen(io.enable) {
        state := state << 1.U(STATEWIDTH.W)
      }
    } else {
      // Clock is NOT free-running. Strobe is required.
      when(~io.aresetn) {
        state := 0.U(STATEWIDTH.W)
      }.elsewhen(io.enable & io.strobe & ~state.orR()) {
        state := 1.U(STATEWIDTH.W)
      }.elsewhen(io.enable & ~io.strobe) {
        state := state << 1.U(STATEWIDTH.W)
      }
    }
    // ========================================================================
    //  Hold Write Flags and Data
    // ========================================================================
    when(~io.aresetn) {
      write := false.B
    }.elsewhen(io.enable & state(0)) {
      write := io.write
    }
    when(~io.aresetn) {
      address := 0.U(TOTALADDR.W)
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
    }.elsewhen(io.enable & ~write & state(STATEWIDTH - 1)) {
      result := frameMemory.read(address)
    }.elsewhen(io.enable & write & state(STATEWIDTH - 1)) {
      frameMemory.write(address, data)
    }

    io.ready := ~state.orR()
    io.result := result
  }
}
