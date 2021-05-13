// See README.md for license details.

package broccoli

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class TextureCellSpec extends FreeSpec with ChiselScalatestTester {
  final val WIDTH = 64
  
  def hexadecimalDump(buf: Array[BigInt]) {
    for(cntY <- 0 to buf.length / WIDTH - 1) {
      print(f"${(cntY * WIDTH)}%08X |")
      for(cntX <- 0 to WIDTH - 1) {
        if((cntY * WIDTH) + cntX < buf.length) {
          print(f" ${buf(cntY * WIDTH + cntX)}%02x")
        }
      }
      println()
    }
  }

  def checkerboard(position: BigInt): BigInt = {
    // Hit X position bitlevel
    if((position & 0x40) != 0) { 
      // Hit Y position next
      if ((position & 0x2000) != 0) 0xFF else 0x00 
    } else { 
      if ((position & 0x2000) != 0) 0x00 else 0xFF
      }
  }

  "TextureCell should display properly" in {
    test(new TextureCell) { dut =>
      // Knock out timeouts w/seemingly undocumented function call
      dut.clock.setTimeout(0)
      val data = new Array[BigInt](4096)
      val goat = new Array[BigInt](4096)

      // reset here
      dut.io.aresetn.poke(false.B)
      dut.clock.step(10)
      dut.io.aresetn.poke(true.B)
      dut.clock.step(10)
      dut.io.enable.poke(true.B)
      dut.io.ready.expect(true.B)
      dut.io.writeTexels.poke(true.B)
      // now write sprite data
      for(cnt <- 0 to data.length - 1) {
        dut.io.address.poke(cnt.U(12.W))
        // NOTE: This is exhibiting some strange behavior. FIXME and ensure
        // the RTL will display properly...
        dut.io.data.poke(checkerboard(cnt << 3).U(8.W))
        dut.io.strobe.poke(true.B)
        dut.clock.step(1)
        dut.io.strobe.poke(false.B)
        dut.clock.step(10)
        dut.io.ready.expect(true.B)
      }
      dut.io.writeTexels.poke(false.B)
      dut.clock.step(10)
      for(cnt <- 0 to data.length - 1) {
          dut.io.currentX.poke((cnt % 64).U(12.W))
          dut.io.currentY.poke((cnt / 64).U(12.W))
          dut.io.strobe.poke(true.B)
          dut.clock.step(1)
          dut.io.strobe.poke(false.B)
          dut.clock.step(10)
          dut.io.ready.expect(true.B)
          data(cnt) = dut.io.textureResult.peek().litValue
          goat(cnt) = checkerboard((((dut.io.currentY.peek().litValue & 0x3F) << 7) | ((dut.io.currentX.peek().litValue & 0x3F) << 0)) << 3)
      }

      hexadecimalDump(data)

      println("Above hardware output should be identical to...")

      hexadecimalDump(goat)
    }
  }
}
