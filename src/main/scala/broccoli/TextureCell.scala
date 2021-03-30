// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class TextureCell extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val data = Input(UInt(8.W))
    val enable = Input(Bool())
    val strobe = Input(Bool())
    
    val hBlank = Input(Bool())
    val vBlank = Input(Bool())
    val currentX = Input(UInt(12.W))
    val currentY = Input(UInt(12.W))

    val textureCoordsA = Input(UInt(16.W))
    val textureCoordsB = Input(UInt(16.W))
    val textureCoordsC = Input(UInt(16.W))
    val textureCoordsD = Input(UInt(16.W))

    val address = Input(UInt(8.W))
    val writeTexels = Input(Bool())
    val writeMatrix = Input(Bool())
    val ready = Output(Bool())
  })

  withReset(~io.aresetn) {
    val state = Reg(UInt(10.W))
    val programCounter = Reg(UInt(16.W))
    val write = Reg(Bool())

    // Captured on state[0]
    // First 5-tuple cycle supports a texture coordinate calc.
    val textureMatrix = Reg(VecInit.tabulate(4)(UInt(16.W)))
    val textureCoordX = Reg(UInt(12.W))
    val textureCoordY = Reg(UInt(12.W))
    // Second 5-tuple cycle supports a texture memory read.
    val textureMemory = SyncReadMem(256, UInt(8.W)) // 16*16 texture memory
    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn) {
      state := 0.U(10.W)
    }.elsewhen(io.strobe & ~state.orR()) {
      state := 1.U(10.W)
    }.elsewhen(io.enable) {
      state := state << 1.U(10.W)
    }
    // ========================================================================
    //  Affine Transformation
    // ========================================================================
    // using matrix-vector multiply:
    // | a b | * | x | --> | x'|
    // | c d | * | y | --> | y'|
    //
    // ax * by = x'
    // cx * dy = y'
    // // TODO: Signed or unsigned handler?
    // === Hold Matrix Element A ===
    when(~io.aresetn) {
      textureMatrix(0) := "h0080".U(16.W)
    }.elsewhen(io.enable & io.writeMatrix & ~io.writeTexels & state(0))
      textureMatrix(0) := io.textureCoordsA
    }
    // === Hold Matrix Element B ===
    when(~io.aresetn) {
      textureMatrix(1) := "h0000".U(16.W)
    }.elsewhen(io.enable & io.writeMatrix & ~io.writeTexels & state(0))
      textureMatrix(1) := io.textureCoordsB
    }
    // === Hold Matrix Element C ===
    when(~io.aresetn) {
      textureMatrix(2) := "h0000".U(16.W)
    }.elsewhen(io.enable & io.writeMatrix & ~io.writeTexels & state(0))
      textureMatrix(2) := io.textureCoordsC
    }
    // === Hold Matrix Element D ===
    when(~io.aresetn) {
      textureMatrix(3) := "h0080".U(16.W)
    }.elsewhen(io.enable & io.writeMatrix & ~io.writeTexels & state(0))
      textureMatrix(3) := io.textureCoordsD
    }
  }
}
