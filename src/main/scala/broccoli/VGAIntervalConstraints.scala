// See README.md for license details.

package broccoli

/** Parameters for a VGAIntervalDriver
  *
  * @param width Maximum rendered pixels, width-wise
  * @param height Maximum rendered pixels, height-wise
  * @param vFrontPorch No-render gap in pixels before horizontal blanking period
  * @param hFrontPorch No-render gap in pixels before vertical blanking period
  * @param vBlank Vertical blanking period in pixels
  * @param hBlank Horizontal blanking period in pixels
  * @param vBackPorch No-render gap in pixels after vertical blanking period
  * @param hBackPorch No-render gap in pixels after horizontal blanking period
  * @param vNegateSync True if the vblank signal goes through negation
  * @param hNegateSync True if the hblank signal goes through negation
  */
abstract class VGAIntervalConstraints {
  val width: Int;
  val height: Int;
  val vFrontPorch: Int;
  val hFrontPorch: Int;
  val vBlank: Int;
  val hBlank: Int;
  val vBackPorch: Int;
  val hBackPorch: Int;
  val vNegateSync: Boolean;
  val hNegateSync: Boolean;
}
