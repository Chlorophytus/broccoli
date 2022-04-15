// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** root project to generate Verilog from
  */
class Broccoli extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val fclk = Input(Clock())

    val outRed = Output(UInt(4.W))
    val outGrn = Output(UInt(4.W))
    val outBlu = Output(UInt(4.W))
    val outVSync = Output(Bool())
    val outHSync = Output(Bool())

    val vBlank = Output(Bool())
    val hBlank = Output(Bool())
  })

  withReset(~io.aresetn) {
    val mVGA = Module(new VGAIntervalDriver)
    val mFramebuffers = Seq.fill(2)(Module(new Framebuffer(320, 240)))
    val which = RegInit(false.B)
    val fired = RegInit(false.B)

    for (framebuffer <- mFramebuffers) {
      framebuffer.clock := io.fclk
      framebuffer.io.aresetn := io.aresetn
      framebuffer.io.enable := io.enable
      framebuffer.io.x := mVGA.io.x >> 1
      framebuffer.io.y := mVGA.io.y >> 1
      framebuffer.io.inCol := Cat(
        "b1111".U(4.W),
        mVGA.io.y(3, 0),
        mVGA.io.x(3, 0)
      )
    }
    mFramebuffers(0).io.write := ~which
    mFramebuffers(1).io.write := which

    when(~mVGA.io.vblankn && ~fired) {
      which := ~which
      fired := true.B
    }

    when(mVGA.io.vblankn) {
      fired := false.B
    }

    mVGA.clock := clock

    mVGA.io.aresetn := io.aresetn
    mVGA.io.enable := io.enable

    io.outVSync := mVGA.io.vSync
    io.outHSync := mVGA.io.hSync

    io.vBlank := mVGA.io.vblankn
    io.hBlank := mVGA.io.hblankn

    io.outRed := Mux(
      which,
      mFramebuffers(0).io.outCol(3, 0),
      mFramebuffers(1).io.outCol(3, 0)
    )
    io.outGrn := Mux(
      which,
      mFramebuffers(0).io.outCol(7, 4),
      mFramebuffers(1).io.outCol(7, 4)
    )
    io.outBlu := Mux(
      which,
      mFramebuffers(0).io.outCol(11, 8),
      mFramebuffers(1).io.outCol(11, 8)
    )
  }
}
