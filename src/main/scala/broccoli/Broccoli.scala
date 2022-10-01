// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** root project to generate Verilog from
  */
class Broccoli extends Module {
  final val FRAMEBUFFER_PEL_WIDTH = 3
  final val NUM_TEXTURECELLS = 12

  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val fclk = Input(Clock())

    val outRedTMDS = Output(Bool())
    val outGrnTMDS = Output(Bool())
    val outBluTMDS = Output(Bool())

    val vBlank = Output(Bool())
    val hBlank = Output(Bool())

    val address = Input(UInt(32.W))
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
  })

  withReset(~io.aresetn) {
    val mVGA = Module(new VGAIntervalDriver)
    val mFramebuffers =
      Seq.fill(2)(Module(new Framebuffer(320, 240, FRAMEBUFFER_PEL_WIDTH)))
    val mTMDSLanes = Seq.fill(3)(Module(new TMDSLane()))
    val mTMDSPumps = Seq.fill(3)(Module(new TMDSPump()))
    val which = RegInit(false.B)
    val fired = RegInit(false.B)
    val red = RegInit(0.U(8.W))
    val grn = RegInit(0.U(8.W))
    val blu = RegInit(0.U(8.W))

    for (framebuffer <- mFramebuffers) {
      framebuffer.clock := io.fclk
      framebuffer.io.aresetn := io.aresetn
      framebuffer.io.enable := io.enable
      framebuffer.io.x := mVGA.io.x >> 1
      framebuffer.io.y := mVGA.io.y >> 1
      framebuffer.io.inCol := Cat(
        mVGA.io.x(FRAMEBUFFER_PEL_WIDTH - 1, 0),
        mVGA.io.y(FRAMEBUFFER_PEL_WIDTH - 1, 0),
        0.U(FRAMEBUFFER_PEL_WIDTH.W)
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

    io.vBlank := mVGA.io.vblankn
    io.hBlank := mVGA.io.hblankn

    mTMDSLanes(0).io.data := Cat(
      Mux(
        which,
        mFramebuffers(0).io.outCol(2, 0),
        mFramebuffers(1).io.outCol(2, 0)
      ),
      0.U(5.W)
    )
    mTMDSLanes(0).io.control := Cat(mVGA.io.vSync, mVGA.io.hSync)

    mTMDSLanes(1).io.data := Cat(
      Mux(
        which,
        mFramebuffers(0).io.outCol(5, 3),
        mFramebuffers(1).io.outCol(5, 3)
      ),
      0.U(5.W)
    )
    mTMDSLanes(1).io.control := 0.U(2.W)

    mTMDSLanes(2).io.data := Cat(
      Mux(
        which,
        mFramebuffers(0).io.outCol(8, 6),
        mFramebuffers(1).io.outCol(8, 6)
      ),
      0.U(5.W)
    )
    mTMDSLanes(2).io.control := 0.U(2.W)

    mTMDSPumps(0).io.in := mTMDSLanes(0).io.out
    mTMDSPumps(1).io.in := mTMDSLanes(1).io.out
    mTMDSPumps(2).io.in := mTMDSLanes(2).io.out

    io.outBluTMDS := mTMDSPumps(0).io.out
    io.outGrnTMDS := mTMDSPumps(1).io.out
    io.outRedTMDS := mTMDSPumps(2).io.out

    for (tmdsPump <- mTMDSPumps) {
      tmdsPump.io.enable := io.enable
      tmdsPump.io.aresetn := io.aresetn
      tmdsPump.clock := io.fclk
    }

    for (tmdsLane <- mTMDSLanes) {
      tmdsLane.clock := io.fclk
      tmdsLane.io.dataEnable := mVGA.io.vblankn && mVGA.io.hblankn
      tmdsLane.io.aresetn := io.aresetn
      tmdsLane.io.enable := io.enable
    }

    io.dataOut := 0.U(32.W)
  }
}
