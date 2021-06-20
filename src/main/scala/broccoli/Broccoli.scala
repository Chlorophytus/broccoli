// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Root project to generate Verilog from
  */
class Broccoli extends Module {
  final val TEXWIDTH = 5

  // Don't touch these unless you have an idea what you're doing.
  final val FRAMEBUFFER_WIDTH = 640
  final val FRAMEBUFFER_HEIGHT = 320
  final val FRAMEBUFFER_DOWNSCALE = 1

  val io = IO(new Bundle {
    val clockP = Input(Clock()) // Main pixel clock
    val clockF = Input(Clock()) // clockP x 5 (0°)
    val clockD = Input(Clock()) // clockP x 5 (-90°)
    val aresetn = Input(Bool())

    val tmdsLane0 = Output(Bool())
    val tmdsLane1 = Output(Bool())
    val tmdsLane2 = Output(Bool())
    val tmdsLaneC = Output(Bool())

    val address = Input(UInt(32.W))
    val dataI = Input(UInt(32.W))
    val dataO = Output(UInt(32.W))

    val debugVBlank = Output(Bool())
    val debugHBlank = Output(Bool())
  })

  withReset(~io.aresetn) {
    val vga = Module(new VGAIntervalDriver(new VGAIntervalConstraints {
      val width = 1280
      val hFrontPorch = 1280 + 110
      val hBlank = 1280 + 110 + 40
      val hBackPorch = 1280 + 110 + 40 + 220
      val hNegateSync = false

      val height = 720
      val vFrontPorch = 720 + 5
      val vBlank = 720 + 5 + 5
      val vBackPorch = 720 + 5 + 5 + 20
      val vNegateSync = false
    }))
    val framebuffer0 = Module(new Framebuffer(FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, true))
    val framebuffer1 = Module(new Framebuffer(FRAMEBUFFER_WIDTH, FRAMEBUFFER_HEIGHT, true))
    val downscaler = Module(new Downscaler(FRAMEBUFFER_DOWNSCALE))
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
    ) % FRAMEBUFFER_HEIGHT.U(framebuffer0.TOTALADDR.W)) * FRAMEBUFFER_WIDTH.U(framebuffer0.TOTALADDR.W)) +
      ((Cat(
        0.U((framebuffer0.TOTALADDR - 12).W),
        downscaler.io.downscaledX
      ) % FRAMEBUFFER_WIDTH.U(framebuffer0.TOTALADDR.W)))

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
    textureMap.io.strobe := false.B

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
