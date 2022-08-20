// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class TextureCell(length: Int) extends Module {
  final val A_OFFSET = 0
  final val B_OFFSET = 1
  final val C_OFFSET = 2
  final val D_OFFSET = 3
  final val X_OFFSET = 4
  final val Y_OFFSET = 5

  final val XA_INTER_OFFSET = 0
  final val YB_INTER_OFFSET = 1
  final val XC_INTER_OFFSET = 2
  final val YD_INTER_OFFSET = 3

  final val X_FINAL_OFFSET = 0
  final val Y_FINAL_OFFSET = 1

  final val SHIFT_FACTOR = 8

  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val write = Input(Bool())
    val ready = Output(Bool())
    val x = Input(UInt(12.W))
    val y = Input(UInt(12.W))
    val address = Input(UInt((length * 2).W))
    val writeTexels = Input(Bool())
    val data = Input(UInt(7.W))

    val outRed = Output(UInt(2.W))
    val outGrn = Output(UInt(2.W))
    val outBlu = Output(UInt(2.W))
    val outTst = Output(Bool())

    val textureCoordA = Input(SInt(16.W))
    val textureCoordB = Input(SInt(16.W))
    val textureCoordC = Input(SInt(16.W))
    val textureCoordD = Input(SInt(16.W))
  })

  val mState = Module(new OHFiniteStateMachine(10))

  mState.io.aresetn := io.aresetn
  mState.io.enable := io.enable
  mState.clock := clock

  withReset(~io.aresetn) {
    val textureResult = RegInit(0.U(8.W))
    val write = RegInit(false.B)
    val writeTexels = RegInit(false.B)
    val address = RegInit(0.U((length * 2).W))
    val data = RegInit(0.U(7.W))
    // Captured on state[0]
    // First 5-tuple cycle supports a texture coordinate calc.
    val textureMatrix = RegInit(VecInit.fill(6)(0.S(16.W)))
    val textureMatrixInter = RegInit(VecInit.fill(4)(0.S(32.W)))
    val textureMatrixFinal = RegInit(VecInit.fill(2)(0.S(32.W)))
    // Second 5-tuple cycle supports a texture memory read.
    // AA BB GG RR
    val textureMemory = SyncReadMem(1 << (length * 2), UInt(7.W))
    // =========================================================================
    //  Hold Write Flags and Data
    // =========================================================================
    when(~io.aresetn) {
      write := 0.B
    }.elsewhen(io.enable && mState.io.output(0)) {
      write := io.write
    }
    when(~io.aresetn) {
      writeTexels := 0.B
    }.elsewhen(io.enable && mState.io.output(0)) {
      writeTexels := io.writeTexels
    }
    when(~io.aresetn) {
      address := 0.U(length.W)
    }.elsewhen(io.enable && mState.io.output(0)) {
      address := io.address
    }
    when(~io.aresetn) {
      data := 0.U(7.W)
    }.elsewhen(io.enable & mState.io.output(0)) {
      data := io.data
    }
    // =========================================================================
    //  Affine Transformation
    // =========================================================================
    // using matrix-vector multiply:
    // | a b | * | x | --> | x'|
    // | c d | * | y | --> | y'|
    //
    // ax + by = x'
    // cx + dy = y'
    // === Hold Matrix ===
    when(~io.aresetn) {
      textureMatrix(A_OFFSET) := (1 << SHIFT_FACTOR).S(16.W)
    }.elsewhen(io.enable && write && ~writeTexels && mState.io.output(1)) {
      textureMatrix(A_OFFSET) := io.textureCoordA
    }
    when(~io.aresetn) {
      textureMatrix(B_OFFSET) := (1 << SHIFT_FACTOR).S(16.W)
    }.elsewhen(io.enable && write && ~writeTexels && mState.io.output(1)) {
      textureMatrix(B_OFFSET) := io.textureCoordB
    }
    when(~io.aresetn) {
      textureMatrix(C_OFFSET) := (1 << SHIFT_FACTOR).S(16.W)
    }.elsewhen(io.enable && write && ~writeTexels && mState.io.output(1)) {
      textureMatrix(C_OFFSET) := io.textureCoordC
    }
    when(~io.aresetn) {
      textureMatrix(D_OFFSET) := (1 << SHIFT_FACTOR).S(16.W)
    }.elsewhen(io.enable && write && ~writeTexels && mState.io.output(1)) {
      textureMatrix(D_OFFSET) := io.textureCoordD
    }
    // === Synchronize X and Y Axes, this is good practice ===
    when(~io.aresetn) {
      textureMatrix(X_OFFSET) := (1 << SHIFT_FACTOR).S(16.W)
    }.elsewhen(io.enable && ~write && ~writeTexels && mState.io.output(1)) {
      textureMatrix(X_OFFSET) := Cat(0.U(8.W), io.x(7, 0)).asSInt
    }
    when(~io.aresetn) {
      textureMatrix(Y_OFFSET) := (1 << SHIFT_FACTOR).S(16.W)
    }.elsewhen(io.enable && ~write && ~writeTexels && mState.io.output(1)) {
      textureMatrix(Y_OFFSET) := Cat(0.U(8.W), io.y(7, 0)).asSInt
    }
    // === Now use DSP48s to calculate X' and Y' ===
    // Each operation is staggered. This allows us to use only 3 DSPs per
    // matrix.
    // === Perform multiplication steps ===
    when(~io.aresetn) {
      textureMatrixInter(XA_INTER_OFFSET) := 0.S(32.W)
    }.elsewhen(io.enable && ~write && ~writeTexels & mState.io.output(2)) {
      textureMatrixInter(XA_INTER_OFFSET) := textureMatrix(
        X_OFFSET
      ) * textureMatrix(A_OFFSET)
    }
    when(~io.aresetn) {
      textureMatrixInter(YB_INTER_OFFSET) := 0.S(32.W)
    }.elsewhen(io.enable && ~write && ~writeTexels & mState.io.output(4)) {
      textureMatrixInter(YB_INTER_OFFSET) := textureMatrix(
        Y_OFFSET
      ) * textureMatrix(B_OFFSET)
    }
    when(~io.aresetn) {
      textureMatrixInter(XC_INTER_OFFSET) := 0.S(32.W)
    }.elsewhen(io.enable && ~write && ~writeTexels & mState.io.output(2)) {
      textureMatrixInter(XC_INTER_OFFSET) := textureMatrix(
        X_OFFSET
      ) * textureMatrix(C_OFFSET)
    }
    when(~io.aresetn) {
      textureMatrixInter(YD_INTER_OFFSET) := 0.S(32.W)
    }.elsewhen(io.enable && ~write && ~writeTexels & mState.io.output(4)) {
      textureMatrixInter(YD_INTER_OFFSET) := textureMatrix(
        Y_OFFSET
      ) * textureMatrix(D_OFFSET)
    }
    // === Perform last additions ===
    // ax + by
    when(~io.aresetn) {
      textureMatrixFinal(X_FINAL_OFFSET) := 0.S(32.W)
    }.elsewhen(io.enable && ~write && ~writeTexels && mState.io.output(5)) {
      textureMatrixFinal(X_FINAL_OFFSET) := textureMatrixInter(
        XA_INTER_OFFSET
      ) +% textureMatrixInter(YB_INTER_OFFSET)
    }
    // cx + dy
    when(~io.aresetn) {
      textureMatrixFinal(Y_FINAL_OFFSET) := 0.S(32.W)
    }.elsewhen(io.enable && ~write && ~writeTexels && mState.io.output(7)) {
      textureMatrixFinal(Y_FINAL_OFFSET) := textureMatrixInter(
        XC_INTER_OFFSET
      ) +% textureMatrixInter(YD_INTER_OFFSET)
    }
    when(~io.aresetn) {
      textureResult := 0.U(8.W)
    }.elsewhen(io.enable && ~write && ~writeTexels) {
      textureResult := textureMemory.read(
        Cat(
          textureMatrixFinal(Y_FINAL_OFFSET)(
            length + SHIFT_FACTOR,
            SHIFT_FACTOR
          ),
          textureMatrixFinal(X_FINAL_OFFSET)(
            length + SHIFT_FACTOR,
            SHIFT_FACTOR
          )
        )
      )
    }.elsewhen(io.enable && writeTexels && mState.io.output(5)) {
      textureMemory.write(address, data)
    }
    io.ready := mState.io.output(9)
    io.outTst := textureResult(6)
    io.outBlu := textureResult(5, 4)
    io.outGrn := textureResult(3, 2)
    io.outRed := textureResult(1, 0)
  }
}
