// See README.md for license details.

package broccoli

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class TextureCellSpec extends FreeSpec with ChiselScalatestTester {
  final val width = 32
  
  def hexadecimalDump(buf: Array[BigInt]) {
    for(cntY <- 0 to buf.length / width - 1) {
      print(f"${(cntY * width)}%08X |")
      for(cntX <- 0 to width - 1) {
        if((cntY * width) + cntX < buf.length) {
          print(f" ${buf(cntY * width + cntX)}%02x")
        }
      }
      println()
    }
  }

  "TextureCell should rotate properly" in {
    test(new TextureCell) { dut =>
      // Knock out timeouts w/seemingly undocumented function call
      dut.clock.setTimeout(0)
      val data = new Array[BigInt](4096)

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
        dut.io.data.poke((cnt % 0x100).U(8.W))
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
      }

      hexadecimalDump(data)
      println(f"This test is a work in progress.")

    }
  }
}
