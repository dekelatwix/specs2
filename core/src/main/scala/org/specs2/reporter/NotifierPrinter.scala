package org.specs2
package reporter

import control._
import text.NotNullStrings._
import execute.Result
import data.Fold
import scalaz.concurrent.Task
import scalaz.stream._
import scalaz.syntax.show._
import specification.core._

/**
 * A Printer can be created from a Notifier implementation
 */
object NotifierPrinter {

  /**
   * create a printer from a notifier
   */
  def printer(notifier: Notifier) = new Printer {
    def prepare(env: Env, specifications: List[SpecStructure]): Action[Unit]  = Actions.unit
    def finalize(env: Env, specifications: List[SpecStructure]): Action[Unit] = Actions.unit

    def fold(env: Env, spec: SpecStructure) = new Fold[Fragment] {
      val args = env.arguments
      type S = Notified

      def prepare = Task.now(())

      def fold = (f: Fragment, notified: Notified) => {
        // if the previous notified was defining the closing of a block
        // the new notify must not define a close action
        val n = if (notified.close) notified.copy(close = false)
                else notified

        f match {
          // a block start. The next text is the "context" name
          case Fragment(Start,_,_) =>
            n.copy(start = true, close = false, hide = true)

          // a block start. The "context" name is the current block name
          case Fragment(End,_ ,_) =>
            n.copy(start = false, close = true, hide = true)

          case f1 if Fragment.isText(f1) =>
            // if a block has just been opened the first text defines the block context
            if (notified.start) n.copy(context = f1.description.shows, start = true, hide = false)
            else                n.copy(context = f1.description.shows, start = false, hide = false)

          case f1 if Fragment.isExample(f1) =>
            n.copy(start = false, hide = false)

          case f1 if Fragment.isStep(f1) =>
            n.copy(start = false, hide = false)

          case _ => n.copy(hide = true)
        }
      }

      lazy val init = Notified(context = "start", start = false, close = false, hide = true)

      def last(s: Notified): Task[Unit] = Task.now(())

      lazy val sink: Sink[Task, (Fragment, Notified)] =
        io.resource(Task.delay { notifier.specStart(spec.name, ""); notifier})(
          (n: Notifier) => Task.delay(n.specEnd(spec.name, "")))(
            (n: Notifier) => Task.delay {
              case (f: Fragment, block: Notified) =>
                Task.now(printFragment(n, f, block))
            })

      def printFragment(n: Notifier, f: Fragment, notified: Notified) = {
        val description = f.description.shows.trim

        val location = f.location.fullLocation(args.traceFilter).getOrElse("no location")
        def duration(f: Fragment) = f.execution.executionTime.totalMillis

        if (!notified.hide) {
          if (notified.start) n.contextStart(notified.context.trim, location)
          else {
            if (Fragment.isExample(f)) {
              n.exampleStarted(description, location)

              def notifyResult(result: Result): Unit =
                result match {
                  case r: execute.Success =>
                    n.exampleSuccess(description, duration(f))

                  case r: execute.Failure =>
                    n.exampleFailure(description, r.message, location, r.exception, r.details, duration(f))

                  case r: execute.Error =>
                    n.exampleError(description, r.message, location, r.exception, duration(f))

                  case r: execute.Skipped =>
                    n.exampleSkipped(description, r.message, location, duration(f))

                  case r: execute.Pending =>
                    n.examplePending(description, r.message, location, duration(f))

                  case execute.DecoratedResult(_, r2) =>
                    notifyResult(r2)
                }

              notifyResult(f.executionResult)
            } else if (Fragment.isStep(f)) {
              try {
                n.stepStarted(location)

                def notifyResult(result: Result): Unit =
                  result match {
                    case r: execute.Success => n.stepSuccess(duration(f))
                    case r: execute.Error   => n.stepError(r.message, location, r.exception, duration(f))
                    case _ => ()
                  }
                notifyResult(f.executionResult)
              // catch AbstractMethod errors coming from Intellij since adding
              // calling new "step" methods on the Notifier interface is not supported yet
              } catch {
                case e: AbstractMethodError if e.getMessage.notNull.contains("JavaSpecs2Notifier") => ()
                case other: Throwable => throw other
              }

            } else if (Fragment.isText(f)) n.text(description, location)
          }
        } else if (notified.close) n.contextEnd(notified.context.trim, location)
      }
    }

    case class Notified(context: String = "", start: Boolean = false, close: Boolean = false, hide: Boolean = false)
  }
}


