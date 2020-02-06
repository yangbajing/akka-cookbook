/**
 * Ammonite script
 * 批量将目录下所有 .rm 视频文件转码为 .mp4 视频文件
 * Batch transcode all .rm video files in the directory to .mp4 video files.
 */
import $ivy.`com.lihaoyi::os-lib:0.6.3`

for (path <- os.walk(os.pwd, _.ext != "rm", maxDepth = 1)) {
  os.proc("ffmpeg", "-i", path.last, path.baseName + ".mp4").call()
}
