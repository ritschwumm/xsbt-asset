package xsbtAsset

import sbt._

import xsbtUtil.types._
import xsbtUtil.{ util => xu }

// TODO should be AssetProcessor, but that leads to an sbt bug (?)
// where compilation fails for an access to the autoImported
// val AssetProcessor's dirless field at first, and then works
// after retrying
object AssetProcessors {
	def selective(filter:FileFilter)(delegate:AssetProcessor):AssetProcessor	=
			input => {
				val (accept, reject)	= input partition (xu.pathMapping.getFile andThen filter.accept)
				delegate(accept) ++ reject
			}
			
	def filtering(filter:FileFilter):AssetProcessor	=
			_ filter (xu.pathMapping.getFile andThen filter.accept)
		
	val dirless:AssetProcessor	=
			filtering(-DirectoryFilter)
		
	val empty:AssetProcessor	=
			identity
}
