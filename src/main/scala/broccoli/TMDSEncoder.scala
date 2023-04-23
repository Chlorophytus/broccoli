package broccoli

import chisel3._
import chisel3.util._

/** DVI lane encoder
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
    val tmdsDDisparity = Output(SInt(5.W))
    val tmdsDXORIfTrue = Output(Bool())
  })

  withReset(!io.aresetn) {
    val mXOR = Module(new TMDSXOR(8, false))
    val mXNOR = Module(new TMDSXOR(8, true))

    mXOR.io.aresetn := io.aresetn
    mXNOR.io.aresetn := io.aresetn
    mXOR.io.input := io.tmdsIData
    mXNOR.io.input := io.tmdsIData

    val rDisparity = RegInit(0.S(5.W))

    val rOnesIData = RegInit(0.U(4.W))
    val rInterQ_M = RegInit(0.U(8.W))
    val rOnesInterQ_M = RegInit(0.U(4.W))
    val rDifferenceInterQ_M = RegInit(0.S(5.W))
    val rInvertQ_M = RegInit(false.B)
    val rUseXOR = RegInit(false.B)
    val rOData = RegInit(0.U(10.W))

    val controlCodes = VecInit(
      "b1101010100".U(10.W), // 00
      "b0010101011".U(10.W), // 01
      "b0101010100".U(10.W), // 10
      "b1010101011".U(10.W) // 11
    )

    rInvertQ_M := Mux(
      (rDisparity === 0.S(5.W) && rOnesInterQ_M === 4.U(4.W)),
      ~rUseXOR,
      (rDisparity > 0.S(5.W) && rOnesInterQ_M > 4.U(4.W)) || (rDisparity < 0.S(
        5.W
      ) && rOnesInterQ_M < 4.U(4.W))
    )

    rOnesIData := PopCount(io.tmdsIData)
    rUseXOR := rOnesIData < 4
      .U(4.W) || (rOnesIData === 4.U(4.W) && io.tmdsIData(0))
    rInterQ_M := Mux(rUseXOR, mXOR.io.output, mXNOR.io.output)
    rOnesInterQ_M := PopCount(rInterQ_M)
    rDifferenceInterQ_M := Cat(rOnesInterQ_M, false.B).asSInt - 8.S(5.W)

    when(!io.aresetn) {
      rOData := 0.U(10.W)
    }.elsewhen(io.enable && io.tmdsIDataEnable) {
      rOData := Cat(rInvertQ_M, rUseXOR, Mux(rInvertQ_M, ~rInterQ_M, rInterQ_M))
    }.elsewhen(io.enable) {
      rOData := controlCodes(io.tmdsIControl)
    }

    when(!io.aresetn) {
      rDisparity := 0.S(5.W)
    }.elsewhen(io.enable && io.tmdsIDataEnable) {
      rDisparity := rDisparity + Mux(
        rInvertQ_M,
        -rDifferenceInterQ_M,
        rDifferenceInterQ_M
      ) + Mux(rInvertQ_M, 1.S(5.W), -1.S(5.W))
    }.elsewhen(io.enable) {
      rDisparity := 0.S(5.W)
    }

    io.tmdsOData := rOData
    io.tmdsDDisparity := rDisparity
    io.tmdsDXORIfTrue := rUseXOR
  }
}
