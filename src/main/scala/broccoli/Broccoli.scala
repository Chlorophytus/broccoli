package broccoli

import chisel3._
import chisel3.util._

class Broccoli extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val clockD0 = Input(Clock())
    val clockD90 = Input(Clock())

    val tmdsOBlu = Output(Bool())
    val tmdsOGrn = Output(Bool())
    val tmdsORed = Output(Bool())
  })

  withReset(!io.aresetn) {
    val mVideo = Module(new VideoProcessor)

    mVideo.clock := clock
    mVideo.io.aresetn := io.aresetn
    mVideo.io.enable := io.enable

    mVideo.io.clockD0 := io.clockD0
    mVideo.io.clockD90 := io.clockD90

    io.tmdsOBlu := mVideo.io.tmdsOBlu
    io.tmdsOGrn := mVideo.io.tmdsOGrn
    io.tmdsORed := mVideo.io.tmdsORed
  }
}
