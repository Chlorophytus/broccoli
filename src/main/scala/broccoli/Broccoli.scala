// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Root project to generate Verilog from
  */
class Broccoli extends Module {
  val io = IO(new Bundle {
    val clockP = Input(Clock())
    val clockD = Input(Clock())
    val aresetn = Input(Bool())

    val tmdsLane0 = Output(Bool())
    val tmdsLane1 = Output(Bool())
    val tmdsLane2 = Output(Bool())
    val tmdsLaneC = Output(Bool())

    val debugVBlank = Output(Bool())
    val debugHBlank = Output(Bool())
  })
  val vga = Module(new VGAIntervalDriver(new VGAIntervalConstraints {
    val width = 640
    val hFrontPorch = 656
    val hBlank = 752
    val hBackPorch = 800
    val hNegateSync = true

    val height = 480
    val vFrontPorch = 490
    val vBlank = 492
    val vBackPorch = 525
    val vNegateSync = true
  }))

  vga.clock := io.clockP
  vga.io.aresetn := io.aresetn

  // TMDS Lanes
  val tmdsLaneModuleB = Module(new TMDSLane())
  val tmdsLaneModuleG = Module(new TMDSLane())
  val tmdsLaneModuleR = Module(new TMDSLane())
  val tmdsLaneModuleC = Module(new TMDSClock())

  tmdsLaneModuleB.io.aresetn := io.aresetn
  tmdsLaneModuleG.io.aresetn := io.aresetn
  tmdsLaneModuleR.io.aresetn := io.aresetn
  tmdsLaneModuleC.io.aresetn := io.aresetn

  io.debugHBlank := vga.io.hBlank
  io.debugVBlank := vga.io.vBlank

  tmdsLaneModuleB.io.de := (~vga.io.hBlank) & (~vga.io.vBlank)
  tmdsLaneModuleG.io.de := (~vga.io.hBlank) & (~vga.io.vBlank)
  tmdsLaneModuleR.io.de := (~vga.io.hBlank) & (~vga.io.vBlank)

  tmdsLaneModuleB.io.d := (vga.io.currentX ^ vga.io.currentY)(9, 2)
  tmdsLaneModuleG.io.d := (vga.io.currentX ^ vga.io.currentY)(8, 1)
  tmdsLaneModuleR.io.d := (vga.io.currentX ^ vga.io.currentY)(7, 0)

  tmdsLaneModuleB.io.c := Cat(vga.io.vSync, vga.io.hSync)
  tmdsLaneModuleG.io.c := 0.U(2.W)
  tmdsLaneModuleR.io.c := 0.U(2.W)

  // DDR
  val tmdsDDRModuleB = Module(new TMDSDDR())
  val tmdsDDRModuleG = Module(new TMDSDDR())
  val tmdsDDRModuleR = Module(new TMDSDDR())
  val tmdsDDRModuleC = Module(new TMDSDDR())

  tmdsDDRModuleB.io.d := tmdsLaneModuleB.io.qq
  tmdsDDRModuleG.io.d := tmdsLaneModuleG.io.qq
  tmdsDDRModuleR.io.d := tmdsLaneModuleR.io.qq
  tmdsDDRModuleC.io.d := tmdsLaneModuleC.io.qq

  io.tmdsLane0 := tmdsDDRModuleB.io.q
  io.tmdsLane1 := tmdsDDRModuleG.io.q
  io.tmdsLane2 := tmdsDDRModuleR.io.q
  io.tmdsLaneC := tmdsDDRModuleC.io.q

  tmdsDDRModuleB.io.clock := io.clockD
  tmdsDDRModuleG.io.clock := io.clockD
  tmdsDDRModuleR.io.clock := io.clockD
  tmdsDDRModuleC.io.clock := io.clockD

  tmdsDDRModuleB.io.aresetn := io.aresetn
  tmdsDDRModuleG.io.aresetn := io.aresetn
  tmdsDDRModuleR.io.aresetn := io.aresetn
  tmdsDDRModuleC.io.aresetn := io.aresetn
}
