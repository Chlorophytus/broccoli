// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Variable-size square rasterizer with affine texture mapping
  *
  * @param length Addressing/dimensions length, this is a power-of-two
  */
class TextureCell(length: Int) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val data = Input(UInt(8.W))
    val enable = Input(Bool())
    val strobe = Input(Bool())

    val currentX = Input(UInt(12.W))
    val currentY = Input(UInt(12.W))

    val textureCoordsA = Input(SInt(12.W))
    val textureCoordsB = Input(SInt(12.W))
    val textureCoordsC = Input(SInt(12.W))
    val textureCoordsD = Input(SInt(12.W))

    val address = Input(UInt((length * 2).W))
    val writeTexels = Input(Bool())
    val writeMatrix = Input(Bool())
    val ready = Output(Bool())

    val textureResult = Output(UInt(8.W))
  })
  final val AOFF = 0
  final val BOFF = 1
  final val COFF = 2
  final val DOFF = 3
  final val XOFF = 4
  final val YOFF = 5

  final val XinterAOFF = 0
  final val YinterBOFF = 1
  final val XinterCOFF = 2
  final val YinterDOFF = 3

  final val XfinalOFF = 0
  final val YfinalOFF = 1

  final val Xmap = 0
  final val Ymap = 1
  withReset(~io.aresetn) {
    val state = Reg(UInt(10.W))
    val textureResult = Reg(UInt(8.W))
    val writeMatrix = Reg(Bool())
    val writeTexels = Reg(Bool())
    val address = Reg(UInt(12.W))
    val data = Reg(UInt(8.W))

    // Captured on state[0]
    // First 5-tuple cycle supports a texture coordinate calc.
    val textureMatrix = Reg(Vec(6, SInt(12.W)))
    val textureMInter = Reg(Vec(4, SInt(24.W)))
    val textureMFinal = Reg(Vec(2, SInt(24.W)))
    // Second 5-tuple cycle supports a texture memory read.
    // AA BB GG RR
    // 64*64 texture memory
    val textureMemory = SyncReadMem(1 << (length * 2), UInt(8.W))
    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn) {
      state := 0.U(10.W)
    }.elsewhen(io.enable & io.strobe & ~state.orR()) {
      state := 1.U(10.W)
    }.elsewhen(io.enable & ~io.strobe) {
      state := state << 1.U(10.W)
    }
    // ========================================================================
    //  Hold Write Flags and Data
    // ========================================================================
    when(~io.aresetn) {
      writeMatrix := 0.B
    }.elsewhen(io.enable & state(0)) {
      writeMatrix := io.writeMatrix
    }
    when(~io.aresetn) {
      writeTexels := 0.B
    }.elsewhen(io.enable & state(0)) {
      writeTexels := io.writeTexels
    }
    when(~io.aresetn) {
      address := 0.U(12.W)
    }.elsewhen(io.enable & state(0)) {
      address := io.address
    }
    when(~io.aresetn) {
      data := 0.U(8.W)
    }.elsewhen(io.enable & state(0)) {
      data := io.data
    }
    // ========================================================================
    //  Affine Transformation
    // ========================================================================
    // using matrix-vector multiply:
    // | a b | * | x | --> | x'|
    // | c d | * | y | --> | y'|
    //
    // ax + by = x'
    // cx + dy = y'
    // // TODO: Signed or unsigned handler?
    // === Hold Matrix Element A ===
    when(~io.aresetn) {
      textureMatrix(AOFF) := "h010".U(12.W).asSInt
    }.elsewhen(io.enable & ~io.strobe & writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(AOFF) := io.textureCoordsA
    }
    // === Hold Matrix Element B ===
    when(~io.aresetn) {
      textureMatrix(BOFF) := "h000".U(12.W).asSInt
    }.elsewhen(io.enable & writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(BOFF) := io.textureCoordsB
    }
    // === Hold Matrix Element C ===
    when(~io.aresetn) {
      textureMatrix(COFF) := "h000".U(12.W).asSInt
    }.elsewhen(io.enable & writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(COFF) := io.textureCoordsC
    }
    // === Hold Matrix Element D ===
    when(~io.aresetn) {
      textureMatrix(DOFF) := "h010".U(12.W).asSInt
    }.elsewhen(io.enable & writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(DOFF) := io.textureCoordsD
    }
    // === Synchronize X and Y Axes, this is good practice ===
    // X
    when(~io.aresetn) {
      textureMatrix(XOFF) := 0.S(12.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(XOFF) := Cat(0.U(6.W), io.currentX(5, 0)).asSInt
    }
    // Y
    when(~io.aresetn) {
      textureMatrix(YOFF) := 0.S(12.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(YOFF) := Cat(0.U(6.W), io.currentY(5, 0)).asSInt
    }
    // === Now use DSP48s to calculate X' and Y' ===
    // Each operation is staggered. This allows us to use only 3 DSPs per
    // matrix.
    // ====== CALCULATE MULTIPLYS ======
    // (X * A)
    when(~io.aresetn) {
      textureMInter(XinterAOFF) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(2)) {
      textureMInter(XinterAOFF) := textureMatrix(XOFF) * textureMatrix(AOFF)
    }
    // (Y * B)
    when(~io.aresetn) {
      textureMInter(YinterBOFF) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(4)) {
      textureMInter(YinterBOFF) := textureMatrix(YOFF) * textureMatrix(BOFF)
    }
    // (X * C)
    when(~io.aresetn) {
      textureMInter(XinterCOFF) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(2)) {
      textureMInter(XinterCOFF) := textureMatrix(XOFF) * textureMatrix(COFF)
    }
    // (Y * D)
    when(~io.aresetn) {
      textureMInter(YinterDOFF) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(4)) {
      textureMInter(YinterDOFF) := textureMatrix(YOFF) * textureMatrix(DOFF)
    }
    // ====== CALCULATE ADDITIONS ======
    // AX + BY
    when(~io.aresetn) {
      textureMFinal(XfinalOFF) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(5)) {
      textureMFinal(XfinalOFF) := textureMInter(XinterAOFF) +% textureMInter(
        YinterBOFF
      )
    }
    // CX + DY
    when(~io.aresetn) {
      textureMFinal(YfinalOFF) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(7)) {
      textureMFinal(YfinalOFF) := textureMInter(XinterCOFF) +% textureMInter(
        YinterDOFF
      )
    }
    // ========================================================================
    //  Map Texture Coordinates
    // ========================================================================
    // h010 affine subpixels (16 of them) == 1px
    // Let's respect that.
    // I was tight on pipeline steps too.
    when(~io.aresetn) {
      textureResult := 0.U(8.W)
    }.elsewhen(io.enable & ~io.strobe & ~writeMatrix & ~writeTexels) {
      textureResult := textureMemory.read(
        Cat(
          textureMFinal(YfinalOFF)(length + 4, 4),
          textureMFinal(XfinalOFF)(length + 4, 4)
        )
      )
    }.elsewhen(io.enable & writeTexels & state(1)) {
      textureMemory.write(address, data)
    }

    io.ready := ~state.orR()
    io.textureResult := textureResult
  }
}
