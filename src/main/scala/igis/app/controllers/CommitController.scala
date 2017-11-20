package igis.app.controllers

import binding.diff.JsDiff
import binding.git.GitCommit
import igis.mvc.{Controller, Node, Request, Response}
import igis.util.GitDiff

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CommitController extends Controller {
  def commitInfo(path: String, node: Node): Future[GitCommit] = {
    node.ipfs.dag.get(path.split("/").head).map(n => n.value.asInstanceOf[GitCommit])
  }

  def diff(head: GitCommit, node: Node): Unit = {
    val parentTree = s"${head.parents(0).cid().toBaseEncodedString()}/tree"
    val headTree = head.tree.cid().toBaseEncodedString()

    new GitDiff(node).compareTrees(parentTree, headTree).foreach(_.foreach {
      case GitDiff.FileAddition(name, file) =>
        println(s"Add $name")
      case GitDiff.FileDeletion(name, file) =>
        println(s"Del $name")
      case GitDiff.FileChange(name, base, head) =>
        println(s"Chg $name")
        BlobController.rawBlob(base.hash.cid().toBaseEncodedString(), node).zip(BlobController.rawBlob(head.hash.cid().toBaseEncodedString(), node)).foreach{ case (b, h) =>
          println(JsDiff.createPatch(name, b, h, "", ""))
        }
    })
  }

  def apply(req: Request): Future[Response] = {
    commitInfo(req.remPath, req.node).map{head =>
      if(head.parents.length != 0) {
        diff(head, req.node)
      }

      Response.withData(html.commit().toString())
    }
  }
}
