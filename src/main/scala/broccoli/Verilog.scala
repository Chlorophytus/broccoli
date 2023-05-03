package broccoli

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.ChiselStage

import com.typesafe.scalalogging._

object Verilog extends App with LazyLogging {
  logger.info("Generating Verilog")

  (new ChiselStage).execute(
    Array("--target", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new Broccoli))
  )
}
