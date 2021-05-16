// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Root project to generate Verilog from
  */
class Broccoli extends Module {
  final val TEXWIDTH = 5
  
  val io = IO(new Bundle {
    val clockP = Input(Clock()) // 25.175MHz
    val clockD = Input(Clock()) // clockP x 5
    val clockT = Input(Clock()) // clockP x 10 (TODO: Clock for downscaler)
    val aresetn = Input(Bool())

    val tmdsLane0 = Output(Bool())
    val tmdsLane1 = Output(Bool())
    val tmdsLane2 = Output(Bool())
    val tmdsLaneC = Output(Bool())

    val debugAddress = Input(UInt(((TEXWIDTH * 2) + 2).W))
    val debugData = Input(UInt(8.W))
    val debugMapAD = Input(UInt(24.W))
    val debugMapBC = Input(UInt(24.W))

    val debugVBlank = Output(Bool())
    val debugHBlank = Output(Bool())
  })
  // VBlank/HBlank interval driver
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

  // Texture mapper
  val textureMap = Module(new TextureCell(TEXWIDTH, true))

  textureMap.clock := io.clockT
  textureMap.io.aresetn := io.aresetn
  textureMap.io.enable := true.B
  textureMap.io.currentX := vga.io.currentX(TEXWIDTH-1, 0)
  textureMap.io.currentY := vga.io.currentY(TEXWIDTH-1, 0)

  // TODO: Connect these
  textureMap.io.textureCoordsA := io.debugMapAD(23, 12).asSInt
  textureMap.io.textureCoordsB := io.debugMapBC(23, 12).asSInt
  textureMap.io.textureCoordsC := io.debugMapBC(11, 0).asSInt
  textureMap.io.textureCoordsD := io.debugMapAD(11, 0).asSInt
  textureMap.io.strobe := false.B
  textureMap.io.data := io.debugData
  textureMap.io.address := io.debugAddress((TEXWIDTH * 2) - 1, 0)
  textureMap.io.writeTexels := io.debugAddress((TEXWIDTH * 2) + 0)
  textureMap.io.writeMatrix := io.debugAddress((TEXWIDTH * 2) + 1)

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

  tmdsLaneModuleB.io.d := Cat(textureMap.io.textureResult(5, 4), "b111111".U(6.W))
  tmdsLaneModuleG.io.d := Cat(textureMap.io.textureResult(3, 2), "b111111".U(6.W))
  tmdsLaneModuleR.io.d := Cat(textureMap.io.textureResult(1, 0), "b111111".U(6.W))

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
