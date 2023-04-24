package broccoli

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import com.typesafe.scalalogging._

class TMDSEncoderSpec
    extends AnyFreeSpec
    with ChiselScalatestTester
    with LazyLogging {
  "TMDSEncoder should output proper control codes" in {
    test(new TMDSEncoder) { dut =>
      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
      dut.io.enable.poke(true.B)

      dut.io.tmdsIDataEnable.poke(false.B)
      dut.io.tmdsIControl.poke(0.U(2.W))
      dut.clock.step(1)
      dut.io.tmdsOData.expect("b1101010100".U(10.W))
      logger.info(
        f"Control 2'b00 => 0x${dut.io.tmdsOData.peek().litValue}%03x (should be 0x354)"
      )

      dut.io.tmdsIControl.poke(1.U(2.W))
      dut.clock.step(1)
      dut.io.tmdsOData.expect("b0010101011".U(10.W))
      logger.info(
        f"Control 2'b01 => 0x${dut.io.tmdsOData.peek().litValue}%03x (should be 0x0ab)"
      )

      dut.io.tmdsIControl.poke(2.U(2.W))
      dut.clock.step(1)
      dut.io.tmdsOData.expect("b0101010100".U(10.W))
      logger.info(
        f"Control 2'b10 => 0x${dut.io.tmdsOData.peek().litValue}%03x (should be 0x154)"
      )

      dut.io.tmdsIControl.poke(3.U(2.W))
      dut.clock.step(1)
      dut.io.tmdsOData.expect("b1010101011".U(10.W))
      logger.info(
        f"Control 2'b11 => 0x${dut.io.tmdsOData.peek().litValue}%03x (should be 0x2ab)"
      )
    }
  }

  "TMDSEncoder should have a stable disparity" in {
    test(new TMDSEncoder) { dut =>
      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
      dut.io.enable.poke(true.B)

      val maxUntil = 256
      var average = 0.0
      var min = 0
      var max = 0
      dut.io.tmdsIDataEnable.poke(true.B)

      for (i <- (0 until maxUntil)) {
        dut.io.tmdsIData.poke(scala.util.Random.nextInt(256))
        dut.clock.step(1)
        val result = dut.io.tmdsDDisparity.peek().litValue
        if (result >= max) {
          max = result.toInt
          logger.info(f"$i maximum disparity hit ($max)")
        }
        if (result <= min) {
          min = result.toInt
          logger.info(f"$i minimum disparity hit ($min)")
        }
        average += result.toDouble
      }

      average /= maxUntil
      logger.info(f"The average disparity is ${average}")
      logger.info(f"The minimum disparity is ${min}")
      logger.info(f"The maximum disparity is ${max}")
    }
  }
}
