package poju4s

import poju4s.{ result => r }
import org.junit.runner._
import org.junit.runner.notification._
import scala.collection.JavaConversions._
import java.util.concurrent.Callable
import exception._

object MonadicRunner {
  val TestAndClass = """([^(]+)\(([^)]+)\)""".r
  implicit def requestToOption(r: Request) = Some(r)
  implicit def runnerToOption(r: Runner) = Some(r)
  implicit def decriptionToOption(r: Description) = Some(r)
  def resolveAssumption(af: Failure) = {
    val TestAndClass(t, g) = af.getDescription.getDisplayName
    af.getException match {
      case p: PendingException => r.Pending(g, Symbol(t))
      case e => r.Ignored(g, Symbol(t))
    }
  }
  def resolveFailure(f: Failure) = {
    val TestAndClass(t, g) = f.getDescription.getDisplayName
    f.getException match {
      case f: FixedButPendingException => r.Fixed(g, Symbol(t))
      case f: AssertionError => r.Failure(g, Symbol(t), f)
      case e => r.Error(g, Symbol(t), e)
    }
  }
}

trait Interaction {
  import MonadicRunner._

  def select(specsToRun: Symbol*): List[() => r.Result] = {
    var rs = List[() => r.Result]()
    for (
      (g, t) <- specDescriptors(specsToRun.toList);
      thldr <- Some(Thread.currentThread.getContextClassLoader);
      clz <- Some(thldr.loadClass(g.getClassName()));
      req <- Request.aClass(clz);
      filtered <- req.filterWith(Description.createTestDescription(clz, t.name))
    ) {
      val callable: () => r.Result = () => {
          var res: r.Result = null
          val juc = new JUnitCore
          juc.addListener(new RunListener() {
            var failed: Option[Failure] = None
            var aFailed: Option[Failure] = None
            override def testAssumptionFailure(af: Failure) = { aFailed = Some(af) }
            override def testIgnored(d: Description) = { res = r.Ignored(g.getClassName, t) }
            override def testFailure(f: Failure) = { failed = Some(f) }
            override def testFinished(d: Description) = {
              res = failed.map(resolveFailure) orElse aFailed.map(resolveAssumption) getOrElse (r.Success(g.getClassName, t))
            }
          })
          juc.run(filtered)
          res
      }
      rs = callable :: rs
    }
    rs.reverse
  }

  private def specDescriptors(specs: List[Symbol]): List[(Description, Symbol)] = specs match {
    case Nil => list
    case _ => for((g, t) <- list if specs.contains(t)) yield (g, t)
  }

  def list: List[(Description, Symbol)] = listForClass(getClass)

  def listForClass(clazz: Class[_]): List[(Description, Symbol)] = for (
    req <- Request.aClass(clazz).toList;
    run <- req.getRunner.toList;
    root <- run.getDescription.toList;
    chd <- root.getChildren.toList;
    TestAndClass(t, c) <- Some(chd.getDisplayName) if t != "initializationError"
  ) yield { (chd, Symbol(chd.getMethodName)) }
}

