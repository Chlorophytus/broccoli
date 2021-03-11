// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class Shader extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val data = Input(UInt(8.W))
    val enable = Input(Bool())
    val strobe = Input(Bool())

    val hBlank = Input(Bool())
    val vBlank = Input(Bool())
    val currentX = Input(UInt(12.W))
    val currentY = Input(UInt(12.W))

    val in = Input(UInt(16.W))

    val write = Output(Bool())
    val ready = Output(Bool())
    val address = Output(UInt(16.W))
    val out = Output(UInt(16.W))

    val debugAluPre0Flags = Output(UInt(8.W))
    val debugAluPre1Flags = Output(UInt(8.W))
    val debugAluPostFlags = Output(UInt(8.W))
  })

  withReset(~io.aresetn) {
    val state = Reg(UInt(5.W)) // TODO: Make me 10-stage?
    val programCounter = Reg(UInt(16.W))
    val write = Reg(Bool())
    val zeroPage = Reg(VecInit.tabulate(16)(UInt(16.W)))
    // val textureMemory = SyncReadMem(4096, UInt(8.W)) // 4kB texture memory
    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn) {
      state := 0.U(5.W)
    }.elsewhen(io.strobe & ~state.orR()) {
      state := 1.U(5.W)
    }.elsewhen(io.enable) {
      state := state << 1.U(5.W)
    }
    // ========================================================================
    //  I have given you all the modules you need.
    // ========================================================================
    val aluPre0 = Module(new ShaderALU())
    val aluPre1 = Module(new ShaderALU())
    val aluPost = Module(new ShaderALU())
    // ========================================================================
    //  Register File
    // ========================================================================
    for ((register, i) <- zeroPage.view.zipWithIndex) {
      when(~io.aresetn) {
        register := 0.U(16.W)
      }.elsewhen(~(programCounter ^ i.U(16.W)).andR() & ~write & state(1)) {
        // if programCounter is i then proceed to set if not wen and if state
        register := io.in
      }
    }
    // TODO: make an io.out memory mapped thingy
    // ========================================================================
    //  Memory
    // ========================================================================
    
  }
}
