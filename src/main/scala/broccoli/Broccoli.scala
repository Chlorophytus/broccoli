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
    val clockF = Input(Clock()) // clockP x 5 (0°)
    val clockD = Input(Clock()) // clockP x 5 (-90°)
    val aresetn = Input(Bool())

    val tmdsLane0 = Output(Bool())
    val tmdsLane1 = Output(Bool())
    val tmdsLane2 = Output(Bool())
    val tmdsLaneC = Output(Bool())

    val debugAddress = Input(UInt(((TEXWIDTH * 2) + 2).W))
    val debugData = Input(UInt(8.W))
    val debugMapAD = Input(UInt(24.W))
    val debugMapBC = Input(UInt(24.W))
    val debugMapXY = Input(UInt(24.W))
    val debugReadCoordX = Output(UInt(12.W))
    val debugReadCoordY = Output(UInt(12.W))

    val debugVBlank = Output(Bool())
    val debugHBlank = Output(Bool())
  })

  withReset(~io.aresetn) {
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
    val framebuffer0 = Module(new Framebuffer(320, 240, true))
    val framebuffer1 = Module(new Framebuffer(320, 240, true))
    val downscaler = Module(new Downscaler(1))
    val textureMap = Module(new TextureCell(TEXWIDTH, true))
    val tmdsLaneModuleB = Module(new TMDSLane())
    val tmdsLaneModuleG = Module(new TMDSLane())
    val tmdsLaneModuleR = Module(new TMDSLane())
    val tmdsLaneModuleC = Module(new TMDSClock())
    val tmdsDDRModuleB = Module(new TMDSDDR())
    val tmdsDDRModuleG = Module(new TMDSDDR())
    val tmdsDDRModuleR = Module(new TMDSDDR())
    val tmdsDDRModuleC = Module(new TMDSDDR())
    // VBlank/HBlank interval driver
    vga.clock := io.clockP
    vga.io.aresetn := io.aresetn

    // Framebuffer and Downscaler
    val stencil = Reg(Bool())
    val pixel = Reg(UInt(9.W))
    val calculatedOffset = Reg(UInt(framebuffer0.TOTALADDR.W))

    calculatedOffset := ((Cat(
      0.U((framebuffer0.TOTALADDR - 12).W),
      downscaler.io.downscaledY
    ) % 240.U(framebuffer0.TOTALADDR.W)) * 320.U(framebuffer0.TOTALADDR.W)) +
      ((Cat(
        0.U((framebuffer0.TOTALADDR - 12).W),
        downscaler.io.downscaledX
      ) % 320.U(framebuffer0.TOTALADDR.W)))

    pixel := Mux(
      textureMap.io.stencilTest,
      "b111111111".U,
      Cat(
        textureMap.io.textureResult(5, 4),
        textureMap.io.textureResult(4),
        textureMap.io.textureResult(3, 2),
        textureMap.io.textureResult(2),
        textureMap.io.textureResult(1, 0),
        textureMap.io.textureResult(0)
      )
    )

    downscaler.io.aresetn := io.aresetn
    framebuffer0.io.aresetn := io.aresetn
    framebuffer1.io.aresetn := io.aresetn
    downscaler.clock := io.clockP
    framebuffer0.clock := io.clockF
    framebuffer1.clock := io.clockF

    downscaler.io.currentX := vga.io.currentX
    downscaler.io.currentY := vga.io.currentY
    io.debugReadCoordX := downscaler.io.downscaledX
    io.debugReadCoordY := downscaler.io.downscaledY

    framebuffer0.io.strobe := false.B
    framebuffer0.io.address := calculatedOffset
    framebuffer0.io.enable := true.B
    framebuffer0.io.write := ~vga.io.currFramebuffer
    framebuffer0.io.data := pixel

    framebuffer1.io.strobe := false.B
    framebuffer1.io.address := calculatedOffset
    framebuffer1.io.enable := true.B
    framebuffer1.io.write := vga.io.currFramebuffer
    framebuffer1.io.data := pixel

    // Texture mapper
    textureMap.clock := io.clockF
    textureMap.io.aresetn := io.aresetn
    textureMap.io.enable := true.B
    textureMap.io.currentX := downscaler.io.downscaledX
    textureMap.io.currentY := downscaler.io.downscaledY

    // TODO: Connect these
    textureMap.io.textureCoordsA := io.debugMapAD(23, 12).asSInt
    textureMap.io.textureCoordsB := io.debugMapBC(23, 12).asSInt
    textureMap.io.textureCoordsC := io.debugMapBC(11, 0).asSInt
    textureMap.io.textureCoordsD := io.debugMapAD(11, 0).asSInt

    textureMap.io.textureCoordsX := io.debugMapXY(23, 12).asSInt
    textureMap.io.textureCoordsY := io.debugMapXY(11, 0).asSInt

    textureMap.io.strobe := false.B
    textureMap.io.data := io.debugData
    textureMap.io.address := io.debugAddress((TEXWIDTH * 2) - 1, 0)
    textureMap.io.writeTexels := io.debugAddress((TEXWIDTH * 2) + 0)
    textureMap.io.writeMatrix := io.debugAddress((TEXWIDTH * 2) + 1)

    // TMDS Lanes
    val resultPixel = Reg(UInt(9.W))
    resultPixel := Mux(
      vga.io.currFramebuffer,
      framebuffer0.io.result,
      framebuffer1.io.result
    )

    tmdsLaneModuleB.io.aresetn := io.aresetn
    tmdsLaneModuleG.io.aresetn := io.aresetn
    tmdsLaneModuleR.io.aresetn := io.aresetn
    tmdsLaneModuleC.io.aresetn := io.aresetn

    io.debugHBlank := vga.io.hBlank
    io.debugVBlank := vga.io.vBlank

    tmdsLaneModuleB.io.de := (~vga.io.hBlank) & (~vga.io.vBlank)
    tmdsLaneModuleG.io.de := (~vga.io.hBlank) & (~vga.io.vBlank)
    tmdsLaneModuleR.io.de := (~vga.io.hBlank) & (~vga.io.vBlank)

    tmdsLaneModuleB.io.d := Cat(
      resultPixel(8, 6),
      "b11111".U(5.W)
    )
    tmdsLaneModuleG.io.d := Cat(
      resultPixel(5, 3),
      "b11111".U(5.W)
    )
    tmdsLaneModuleR.io.d := Cat(
      resultPixel(2, 0),
      "b11111".U(5.W)
    )

    tmdsLaneModuleB.io.c := Cat(vga.io.vSync, vga.io.hSync)
    tmdsLaneModuleG.io.c := 0.U(2.W)
    tmdsLaneModuleR.io.c := 0.U(2.W)

    // DDR
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
}
