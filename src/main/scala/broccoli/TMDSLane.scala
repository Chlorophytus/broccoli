// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class TMDSLane extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val de = Input(Bool())
    val d = Input(UInt(8.W))
    val c = Input(UInt(2.W))

    val qq = Output(UInt(2.W))

    val debugCnt = Output(SInt(12.W))
    val debugQWhich = Output(Bool())
    val debugQOut = Output(UInt(10.W))
  })
  withReset(~io.aresetn) {
    val qq = Reg(UInt(2.W))
    val q_out = Reg(Vec(2, UInt(10.W)))
    val q_m = Reg(UInt(9.W))

    val cnt = Reg(SInt(12.W))
    val n1_q_m = Reg(SInt(12.W))
    val n1_d = Reg(SInt(12.W))
    val n0_q_m = Reg(SInt(12.W))
    val n0_d = Reg(SInt(12.W))

    val qState = Reg(UInt(5.W)) // Enum support is broken.
    val qWhich = Reg(Bool())
    val deHoldAperture = Reg(Bool())
    val qIntermediary0 = Reg(Bool())
    val qIntermediary1 = Reg(Bool())

    when(~io.aresetn) {
      qWhich := false.B
    }.elsewhen(qState(4)) { qWhich := ~qWhich }

    when(~io.aresetn) {
      deHoldAperture := false.B
    }.elsewhen(qState(4)) { deHoldAperture := io.de }

    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn || qState(4)) {
      qState := 1.U(5.W)
    }.otherwise {
      qState := qState << 1.U(5.W)
    }
    // ========================================================================
    //  INTERMEDIATES
    // ========================================================================
    when(~io.aresetn | ~deHoldAperture) {
      qIntermediary0 := false.B
    }.elsewhen(qState(2)) {
      qIntermediary0 := (~(cnt.asUInt.orR())) || (n1_q_m === n0_q_m)
    }
    when(~io.aresetn | ~deHoldAperture) {
      qIntermediary1 := false.B
    }.elsewhen(qState(2)) {
      qIntermediary1 := ((cnt > 0.S(12.W)) && (n1_q_m > n0_q_m)) || ((cnt < 0.S(
        12.W
      )) && (n1_q_m < n0_q_m))
    }
    // ========================================================================
    //  Q_OUT
    // ========================================================================
    when(~io.aresetn) {
      q_out := VecInit(0.U(10.W), 0.U(10.W))
    }.elsewhen(~deHoldAperture) {
      q_out(~qWhich) := Mux(
        io.c(1),
        Mux(io.c(0), "b1010101011".U(10.W), "b0101010100".U(10.W)),
        Mux(io.c(0), "b0010101011".U(10.W), "b1101010100".U(10.W))
      )
    }.elsewhen(qState(3)) {
      q_out(~qWhich) := Mux(
        qIntermediary0,
        Cat(~q_m(8), q_m(8), Mux(q_m(8), q_m(7, 0), ~q_m(7, 0))),
        Mux(
          qIntermediary1,
          Cat(true.B, q_m(8), ~q_m(7, 0)),
          Cat(false.B, q_m(8), q_m(7, 0))
        )
      )
    }
    when(qState(0)) { qq := q_out(qWhich)(1, 0) }
    when(qState(1)) { qq := q_out(qWhich)(3, 2) }
    when(qState(2)) { qq := q_out(qWhich)(5, 4) }
    when(qState(3)) { qq := q_out(qWhich)(7, 6) }
    when(qState(4)) { qq := q_out(qWhich)(9, 8) }
    io.qq := Cat(qq(0), qq(1))
    // ========================================================================
    //  Q_M
    // ========================================================================
    when(~io.aresetn) {
      q_m := 0.U(9.W)
    }.elsewhen(qState(1)) {
      q_m := Mux(
        n1_d > 4.S(12.W) || ((n1_d === 4.S(12.W)) && ~io.d(0)),
        Cat(
          false.B,
          (~io.d(7, 0)).xorR(),
          (~io.d(6, 0)).xorR(),
          (~io.d(5, 0)).xorR(),
          (~io.d(4, 0)).xorR(),
          (~io.d(3, 0)).xorR(),
          (~io.d(2, 0)).xorR(),
          (~io.d(1, 0)).xorR(),
          io.d(0)
        ),
        Cat(
          true.B,
          io.d(7, 0).xorR(),
          io.d(6, 0).xorR(),
          io.d(5, 0).xorR(),
          io.d(4, 0).xorR(),
          io.d(3, 0).xorR(),
          io.d(2, 0).xorR(),
          io.d(1, 0).xorR(),
          io.d(0)
        )
      )
    }
    // ========================================================================
    //  COUNTER
    // ========================================================================
    when(~io.aresetn | ~deHoldAperture) {
      cnt := 0.S(12.W)
    }.elsewhen(qState(4)) {
      cnt := Mux(
        qIntermediary0,
        Mux(
          q_m(8),
          cnt + (n1_q_m - n0_q_m),
          cnt + (n0_q_m - n1_q_m)
        ),
        Mux(
          qIntermediary1,
          cnt + (q_m(8) * 2.S(12.W)) + (n0_q_m - n1_q_m),
          cnt - (~q_m(8) * 2.S(12.W)) + (n1_q_m - n0_q_m)
        )
      )
    }
    // ========================================================================
    //  N1's (the dreaded POPCNT)
    // ========================================================================
    // D side
    when(~io.aresetn | ~deHoldAperture) {
      n1_d := 4.S(12.W)
    }.elsewhen(qState(0)) {
//       n1_d := ((Cat(0.U(11.W), io.d(0)).asSInt + Cat(
//         0.U(11.W),
//         io.d(1)
//       ).asSInt) + (Cat(0.U(11.W), io.d(2)).asSInt + Cat(
//         0.U(11.W),
//         io.d(3)
//       ).asSInt)) + (Cat(0.U(11.W), io.d(4)).asSInt + Cat(
//         0.U(11.W),
//         io.d(5)
//       ).asSInt) + (Cat(0.U(11.W), io.d(6)).asSInt + Cat(
//         0.U(11.W),
//         io.d(7)
//       ).asSInt)
      n1_d :=
    }
    // Q_M side
    when(~io.aresetn | ~deHoldAperture) {
      n1_q_m := 4.S(12.W)
    }.elsewhen(qState(1)) {
      n1_q_m := ((Cat(0.U(11.W), q_m(0)).asSInt + Cat(
        0.U(11.W),
        q_m(1)
      ).asSInt) + (Cat(0.U(11.W), q_m(2)).asSInt + Cat(
        0.U(11.W),
        q_m(3)
      ).asSInt)) + (Cat(0.U(11.W), q_m(4)).asSInt + Cat(
        0.U(11.W),
        q_m(5)
      ).asSInt) + (Cat(0.U(11.W), q_m(6)).asSInt + Cat(
        0.U(11.W),
        q_m(7)
      ).asSInt)
    }

    n0_d := 8.S(12.W) - n1_d
    n0_q_m := 8.S(12.W) - n1_q_m

    io.debugQOut := q_out(qWhich)
    io.debugQWhich := qWhich
    io.debugCnt := cnt
  }
}
