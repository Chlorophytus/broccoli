package broccoli

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.ChiselStage
import com.typesafe.scalalogging._
import java.io.File

object Verilog extends App with LazyLogging {
  // Generate a directory for SystemVerilog files
  val generated_dir = new File("generated")
  if (!generated_dir.exists()) {
    logger.info("Creating `generated` directory for SystemVerilog files")
    generated_dir.mkdir()
  } else {
    logger.info("The `generated` directory for SystemVerilog files is present")
    logger.info("To regenerate it, delete the directory's contents")
  }

  // Generate the SystemVerilog into this directory
  logger.info(
    f"Generating SystemVerilog into ${generated_dir.getAbsolutePath()}"
  )
  val arguments = Array(
    "--target",
    "verilog",
    "--split-verilog",
    "--target-dir",
    generated_dir.toString()
  )
  val annotations = Seq(
    ChiselGeneratorAnnotation(() => new Broccoli)
  )
  (new ChiselStage).execute(arguments, annotations)
}
