package broccoli

import chisel3._
import chisel3.util._

/** DVI lane encoder.
  *
  * based off https://gist.github.com/alsrgv/3cf171c17fffe25806693c26ebb276a8
  */
class TMDSEncoder extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())

    // Input
    val tmdsIDataEnable = Input(Bool())
    val tmdsIData = Input(UInt(8.W))
    val tmdsIControl = Input(UInt(2.W))

    // Output
    val tmdsOData = Output(UInt(10.W))

    // Debug
    val tmdsDDisparity = Output(SInt(6.W))
    val tmdsDXORIfTrue = Output(Bool())
  })

  withReset(!io.aresetn) {
    val mXOR = Module(new TMDSXOR(8, false))
    val mXNOR = Module(new TMDSXOR(8, true))

    val rDisparity = RegInit(0.S(6.W))
    val rState = RegInit(1.U(5.W))
    val rOnesIData = RegInit(0.U(4.W))
    val rInterQ_M = RegInit(0.U(8.W))
    val rOnesInterQ_M = RegInit(0.U(6.W))
    val rZerosInterQ_M = RegInit(0.U(6.W))
    val rInvertQ_M = RegInit(false.B)
    val rUseXOR = RegInit(false.B)
    val rOData = RegInit(0.U(10.W))
    val rIData = RegInit(0.U(8.W))
    val rDeltaDisparity = RegInit(0.S(6.W))

    val controlCodes = VecInit(
      "b1101010100".U(10.W), // 00
      "b0010101011".U(10.W), // 01
      "b0101010100".U(10.W), // 10
      "b1010101011".U(10.W) // 11
    )


    mXOR.io.aresetn := io.aresetn
    mXNOR.io.aresetn := io.aresetn
    mXOR.io.input := rIData
    mXNOR.io.input := rIData
    // ========================================================================
    when(!io.aresetn) {
      rState := 1.U(5.W)
    }.elsewhen(io.enable) {
      rState := rState.rotateLeft(1)
    }
    // ========================================================================
    when(!io.aresetn) {
      rIData := 0.U(8.W)
    }.elsewhen(io.enable && rState(0)) {
      rIData := io.tmdsIData
    }
    // ========================================================================
    when(!io.aresetn) {
      rInterQ_M := 0.U(8.W)
    }.elsewhen(io.enable && rState(1)){
      rInterQ_M := Mux(rUseXOR, mXOR.io.output, mXNOR.io.output)
    }
    when(!io.aresetn) {
      rOnesIData := 0.U(4.W)
    } .elsewhen(io.enable && rState(1)) {
      rOnesIData := PopCount(rIData)
    }
    // ========================================================================
    when(!io.aresetn) {
      rInvertQ_M := false.B
    }.elsewhen(io.enable && rState(2)) {
      rInvertQ_M := Mux(
        (rDisparity === 0.S(6.W) && rOnesInterQ_M === 4.U(6.W)),
        ~rUseXOR,
        (rDisparity > 0.S(6.W) && rOnesInterQ_M > 4.U(6.W)) || (rDisparity < 0.S(
          6.W
        ) && rOnesInterQ_M < 4.U(6.W))
      )
    }
    when(!io.aresetn) {
      rUseXOR := false.B
    }.elsewhen(io.enable && rState(2)) {
      rUseXOR := rOnesIData < 4
        .U(4.W) || (rOnesIData === 4.U(4.W) && rIData(0))
    }
    when(!io.aresetn) {
      rOnesInterQ_M := 0.U(6.W)
    }.elsewhen(io.enable && rState(2)) {
      rOnesInterQ_M := PopCount(rInterQ_M)
    }
    when(!io.aresetn) {
      rZerosInterQ_M := 0.U(6.W)
    }.elsewhen(io.enable && rState(2)) {
      rZerosInterQ_M := 8.U(6.W) - PopCount(rInterQ_M)
    }
    // ========================================================================
    when(!io.aresetn) {
      rDeltaDisparity := 0.S(6.W)
    }.elsewhen(io.enable && rState(3) && (rDisparity === 0.S(6.W) || rOnesInterQ_M === rZerosInterQ_M)) {
      rDeltaDisparity := Mux(rUseXOR, rZerosInterQ_M.asSInt - rOnesInterQ_M.asSInt, rOnesInterQ_M.asSInt - rZerosInterQ_M.asSInt)
    }.elsewhen(io.enable && rState(3) && ((rDisparity > 0.S(6.W) && rOnesInterQ_M > rZerosInterQ_M) || (rDisparity < 0.S(6.W) && rOnesInterQ_M < rZerosInterQ_M))) {
      rDeltaDisparity := Mux(rUseXOR, 0.S(6.W), 2.S(6.W)) + (rZerosInterQ_M.asSInt - rOnesInterQ_M.asSInt)
    }.elsewhen(io.enable && rState(3)) {
      rDeltaDisparity := Mux(rUseXOR, -2.S(6.W), 0.S(6.W)) + (rOnesInterQ_M.asSInt - rZerosInterQ_M.asSInt)
    }
    // ========================================================================
    when(!io.aresetn) {
      rOData := 0.U(10.W)
    }.elsewhen(io.enable && rState(4) && io.tmdsIDataEnable) {
      rOData := Cat(rInvertQ_M, rUseXOR, Mux(rInvertQ_M, ~rInterQ_M, rInterQ_M))
    }.elsewhen(io.enable && rState(4)) {
      rOData := controlCodes(io.tmdsIControl)
    }
    when(!io.aresetn) {
      rDisparity := 0.S(6.W)
    }.elsewhen(io.enable && rState(4) && io.tmdsIDataEnable) {
      rDisparity := rDisparity + rDeltaDisparity
    }.elsewhen(io.enable && rState(4)) {
      rDisparity := 0.S(6.W)
    }
    // ========================================================================
    io.tmdsOData := rOData
    io.tmdsDDisparity := rDisparity
    io.tmdsDXORIfTrue := rUseXOR
  }
}
