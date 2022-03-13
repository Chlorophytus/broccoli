// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** root project to generate Verilog from
  */
class Broccoli extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val clockP = Input(Clock()) // Main pixel clock
    val clockF = Input(Clock()) // clockP x 5 (0degs phase)
    val clockD = Input(Clock()) // clockP x 5 (-90degs phase)
  })
  
  withReset(~io.aresetn) {
    
  }
}
