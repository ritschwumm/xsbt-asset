import xsbtUtil.types._

package object xsbtAsset {
	type AssetProcessor	= Seq[PathMapping]=>Seq[PathMapping]
}
