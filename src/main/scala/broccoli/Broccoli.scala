// See README.md for license details.

package broccoli

import chisel3._

/** Root project to generate Verilog from
  */
class Broccoli extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val hSync = Output(Bool())
    val vSync = Output(Bool())

    val red = Output(UInt(4.W))
    val grn = Output(UInt(4.W))
    val blu = Output(UInt(4.W))
  })
  // I've yet to find a monitor that supports CVT
  // gtf 1024 768 60
  // Modeline "1024x768_60.00"  64.11  1024 1080 1184 1344  768 769 772 795  -HSync +Vsync
  val vga = Module(new VGAIntervalDriver(new VGAIntervalConstraints {
    val height = 1024
    val hFrontPorch = 1080
    val hBlank = 1184
    val hBackPorch = 1344
    val hNegateSync = true

    val width = 768
    val vFrontPorch = 769
    val vBlank = 772
    val vBackPorch = 795
    val vNegateSync = false
  }))

  vga.io.aresetn := io.aresetn
  io.hSync := vga.io.hSync
  io.vSync := vga.io.vSync
  io.red := (vga.io.currentX ^ vga.io.currentY)(3, 0)
  io.grn := (vga.io.currentX ^ vga.io.currentY)(4, 1)
  io.blu := (vga.io.currentX ^ vga.io.currentY)(5, 2)
}
