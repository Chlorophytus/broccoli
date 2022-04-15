// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class Framebuffer(width: Int, height: Int) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val write = Input(Bool())
    val ready = Output(Bool())
    val x = Input(UInt(12.W))
    val y = Input(UInt(12.W))
    val inCol = Input(UInt(12.W))
    val outCol = Output(UInt(12.W))
  })
  withReset(~io.aresetn) {
    val mState = Module(new OHFiniteStateMachine(5))
    val mMemory = SyncReadMem(width * height, UInt(12.W))
    val index = RegInit(0.U(24.W))
    val x = RegInit(0.U(12.W))
    val y = RegInit(0.U(12.W))
    val color = RegInit(0.U(12.W))

    mState.clock := clock
    mState.io.aresetn := io.aresetn
    mState.io.enable := io.enable

    when(io.enable && mState.io.output(0)) {
      y := io.y
    }
    when(io.enable && mState.io.output(0)) {
      x := io.x
    }
    when(io.enable && mState.io.output(0)) {
      color := io.inCol
    }
    // I know this instantiates a FMA so I will make it good
    when(io.enable && mState.io.output(1)) {
      index := (y * width.asUInt) + x
    }
    when(io.enable && mState.io.output(3) && io.write) {
      mMemory.write(index, color)
    }
    io.outCol := mMemory.read(index)
    io.ready := mState.io.output(4)
  }
}
