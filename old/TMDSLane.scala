// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS (Transition Minimized Differential Signal) lane
  *
  * useful for DVI video signalling
  */
class TMDSLane extends Module {
  final val cntWidth = 12
  final val popcntWidth = 3
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val wen = Input(Bool())
    val strobe = Input(Bool())

    val de = Input(Bool())
    val d = Input(UInt(8.W))
    val c = Input(UInt(2.W))

    val qq = Output(UInt(2.W))

    val debugCnt = Output(SInt(cntWidth.W))
    val debugQWhich = Output(Bool())
    val debugQOut = Output(UInt(10.W))
  })

  withReset(~io.aresetn) {
    // hold on by registers
    val de = false.B
    val d = 0.U(8.W)
    val c = 0.U(2.W)

    // n1(D)
    val mPopCountPhase1 = Module(new PopulationCount(3))
    val n1d = 0.U(cntWidth.W)

    // q_m
    val qm = 0.U(9.W)

    // n0(q_m[7:0]) for 1st
    // n1(q_m[7:0]) for 2nd
    val mPopCountsPhase2 = Seq.fill(2)(Module(new PopulationCount(popcntWidth)))

    val nqm = VecInit(4.U(cntWidth.W), 4.U(cntWidth.W))

    // cnt
    val cnt = 0.S(cntWidth.W)

    val state = 0.U(5.W)

    val branchFor0To1 = false.B
    val branchFor1To2 = false.B
    val branchFor2To3 = false.B

    // FIFO thing
    val fifos = VecInit(0.U(10.W), 0.U(10.W))
    val readyFifo = true.B

    mPopCountPhase1.io.input := d
    mPopCountsPhase2(0).io.input := ~qm
    mPopCountsPhase2(1).io.input := qm
    // STATE TABLE:
    // 0) xchg q_out with FIFO; store input D, DE, C
    // 1) Check if DE hi: branch to 2 if so, otherwise do special !DE amnesia
    // 2a) Check if (N1{D}>4) OR (N1{D} == 4 AND ~D[0]) ...
    // 2b( 2a)) this causes a cascade of __XNOR__
    // 2b(!2a)) this causes a cascade of __XOR__
    // 3) Check if (LastCnt == 0) OR (N1{q_m[7:0]} == N0{q_m[7:0]}): branch
    // 4( 3)) Check if (q_m[8]), determines Cnt accumulator delta
    // 4(!3)) See detailed explanation of this condition of TMDS.
    when(~io.aresetn) {
      readyFifo := true.B
    }.elsewhen(state(0)) {
      readyFifo := ~readyFifo
    }
    when(~io.aresetn) {
      fifos := VecInit(0.U(10.W), 0.U(10.W))
    }
    switch(state) {
      is(1.U(5.W) << 0) { io.qq := fifos(readyFifo)(9, 8) }
      is(1.U(5.W) << 1) { io.qq := fifos(readyFifo)(7, 6) }
      is(1.U(5.W) << 2) { io.qq := fifos(readyFifo)(5, 4) }
      is(1.U(5.W) << 3) { io.qq := fifos(readyFifo)(3, 2) }
      is(1.U(5.W) << 4) { io.qq := fifos(readyFifo)(1, 0) }
    }
    // =========================================================================
    // STUFF FOR FLIPFLOPPING REGISTERS
    // =========================================================================
    when(~io.aresetn) {
      d := 0.U(8.W)
    }.elsewhen(io.wen && state(0)) {
      d := io.d
    }
    when(~io.aresetn) {
      de := false.B
    }.elsewhen(io.wen && state(0)) {
      de := io.de
    }
    when(~io.aresetn) {
      c := 0.U(2.W)
    }.elsewhen(io.wen && state(0)) {
      c := io.c
    }

    when(~io.aresetn) {
      n1d := 0.U(cntWidth.W)
    }.elsewhen(state(0)) {
      n1d := mPopCountPhase1.io.output
    }

    for (i <- 0 to 2) {
      nqm(i) := Cat(
        0.U((cntWidth - popcntWidth).W),
        mPopCountsPhase2(i).io.output
      )
    }
    // =========================================================================
    // TMDS BRANCHING DETERMINE
    // =========================================================================
    // Branching S[1] => S[2]
    // When:
    // (cnt == 0) => Branch TRUE
    // (n1(q_m[7:0]) == n0(q_m[7:0])) => Branch TRUE
    // Default => Branch FALSE
    when(~io.aresetn) {
      branchFor1To2 := false.B
    }.elsewhen(state(1)) {
      branchFor1To2 := ~(cnt.asUInt().andR()) || (nqm(0) === nqm(1))
    }
    // Branching S[2] => S[3] (when S[1] => S[2] branch FALSE)
    // When:
    // (cnt > 0) && (n1(q_m[7:0]) > 4) => Branch TRUE
    // (cnt < 0) && (n1(q_m[7:0]) < 4) => Branch TRUE
    // Default => Branch FALSE
    when(~io.aresetn) {
      branchFor2To3 := false.B
    }.elsewhen(state(1)) {
      branchFor2To3 := ((cnt > 0.S(cntWidth.W)) && (nqm(1) > nqm(
        0
      ))) || ((cnt > 0.S(cntWidth.W)) && (nqm(1) > nqm(0)))
    }
    // =========================================================================
    // TMDS COUNTER DETERMINE
    // =========================================================================
    when(~io.aresetn || ~de) {
      cnt := 0.S(cntWidth.W)
    }.elsewhen(state(3) && branchFor1To2) {
      cnt := cnt + Mux(qm(8), nqm(1) - nqm(0), nqm(0) - nqm(1)).asSInt()
    }.elsewhen(state(3)) {
      cnt := Mux(
        branchFor2To3,
        (cnt + Cat(0.U((cntWidth - 2).W), qm(8), false.B)
          .asSInt() + (nqm(0) - nqm(1)).asSInt()),
        (cnt - Cat(0.U((cntWidth - 2).W), qm(8), false.B)
          .asSInt() + (nqm(1) - nqm(0)).asSInt())
      )
    }
  }
}
