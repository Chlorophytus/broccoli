// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS (Transition Minimized Differential Signal) lane
  *
  * useful for DVI video signalling
  */
class TMDSLane extends Module {
  final val CONTROL_CODE_0 = "b1101010100"
  final val CONTROL_CODE_1 = "b0010101011"
  final val CONTROL_CODE_2 = "b0101010100"
  final val CONTROL_CODE_3 = "b1010101011"
  final val CNT_WIDTH = 6
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val de = Input(Bool())
    val d = Input(UInt(8.W))
    val c = Input(UInt(2.W))
    val qq = Output(UInt(2.W))

    val debugCnt = Output(SInt(CNT_WIDTH.W))
  })

  withReset(~io.aresetn) {
    val controlCodes = VecInit(
      CONTROL_CODE_0.U(10.W),
      CONTROL_CODE_1.U(10.W),
      CONTROL_CODE_2.U(10.W),
      CONTROL_CODE_3.U(10.W)
    )

    // Lookup Tables: XORs and XNORs
    val mXOR = Module(new TMDSLaneXOR(8, false))
    val mXNOR = Module(new TMDSLaneXOR(8, true))
    mXOR.io.aresetn := io.aresetn
    mXNOR.io.aresetn := io.aresetn
    mXOR.io.input := io.d
    mXNOR.io.input := io.d

    // Two 10-to-2 pumps
    val mPumps = Seq.fill(2)(Module(new TMDSLanePump))
    val mPumpsSwap = false.B
    val qParallel = RegInit(0.U(10.W))
    for (pump <- mPumps) {
      pump.io.aresetn := io.aresetn
      pump.io.d := qParallel
    }
    io.qq := Mux(mPumpsSwap, mPumps(0).io.qq, mPumps(1).io.qq)
    mPumps(0).io.wen := ~mPumpsSwap
    mPumps(1).io.wen := mPumpsSwap

    val qParallelPre = RegInit(0.U(9.W))
    when(~io.aresetn) {
      qParallelPre := 0.U(9.W)
    }.elsewhen(
      (PopCount(io.d) > 4.U(4.W)) || ((PopCount(io.d) === 4.U(4.W)) && !io
        .d(0))
    ) { qParallelPre := mXNOR.io.output }
      .otherwise {
        qParallelPre := mXOR.io.output
      }
    when(~io.aresetn) {
      q
    }
    
  }
}
