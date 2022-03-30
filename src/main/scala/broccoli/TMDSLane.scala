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

    val enable = Input(Bool())

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

    val mState = Module(new OHFiniteStateMachine(5))
    mState.io.aresetn := io.aresetn
    mState.io.enable := io.enable

    // Lookup Tables: XORs and XNORs
    val mXOR = Module(new TMDSLaneXOR(8, false))
    val mXNOR = Module(new TMDSLaneXOR(8, true))
    val n1sD = Reg(0.S(4.W))
    mXOR.io.aresetn := io.aresetn
    mXNOR.io.aresetn := io.aresetn
    mXOR.io.input := io.d
    mXNOR.io.input := io.d

    // Two 10-to-2 pumps
    val mPumps = Seq.fill(2)(Module(new TMDSLanePump))
    val mPumpsSwap = false.B
    val qParallelPre = RegInit(0.U(9.W))
    val qParallelInter = RegInit(0.U(9.W))
    val qParallel = RegInit(0.U(10.W))
    val qCondDecodes = RegInit(0.U(2.W))
    for (pump <- mPumps) {
      pump.io.aresetn := io.aresetn
      pump.io.d := qParallel
    }
    io.qq := Mux(mPumpsSwap, mPumps(0).io.qq, mPumps(1).io.qq)
    mPumps(0).io.wen := ~mPumpsSwap
    mPumps(1).io.wen := mPumpsSwap

    val cnt = RegInit(0.S(CNT_WIDTH.W))
    io.debugCnt := cnt

    // Lock on TMDS control commands
    val lockOnC = RegInit(0.U(2.W))
    when(~io.aresetn) {
      lockOnC := 0.U(2.W)
    }.elsewhen(mState.io.output(0)) {
      lockOnC := RegNext(io.c)
    }

    // Lock on TMDS data enable bit
    val lockOnDE = RegInit(false.B)
    when(~io.aresetn) {
      lockOnDE := false.B
    }.elsewhen(mState.io.output(0)) {
      lockOnDE := RegNext(io.de)
    }

    // Lock on TMDS data byte
    val lockOnD = RegInit(0.U(8.W))
    when(~io.aresetn) {
      lockOnD := false.B
    }.elsewhen(mState.io.output(0)) {
      lockOnD := RegNext(io.d)
    }
    when(~io.aresetn) {
      n1sD := 0.S(4.W)
    }.elsewhen(mState.io.output(1)) {
      n1sD := RegNext(PopCount(lockOnD))
    }
    when(~io.aresetn) {
      qParallelPre := 0.U(9.W)
    }.elsewhen(mState.io.output(2)) {
      qParallelPre := RegNext(
        Mux(
          (n1sD > 4.S(4.W) || ((n1sD === 4.S(4.W)) && lockOnD(0))),
          mXNOR.io.output,
          mXOR.io.output
        )
      )
    }
    val n0sM = Reg(0.S(4.W))
    val n1sM = Reg(0.S(4.W))
    when(~io.aresetn) {
      n0sM := 0.S(4.W)
    }.elsewhen(mState.io.output(3)) {
      n0sM := RegNext(PopCount(~qParallelPre(7, 0)))
    }
    when(~io.aresetn) {
      n1sM := 0.S(4.W)
    }.elsewhen(mState.io.output(3)) {
      n1sM := RegNext(PopCount(qParallelPre(7, 0)))
    }
    when(~io.aresetn) {
      qParallel := 0.U(10.W)
    }.elsewhen(mState.io.output(4)) {
      qParallel := Mux((cnt === 0.S(CNT_WIDTH.W)) || (n1sM === n0sM), Cat(qParallelPre(8), ~qParallelPre(8), Mux(qParallelPre(8), qParallelPre(7,0), qPa))
        
        
         Cat(
        ,
        ((cnt > 0.S(CNT_WIDTH.W)) && (n1sM > n0sM)) || ((cnt < 0.S(CNT_WIDTH.W)) && (n0sM > n1sM)) 
      )
    }
  }

  // withReset(~io.aresetn) {
  //   val mState = Module(new OHFiniteStateMachine(5))
  //   val mXOR = Module(new TMDSLaneXOR(8, false))
  //   val mXNOR = Module(new TMDSLaneXOR(8, true))
  //   val cnt = RegInit(0.S(CNT_WIDTH.W))
  //   val qq = RegInit(0.U(2.W))
  //   val nextOut = RegInit(0.U(10.W))
  //   val lockOnD = RegInit(0.U(8.W))
  //   val lockOnM = RegInit(0.U(9.W))
  //   val lockOnDE = RegInit(false.B)
  //   val lockOnC = RegInit(0.U(2.W))
  //   val qNsEq = RegInit(false.B)
  //   val qDsP1 = RegInit(0.S(4.W))
  //   val qMsP0 = RegInit(0.S(4.W))
  //   val qMsP1 = RegInit(0.S(4.W))
  //   val lockOnZeroed = RegInit(false.B)
  //   val lockOnUnevenMag = RegInit(false.B)

  //   mState.io.aresetn := io.aresetn
  //   mState.io.enable := io.enable

  //   mXOR.io.aresetn := io.aresetn
  //   mXNOR.io.aresetn := io.aresetn

  //   mXOR.io.input := lockOnD
  //   mXNOR.io.input := lockOnD

  //   when(~io.aresetn) {
  //     lockOnC := 0.U(2.W)
  //     lockOnD := 0.U(8.W)
  //     lockOnDE := false.B
  //   }.elsewhen(mState.io.output(0)) {
  //     lockOnC := RegNext(io.c)
  //     lockOnD := RegNext(io.d)
  //     lockOnDE := RegNext(io.de)
  //   }

  //   when(~io.aresetn) {
  //     qDsP1 := 0.S(4.W)
  //   }.elsewhen(mState.io.output(1)) {
  //     qDsP1 := RegNext(PopCount(lockOnD))
  //   }

  //   when(~io.aresetn) {
  //     lockOnM := 0.U(9.W)
  //   }.elsewhen(mState.io.output(2)) {
  //     lockOnM := RegNext(
  //       Mux(
  //         (qDsP1 > 4
  //           .S(4.W)) || ((qDsP1 === 4.S(4.W)) && ~lockOnD(0)),
  //         mXNOR.io.output,
  //         mXOR.io.output
  //       )
  //     )
  //   }

  //   when(~io.aresetn) {
  //     lockOnZeroed := false.B
  //     lockOnUnevenMag := false.B
  //   }.elsewhen(mState.io.output(3)) {
  //     lockOnZeroed := RegNext((~cnt.asUInt.orR) || (PopCount(
  //       ~lockOnM(7, 0)
  //     ) === PopCount(lockOnM(7, 0))))
  //     lockOnUnevenMag := RegNext(((cnt > 0.S(CNT_WIDTH.W)) || ((cnt < 0.S(CNT_WIDTH.W)) && (PopCount(~lockOnM(7, 0)) > PopCount(lockOnM(7, 0))))))
  //   }

  //   when(~io.aresetn) {
  //     nextOut := 0.U(10.W)
  //   }.elsewhen(~lockOnDE & mState.io.output(4)) {
  //     switch(lockOnC) {
  //       is("b00".U(2.W)) { nextOut := RegNext(CONTROL_CODE_0.U(10.W)) }
  //       is("b01".U(2.W)) { nextOut := RegNext(CONTROL_CODE_1.U(10.W)) }
  //       is("b10".U(2.W)) { nextOut := RegNext(CONTROL_CODE_2.U(10.W)) }
  //       is("b11".U(2.W)) { nextOut := RegNext(CONTROL_CODE_3.U(10.W)) }
  //     }
  //     cnt := RegNext(0.S(CNT_WIDTH.W))
  //   }.elsewhen(mState.io.output(4)) {
  //   }
  //   when(~io.aresetn) {
  //     switch(mState.io.output) {
  //       is("b00001".U(5.W)) {
  //         qq := nextOut(1, 0)
  //       }
  //       is("b00010".U(5.W)) {
  //         qq := nextOut(3, 2)
  //       }
  //       is("b00100".U(5.W)) {
  //         qq := nextOut(5, 4)
  //       }
  //       is("b01000".U(5.W)) {
  //         qq := nextOut(7, 6)
  //       }
  //       is("b10000".U(5.W)) {
  //         qq := nextOut(9, 8)
  //       }
  //     }
  //   }
  //   io.qq := qq
  //   io.debugCnt := cnt
  //   io.debugQOut := nextOut
  // }
}
