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

    val outRed = Output(UInt(4.W))
    val outGrn = Output(UInt(4.W))
    val outBlu = Output(UInt(4.W))
    val outVSync = Output(Bool())
    val outHSync = Output(Bool())

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
    val which = RegInit(false.B)
    val fired = RegInit(false.B)
    val mSprites = Seq.fill(NUM_TEXTURECELLS)(Module(new TextureCell(5)))
    val mSpritesAccumulator = RegInit(VecInit.fill(3)(0.U(3.W)))

    mSpritesAccumulator(0) := mSprites.foldLeft(0.U(3.W))(
      (accumulator, next_tu) => {
        Cat(
          Mux(next_tu.io.outTst, next_tu.io.outRed, accumulator),
          false.B
        )
      }
    )
    mSpritesAccumulator(1) := mSprites.foldLeft(0.U(3.W))(
      (accumulator, next_tu) => {
        Cat(
          Mux(next_tu.io.outTst, next_tu.io.outGrn, accumulator),
          false.B
        )
      }
    )
    mSpritesAccumulator(2) := mSprites.foldLeft(0.U(3.W))(
      (accumulator, next_tu) => {
        Cat(
          Mux(next_tu.io.outTst, next_tu.io.outBlu, accumulator),
          false.B
        )
      }
    )

    for (framebuffer <- mFramebuffers) {
      framebuffer.clock := io.fclk
      framebuffer.io.aresetn := io.aresetn
      framebuffer.io.enable := io.enable
      framebuffer.io.x := mVGA.io.x >> 1
      framebuffer.io.y := mVGA.io.y >> 1
      framebuffer.io.inCol := Cat(
        mSpritesAccumulator(2),
        mSpritesAccumulator(1),
        mSpritesAccumulator(0)
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
      mFramebuffers(0).io
        .outCol((FRAMEBUFFER_PEL_WIDTH * 1) - 1, FRAMEBUFFER_PEL_WIDTH * 0),
      mFramebuffers(1).io
        .outCol((FRAMEBUFFER_PEL_WIDTH * 1) - 1, FRAMEBUFFER_PEL_WIDTH * 0)
    )
    io.outGrn := Mux(
      which,
      mFramebuffers(0).io
        .outCol((FRAMEBUFFER_PEL_WIDTH * 2) - 1, FRAMEBUFFER_PEL_WIDTH * 1),
      mFramebuffers(1).io
        .outCol((FRAMEBUFFER_PEL_WIDTH * 2) - 1, FRAMEBUFFER_PEL_WIDTH * 1)
    )
    io.outBlu := Mux(
      which,
      mFramebuffers(0).io
        .outCol((FRAMEBUFFER_PEL_WIDTH * 3) - 1, FRAMEBUFFER_PEL_WIDTH * 2),
      mFramebuffers(1).io
        .outCol((FRAMEBUFFER_PEL_WIDTH * 3) - 1, FRAMEBUFFER_PEL_WIDTH * 2)
    )
  }
}
