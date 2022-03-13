// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface
import scala.util.Random

class TMDSLaneSpec extends AnyFreeSpec with ChiselScalatestTester {
  "should roll a reliable Cnt(t-1) average" in {
    test(new TMDSLane) { dut =>
      {
        // reset here
        dut.io.aresetn.poke(false.B)
        dut.io.aresetn.poke(true.B)
        dut.io.enable.poke(true.B)
        dut.io.de.poke(true.B)
        dut.io.d.poke(0.U)
        dut.io.c.poke(0.U)
        val integral = Seq.fill(20){
          dut.io.d.poke((Random.nextBytes(1)(0) + 128).U(8.W))
          dut.clock.step(5)
          val step = dut.io.debugCnt.peek().litValue
          println(f"Step $step")
          step
        }
        val avg = integral.sum.toDouble / integral.size
        println(f"With average $avg")
        assert(Math.abs(avg) < 2.0)
      }
    }
  }
}
