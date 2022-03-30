// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface
import scala.util.Random

class TMDSLanePumpSpec extends AnyFreeSpec with ChiselScalatestTester {
  "should pump properly" in {
    test(new TMDSLanePump) { dut =>
      {
        // reset here
        dut.io.aresetn.poke(false.B)
        dut.io.aresetn.poke(true.B)

        dut.io.wen.poke(true.B)
        dut.io.d.poke("b1010101111".U(10.W))
        dut.clock.step(1)
        dut.io.wen.poke(false.B)
        dut.clock.step(1)
        dut.io.qq.expect("b11".U)
        dut.clock.step(1)
        dut.io.qq.expect("b11".U)
        dut.clock.step(1)
        dut.io.qq.expect("b10".U)
        dut.clock.step(1)
        dut.io.qq.expect("b10".U)
        dut.clock.step(1)
        dut.io.qq.expect("b10".U)
        dut.clock.step(1)
        dut.io.qq.expect("b11".U)
      }
    }
  }
}
