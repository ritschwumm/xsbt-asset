package xsbtAsset

import sbt._
import Keys.TaskStreams

import xsbtUtil.types._
import xsbtUtil.{ util => xu }

object Import {
	type AssetProcessor		= xsbtAsset.AssetProcessor
	val AssetProcessor		= xsbtAsset.AssetProcessors
	
	val assetProcess		= taskKey[Seq[PathMapping]]("process staged assets")
	val assetPipeline		= settingKey[Seq[TaskKey[AssetProcessor]]]("pipeline applied to staged assets")
	val assetProcessors		= taskKey[Seq[AssetProcessor]]("processors applied to staged assets")
	
	val assetStage			= taskKey[Seq[PathMapping]]("stage assets for processing")
	
	val assetSourceDir		= settingKey[File]("directory with assets")
	val assetSourceFiles	= taskKey[Traversable[PathMapping]]("asset files")
	
	val assetExplode		= taskKey[Seq[PathMapping]]("explode libraries for processing")
	val assetDependencies	= taskKey[Seq[File]]("additional content libraries as dependencies")
	val assetExplodeDir		= settingKey[File]("a place to unpack dependencies")
}

object AssetPlugin extends AutoPlugin {
	val assetConfig		= config("asset").hide
	
	//------------------------------------------------------------------------------
	//## exports
	
	lazy val autoImport	= Import
	import autoImport._
	
	override val requires:Plugins		= plugins.JvmPlugin
	
	override val trigger:PluginTrigger	= noTrigger
	
	override lazy val projectConfigurations:Seq[Configuration]	=
			Vector(
				assetConfig
			)
	
	override lazy val projectSettings:Seq[Def.Setting[_]]	=
			Vector(
				assetProcess		:= (assetProcessors.value foldLeft assetStage.value) { (inputs, processor) => processor(inputs) },
				assetPipeline		:= Vector.empty,
				assetProcessors		<<= xu.task chain assetPipeline,
				
				assetStage			:= assetExplode.value ++ assetSourceFiles.value.toVector,
				
				assetSourceDir		:= (Keys.sourceDirectory in Compile).value / "asset",
				assetSourceFiles	:= xu.find allMapped assetSourceDir.value,
				
				assetExplode		:=
						explodeTask(
							streams			= Keys.streams.value,
							dependencies	= assetDependencies.value,
							explodeDir		= assetExplodeDir.value
						),
				assetDependencies	:= Keys.update.value select configurationFilter(name = assetConfig.name),
				assetExplodeDir		:= Keys.target.value / "asset",
			
				// Keys.ivyConfigurations	+= assetConfig,
				Keys.watchSources	:= Keys.watchSources.value ++ (assetSourceFiles.value map xu.pathMapping.getFile)//,
			)

	//------------------------------------------------------------------------------
	//## tasks
	
	/** explode dependencies */
	private def explodeTask(
		streams:TaskStreams,
		dependencies:Traversable[File],
		explodeDir:File
	):Seq[PathMapping]	= {
		streams.log info s"extracting ${dependencies.size} asset libraries to ${explodeDir}"
		val exploded	=
				dependencies.toVector flatMap { dependency	=>
					val out	= explodeDir / dependency.getName
					IO unzip (dependency, out, -xu.filter.JarManifestFilter)
					xu.find allMapped out
				}
				
		streams.log info s"cleaning up ${explodeDir}"
		val explodedFiles	= (exploded map { xu.pathMapping.getFile }).toSet
		xu.file cleanupDir (explodeDir, explodedFiles)
		exploded
	}
}