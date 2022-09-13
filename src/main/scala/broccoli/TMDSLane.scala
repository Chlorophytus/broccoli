// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS encoder, requires a shift register to serialize data
 * 
 * loosely based off https://gist.github.com/alsrgv/3cf171c17fffe25806693c26ebb276a8
 */
class TMDSLane extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val dataEnable = Input(Bool())
    val data = Input(UInt(8.W))
    val control = Input(UInt(2.W))
    val out = Output(UInt(10.W))
    val debugDisparity = Output(SInt(5.W))
    val debugUseXOR = Output(Bool())
    val debugInvertQM = Output(Bool())
  })

  withReset(~io.aresetn) {
    val mXorNegative = Module(new TMDSLaneXOR(8, true))
    val mXorPositive = Module(new TMDSLaneXOR(8, false))
    val mState = Module(new OHFiniteStateMachine(5))
    val disparity = RegInit(0.S(5.W))
    val popCountDIn = RegInit(0.U(4.W))
    val data = RegInit(0.U(8.W))
    val dataEnable = RegInit(false.B)
    val q_m = RegInit(0.U(8.W))
    val popCountQ_M = RegInit(0.U(4.W))
    val differenceQ_M = RegInit(0.S(5.W))
    val useXOR = RegInit(false.B)
    val invertQ_M = RegInit(false.B)
    val control = RegInit(0.U(2.W))
    val controlCodes =
      VecInit(
        "b1101010100".U(10.W), // 00
        "b0010101011".U(10.W), // 01
        "b0101010100".U(10.W), // 10
        "b1010101011".U(10.W) // 11
      )
    val out = RegInit(0.U(10.W))
    //State
    mState.clock := clock
    mState.io.aresetn := io.aresetn
    mState.io.enable := io.enable
    // XNOR
    mXorNegative.clock := clock
    mXorNegative.io.aresetn := io.aresetn
    mXorNegative.io.input := data
    // XOR
    mXorPositive.clock := clock
    mXorPositive.io.aresetn := io.aresetn
    mXorPositive.io.input := data
    // Outputs
    io.out := out
    io.debugDisparity := disparity
    io.debugUseXOR := useXOR
    io.debugInvertQM := invertQ_M
    // lock on
    when(~io.aresetn) {
      data := 0.U(8.W)
    }.elsewhen(io.enable && mState.io.output(0)) {
      data := io.data
    }
    when(~io.aresetn) {
      dataEnable := 0.U(8.W)
    }.elsewhen(io.enable && mState.io.output(0)) {
      dataEnable := io.dataEnable
    }
    when(~io.aresetn) {
      control := 0.U(2.W)
    }.elsewhen(io.enable && mState.io.output(0)) {
      control := io.control
    }
    // get the POPCNT
    when(~io.aresetn) {
      popCountDIn := 0.U(4.W)
    }.elsewhen(io.enable && mState.io.output(1)) {
      popCountDIn := PopCount(data)
    }
    // XOR or XNOR?
    when(~io.aresetn) {
      useXOR := false.B
    }.elsewhen(io.enable && mState.io.output(1)) {
      useXOR := (popCountDIn < 4.U(4.W)) || ((popCountDIn === 4.U(4.W)) && data(
        0
      ))
    }
    // determine Q_M
    when(~io.aresetn) {
      q_m := 0.U(8.W)
    }.elsewhen(io.enable && mState.io.output(2)) {
      q_m := Mux(useXOR, mXorPositive.io.output, mXorNegative.io.output)
    }

    when(~io.aresetn) {
      popCountQ_M := 0.U(8.W)
    }.elsewhen(io.enable && mState.io.output(2)) {
      popCountQ_M := PopCount(
        Mux(useXOR, mXorPositive.io.output, mXorNegative.io.output)
      )
    }
    // invert q_m or not?
    when(~io.aresetn) {
      invertQ_M := false.B
    }.elsewhen(io.enable && mState.io.output(3)) {
      invertQ_M := Mux(
        disparity === 0.S(12.W) && popCountQ_M === 4.U(4.W),
        ~useXOR,
        ((disparity > 0.S(5.W)) && (popCountQ_M > 4.U(
          4.W
        ))) || ((disparity < 0.S(5.W)) && popCountQ_M < 4.U(4.W))
      )
    }
    // other magic value
    when(~io.aresetn) {
      differenceQ_M := 0.S(5.W)
    }.elsewhen(io.enable && mState.io.output(3)) {
      differenceQ_M := (popCountQ_M << 1).asSInt - 8.S(5.W)
    }
    // disparity counter
    when(~io.aresetn) {
      disparity := 0.S(5.W)
    }.elsewhen(io.enable && ~dataEnable && mState.io.output(4)) {
      disparity := 0.S(5.W)
    }.elsewhen(io.enable && dataEnable && mState.io.output(4)) {
      disparity := disparity + Mux(
        invertQ_M,
        -differenceQ_M,
        differenceQ_M
      ) + Mux(invertQ_M, 1.S(5.W), -1.S(5.W))
    }
    // output
    when(~io.aresetn) {
      out := 0.U(10.W)
    }.elsewhen(io.enable && ~dataEnable && mState.io.output(4)) {
      out := controlCodes(control)
    }.elsewhen(io.enable && dataEnable && mState.io.output(4)) {
      out := Cat(invertQ_M, useXOR, Mux(invertQ_M, ~q_m, q_m))
    }
  }
}
