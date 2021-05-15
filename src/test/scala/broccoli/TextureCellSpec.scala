// See README.md for license details.

package broccoli

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class TextureCellSpec extends FreeSpec with ChiselScalatestTester {
  final val TEXWIDTH = 6
  
  def hexadecimalDump(buf: Array[BigInt]) {
    for(cntY <- 0 to buf.length / (1 << TEXWIDTH) - 1) {
      print(f"${(cntY * (1 << TEXWIDTH))}%08X |")
      for(cntX <- 0 to (1 << TEXWIDTH) - 1) {
        if((cntY * (1 << TEXWIDTH)) + cntX < buf.length) {
          print(f" ${buf(cntY * (1 << TEXWIDTH) + cntX)}%02x")
        }
      }
      println()
    }
  }

  def checkerboard(position: BigInt): BigInt = {
    // Hit X position bitlevel
    if((position & (1 << TEXWIDTH)) != 0) { 
      // Hit Y position next
      if ((position & (1 << ((TEXWIDTH * 2) + 1))) != 0) 0xFF else 0x00 
    } else { 
      if ((position & (1 << ((TEXWIDTH * 2) + 1))) != 0) 0x00 else 0xFF
      }
  }

  "TextureCell should display properly" in {
    test(new TextureCell(TEXWIDTH)) { dut =>
      // Knock out timeouts w/seemingly undocumented function call
      dut.clock.setTimeout(0)
      val data = new Array[BigInt](1 << (TEXWIDTH * 2))
      val goat = new Array[BigInt](1 << (TEXWIDTH * 2))

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
        dut.io.address.poke(cnt.U((TEXWIDTH*2).W))
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
          dut.io.currentX.poke((cnt % (1 << TEXWIDTH)).U(12.W))
          dut.io.currentY.poke((cnt / (1 << TEXWIDTH)).U(12.W))
          dut.io.strobe.poke(true.B)
          dut.clock.step(1)
          dut.io.strobe.poke(false.B)
          dut.clock.step(10)
          dut.io.ready.expect(true.B)
          data(cnt) = dut.io.textureResult.peek().litValue

          val goatSieve = ((1 << TEXWIDTH) - 1)
          val goatY = (dut.io.currentY.peek().litValue & goatSieve) <<
                            (TEXWIDTH + 1)
          val goatX = (dut.io.currentX.peek().litValue & goatSieve) << 0
          goat(cnt) = checkerboard((goatY | goatX) << 3)
      }

      hexadecimalDump(data)

      println("Above hardware output should be identical to...")

      hexadecimalDump(goat)
    }
  }
}
