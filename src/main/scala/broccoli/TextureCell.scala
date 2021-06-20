// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Variable-size square rasterizer with affine texture mapping
  *
  * @param length Addressing/dimensions length, this is a power-of-two
  * @param freeRunning Sets whether the state machine is free-running
  */
class TextureCell(length: Int, freeRunning: Boolean) extends Module {
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
    val textureCoordsX = Input(SInt(12.W))
    val textureCoordsY = Input(SInt(12.W))

    val address = Input(UInt((length * 2).W))
    val writeTexels = Input(Bool())
    val writeMatrix = Input(Bool())
    val ready = Output(Bool())
    val stencilTest = Output(Bool())

    val textureResult = Output(UInt(8.W))
  })
  final val AOFF = 0
  final val BOFF = 1
  final val COFF = 2
  final val DOFF = 3
  final val XOFF = 4
  final val YOFF = 5
  final val X0OFF = 6
  final val Y0OFF = 7

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
    val stencilTestX = Reg(Bool())
    val stencilTestY = Reg(Bool())

    // Captured on state[0]
    // First 5-tuple cycle supports a texture coordinate calc.
    val textureMatrix = Reg(Vec(8, SInt(12.W)))
    val textureMInter = Reg(Vec(2, SInt(24.W)))
    val textureMFinal = Reg(Vec(2, SInt(24.W)))
    // Second 5-tuple cycle supports a texture memory read.
    // AA BB GG RR
    // 64*64 texture memory
    val textureMemory = SyncReadMem(1 << (length * 2), UInt(8.W))
    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    if (freeRunning) {
      // Clock is free-running. Strobe is not required.
      when(~io.aresetn) {
        state := 0.U(10.W)
      }.elsewhen(io.enable & ~state.orR()) {
        state := 1.U(10.W)
      }.elsewhen(io.enable) {
        state := state << 1.U(10.W)
      }
    } else {
      // Clock is NOT free-running. Strobe is required.
      when(~io.aresetn) {
        state := 0.U(10.W)
      }.elsewhen(io.enable & io.strobe & ~state.orR()) {
        state := 1.U(10.W)
      }.elsewhen(io.enable & ~io.strobe) {
        state := state << 1.U(10.W)
      }
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
    // === Hold Matrix Element X0 ===
    when(~io.aresetn) {
      textureMatrix(X0OFF) := "h000".U(12.W).asSInt
    }.elsewhen(io.enable & writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(X0OFF) := io.textureCoordsX
    }
    // === Hold Matrix Element Y0 ===
    when(~io.aresetn) {
      textureMatrix(Y0OFF) := "h000".U(12.W).asSInt
    }.elsewhen(io.enable & writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(Y0OFF) := io.textureCoordsY
    }
    // === Synchronize X and Y Axes, this is good practice ===
    // X
    when(~io.aresetn) {
      textureMatrix(XOFF) := 0.S(12.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(XOFF) := Cat(false.B, io.currentX(10, 0)).asSInt
    }
    // Y
    when(~io.aresetn) {
      textureMatrix(YOFF) := 0.S(12.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(1)) {
      textureMatrix(YOFF) := Cat(false.B, io.currentY(10, 0)).asSInt
    }
    // === Now use DSP48s to calculate X' and Y' ===
    // Each operation is staggered. This allows us to use only 2 DSPs per
    // matrix.
    // ====== CALCULATE MULTIPLYS ======
    // (X * A) @ S[2]
    // (X * C) @ S[5]
    when(~io.aresetn) {
      textureMInter(0) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(2)) {
      textureMInter(0) := (textureMatrix(XOFF) - textureMatrix(
        X0OFF
      )) * textureMatrix(AOFF)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(5)) {
      textureMInter(0) := (textureMatrix(XOFF) - textureMatrix(
        X0OFF
      )) * textureMatrix(COFF)
    }
    // (Y * B) @ S[2]
    // (Y * D) @ S[5]
    when(~io.aresetn) {
      textureMInter(1) := 0.S(24.W)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(2)) {
      textureMInter(1) := (textureMatrix(YOFF) - textureMatrix(
        Y0OFF
      )) * textureMatrix(BOFF)
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(5)) {
      textureMInter(1) := (textureMatrix(YOFF) - textureMatrix(
        Y0OFF
      )) * textureMatrix(DOFF)
    }
    // ====== CALCULATE ADDITIONS ======
    // AX + BY @ S[4]
    // CX + DY @ S[7]
    when(~io.aresetn) {
      textureMFinal := VecInit(0.S(24.W), 0.S(24.W))
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(4)) {
      textureMFinal(XfinalOFF) := textureMInter(0) + textureMInter(1) + Cat(
        textureMatrix(X0OFF)(11),
        0.U(12.W),
        textureMatrix(X0OFF)(10, 0)
      ).asSInt
    }.elsewhen(io.enable & ~writeMatrix & ~writeTexels & state(7)) {
      textureMFinal(YfinalOFF) := textureMInter(0) + textureMInter(1) + Cat(
        textureMatrix(Y0OFF)(11),
        0.U(12.W),
        textureMatrix(Y0OFF)(10, 0)
      ).asSInt
    }
    // ========================================================================
    //  Map Texture Coordinates
    // ========================================================================
    // h010 affine subpixels (16 of them) == 1px
    // Let's respect that.
    // I was tight on pipeline steps too.
    if (freeRunning) {
      when(~io.aresetn) {
        textureResult := 0.U(8.W)
      }.elsewhen(io.enable & state(9) & ~writeMatrix & ~writeTexels) {
        textureResult := textureMemory.read(
          Cat(
            textureMFinal(YfinalOFF)(23),
            textureMFinal(YfinalOFF)(length + 3, 4),
            textureMFinal(XfinalOFF)(23),
            textureMFinal(XfinalOFF)(length + 3, 4)
          )
        )
      }.elsewhen(io.enable & writeTexels & state(1)) {
        textureMemory.write(address, data)
      }
    } else {
      when(~io.aresetn) {
        textureResult := 0.U(8.W)
      }.elsewhen(io.enable & ~io.strobe & ~writeMatrix & ~writeTexels) {
        textureResult := textureMemory.read(
          Cat(
            textureMFinal(YfinalOFF)(23),
            textureMFinal(YfinalOFF)(length + 3, 4),
            textureMFinal(XfinalOFF)(23),
            textureMFinal(XfinalOFF)(length + 3, 4)
          )
        )
      }.elsewhen(io.enable & writeTexels & state(1)) {
        textureMemory.write(address, data)
      }
    }
    // ========================================================================
    //  Stencil Testing
    // ========================================================================
    // True if we don't draw here, false if we do draw here
    when(~io.aresetn) {
      stencilTestX := true.B
    }.elsewhen(io.enable & state(9) & ~writeMatrix & ~writeTexels) {
      stencilTestX := ((1.S(12.W) << length) < Cat(
        textureMFinal(XfinalOFF)(23),
        textureMFinal(XfinalOFF)(14, 4)
      ).asSInt) || (0.S(12.W) > Cat(
        textureMFinal(XfinalOFF)(23),
        textureMFinal(XfinalOFF)(14, 4)
      ).asSInt)
    }

    when(~io.aresetn) {
      stencilTestY := true.B
    }.elsewhen(io.enable & state(9) & ~writeMatrix & ~writeTexels) {
      stencilTestY := ((1.S(12.W) << length) < Cat(
        textureMFinal(YfinalOFF)(23),
        textureMFinal(YfinalOFF)(14, 4)
      ).asSInt) || (0.S(12.W) > Cat(
        textureMFinal(YfinalOFF)(23),
        textureMFinal(YfinalOFF)(14, 4)
      ).asSInt)
    }

    io.ready := ~state.orR()
    io.textureResult := textureResult
    io.stencilTest := stencilTestX | stencilTestY
  }
}
